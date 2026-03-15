package dev.isaelsousa.app_manager_device.models;

data class CheckUpdate(
    var appName: String,
    var version: String,
    var deviceVersion: String,
    var shouldUpdate: Boolean,
    var lastUpdateAppOnDevice: String
    )
