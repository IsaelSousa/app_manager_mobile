package br.com.sequor.launcher.models

data class CheckUpdate(
    var appName: String,
    var version: String,
    var deviceVersion: String,
    var shouldUpdate: Boolean,
    var lastUpdateAppOnDevice: String
    )
