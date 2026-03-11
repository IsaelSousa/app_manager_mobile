package dev.isaelsousa.app_manager_device.data.remote

import dev.isaelsousa.app_manager_device.data.model.ResponseModel
import dev.isaelsousa.app_manager_device.models.AppDevice
import dev.isaelsousa.app_manager_device.models.AppManager
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppManagerApi {

    // App
    @GET("api/AppManager/list-apps")
    suspend fun listApps(): ResponseModel<List<AppManager>>

    @POST("api/AppManager/create-or-update")
    suspend fun createOrUpdate(@Body app: AppManager): ResponseModel<AppManager>

    @DELETE("api/AppManager/delete/{id}")
    suspend fun deleteApp(@Path("id") id: Int): ResponseModel<Boolean>

    // Device

    @GET("api/AppDevice/list-apps-by-device")
    suspend fun listAppsByDevice(@Query("device") device: String): ResponseModel<AppDevice>

    @POST("api/AppDevice/create-or-update")
    suspend fun createOrUpdateDevice(@Body app: AppDevice): ResponseModel<AppDevice>

    @DELETE("api/AppDevice/delete/{id}")
    suspend fun deleteDevice(@Path("id") id: Int): ResponseModel<Boolean>
}