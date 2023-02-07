package com.exclr8.xen4.model

data class PushNotificationRequest(
    var DeviceTypeId: Int,
    var DeviceUID: String,
    var DeviceName: String,
    var DeviceDescription: String,
    var Token: String
)
