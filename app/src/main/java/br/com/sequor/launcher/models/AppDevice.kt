package br.com.sequor.launcher.models

data class AppDevice (
    val id: String? = null,
    var device: String,
    var appManagerId: String,
    var uri: String,
    var version: String,
    val createdAt: String? = null,
    val lastUpdate: String? = null,
    val isDeleted: Boolean = false
)