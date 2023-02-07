package com.exclr8.xen4.model

data class PasswordResponse(
    var Success: Boolean,
    var Offline: Boolean,
    val Exception: Any,
    var Status: Int,
    var LoginOrEmail: String,
)
