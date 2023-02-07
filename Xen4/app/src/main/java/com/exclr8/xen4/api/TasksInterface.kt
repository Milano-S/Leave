package com.exclr8.xen4.api

import com.exclr8.xen4.model.TaskListResponse
import com.exclr8.xen4.model.UserTasksRequest
import com.exclr8.xen4.url.Urls
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TasksInterface {
    @POST(Urls.userTasks)
    fun getUserTasks(
        @Body taskRequest: UserTasksRequest,
        @Header("USER_TOKEN_KEY") userTokenKey: String
    ): Call<TaskListResponse>
}