package dev.isaelsousa.app_manager_device.data.model

data class ResponseModel<T>(
    var message: String? = null,
    var data: T? = null,
    var status: Boolean = false
) {
    constructor(data: T?, status: Boolean) : this(message = null, data = data, status = status)

    constructor(status: Boolean, message: String) : this(message = message, data = null, status = status)

    constructor(data: T?) : this(
        message = null,
        data = data,
        status = data != null
    )

    fun addMessage(message: String): ResponseModel<T> {
        this.message = message
        return this
    }
}