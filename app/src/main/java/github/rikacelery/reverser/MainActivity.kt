package github.rikacelery.reverser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.rikacelery.reverser.ui.theme.ReverserTheme

class MainActivity : ComponentActivity() {
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val fileName = getFileName(it)
                Log.i("MAIN", fileName)
                when {
                    fileName.endsWith(".reverse", ignoreCase = true) -> {
                        // Open DecodeActivity
                        val intent =
                            Intent(this@MainActivity, DecodeActivity::class.java).apply {
                                putExtra("fileUri", uri)
                            }
                        startActivity(intent)
                    }
                    else -> {
                        // Open EncodeActivity
                        val intent =
                            Intent(this@MainActivity, EncodeActivity::class.java).apply {
                                putExtra("fileUri", uri)
                            }
                        startActivity(intent)
                    }
                }
            }
        }

    private fun readFileContent(uri: Uri) {
//        try {
//            val inputStream = contentResolver.openInputStream(uri)
//            val content = inputStream?.bufferedReader().use { it?.readText() }
//            // 处理文件内容
//
//            Log.d("FileContent", content ?: "No content")
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
    }

    fun openFileSelector() {
        getContent.launch("*/*") // 可以根据需要指定 MIME 类型，例如 "image/*"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReverserTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier =
                            Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                    ) {
                        Greeting(
                            name = stringResource(R.string.click_me),
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .clickable {
                                        openFileSelector()
                                    },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ReverserTheme {
        Greeting("Android")
    }
}
