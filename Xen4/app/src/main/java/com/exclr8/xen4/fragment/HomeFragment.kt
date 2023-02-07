package com.exclr8.xen4.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.exclr8.xen4.R
import com.exclr8.xen4.activity.TaskActivity
import com.exclr8.xen4.api.TasksInterface
import com.exclr8.xen4.api.UserDetailsInterface
import com.exclr8.xen4.api.WebUrlInterface
import com.exclr8.xen4.databinding.FragmentHomeBinding
import com.exclr8.xen4.model.TaskListResponse
import com.exclr8.xen4.model.UrlData
import com.exclr8.xen4.model.UserDetailsResponse
import com.exclr8.xen4.model.UserTasksRequest
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.viewModel.ViewModelXen
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val vm: ViewModelXen by activityViewModels()

    private val baseUrl: String by lazy {
        SharedPreference(requireContext()).getValueString("baseUrl").toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        runBlocking {
            getUserDetails()
        }
        getUrls()
        redirectToTask(vm.hasUrlIntent)

        val parentActivity = (activity as AppCompatActivity)
        val actionBar = parentActivity.supportActionBar
        parentActivity.findViewById<DrawerLayout>(R.id.dlMain)
            .setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        actionBar?.apply {
            show()
            title = getString(R.string.Hi, vm.userDetails.Firstname)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#19426f")))
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        //binding.tvUserName.text = getString(R.string.Hi, vm.userDetails.Firstname)
        binding.clLeaveRequest.setOnClickListener { loadWebView("Create Leave Request") }
        binding.clDetails.setOnClickListener { loadWebView("View Employee Details") }
        binding.clHistory.setOnClickListener { loadWebView("Leave Request History") }
        binding.clTasks.setOnClickListener { redirectToTaskFrag() }
        binding.clLedger.setOnClickListener { loadWebView("View Leave Ledger") }
    }

    private fun loadWebView(pageTitle: String) {
        val urls = vm.webUrls
        urls.forEach { item ->
            if (item.Title == pageTitle) {
                vm.setCurrentPageUrlVM(item.Url)
            }
        }
        findNavController().navigate(R.id.action_homeFragment_to_webViewFragment)
    }

    private fun getUrls() {
        //vm.webUrls.clear()
        val baseUrl = SharedPreference(requireContext()).getValueString("baseUrl")
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl!!)
            .build()
            .create(WebUrlInterface::class.java)

        val retrofitData = retrofitBuilder.getUrls(
            appTokenKey = SharedPreference(requireContext()).getValueString("appToken").toString()
        )
        retrofitData.enqueue(object : Callback<UrlData> {
            override fun onResponse(call: Call<UrlData>, response: Response<UrlData>) {
                val responseData = response.body()
                if (responseData != null) {
                    vm.setWebUrls(responseData)
                }
                Log.i(TAG, response.body().toString())
            }

            override fun onFailure(call: Call<UrlData>, t: Throwable) {
                Log.i(TAG, t.message.toString())
            }
        })
    }

    private fun getUserDetails() {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .build()
            .create(UserDetailsInterface::class.java)

        val userTokenKey = SharedPreference(requireContext()).getValueString("userKey").toString()
        val retrofitData = retrofitBuilder.getUserDetails(userTokenKey)
        retrofitData.enqueue(object : Callback<UserDetailsResponse> {
            override fun onResponse(
                call: Call<UserDetailsResponse>,
                response: Response<UserDetailsResponse>
            ) {
                Log.i(TAG, response.body().toString())
                val details = response.body()
                if (details != null) {
                    vm.setUserDetailsVM(details)
                    //binding.tvUserName.text = getString(R.string.Hi, response.body()!!.Firstname)
                    val parentActivity = (activity as AppCompatActivity)
                    val actionBar = parentActivity.supportActionBar
                    parentActivity.findViewById<DrawerLayout>(R.id.dlMain)
                        .setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    actionBar?.title = getString(R.string.Hi, vm.userDetails.Firstname)
                }
            }

            override fun onFailure(call: Call<UserDetailsResponse>, t: Throwable) {
                Log.i(TAG, t.message.toString())
            }
        })
    }

    private fun redirectToTaskFrag(){
        runBlocking { vm.getUserTasks(0, 10, requireContext()) }
        findNavController().navigate(R.id.action_homeFragment_to_taskFragment)
    }

    private fun redirectToTask(hasIntent: Boolean){
        if (hasIntent){
            Log.i(TAG, "Has Intent = True")
            val intent = Intent(requireContext(), TaskActivity::class.java)
            intent.putExtra("taskUrl", vm.taskActivityUrl)
            startActivity(intent)
        }
    }
}