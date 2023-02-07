package com.exclr8.xen4.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.exclr8.xen4.R
import com.exclr8.xen4.adapter.BaseUrlAdapter
import com.exclr8.xen4.api.AuthInterface
import com.exclr8.xen4.api.LoginInterface
import com.exclr8.xen4.databinding.FragmentLoginBinding
import com.exclr8.xen4.model.*
import com.exclr8.xen4.room.UrlsDatabaseBuilder
import com.exclr8.xen4.service.FirebaseMessagingServiceXen
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.url.Urls
import com.exclr8.xen4.viewModel.ViewModelXen
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "LoginFragment"

class LoginFragment : androidx.fragment.app.Fragment() {

    private lateinit var binding: FragmentLoginBinding

    private val vm: ViewModelXen by activityViewModels()

    private var appToken = ""

    //private val sp: SharedPreference by lazy { SharedPreference(requireContext()) }

    private lateinit var fragContext: Context

    private lateinit var adapter: BaseUrlAdapter

    private lateinit var urlList: List<UrlsData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        urlList = runBlocking {
            UrlsDatabaseBuilder.getInstance(requireContext()).UrlDao().getAllBaseUrls()
        }

        SharedPreference(requireContext()).save("baseUrl", Urls.baseUrl)
        //SharedPreference(requireContext()).save("baseUrl", "http://192.168.1.60:8081/")

        val parentActivity = (activity as AppCompatActivity)
        parentActivity.findViewById<DrawerLayout>(R.id.dlMain)
            .setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val actionBar = parentActivity.supportActionBar
        actionBar?.hide()
        runBlocking {
            authenticateApp()
            getFCMToken()
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)
        val sp = SharedPreference(requireContext())
        fragContext = requireContext()

        //binding.etUsername.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        vm.setForgotPasswordVM(false)
        if (!vm.checkForInternet(requireContext())) {
            binding.btnLogin.isEnabled = false
            binding.btnLogin.isClickable = false
            binding.btnLogin.text = getString(R.string.Offline)
            binding.tvForgotPassword.isClickable = false
        }

        //DEBUG MENU STUFF
        debugMenu()
        //

        val userName = sp.getValueString("userName")
        val userPassword = sp.getValueString("userPassword")
        if (userName != null || userPassword != null && sp.getValueBoolean("signedIn", false)) {
            runBlocking { authenticateApp() }
            runBlocking { vm.getUserTasks(0, 10, requireContext()) }
            login(
                LoginDetails(
                    UserName = userName.toString(),
                    Password = userPassword.toString()
                ), sp.getValueString("appToken").toString()
            )
        }
        binding.btnLogin.setOnClickListener {
            if (!vm.checkForInternet(requireContext())) {
                binding.btnLogin.isEnabled = false
                binding.btnLogin.isClickable = false
                binding.btnLogin.text = getString(R.string.Offline)
            }
            if (binding.etUsername.text!!.isEmpty() || binding.etPassword.text!!.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Username or Password is Empty",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                binding.btnLogin.isEnabled = false
                binding.btnLogin.isClickable = false
                login(
                    LoginDetails(
                        UserName = binding.etUsername.text.toString(),
                        Password = binding.etPassword.text.toString()
                    ), sp.getValueString("appToken").toString()
                )
                sp.save("userName", binding.etUsername.text.toString())
                sp.save("userPassword", binding.etPassword.text.toString())
            }
        }
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_passwordResetFragment)
        }
    }

    //Authenticate
    private fun authenticateApp() {
        val baseUrl = SharedPreference(requireContext()).getValueString("baseUrl")
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl!!)
            .build()
            .create(AuthInterface::class.java)

        val authObject = AuthRequest(
            ApplicationKey = Urls.appKey,
            //AppVersion = BuildConfig.VERSION_NAME,
            AppVersion = "uat",
            OSName = "Android"
        )

        val retrofitData = retrofitBuilder.authorizeApp(authObject)

        Handler(Looper.getMainLooper()).postDelayed(
            {
                retrofitData.enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<AuthResponse>,
                        response: Response<AuthResponse>
                    ) {
                        val appTokenKey = response.body()?.TokenKey
                        SharedPreference(fragContext).save("appToken", appTokenKey.toString())
                        vm.setAppTokenKeyVM(appTokenKey.toString())
                        Log.i(TAG, response.body().toString())
                        appToken = appTokenKey.toString()
                        binding.llLogin2.isVisible = true
                        binding.clSplash.isVisible = false
                    }

                    override fun onFailure(call: retrofit2.Call<AuthResponse>, t: Throwable) {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.isClickable = true
                        binding.llLogin2.isVisible = true
                        binding.clSplash.isVisible = false
                        Toast.makeText(
                            requireContext(),
                            "Authentication Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i(TAG, t.message.toString())
                    }
                })
            }, 1200
        )
    }

    //Login
    private fun login(loginDetails: LoginDetails, appTokenKey: String) {
        runBlocking { authenticateApp() }
        val baseUrl = SharedPreference(requireContext()).getValueString("baseUrl")
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl!!)
            .build()
            .create(LoginInterface::class.java)

        val retrofitData = retrofitBuilder.login(loginDetails, appTokenKey)
        retrofitData.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(
                call: retrofit2.Call<LoginResponse>,
                response: Response<LoginResponse>
            ) {
                Log.i(TAG, response.body().toString())
                if (response.body() != null) {
                    if (response.body()!!.Success) {
                        //SharedPreference(requireContext()).removeValue("userKey")
                        SharedPreference(requireContext()).save("userKey", response.body()!!.TokenKey)
                        runBlocking {
                            runBlocking { vm.getUserTasks(0, 10, requireContext()) }
                            FirebaseMessagingServiceXen().sendRegistrationToServer(
                                PushNotificationRequest(
                                    DeviceTypeId = 2,
                                    DeviceUID = Settings.Secure.ANDROID_ID,
                                    DeviceName = Build.BRAND,
                                    DeviceDescription = Build.MODEL,
                                    Token = SharedPreference(requireContext()).getValueString("fcmToken")
                                        .toString()
                                ), response.body()!!.TokenKey,
                                url = SharedPreference(requireContext()).getValueString("baseUrl")
                                    .toString()
                            )
                        }
                        SharedPreference(requireContext()).save("signedIn", true)
                        SharedPreference(requireContext()).save("userKey", response.body()!!.TokenKey)
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.isClickable = true
                        binding.etUsername.text!!.clear()
                        binding.etPassword.text!!.clear()
                        SharedPreference(requireContext()).save("signedIn", false)
                        Toast.makeText(
                            requireContext(),
                            "Error Signing In",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.isClickable = true
                    Toast.makeText(
                        requireContext(),
                        "Sign In Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<LoginResponse>, t: Throwable) {
                Log.i(TAG, t.message.toString())
            }
        })
    }

    //FCM Token
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                SharedPreference(requireContext()).save("fcmToken", token)
                Log.d(TAG, token)
            } else {
                Log.i(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
        })
    }

    //Debug
    private fun debugMenu() {
        val sp = SharedPreference(requireContext())
        val urlDb = UrlsDatabaseBuilder.getInstance(requireContext()).UrlDao()
        (urlList as MutableList<UrlsData>).reverse()
        binding.btnUrlDebug.setOnClickListener {
            binding.llLogin2.isVisible = false
            binding.clDebug.isVisible = true
        }
        adapter = BaseUrlAdapter(requireContext(), urlList)
        binding.rvBaseUrls.adapter = adapter
        adapter.setOnUrlClick(object : BaseUrlAdapter.OnUrlClick {
            override fun onUrlClick(position: Int) {
                urlList[position]
                binding.tvUrlDebug.text = getString(R.string.url, sp.getValueString("baseUrl"))
            }
        })
        binding.rvBaseUrls.layoutManager = LinearLayoutManager(requireContext())
        binding.tvUrlDebug.text = getString(R.string.url, sp.getValueString("baseUrl"))
        binding.etBase.setText(sp.getValueString("baseUrl"))
        binding.etBase.addTextChangedListener {
            if (binding.etBase.text.toString().isEmpty()) {
                "Url : " + sp.getValueString("baseUrl").toString()
            }
        }
        binding.btnConfirmUrl.setOnClickListener {
            if (binding.etBase.text!!.isEmpty()) {
                Toast.makeText(requireContext(), "Text is empty", Toast.LENGTH_SHORT).show()
            } else {
                val url = binding.etBase.text.toString()
                sp.save("baseUrl", url)
                runBlocking { urlDb.insertUrl(UrlsData(baseUrl = url)) }
                urlList = runBlocking {
                    UrlsDatabaseBuilder.getInstance(requireContext()).UrlDao().getAllBaseUrls()
                }
                (urlList as MutableList<UrlsData>).reverse()
                adapter = BaseUrlAdapter(requireContext(), urlList)
                binding.rvBaseUrls.adapter = adapter
                binding.rvBaseUrls.layoutManager = LinearLayoutManager(requireContext())
                adapter.setOnUrlClick(object : BaseUrlAdapter.OnUrlClick {
                    override fun onUrlClick(position: Int) {
                        val currentUrl = urlList[position]
                        sp.save("baseUrl", currentUrl.baseUrl)
                        binding.tvUrlDebug.text =
                            getString(R.string.url, sp.getValueString("baseUrl").toString())
                    }
                })
                binding.tvUrlDebug.text =
                    getString(R.string.url, sp.getValueString("baseUrl").toString())
            }
        }
        binding.btnClear.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                urlDb.deleteAllUrls()
            }
            (urlList as MutableList<UrlsData>).clear()
            adapter = BaseUrlAdapter(requireContext(), urlList)
            binding.rvBaseUrls.adapter = adapter
            binding.rvBaseUrls.layoutManager = LinearLayoutManager(requireContext())
            adapter.setOnUrlClick(object : BaseUrlAdapter.OnUrlClick {
                override fun onUrlClick(position: Int) {
                    val currentUrl = urlList[position]
                    sp.save("baseUrl", currentUrl.baseUrl)
                    binding.tvUrlDebug.text =
                        getString(R.string.url, sp.getValueString("baseUrl").toString())
                }
            })
            SharedPreference(requireContext()).save("baseUrl", Urls.baseUrl)
            binding.tvUrlDebug.text =
                getString(R.string.url, sp.getValueString("baseUrl").toString())
            binding.etBase.text!!.clear()
        }
        binding.btnCloseDebug.setOnClickListener {
            binding.llLogin2.isVisible = true
            binding.clDebug.isVisible = false
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
        }
        adapter.setOnUrlClick(object : BaseUrlAdapter.OnUrlClick {
            override fun onUrlClick(position: Int) {
                val currentUrl = urlList[position]
                sp.save("baseUrl", currentUrl.baseUrl)
                binding.tvUrlDebug.text =
                    getString(R.string.url, sp.getValueString("baseUrl").toString())
            }
        })
    }
}