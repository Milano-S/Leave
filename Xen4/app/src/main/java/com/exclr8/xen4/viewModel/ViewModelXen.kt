package com.exclr8.xen4.viewModel

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.exclr8.xen4.api.TasksInterface
import com.exclr8.xen4.model.*
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.url.Urls.Companion.baseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL

private const val TAG = "ViewModel"

class ViewModelXen : androidx.lifecycle.ViewModel() {

    var currentDocument = ""
    fun setCurrentDocumentVM(title: String) {
        currentDocument = title
    }

    var hasUrlIntent = false
    fun setHasUrlIntentVM(hasIntent: Boolean) {
        hasUrlIntent = hasIntent
    }

    var taskActivityUrl = ""
    fun setTaskActivityUrlVM(url: String) {
        taskActivityUrl = url
    }

    var appTokenKey = ""
    fun setAppTokenKeyVM(token: String) {
        appTokenKey = token
    }

    var webUrls = mutableListOf<LeaveWebsiteLink>()
    fun setWebUrls(urlData: UrlData) {
        urlData.LeaveWebsiteLinks.forEach { link ->
            webUrls.addAll(mutableListOf(link))
        }
    }

    var taskList = mutableListOf<Task>()
    fun setTaskList(userTaskList: TaskListResponse) {
        userTaskList.Tasks.forEach { task ->
            taskList.addAll(mutableListOf(task))
        }
    }

    fun getUserTasks(skip: Int, take: Int, context: Context) {

        val userTasksRequest = UserTasksRequest(skip, take)
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(SharedPreference(context).getValueString("baseUrl").toString())
            .build()
            .create(TasksInterface::class.java)

        val retrofitData = retrofitBuilder.getUserTasks(
            userTasksRequest,
            SharedPreference(context).getValueString("userKey").toString()
        )
        retrofitData.enqueue(object : Callback<TaskListResponse> {
            override fun onResponse(
                call: Call<TaskListResponse>,
                response: Response<TaskListResponse>
            ) {
                if (response.body() != null) {
                    taskList.clear()
                    setTaskList(response.body()!!)
                }
            }

            override fun onFailure(call: Call<TaskListResponse>, t: Throwable) {
                Log.i(TAG, t.message.toString())
            }
        })
    }

    var currentPageUrl = ""
    fun setCurrentPageUrlVM(url: String) {
        currentPageUrl = url
    }

    var forgotPassword = false
    fun setForgotPasswordVM(bool: Boolean) {
        forgotPassword = bool
    }

    var userDetails = UserDetailsResponse(
        "",
        "",
        "",
        "",
        "",
        Offline = true,
        Success = false,
        Surname = ""
    )

    fun setUserDetailsVM(details: UserDetailsResponse) {
        userDetails = details
    }

    //Check Internet Connection
    fun checkForInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    suspend fun downloadPdf(context: Context, url: String?, title: String): Long {

        val doesPdfExist = checkIfPdfExists(url.toString())

        if (!doesPdfExist || File("/storage/emulated/0/Download/$title").isFile) {
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
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setTitle(title)
            downloadReference = dm.enqueue(request)

            return downloadReference
        }
    }

    private suspend fun checkIfPdfExists(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val u = URL(url)
                val urlCon = u.openConnection()
                val br = BufferedReader(InputStreamReader(urlCon.getInputStream()))
                Log.i(TAG, br.toString())
                return@withContext true
            } catch (e: Exception) {
                Log.i(TAG, e.message.toString())
                return@withContext false
            }
        }
    }

    //Check and request permission.
    fun checkPermission(
        permission: String,
        requestCode: Int,
        activity: Activity,
        context: Context
    ) {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        } else {
            Log.i(TAG, PackageManager.PERMISSION_DENIED.toString())
        }
    }
}