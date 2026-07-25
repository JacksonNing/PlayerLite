package com.wxy.playerlite.playback.process

import android.content.Context
import com.wxy.playerlite.playback.model.MusicInfo
import com.wxy.playerlite.playback.model.PlaybackAudioQuality
import com.wxy.playerlite.player.AudioMeta
import com.wxy.playerlite.player.AudioMetaDisplay
import com.wxy.playerlite.player.AudioEffectPreset
import com.wxy.playerlite.player.INativePlayer
import com.wxy.playerlite.player.PlaybackOutputInfo
import com.wxy.playerlite.player.source.IPlaysource
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackProcessRuntimePreparationTest {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() {
        serviceScope.cancel()
    }

    @Test
    fun prepareCurrent_whenConcurrent_shouldShareSinglePreparation() = runBlocking {
        val preparer = ControlledTrackPreparer(blockedTrackId = "track-1")
        val runtime = createRuntime(preparer)
        runtime.setQueue(mediaItems = listOf(track("track-1").toMediaItem()), startIndex = 0)

        val first = async { runtime.prepareCurrent() }
        withTimeout(1_000L) { preparer.started.await() }
        val second = async { runtime.prepareCurrent() }
        yield()

        assertEquals(1, preparer.prepareCalls.get())

        preparer.releaseBlockedPreparation.complete(Unit)
        first.await()
        second.await()

        assertEquals(1, preparer.prepareCalls.get())
        assertFalse(runtime.state.value.isPreparing)
        assertEquals("track-1", runtime.state.value.currentTrack?.id)
        runtime.release()
    }

    @Test
    fun prepareCurrent_whenFirstCallerCancels_shouldKeepSharedPreparationAlive() = runBlocking {
        val preparer = ControlledTrackPreparer(blockedTrackId = "track-1")
        val runtime = createRuntime(preparer)
        runtime.setQueue(mediaItems = listOf(track("track-1").toMediaItem()), startIndex = 0)

        val first = async { runtime.prepareCurrent() }
        withTimeout(1_000L) { preparer.started.await() }
        first.cancel()
        first.join()

        val second = async { runtime.prepareCurrent() }
        yield()
        assertEquals(1, preparer.prepareCalls.get())

        preparer.releaseBlockedPreparation.complete(Unit)
        second.await()

        assertEquals(1, preparer.prepareCalls.get())
        assertFalse(runtime.state.value.isPreparing)
        runtime.release()
    }

    @Test
    fun setActiveIndex_whenPreparationIsRunning_shouldCancelStalePreparation() = runBlocking {
        val preparer = ControlledTrackPreparer(blockedTrackId = "track-1")
        val runtime = createRuntime(preparer)
        runtime.setQueue(
            mediaItems = listOf(
                track("track-1").toMediaItem(),
                track("track-2").toMediaItem()
            ),
            startIndex = 0
        )

        val stalePreparation = async { runtime.prepareCurrent() }
        withTimeout(1_000L) { preparer.started.await() }

        assertTrue(runtime.setActiveIndex(1))
        stalePreparation.join()
        runtime.prepareCurrent()

        assertEquals(1, preparer.cancelledCalls.get())
        assertEquals(2, preparer.prepareCalls.get())
        assertEquals("track-2", runtime.state.value.currentTrack?.id)
        assertFalse(runtime.state.value.isPreparing)
        runtime.release()
    }

    @Test
    fun setPreferredAudioQuality_whenStoppedTrackIsPrepared_shouldPrepareNewQuality() = runBlocking {
        val preparer = RecordingTrackPreparer()
        val runtime = createRuntime(preparer)
        runtime.setQueue(mediaItems = listOf(track("track-1").toMediaItem()), startIndex = 0)
        runtime.prepareCurrent()

        assertTrue(runtime.setPreferredAudioQuality(PlaybackAudioQuality.LOSSLESS))
        withTimeout(1_000L) {
            while (preparer.requestedQualities.size < 2) {
                yield()
            }
        }

        assertEquals(
            listOf(PlaybackAudioQuality.EXHIGH, PlaybackAudioQuality.LOSSLESS),
            preparer.requestedQualities
        )
        assertEquals(PlaybackAudioQuality.LOSSLESS, runtime.state.value.appliedAudioQuality)
        assertFalse(runtime.state.value.isPreparing)
        runtime.release()
    }

    @Test
    fun playCurrent_whenSourceOpenFails_shouldReleasePreparedSource() = runBlocking {
        val source = RecordingFailureSource(
            openResult = IPlaysource.AudioSourceCode.ASC_OPEN_NOT_READ_ERROR
        )
        val runtime = createRuntime(FixedSourceTrackPreparer(source))
        runtime.setQueue(mediaItems = listOf(track("track-1").toMediaItem()), startIndex = 0)

        runtime.playCurrent()

        assertEquals(1, source.stopCalls)
        assertEquals(1, source.abortCalls)
        assertEquals(1, source.closeCalls)
        assertFalse(runtime.state.value.playWhenReady)
        assertFalse(runtime.state.value.isSeekSupported)
        assertEquals("Source open failed(3)", runtime.state.value.statusText)
        runtime.release()
    }

    @Test
    fun playCurrent_afterStop_shouldPrepareAndOpenFreshSource() = runBlocking {
        val preparer = StopInvalidatingTrackPreparer()
        val runtime = createRuntime(preparer)
        runtime.setQueue(mediaItems = listOf(track("track-1").toMediaItem()), startIndex = 0)
        runtime.prepareCurrent()
        val firstSource = preparer.sources.single()

        runtime.stop()
        runtime.playCurrent()

        assertEquals(2, preparer.sources.size)
        assertEquals(1, firstSource.stopCalls)
        assertEquals(1, firstSource.abortCalls)
        assertEquals(1, firstSource.closeCalls)
        assertEquals(1, preparer.sources.last().openCalls)
        assertFalse(runtime.state.value.statusText.startsWith("Source open failed"))
        runtime.release()
    }

    @Test
    fun playCurrent_whenSourceRewindFails_shouldReleasePreparedSource() = runBlocking {
        val source = RecordingFailureSource(rewindResult = -1L)
        val runtime = createRuntime(FixedSourceTrackPreparer(source))
        runtime.setQueue(mediaItems = listOf(track("track-1").toMediaItem()), startIndex = 0)
        runtime.prepareCurrent()
        preparedSourceSession(runtime).markPlaybackStarting("track-1")

        runtime.playCurrent()

        assertEquals(1, source.stopCalls)
        assertEquals(1, source.abortCalls)
        assertEquals(1, source.closeCalls)
        assertFalse(runtime.state.value.playWhenReady)
        assertFalse(runtime.state.value.isSeekSupported)
        assertEquals("Source rewind failed", runtime.state.value.statusText)
        runtime.release()
    }

    private fun createRuntime(trackPreparer: TrackPreparer): PlaybackProcessRuntime {
        return PlaybackProcessRuntime(
            appContext = RuntimeEnvironment.getApplication() as Context,
            serviceScope = serviceScope,
            nativePlayerFactory = { FakeNativePlayer() },
            trackPreparer = trackPreparer
        )
    }

    private fun track(id: String): MusicInfo {
        return MusicInfo(
            id = id,
            songId = id,
            title = id,
            playbackUri = "https://example.com/$id.mp3"
        )
    }

    private fun preparedSourceSession(runtime: PlaybackProcessRuntime): PreparedSourceSession {
        val field = PlaybackProcessRuntime::class.java.getDeclaredField("sourceSession")
        field.isAccessible = true
        return field.get(runtime) as PreparedSourceSession
    }

    private class FixedSourceTrackPreparer(
        private val source: IPlaysource
    ) : TrackPreparer {
        override suspend fun prepare(
            item: PlaybackTrack,
            preferredAudioQuality: PlaybackAudioQuality
        ): PreparationResult {
            return PreparationResult.Ready(
                source = source,
                mediaMeta = AudioMetaDisplay(
                    codec = "aac",
                    sampleRate = "44100 Hz",
                    channels = "2",
                    bitRate = "128 kbps",
                    durationMs = 10_000L
                ),
                isSeekSupported = true,
                appliedAudioQuality = preferredAudioQuality
            )
        }
    }

    private class RecordingTrackPreparer : TrackPreparer {
        val requestedQualities = mutableListOf<PlaybackAudioQuality>()

        override suspend fun prepare(
            item: PlaybackTrack,
            preferredAudioQuality: PlaybackAudioQuality
        ): PreparationResult {
            requestedQualities += preferredAudioQuality
            return PreparationResult.Ready(
                source = FakePlaySource(sourceId = "${item.id}:${preferredAudioQuality.wireValue}"),
                mediaMeta = AudioMetaDisplay(
                    codec = "aac",
                    sampleRate = "44100 Hz",
                    channels = "2",
                    bitRate = "128 kbps",
                    durationMs = 10_000L
                ),
                isSeekSupported = true,
                appliedAudioQuality = preferredAudioQuality
            )
        }
    }

    private class ControlledTrackPreparer(
        private val blockedTrackId: String
    ) : TrackPreparer {
        val prepareCalls = AtomicInteger(0)
        val cancelledCalls = AtomicInteger(0)
        val started = CompletableDeferred<Unit>()
        val releaseBlockedPreparation = CompletableDeferred<Unit>()

        override suspend fun prepare(
            item: PlaybackTrack,
            preferredAudioQuality: PlaybackAudioQuality
        ): PreparationResult {
            prepareCalls.incrementAndGet()
            if (item.id == blockedTrackId) {
                started.complete(Unit)
                try {
                    releaseBlockedPreparation.await()
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    cancelledCalls.incrementAndGet()
                    throw cancelled
                }
            }
            return PreparationResult.Ready(
                source = FakePlaySource(sourceId = item.id),
                mediaMeta = AudioMetaDisplay(
                    codec = "aac",
                    sampleRate = "44100 Hz",
                    channels = "2",
                    bitRate = "128 kbps",
                    durationMs = 10_000L
                ),
                isSeekSupported = true,
                appliedAudioQuality = preferredAudioQuality
            )
        }
    }

    private class StopInvalidatingTrackPreparer : TrackPreparer {
        val sources = mutableListOf<StopInvalidatingSource>()

        override suspend fun prepare(
            item: PlaybackTrack,
            preferredAudioQuality: PlaybackAudioQuality
        ): PreparationResult {
            val source = StopInvalidatingSource(sourceId = "${item.id}:${sources.size}")
            sources += source
            return PreparationResult.Ready(
                source = source,
                mediaMeta = AudioMetaDisplay(
                    codec = "aac",
                    sampleRate = "44100 Hz",
                    channels = "2",
                    bitRate = "128 kbps",
                    durationMs = 10_000L
                ),
                isSeekSupported = true,
                appliedAudioQuality = preferredAudioQuality
            )
        }
    }

    private class StopInvalidatingSource(
        override val sourceId: String
    ) : IPlaysource {
        var openCalls = 0
        var stopCalls = 0
        var abortCalls = 0
        var closeCalls = 0
        private var stopped = false

        override fun setSourceMode(mode: IPlaysource.SourceMode) = Unit

        override fun open(): IPlaysource.AudioSourceCode {
            openCalls += 1
            return if (stopped) {
                IPlaysource.AudioSourceCode.ASC_ABORT
            } else {
                IPlaysource.AudioSourceCode.ASC_SUCCESS
            }
        }

        override fun stop() {
            stopCalls += 1
            stopped = true
        }

        override fun abort() {
            abortCalls += 1
        }

        override fun close() {
            closeCalls += 1
        }

        override fun size(): Long = 0L

        override fun cacheSize(): Long = 0L

        override fun supportFastSeek(): Boolean = true

        override fun read(buffer: ByteArray, size: Int): Int = 0

        override fun seek(offset: Long, whence: Int): Long = offset
    }

    private class FakePlaySource(
        override val sourceId: String
    ) : IPlaysource {
        override fun setSourceMode(mode: IPlaysource.SourceMode) = Unit

        override fun open(): IPlaysource.AudioSourceCode =
            IPlaysource.AudioSourceCode.ASC_SUCCESS

        override fun stop() = Unit

        override fun abort() = Unit

        override fun close() = Unit

        override fun size(): Long = 0L

        override fun cacheSize(): Long = 0L

        override fun supportFastSeek(): Boolean = true

        override fun read(buffer: ByteArray, size: Int): Int = 0

        override fun seek(offset: Long, whence: Int): Long = offset
    }

    private class RecordingFailureSource(
        private val openResult: IPlaysource.AudioSourceCode =
            IPlaysource.AudioSourceCode.ASC_SUCCESS,
        private val rewindResult: Long = 0L,
        override val sourceId: String = "recording-failure-source"
    ) : IPlaysource {
        var stopCalls = 0
        var abortCalls = 0
        var closeCalls = 0

        override fun setSourceMode(mode: IPlaysource.SourceMode) = Unit

        override fun open(): IPlaysource.AudioSourceCode = openResult

        override fun stop() {
            stopCalls += 1
        }

        override fun abort() {
            abortCalls += 1
        }

        override fun close() {
            closeCalls += 1
        }

        override fun size(): Long = 0L

        override fun cacheSize(): Long = 0L

        override fun supportFastSeek(): Boolean = true

        override fun read(buffer: ByteArray, size: Int): Int = 0

        override fun seek(offset: Long, whence: Int): Long = rewindResult
    }

    private class FakeNativePlayer : INativePlayer {
        override fun setProgressListener(listener: ((Long) -> Unit)?) = Unit

        override fun setPlaybackOutputInfoListener(listener: ((PlaybackOutputInfo) -> Unit)?) = Unit

        override fun setPlaybackSpeed(speed: Float): Int = 0

        override fun setAudioEffectPreset(audioEffectPreset: AudioEffectPreset): Int = 0

        override fun playFromSource(source: IPlaysource): Int = 0

        override fun pause(): Int = 0

        override fun resume(): Int = 0

        override fun seek(positionMs: Long): Int = 0

        override fun getDurationFromSource(source: IPlaysource): Long = 0L

        override fun loadAudioMetaFromSource(source: IPlaysource): AudioMeta {
            return AudioMeta(
                codec = "aac",
                sampleRateHz = 44_100,
                channels = 2,
                bitRate = 128_000L,
                durationMs = 10_000L
            )
        }

        override fun loadAudioMetaDisplayFromSource(source: IPlaysource): AudioMetaDisplay {
            return AudioMetaDisplay(
                codec = "aac",
                sampleRate = "44100 Hz",
                channels = "2",
                bitRate = "128 kbps",
                durationMs = 10_000L
            )
        }

        override fun playbackState(): Int = PLAYBACK_STATE_STOPPED

        override fun stop() = Unit

        override fun close() = Unit

        override fun lastError(): String = "ok"
    }
}
