package com.exclr8.xen4.model

data class AuthResponse(
    val Exception: Any,
    val ExpiryUtc: String,
    val Offline: Boolean,
    val Success: Boolean,
    val TokenKey: String,
    val TokenKeyName: String
)
