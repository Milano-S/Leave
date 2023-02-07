package com.exclr8.xen4.model

data class LoginResponse(
    val Exception: Any,
    val ExpiryUtc: String,
    val Offline: Boolean,
    val Success: Boolean,
    val TokenKey: String,
    val TokenKeyName: String
)
