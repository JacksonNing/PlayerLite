package com.wxy.playerlite.playback.process

import android.net.Uri
import java.io.ByteArrayInputStream
import java.util.function.Supplier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaSourceRepositoryTest {
    @Test
    fun createPlayableSource_allowsReadableMediaStoreUriWithoutPersistedPermission() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://media/external/audio/media/6345")
        Shadows.shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri,
            Supplier { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        )
        val repository = MediaSourceRepository(context)

        assertFalse(repository.hasPersistedReadPermission(uri))
        assertNotNull(repository.createPlayableSource(uri))
    }

    @Test
    fun createPlayableSource_rejectsUnreadableContentUri() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://media/external/audio/media/missing")
        Shadows.shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri,
            Supplier { throw SecurityException("read denied") }
        )
        val repository = MediaSourceRepository(context)

        assertTrue(repository.createPlayableSource(uri) == null)
    }
}
