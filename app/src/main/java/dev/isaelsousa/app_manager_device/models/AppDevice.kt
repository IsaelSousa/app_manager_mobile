package dev.isaelsousa.app_manager_device.models

data class AppDevice (
    val id: String,
    val device: String,
    val uri: String,
    val version: String,
    val createdAt: String,
    val lastUpdate: String,
    val isDeleted: Boolean = false
)