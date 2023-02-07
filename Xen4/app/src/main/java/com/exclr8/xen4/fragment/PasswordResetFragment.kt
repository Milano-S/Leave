package com.exclr8.xen4.fragment

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.exclr8.xen4.R
import com.exclr8.xen4.api.ResetPasswordInterface
import com.exclr8.xen4.databinding.FragmentPasswordResetBinding
import com.exclr8.xen4.model.LoginOrEmail
import com.exclr8.xen4.model.PasswordResponse
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.url.Urls
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "PasswordResetFragment"

class PasswordResetFragment : Fragment() {

    private lateinit var binding: FragmentPasswordResetBinding

    //private val vm: ViewModelXen by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_password_reset, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPasswordResetBinding.bind(view)

        /*if (binding.btnReset.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val param = binding.tvBig.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 0, 0, 0)
            binding.tvBig.layoutParams = param
        }*/

        val appToken = SharedPreference(requireContext()).getValueString("appToken").toString()
        binding.miBurger.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        binding.btnBacktoLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        binding.btnReset.setOnClickListener {
            if (binding.etUsernameT.text!!.isEmpty()) {
                Toast.makeText(requireContext(), "Login/Email is Empty", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnReset.apply {
                    isClickable = false
                    isEnabled = false
                }
                resetPassword(
                    binding.etUsernameT.text.toString(),
                    appToken
                )
            }
        }
    }

    private fun resetPassword(loginOrEmail: String, appToken: String) {
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(Urls.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ResetPasswordInterface::class.java)

        val retrofitData = retrofitBuilder.resetPassword(
            LoginOrEmail = LoginOrEmail(loginOrEmail),
            appTokenKey = appToken
        )
        retrofitData.enqueue(object : Callback<PasswordResponse> {
            override fun onResponse(
                call: Call<PasswordResponse>,
                response: Response<PasswordResponse>
            ) {
                Log.i(TAG, response.body().toString())
                binding.btnReset.apply {
                    isClickable = true
                    isEnabled = true
                }
                if (response.code() == 204) {
                    showAlertDialog(
                        title = "Success",
                        message = "Email sent Successfully"
                    )
                } else {
                    showAlertDialog(
                        title = "Unsuccessful",
                        message = "Password reset Unsuccessful"
                    )
                }
            }

            override fun onFailure(call: Call<PasswordResponse>, t: Throwable) {
                Log.i(TAG, t.message.toString())
                binding.btnReset.apply {
                    isClickable = true
                    isEnabled = true
                }
                showAlertDialog(
                    title = "Unsuccessful",
                    message = "Password Reset Unsuccessful"
                )
            }
        })
    }

    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("Ok") { _, _ ->
            findNavController().navigate(R.id.loginFragment)
        }

        builder.show()
    }
}