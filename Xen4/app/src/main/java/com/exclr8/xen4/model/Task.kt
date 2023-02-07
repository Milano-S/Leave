package com.exclr8.xen4.model

data class Task(
    val ActivityGuid: String,
    val ActivityName: String,
    val EmployeeName: String,
    val Header: String,
    val InstanceId: Int,
    val IsOpen: Boolean,
    val LeaveType: String,
    val OpenAge: String,
    val OpenedDateTime: String,
    val ProcessName: String,
    val Url : String
)