package com.exclr8.xen4.model

data class UserDetailsResponse(
    val CellNo: String,
    val Email: String,
    val Exception: Any,
    val Firstname: String,
    val LastPasswordChangeUTC: String,
    val Offline: Boolean,
    val Success: Boolean,
    val Surname: String
)