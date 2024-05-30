package github.rikacelery.reverser

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.rikacelery.reverser.reverser.Core
import github.rikacelery.reverser.reverser.MetaData
import github.rikacelery.reverser.util.file.getFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

class EncodeActivity : ComponentActivity() {
    lateinit var ouri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    intent.putExtra("fileUri", uri)
                    Toast.makeText(this, "File: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
                }
            }

            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    intent.putExtra("fileUri", uri)
                }
            }
        }
        setContent {
            EncodeScreen()
        }
    }

    private fun shareFile() {
        ouri?.let { uri ->
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            startActivity(Intent.createChooser(shareIntent, "Share file"))
        }
    }

    private fun getEncodedFileUri(fileName: String): Uri? {
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName + (".reverse"))
        val cursor =
            contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )

        return if (cursor?.moveToFirst() == true) {
            runBlocking(Dispatchers.Main) {
                Toast.makeText(applicationContext, "以存在相同文件,将覆盖:" + fileName, Toast.LENGTH_SHORT)
                    .show()
            }
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
            val existingUri =
                ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    id,
                )
            cursor.close()
            existingUri
        } else {
            cursor?.close()
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + (".reverse"))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/reverser")
                }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri
        }
    }

    @Suppress("ktlint:standard:function-naming")
    @Composable
    fun EncodeScreen() {
        val fileUri = intent.getParcelableExtra<Uri>("fileUri")

        var detail by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Encoding...") }
        var enable by remember {
            mutableStateOf(false)
        }
        Column {
            Text(text = stringResource(R.string.encode_title), fontStyle = MaterialTheme.typography.headlineSmall.fontStyle)

            Text(
                detail.let {
                    if (it.isNotBlank()) {
                        it + "\n"
                    } else {
                        ""
                    }
                } + status,
                fontSize = 18.sp,
                modifier = Modifier.padding(16.dp),
            )
            // ...
            IconButton(
                onClick = { shareFile() },
                modifier = Modifier.padding(16.dp),
                enabled = enable,
            ) {
                Icon(imageVector = Icons.Rounded.Share, contentDescription = "share to")
            }
        }
        LaunchedEffect(fileUri) {
            launch(Dispatchers.IO) {
                fileUri?.let {
                    try {
                        val inputStream = contentResolver.openInputStream(it)
                        ouri = getEncodedFileUri(getFileName(it))!!
                        val outputStream = contentResolver.openOutputStream(ouri)
                        if (inputStream != null && outputStream != null) {
                            val meta =
                                MetaData(1024, Instant.now().epochSecond, false, getFileName(it))

                            Core.writeMeta(outputStream, meta)
                            val total = getFileSize(applicationContext, it)
                            detail = meta.toString() + "\nSize:" + total
                            var sum = 0
                            var i = 0
                            Core.reverse(inputStream, outputStream, meta.blockSize.toInt()) {
                                sum += it
                                if (i++ % 10 == 0) {
                                    status =
                                        "Reversing... %5.2f%% %d/%d".format(
                                            sum.toFloat().times(100).div(total),
                                            sum,
                                            total,
                                        )
                                }
                            }
                            status =
                                "Reversing... %5.2f%% %d/%d".format(
                                    sum.toFloat().times(100).div(total),
                                    sum,
                                    total,
                                )
                            status += "\nReversed!!!🎉 %d/%d".format(sum, total)
                            inputStream.close()
                            outputStream.flush()
                            outputStream.close()
                            enable = true
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        status = "Error encoding file"
                    }
                }
            }
        }
    }

    private fun getEncodedOutputStream(_uri: Uri): OutputStream? {
        val fileName = getFileName(_uri) + ".reverse"
        Log.d("Encode", "filename: " + fileName)
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/reverser",
                )
            }
        ouri =
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!

        return try {
            contentResolver.openOutputStream(ouri)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun processFile(
        inputStream: InputStream,
        outputStream: OutputStream,
    ) {
        val content = inputStream.bufferedReader().use { it.readText() }
        val reversedContent = content.reversed()
        outputStream.bufferedWriter().use { it.write(reversedContent) }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use { c ->
                c?.let {
                    if (it.moveToFirst()) {
                        result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "default_name"
    }
}
