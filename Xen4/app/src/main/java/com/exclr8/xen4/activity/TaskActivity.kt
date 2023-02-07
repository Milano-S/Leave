package com.exclr8.xen4.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.exclr8.xen4.R
import com.exclr8.xen4.databinding.ActivityTaskBinding
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.url.Urls.Companion.webUrl
import com.exclr8.xen4.viewModel.ViewModelXen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

private const val INPUT_FILE_REQUEST_CODE = 1
private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
private const val TAG = "TaskActivity"

class TaskActivity : AppCompatActivity() {

    private val miBack: ImageButton by lazy { findViewById(R.id.miBurger) }
    private val webView: WebView by lazy { findViewById(R.id.wvWebView) }
    private val pbWebView: ProgressBar by lazy { findViewById(R.id.pbWebView) }
    private lateinit var vm: ViewModelXen
    private var mCameraPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        vm = ViewModelProvider(this)[ViewModelXen::class.java]
        vm.setHasUrlIntentVM(false)

        val actionBar = supportActionBar
        actionBar?.apply {
            hide()
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onStart() {
        super.onStart()

        val taskUrl = intent.getStringExtra("taskUrl")
        //Toast.makeText(this, taskUrl.toString(), Toast.LENGTH_SHORT).show()

        val extraHeader: MutableMap<String, String> = HashMap()
        extraHeader["USER_TOKEN_KEY"] = SharedPreference(this).getValueString("userKey").toString()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pbWebView.isVisible = false
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val loadingUrl = request?.url.toString()
                if (loadingUrl.contains("Download")) {
                    //webView.loadUrl(request?.url.toString(), extraHeader)
                    CoroutineScope(Dispatchers.IO).launch {
                        val docName = request?.url.toString()
                        vm.setCurrentDocumentVM(docName)
                        val i = Intent(this@TaskActivity, DocumentActivity::class.java)
                        i.putExtra("documentUrl", docName)
                        startActivity(i)
                    }
                }
                if (loadingUrl.contains("/CUI/Tasklist")) {
                    Log.i(TAG, "Current Url :$loadingUrl")
                    startActivity(Intent(this@TaskActivity, MainActivity::class.java))
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        /*if (taskUrl.toString().contains("/CUI/Tasklist")){
            Log.i(TAG, "Current Url :$taskUrl")
            startActivity(Intent(this@TaskActivity, MainActivity::class.java))
        }*/
        webView.loadUrl(taskUrl.toString(), extraHeader)
        fileAttach()

        webView.settings.apply {
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            supportZoom()
            useWideViewPort = true
        }

        miBack.setOnClickListener {
            //vm.setHasUrlIntentVM(false)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private suspend fun downloadPdf(context: Context, url: String?, title: String): Long {

        val doesPdfExist = checkIfFileExists(url.toString())
        if (!doesPdfExist) {
            //redirectToDocument()
            return 0L
        } else {
            val downloadReference: Long
            val dm: DownloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)

            //Pdf Save Location
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                title
            )
            request.setTitle(title)
            downloadReference = dm.enqueue(request)

            redirectToDocument()
            return downloadReference
        }
    }

    private suspend fun checkIfFileExists(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val u = URL(url)
                val urlCon = u.openConnection()
                val br = BufferedReader(InputStreamReader(urlCon.getInputStream()))
                //determineFileType(urlCon.getInputStream())
                Log.i(TAG, br.toString())
                return@withContext true
            } catch (e: Exception) {
                Log.i(TAG, e.message.toString())
                return@withContext false
            }
        }
    }

    private fun redirectToDocument() {
        //Nav Controller
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fcvMain) as NavHostFragment?
        val navController = navHostFragment!!.navController
        CoroutineScope(Dispatchers.Main).launch {
            navController.navigate(R.id.action_webViewFragment_to_documentFragment)
        }
    }

    private fun fileAttach() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                //contentSelectionIntent.type = "application/pdf"

                //contentSelectionIntent.setType("application/*")
                contentSelectionIntent.setType("*/*")

                //val mimeTypes = mutableListOf<String>("image/*", "video/*")

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Document Selection")

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

                return true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri>? = null
        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
        return
    }
}