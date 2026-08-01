package com.wxy.playerlite.playback.process

import android.net.Uri
import com.wxy.playerlite.playback.model.MusicInfo
import com.wxy.playerlite.player.AudioEffectPreset
import com.wxy.playerlite.player.AudioMeta
import com.wxy.playerlite.player.AudioMetaDisplay
import com.wxy.playerlite.player.INativePlayer
import com.wxy.playerlite.player.PlaybackOutputInfo
import com.wxy.playerlite.player.source.IPlaysource
import java.io.ByteArrayInputStream
import java.util.function.Supplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackPreparationCoordinatorLocalContentTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun prepare_allowsReadableMediaStoreUriWithoutPersistedPermission() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://media/external/audio/media/6345")
        Shadows.shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri,
            Supplier { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        )
        val playbackCoordinator = PlaybackCoordinator(
            player = NoOpNativePlayer,
            scope = scope,
            queryDispatcher = Dispatchers.Unconfined
        )
        val coordinator = TrackPreparationCoordinator(
            sourceRepository = MediaSourceRepository(context),
            playbackCoordinator = playbackCoordinator,
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = coordinator.prepare(
            PlaybackTrack(
                MusicInfo(
                    id = "local-6345",
                    title = "Local",
                    playbackUri = uri.toString()
                )
            )
        )

        assertTrue(result is PreparationResult.Ready)
        (result as PreparationResult.Ready).source.close()
    }
}

private object NoOpNativePlayer : INativePlayer {
    override fun setProgressListener(listener: ((Long) -> Unit)?) = Unit
    override fun setPlaybackOutputInfoListener(listener: ((PlaybackOutputInfo) -> Unit)?) = Unit
    override fun setPlaybackSpeed(speed: Float): Int = 0
    override fun setAudioEffectPreset(audioEffectPreset: AudioEffectPreset): Int = 0
    override fun playFromSource(source: IPlaysource): Int = 0
    override fun pause(): Int = 0
    override fun resume(): Int = 0
    override fun seek(positionMs: Long): Int = 0
    override fun getDurationFromSource(source: IPlaysource): Long = 0L
    override fun loadAudioMetaFromSource(source: IPlaysource): AudioMeta = AudioMeta(
        codec = "aac",
        sampleRateHz = 44_100,
        channels = 2,
        bitRate = 128_000L,
        durationMs = 0L
    )
    override fun loadAudioMetaDisplayFromSource(source: IPlaysource): AudioMetaDisplay =
        AudioMetaDisplay(
            codec = "aac",
            sampleRate = "44100 Hz",
            channels = "2",
            bitRate = "128 kbps",
            durationMs = 0L
        )
    override fun playbackState(): Int = 0
    override fun stop() = Unit
    override fun close() = Unit
    override fun lastError(): String = ""
}
