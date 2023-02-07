package com.exclr8.xen4.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.webkit.WebView
import android.widget.*
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.exclr8.xen4.R
import com.exclr8.xen4.fragment.DocumentFragment
import com.exclr8.xen4.viewModel.ViewModelXen
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.*
import javax.net.ssl.HttpsURLConnection

private const val TAG = "DocumentActivity"
private const val STORAGE_PERMISSION_CODE = 102

class DocumentActivity : AppCompatActivity() {

    private lateinit var vm: ViewModelXen
    private var docType = ""

    private val pbPdf: ProgressBar by lazy { findViewById(R.id.pbPdf) }
    private val ivDocument: ImageView by lazy { findViewById(R.id.ivDocument) }
    private val pdfView: PDFView by lazy { findViewById(R.id.pdfView) }
    private val miBurger: ImageView by lazy { findViewById(R.id.miBurger) }
    private val btnDownload: Button by lazy { findViewById(R.id.btnDownload) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document)

        vm = ViewModelProvider(this)[ViewModelXen::class.java]

        val actionBar = supportActionBar
        actionBar?.apply {
            hide()
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
        vm.checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            STORAGE_PERMISSION_CODE,
            this@DocumentActivity,
            this
        )
    }

    override fun onStart() {
        super.onStart()

        val currentDocument = intent.getStringExtra("documentUrl").toString()
        if (determineFileType(currentDocument, pbPdf).toString().contains("image")) {
            ivDocument.isVisible = true
            Glide.with(this).load(currentDocument).into(ivDocument)
        } else {
            pdfView.isVisible = true
            RetrievePDFFromURL(
                pdfView = pdfView,
                pbPdf = pbPdf
            ).execute(currentDocument)
        }
        miBurger.setOnClickListener { this@DocumentActivity.finish() }
        btnDownload.setOnClickListener {
            val title =
                if (determineFileType(currentDocument, pbPdf).toString().contains("image")) {
                    "Xen4-" + Calendar.getInstance().timeInMillis.toString() + ".jpg"
                } else {
                    "Xen4-" + Calendar.getInstance().timeInMillis.toString() + ".pdf"
                }
            CoroutineScope(Dispatchers.IO).launch {
                vm.downloadPdf(
                    context = this@DocumentActivity,
                    url = currentDocument,
                    title = title
                )
            }
        }
    }

    private fun determineFileType(url: String, pbPdf: ProgressBar): String? {
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val urlConnection: HttpURLConnection = URL(url).openConnection() as HttpsURLConnection
        return if (urlConnection.responseCode == 200) {
            val fileType =
                URLConnection.guessContentTypeFromStream(BufferedInputStream(urlConnection.inputStream))
            docType = fileType ?: "NULL"
            pbPdf.isVisible = false
            fileType
        } else {
            "NULL"
        }
    }

    private class RetrievePDFFromURL(
        pdfView: PDFView,
        pbPdf: ProgressBar,
    ) : AsyncTask<String, Void, InputStream>() {

        private val mypdfView: PDFView = pdfView
        private val pbPdf: ProgressBar = pbPdf

        // on below line we are calling our do in background method.
        override fun doInBackground(vararg params: String?): InputStream? {
            // on below line we are creating a variable for our input stream.
            var inputStream: InputStream? = null
            try {
                val url = URL(params[0])
                val urlConnection: HttpURLConnection = url.openConnection() as HttpsURLConnection
                if (urlConnection.responseCode == 200) {
                    inputStream = BufferedInputStream(urlConnection.inputStream)
                }
            } catch (e: Exception) {
                pbPdf.isVisible = false
                e.printStackTrace()
                return null
            }
            return inputStream
        }

        override fun onPostExecute(result: InputStream?) {
            if (result == null) {
                Log.i(TAG, "Document is NULL")
            }
            val fileType = URLConnection.guessContentTypeFromStream(result)
            mypdfView.fromStream(result).load()
            pbPdf.isVisible = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}