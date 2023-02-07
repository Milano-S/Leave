package com.exclr8.xen4.model

data class FCMResponse(
    var Success: Boolean,
    var Offline: Boolean,
    var Status : Int,
    var Exception: Exception
)