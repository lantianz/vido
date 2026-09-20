package com.vido.local.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MediaRepository(
    private val context: Context,
    private val privateMediaDao: PrivateMediaDao,
    private val directoryDao: PrivateDirectoryDao,
    private val playbackDao: PlaybackDao,
) {
    val privateVideos: Flow<List<VideoItem>> = privateMediaDao.observeAll().map { items ->
        items.map { item ->
            VideoItem(
                uri = item.uri,
                title = item.title,
                durationMs = item.durationMs,
                sizeBytes = item.sizeBytes,
                modifiedMs = item.modifiedMs,
                folder = item.folder,
                isPrivate = true,
            )
        }
    }
    val privateDirectories: Flow<List<PrivateDirectoryEntity>> = directoryDao.observeAll()

    suspend fun loadSystemVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val size = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val changed = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val folder = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            buildList {
                while (cursor.moveToNext()) {
                    add(VideoItem(
                        uri = ContentUris.withAppendedId(collection, cursor.getLong(id)).toString(),
                        title = cursor.getString(title).orEmpty().ifBlank { "未命名视频" },
                        durationMs = cursor.getLong(duration),
                        sizeBytes = cursor.getLong(size),
                        modifiedMs = cursor.getLong(changed) * 1_000,
                        folder = cursor.getString(folder).orEmpty().ifBlank { "其他" },
                        isPrivate = false,
                    ))
                }
            }
        } ?: emptyList()
    }

    suspend fun addDirectory(treeUri: String, name: String) {
        directoryDao.upsert(PrivateDirectoryEntity(treeUri, name, System.currentTimeMillis()))
    }

    suspend fun removeDirectory(treeUri: String) {
        directoryDao.delete(treeUri)
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                android.net.Uri.parse(treeUri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        // Cached records cannot be tied safely to a removed root when folders share a display name.
        privateMediaDao.clear()
    }

    suspend fun scanPrivateDirectories(extensions: Set<String>): ScanResult = withContext(Dispatchers.IO) {
        if (extensions.isEmpty()) {
            privateMediaDao.clear()
            return@withContext ScanResult(0, 0, 0)
        }
        var rejected = 0
        var inaccessible = 0
        val found = mutableListOf<PrivateMediaEntity>()
        val directories = directoryDao.observeAll().first()
        directories.forEach { directory ->
            val root = DocumentFile.fromTreeUri(context, android.net.Uri.parse(directory.treeUri))
            if (root == null || !root.canRead()) {
                inaccessible++
                return@forEach
            }
            walk(root) { file ->
                val name = file.name.orEmpty()
                val extension = name.substringAfterLast('.', "").lowercase()
                if (extension !in extensions) return@walk
                val video = inspectVideo(file, directory.displayName)
                if (video == null) rejected++ else found += video
            }
        }
        privateMediaDao.clear()
        privateMediaDao.upsert(found)
        ScanResult(found.size, rejected, inaccessible)
    }

    suspend fun playbackPosition(uri: String): Long = playbackDao.positionFor(uri) ?: 0L

    suspend fun savePlaybackPosition(uri: String, positionMs: Long) {
        playbackDao.save(PlaybackPositionEntity(uri, positionMs.coerceAtLeast(0), System.currentTimeMillis()))
    }

    private fun walk(directory: DocumentFile, consume: (DocumentFile) -> Unit) {
        directory.listFiles().forEach { child ->
            when {
                child.isDirectory -> walk(child, consume)
                child.isFile -> consume(child)
            }
        }
    }

    private fun inspectVideo(file: DocumentFile, folder: String): PrivateMediaEntity? {
        val uri = file.uri
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            val videoTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString("mime")?.startsWith("video/") == true
            } ?: return null
            val format = extractor.getTrackFormat(videoTrack)
            val duration = format.getLongOrDefault("durationUs") / 1_000
            val title = file.name.orEmpty().ifBlank { "未命名视频" }
            PrivateMediaEntity(
                uri = uri.toString(),
                title = title,
                durationMs = duration,
                sizeBytes = file.length(),
                modifiedMs = file.lastModified(),
                folder = folder,
                scannedAtMs = System.currentTimeMillis(),
            )
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}

private fun android.media.MediaFormat.getLongOrDefault(key: String): Long =
    if (containsKey(key)) getLong(key) else 0L
