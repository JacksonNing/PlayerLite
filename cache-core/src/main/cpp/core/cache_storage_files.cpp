#include "cache_runtime.h"

#include <fstream>
#include <sstream>

namespace cachecore {

bool CacheRuntime::EnsureStorageFilesLocked(
        const std::string& resource_key,
        int32_t block_size_bytes,
        int64_t content_length_hint,
        int64_t duration_ms_hint,
        SessionStorageFiles* out_storage) {
    if (out_storage == nullptr || resource_key.empty() || block_size_bytes <= 0 || !EnsureRootDirLocked()) {
        return false;
    }

    const auto data_file = cache_root_path_ / (resource_key + ".data");
    const auto config_file = cache_root_path_ / (resource_key + "_config.json");
    const auto extra_file = cache_root_path_ / (resource_key + "_extra.json");

    {
        std::error_code ec;
        if (!std::filesystem::exists(data_file, ec)) {
            std::ofstream create_data(data_file, std::ios::binary);
            create_data.close();
            if (!create_data.good() && !std::filesystem::exists(data_file)) {
                return false;
            }
        }
    }

    if (!std::filesystem::exists(config_file)) {
        const auto temp_config_file = std::filesystem::path(config_file.string() + ".init.tmp");
        std::ofstream config_output(temp_config_file, std::ios::binary | std::ios::trunc);
        config_output << BuildConfigJson(
                resource_key,
                block_size_bytes,
                content_length_hint,
                duration_ms_hint,
                {},
                {},
                NowEpochMs());
        config_output.close();
        if (!config_output.good()) {
            return false;
        }
        std::error_code rename_error;
        std::filesystem::rename(temp_config_file, config_file, rename_error);
        if (rename_error) {
            std::error_code remove_error;
            std::filesystem::remove(temp_config_file, remove_error);
            return false;
        }
    } else {
        std::ifstream config_input(config_file, std::ios::binary);
        std::stringstream buffer;
        buffer << config_input.rdbuf();
        const auto existing = buffer.str();
        if (existing.find("\"resourceKey\"") == std::string::npos ||
            existing.find("\"" + EscapeJson(resource_key) + "\"") == std::string::npos) {
            return false;
        }
    }

    if (!std::filesystem::exists(extra_file)) {
        const auto temp_extra_file = std::filesystem::path(extra_file.string() + ".init.tmp");
        std::ofstream extra_output(temp_extra_file, std::ios::binary | std::ios::trunc);
        extra_output << "{\n}\n";
        extra_output.close();
        if (!extra_output.good()) {
            return false;
        }
        std::error_code rename_error;
        std::filesystem::rename(temp_extra_file, extra_file, rename_error);
        if (rename_error) {
            std::error_code remove_error;
            std::filesystem::remove(temp_extra_file, remove_error);
            return false;
        }
    }

    const auto snapshot = ReadStorageSnapshotLocked(
            config_file,
            block_size_bytes,
            content_length_hint,
            duration_ms_hint);

    out_storage->data_file = data_file;
    out_storage->config_file = config_file;
    out_storage->extra_file = extra_file;
    out_storage->block_size_bytes = snapshot.block_size_bytes;
    out_storage->content_length = snapshot.content_length;
    out_storage->duration_ms = snapshot.duration_ms;
    out_storage->cached_blocks = snapshot.block_indexes;
    out_storage->completed_ranges = snapshot.completed_ranges;
    out_storage->last_access_epoch_ms = snapshot.last_access_epoch_ms;
    return true;
}

CacheRuntime::StorageSnapshot CacheRuntime::ReadStorageSnapshotLocked(
        const std::filesystem::path& config_file,
        int32_t fallback_block_size_bytes,
        int64_t fallback_content_length,
        int64_t fallback_duration_ms) {
    StorageSnapshot snapshot;
    snapshot.block_size_bytes = fallback_block_size_bytes > 0 ? fallback_block_size_bytes : 64 * 1024;
    snapshot.content_length = fallback_content_length;
    snapshot.duration_ms = fallback_duration_ms;
    snapshot.last_access_epoch_ms = -1;

    std::ifstream config_input(config_file, std::ios::binary);
    std::stringstream buffer;
    buffer << config_input.rdbuf();
    const auto json = buffer.str();
    if (json.empty()) {
        return snapshot;
    }

    const auto parsed_block_size = ParseLongField(json, "blockSizeBytes");
    if (parsed_block_size.has_value() && parsed_block_size.value() > 0) {
        snapshot.block_size_bytes = static_cast<int32_t>(parsed_block_size.value());
    }
    const auto parsed_content_length = ParseLongField(json, "contentLength");
    if (parsed_content_length.has_value()) {
        snapshot.content_length = parsed_content_length.value();
    }
    const auto parsed_duration = ParseLongField(json, "durationMs");
    if (parsed_duration.has_value()) {
        snapshot.duration_ms = parsed_duration.value();
    }

    snapshot.block_indexes = ParseLongSetField(json, "blocks");
    snapshot.completed_ranges = ParseRangesField(json, "completedRanges");
    if (snapshot.completed_ranges.empty() && !snapshot.block_indexes.empty()) {
        snapshot.completed_ranges = TrunkIndex::BuildRangesFromBlocks(
                snapshot.block_indexes,
                snapshot.block_size_bytes);
    }

    const auto parsed_last_access = ParseLongField(json, "lastAccessEpochMs");
    if (parsed_last_access.has_value()) {
        snapshot.last_access_epoch_ms = parsed_last_access.value();
    }

    return snapshot;
}

CacheRuntime::SessionConfigSnapshot CacheRuntime::CaptureConfigSnapshotLocked(
        SessionState* session) {
    SessionConfigSnapshot snapshot;
    if (session == nullptr) {
        return snapshot;
    }
    snapshot.session_id = session->session_id;
    snapshot.version = ++session->next_metadata_snapshot_version;
    snapshot.resource_key = session->resource_key;
    snapshot.config_file = session->storage.config_file;
    snapshot.storage.block_size_bytes = session->storage.block_size_bytes;
    snapshot.storage.content_length = session->storage.content_length;
    snapshot.storage.duration_ms = session->storage.duration_ms;
    snapshot.storage.block_indexes = session->storage.cached_blocks;
    snapshot.storage.completed_ranges = session->storage.completed_ranges;
    snapshot.storage.last_access_epoch_ms = session->storage.last_access_epoch_ms;
    return snapshot;
}

bool CacheRuntime::PersistConfigSnapshot(
        const std::shared_ptr<SessionState>& session,
        const SessionConfigSnapshot& snapshot) {
    if (session == nullptr || snapshot.session_id <= 0 || snapshot.version == 0 ||
        snapshot.config_file.empty()) {
        return false;
    }

    std::lock_guard<std::mutex> io_lock(session->metadata_io_mutex);
    if (snapshot.version <= session->persisted_metadata_snapshot_version) {
        return true;
    }

    std::vector<Range> ranges = snapshot.storage.completed_ranges;
    if (ranges.empty() && !snapshot.storage.block_indexes.empty()) {
        ranges = TrunkIndex::BuildRangesFromBlocks(
                snapshot.storage.block_indexes,
                snapshot.storage.block_size_bytes);
    }

    const auto temp_file = std::filesystem::path(
            snapshot.config_file.string() + "." + std::to_string(snapshot.session_id) + "." +
            std::to_string(snapshot.version) + ".tmp");
    std::ofstream output(temp_file, std::ios::binary | std::ios::trunc);
    output << BuildConfigJson(
            snapshot.resource_key,
            snapshot.storage.block_size_bytes,
            snapshot.storage.content_length,
            snapshot.storage.duration_ms,
            snapshot.storage.block_indexes,
            ranges,
            snapshot.storage.last_access_epoch_ms > 0
                    ? snapshot.storage.last_access_epoch_ms
                    : NowEpochMs());
    output.close();
    if (!output.good()) {
        std::error_code remove_error;
        std::filesystem::remove(temp_file, remove_error);
        return false;
    }

    std::error_code rename_error;
    std::filesystem::rename(temp_file, snapshot.config_file, rename_error);
    if (rename_error) {
        std::error_code remove_error;
        std::filesystem::remove(temp_file, remove_error);
        return false;
    }
    session->persisted_metadata_snapshot_version = snapshot.version;
    return true;
}

int64_t CacheRuntime::AvailableSizeInRanges(
        const std::vector<Range>& ranges,
        int64_t offset) {
    if (offset < 0) {
        return 0;
    }
    for (const auto& range : ranges) {
        if (range.start <= offset && range.end > offset) {
            return range.end - offset;
        }
        if (range.start > offset) {
            return 0;
        }
    }
    return 0;
}

}  // namespace cachecore
