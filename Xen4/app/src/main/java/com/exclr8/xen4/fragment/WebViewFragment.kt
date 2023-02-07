package com.exclr8.xen4.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.exclr8.xen4.R
import com.exclr8.xen4.databinding.FragmentWebViewBinding
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.viewModel.ViewModelXen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


private const val INPUT_FILE_REQUEST_CODE = 1
private const val TAG = "WebViewFragment"

class WebViewFragment : Fragment() {

    private lateinit var binding: FragmentWebViewBinding
    private val vm: ViewModelXen by activityViewModels()

    private var mCameraPhotoPath: String? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var currentUrl = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val parentActivity = (activity as AppCompatActivity)
        val dl = parentActivity.findViewById<DrawerLayout>(R.id.dlMain)
        dl?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val actionBar = parentActivity.supportActionBar
        actionBar?.apply {
            hide()
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentWebViewBinding.bind(view)

        val webView = view.findViewById<WebView>(R.id.wvWebView)

        shouldClearBrowser(
            SharedPreference(requireContext()).getValueBoolean("shouldClearWebView", false),
            binding.wvWebView
        )

        //val webUrl = Urls.baseUrl + vm.currentPageUrl
        val baseUrl = SharedPreference(requireContext()).getValueString("baseUrl")
        val webUrl = baseUrl + vm.currentPageUrl
        //val webUrl = baseUrl + "CUI/TaskList/OpenWorkItemByInstance?instanceId=17&activityGuid=46a73823-e522-4d51-9cf8-b852153b6726"

        val extraHeader: MutableMap<String, String> = HashMap()
        extraHeader["USER_TOKEN_KEY"] = SharedPreference(requireContext()).getValueString("userKey").toString()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.pbWebView.isVisible = false
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val loadingUrl = request?.url.toString()
                Log.i(TAG, "Loading Url : $loadingUrl")
                currentUrl = loadingUrl
                if (loadingUrl.contains("Download")) {
                    webView.loadUrl(request?.url.toString(), extraHeader)
                    CoroutineScope(Dispatchers.IO).launch {
                        val docName = request?.url.toString()
                        downloadPdf(requireContext(), request?.url.toString(), "Xen4$docName")
                        vm.setCurrentDocumentVM(docName)
                    }
                }
                if (loadingUrl.contains("/CUI/Tasklist")) {
                    findNavController().navigate(R.id.taskFragment)
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.settings.apply {
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            supportZoom()

            loadWithOverviewMode = true

            setRenderPriority(WebSettings.RenderPriority.HIGH)
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            databaseEnabled = true
            domStorageEnabled = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            useWideViewPort = true
            saveFormData = true
            setEnableSmoothTransition(true)
        }
        //webView.setInitialScale(0)
        webView.loadUrl(webUrl, extraHeader)
        fileAttach()

        binding.miBurger.setOnClickListener {
            if (currentUrl.contains("KeyValue")){
                findNavController().navigate(R.id.taskFragment)
            }else{
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    private fun shouldClearBrowser(shouldClear: Boolean, webView: WebView) {
        if (shouldClear) {
            CookieSyncManager.createInstance(requireContext())
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            webView.clearCache(true)
            SharedPreference(requireContext()).save("shouldClearWebView", false)
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

    private fun redirectToDocument() {
        CoroutineScope(Dispatchers.Main).launch {
            findNavController().navigate(R.id.action_webViewFragment_to_documentFragment)
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


    private fun fileAttach() {
        binding.wvWebView.webChromeClient = object : WebChromeClient() {
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