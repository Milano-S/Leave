package com.exclr8.xen4.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import com.exclr8.xen4.R
import com.exclr8.xen4.databinding.FragmentDocumentBinding
import com.exclr8.xen4.viewModel.ViewModelXen
import java.io.File
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.StrictMode
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.util.Calendar
import javax.net.ssl.HttpsURLConnection

private const val STORAGE_PERMISSION_CODE = 102
private const val TAG = "DocumentFragment"
class DocumentFragment : Fragment() {

    //View Binding
    private lateinit var binding: FragmentDocumentBinding

    //View Model
    private val vm: ViewModelXen by activityViewModels()

    private var docType = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_document, container, false)

        setHasOptionsMenu(true)
        val parentActivity = (activity as AppCompatActivity)
        val dl = parentActivity.findViewById<DrawerLayout>(R.id.dlMain)
        dl?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val actionBar = parentActivity.supportActionBar
        actionBar?.apply {
            hide()
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
        vm.checkPermission(
            READ_EXTERNAL_STORAGE,
            STORAGE_PERMISSION_CODE,
            requireActivity(),
            requireContext()
        )

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDocumentBinding.bind(view)

        val policy : StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val currentDocument = vm.currentDocument
        if (determineFileType(currentDocument, binding.pbPdf).toString().contains("image")) {
            binding.ivDocument.isVisible = true
            Glide.with(requireContext()).load(currentDocument).into(binding.ivDocument)
        } else {
            binding.pdfView.isVisible = true
            RetrievePDFFromURL(
                pdfView = binding.pdfView,
                pbPdf = binding.pbPdf,
            ).execute(vm.currentDocument)
        }

        binding.miBurger.setOnClickListener {
            if (vm.currentPageUrl.contains("EmployeeLeaveRequest")){
                findNavController().navigate(R.id.homeFragment)
            }else{
                findNavController().navigate(R.id.action_documentFragment_to_webViewFragment)
            }
        }
        binding.btnDownload.setOnClickListener {
            val title = if(determineFileType(currentDocument, binding.pbPdf).toString().contains("image")){
                "Xen4-" + Calendar.getInstance().timeInMillis.toString() + ".jpg"
            }else{
                "Xen4-" + Calendar.getInstance().timeInMillis.toString() + ".pdf"
            }
            CoroutineScope(Dispatchers.IO).launch {
                vm.downloadPdf(
                    context = requireContext(),
                    url = vm.currentDocument,
                    title = title
                )
            }
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

    private fun determineFileType(url: String, pbPdf: ProgressBar): String? {
        val urlConnection: HttpURLConnection = URL(url).openConnection() as HttpsURLConnection
        return if (urlConnection.responseCode == 200) {
            val fileType = URLConnection.guessContentTypeFromStream(BufferedInputStream(urlConnection.inputStream))
            docType = fileType ?: "NULL"
            pbPdf.isVisible = false
            fileType
        } else {
            "NULL"
        }
    }

    /*private fun retrievePDFFromDevice(title: String) {
        val path = "/storage/emulated/0/Download/$title"
        binding.pdfView.fromFile(File(path)).load()
    }*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Storage Permission Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Storage Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}