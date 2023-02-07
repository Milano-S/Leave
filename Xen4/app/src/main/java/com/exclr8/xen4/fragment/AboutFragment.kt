package com.exclr8.xen4.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.exclr8.xen4.BuildConfig
import com.exclr8.xen4.R
import com.exclr8.xen4.databinding.FragmentAboutBinding
import com.exclr8.xen4.sharedPref.SharedPreference


class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAboutBinding.bind(view)

        binding.miBurger.setOnClickListener { findNavController().navigate(R.id.homeFragment) }

        binding.tvVersion.text = "Version : " + BuildConfig.VERSION_NAME
        binding.tvFCM.text = "FCM Token : " + SharedPreference(requireContext()).getValueString("fcmToken")
    }
}