package com.phucdv.musicdemo

import android.content.Context
import android.provider.MediaStore.Audio.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(val context: Context) {

    public suspend fun getAllSongs() = withContext(Dispatchers.IO) {
        val listSongs = ArrayList<Song>()

        try {
            val contentResolver = context.contentResolver

            val fields = arrayOf(
                Media._ID,
                Media.TITLE
            )
            contentResolver.query(
                Media.EXTERNAL_CONTENT_URI, fields, null,
                null, "${Media.TITLE_KEY} ASC"
            )?.let { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Media._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(Media.TITLE)

                while (cursor.moveToNext()) {
                    val id = cursor.getInt(idIndex)
                    val title = cursor.getString(titleIndex)

                    val song = Song(id, title)
                    listSongs.add(song)
                }

                cursor.close()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        listSongs
    }
}