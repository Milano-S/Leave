package com.exclr8.xen4.model

import android.os.Build
import com.exclr8.xen4.BuildConfig

data class LoginDetails(
    var UserName: String,
    var Password: String,
    var OSName: String = "Android",
    var OSVersion: String =  Build.VERSION.RELEASE,
    //var AppVersion: String = BuildConfig.VERSION_NAME,
    var AppVersion: String = "uat",
    var DeviceBrand: String = Build.MANUFACTURER,
    var DeviceModel: String = Build.MODEL
)
