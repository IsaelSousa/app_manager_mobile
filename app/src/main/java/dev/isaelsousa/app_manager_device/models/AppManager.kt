package dev.isaelsousa.app_manager_device.models

data class AppManager(
    val id: String,
    val title: String,
    val url: String,
    val iconUri: String,
    val version: String,
    val createdAt: String,
    val lastUpdate: String,
    val isDeleted: Boolean = false,
    val devices: List<AppDevice> = mutableListOf()
)