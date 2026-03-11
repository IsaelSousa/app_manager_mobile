package dev.isaelsousa.app_manager_device.data.remote

import dev.isaelsousa.app_manager_device.data.model.ResponseModel
import dev.isaelsousa.app_manager_device.models.AppManager
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AppManagerApi {

    @GET("api/AppManager/list-apps")
    suspend fun listApps(): ResponseModel<List<AppManager>>

    @POST("api/AppManager/create-or-update")
    suspend fun createOrUpdate(@Body app: AppManager): ResponseModel<AppManager>

    @DELETE("api/AppManager/delete/{id}")
    suspend fun deleteApp(@Path("id") id: Int): ResponseModel<Boolean>
}