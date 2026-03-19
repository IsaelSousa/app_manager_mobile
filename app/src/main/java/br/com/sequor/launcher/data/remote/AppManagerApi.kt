package br.com.sequor.launcher.data.remote

import br.com.sequor.launcher.data.model.ResponseModel
import br.com.sequor.launcher.models.AppDevice
import br.com.sequor.launcher.models.AppManager
import br.com.sequor.launcher.models.CheckUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppManagerApi {

    // App
    @GET("api/AppManager/list-apps")
    suspend fun listApps(@Query("device") device: String = ""): ResponseModel<List<AppManager>>

    @POST("api/AppManager/create-or-update")
    suspend fun createOrUpdate(@Body app: AppManager): ResponseModel<AppManager>

    @DELETE("api/AppManager/delete/{id}")
    suspend fun deleteApp(@Path("id") id: String): ResponseModel<Boolean>

    // Device

    @GET("api/AppDevice/list-apps-by-device")
    suspend fun listAppsByDevice(@Query("device") device: String): ResponseModel<AppDevice>

    @POST("api/AppDevice/create-or-update")
    suspend fun createOrUpdateDevice(@Body app: AppDevice): ResponseModel<AppDevice>

    @DELETE("api/AppDevice/delete/{id}")
    suspend fun deleteDeviceApp(@Path("id") id: String): ResponseModel<Boolean>

    // Check Update

    @GET("api/CheckUpdate/to-update")
    suspend fun checkUpdate(@Query("device") device: String): ResponseModel<List<CheckUpdate>>
}