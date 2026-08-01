package com.wxy.playerlite.feature.player.runtime

import android.net.Uri
import com.wxy.playerlite.core.playlist.PlaylistItem
import com.wxy.playerlite.core.playlist.PlaylistItemType
import java.io.ByteArrayInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.function.Supplier

@RunWith(RobolectricTestRunner::class)
class MediaSourceRepositoryTest {
    @Test
    fun isPlaylistItemReadable_allowsOnlineSongEntriesWithoutLocalUri() {
        val repository = MediaSourceRepository(RuntimeEnvironment.getApplication())
        val item = PlaylistItem(
            id = "queue-online-1",
            uri = "",
            displayName = "夜曲",
            songId = "1973665667",
            itemType = PlaylistItemType.ONLINE
        )

        assertTrue(repository.isPlaylistItemReadable(item))
    }

    @Test
    fun contentUri_isReadableWithoutPersistedPermissionWhenResolverCanOpenIt() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://media/external/audio/media/6345")
        Shadows.shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri,
            Supplier { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        )
        val repository = MediaSourceRepository(context)

        assertTrue(repository.hasPersistedReadPermission(uri).not())
        assertTrue(repository.hasReadableAccess(uri))
        assertNotNull(repository.createPlayableSource(uri))
    }

    @Test
    fun contentUri_isUnreadableWhenResolverCannotOpenIt() {
        val context = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://media/external/audio/media/missing")
        Shadows.shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri,
            Supplier { throw SecurityException("read denied") }
        )
        val repository = MediaSourceRepository(context)

        assertFalse(repository.hasReadableAccess(uri))
        assertTrue(repository.createPlayableSource(uri) == null)
    }
}
