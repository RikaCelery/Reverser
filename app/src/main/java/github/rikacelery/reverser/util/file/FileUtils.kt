package github.rikacelery.reverser.util.file

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns

@SuppressLint("Range")
fun getFileSize(
    context: Context,
    uri: Uri,
): Long {
    var size: Long = 0
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            size = it.getLong(it.getColumnIndex(OpenableColumns.SIZE))
        }
    }
    return size
}
