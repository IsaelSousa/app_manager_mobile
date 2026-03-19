package br.com.sequor.launcher.data.model

data class ResponseModel<T>(
    val message: String = "",
    val data: T?,
    val status: Boolean = false
) {

    fun addData(data: T): ResponseModel<T> {
        return this.copy(data = data)
    }

    fun addStatus(status: Boolean): ResponseModel<T> {
        return this.copy(status = status)
    }

    fun addMessage(message: String): ResponseModel<T> {
        return this.copy(message = message)
    }
}