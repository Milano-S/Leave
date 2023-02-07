package com.exclr8.xen4.model

import com.exclr8.xen4.BuildConfig

data class AuthRequest(
    var ApplicationKey: String,
    var AppVersion: String,
    var OSName: String
)
