package dev.isaelsousa.app_manager_device.activities

import AppAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.isaelsousa.app_manager_device.R
import dev.isaelsousa.app_manager_device.models.AppManager
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import dev.isaelsousa.app_manager_device.data.network.client
import dev.isaelsousa.app_manager_device.data.network.retrofit
import dev.isaelsousa.app_manager_device.data.remote.AppManagerApi
import dev.isaelsousa.app_manager_device.models.AppDevice
import dev.isaelsousa.app_manager_device.models.DeviceActionType
import dev.isaelsousa.app_manager_device.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.File

class MainActivity : AppCompatActivity() {
    private val api = retrofit.create(AppManagerApi::class.java)
    private lateinit var adapter: AppAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }

        recycler();
    }

    fun recycler() {
        val rvAppList = findViewById<RecyclerView>(R.id.rvAppList)

        adapter = AppAdapter(mutableListOf(), context = this@MainActivity) { app, type ->
            executeInstall(app, type);
        }

        rvAppList.layoutManager = LinearLayoutManager(this)
        rvAppList.adapter = adapter

        fetchData();
    }

    @SuppressLint("HardwareIds")
    private fun executeInstall(app: AppManager, type: DeviceActionType) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            verifyPermission()
            return
        }

        lifecycleScope.launch {
            try {
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                if (type == DeviceActionType.Download) {
                    Toast.makeText(this@MainActivity, "Baixando APK...", Toast.LENGTH_SHORT).show()
                    val localPath = downloadApk(app.url, "${app.title}.apk")
                    val data = AppDevice(device = androidId, appManagerId = app.id, uri = localPath, version = app.version);

                    val json = Gson()
                    println("Aki ${json.toJson(data)}")

                    val resp = api.createOrUpdateDevice(data);
                    if (resp.status) {
                        fetchData();
                    }
                }

                if (type == DeviceActionType.Open && !app.devices.isEmpty()) {
                    val first = app.devices.first();
                    val packageName = AppUtils.getPackageNameFromApk(this@MainActivity, first.uri) ?: "";
                    if (!packageName.isEmpty()) {
                        AppUtils.openApp(packageName, this@MainActivity)
                    }
                }

                if (type == DeviceActionType.Install && !app.devices.isEmpty()) {
                    val first = app.devices.first();

                    val packageName = AppUtils.getPackageNameFromApk(this@MainActivity, first.uri)

                    if (!AppUtils.isAppInstalled(packageName, this@MainActivity)) {
                        installApk(first.uri)
                        fetchData()
                    } else {
                        Toast.makeText(this@MainActivity, "App ja instalado!", Toast.LENGTH_SHORT).show()
                    }
                }

                if (type == DeviceActionType.Update && !app.devices.isEmpty()) {
                    val first = app.devices.first();
                    Toast.makeText(this@MainActivity, "Baixando Atualização da APK...", Toast.LENGTH_SHORT).show()
                    val localPath = downloadApk(app.url, "${app.title}.apk")
                    first.uri = localPath

                    val resp = api.createOrUpdateDevice(first);
                    if (resp.status) {
                        installApk(resp.data?.uri ?: "")
                        fetchData()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun downloadApk(url: String, fileName: String): String = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Erro: ${response.code}")

            val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(storageDir, fileName)

            val sink = file.sink().buffer()
            val source = response.body?.source() ?: throw Exception("Corpo vazio")

            try {
                sink.writeAll(source)
            } finally {
                sink.close()
                source.close()
            }

            return@withContext file.absolutePath
        }
    }

    private fun installApk(path: String) {
        if (path.isEmpty()) return

        val file = File(path)

        if (file.exists() && file.canRead()) {
            val contentUri = FileProvider.getUriForFile(
                this@MainActivity,
                "dev.isaelsousa.app_manager_device.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            this@MainActivity.startActivity(intent)
        }
    }

    private fun verifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, "Por favor, autorize a instalação para continuar.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun fetchData() {
        lifecycleScope.launch {
            try {
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val response = api.listApps(androidId)

                if (response.status) {
                    val list = response.data ?: emptyList()
                    adapter.updateData(list)
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    swipeRefreshLayout.isRefreshing = false
                    println("Erro do servidor: ${response.message}")
                }
            } catch (e: Exception) {
                swipeRefreshLayout.isRefreshing = false
                println("Erro de conexão: ${e.message}")
            }
        }
    }
}