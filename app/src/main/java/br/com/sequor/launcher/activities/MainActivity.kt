package br.com.sequor.launcher.activities

import AppAdapter
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.sequor.launcher.R
import br.com.sequor.launcher.models.AppManager
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.com.sequor.launcher.data.network.NetworkModule
import br.com.sequor.launcher.data.remote.AppManagerApi
import br.com.sequor.launcher.models.AppDevice
import br.com.sequor.launcher.models.DeviceActionType
import br.com.sequor.launcher.services.AppConfig
import br.com.sequor.launcher.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.File
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.sequor.launcher.data.model.ResponseModel
import br.com.sequor.launcher.services.UpdateCheckWorker
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: AppAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvAppList: RecyclerView
    private lateinit var cardEmpty: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupUpdateWorker()

        rvAppList = findViewById(R.id.rvAppList)
        cardEmpty = findViewById(R.id.cardEmpty)

        rvAppList.visibility = View.GONE
        cardEmpty.visibility = View.VISIBLE

        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        btnSettings.setOnClickListener { v ->
            showSettingsDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            validateDefaultServer()
        }

        recycler();
    }

    fun recycler() {
        adapter = AppAdapter(mutableListOf(), context = this@MainActivity) { app, type ->
            executeInstall(app, type);
        }

        rvAppList.layoutManager = LinearLayoutManager(this)
        rvAppList.adapter = adapter

        validateDefaultServer()
    }

    @SuppressLint("HardwareIds")
    private fun executeInstall(app: AppManager, type: DeviceActionType) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            verifyPermission()
            return
        }

        lifecycleScope.launch {
            try {
                val api = NetworkModule.getRetrofitInstance(AppConfig.getBaseUrl(this@MainActivity)).create(AppManagerApi::class.java)

                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                if (type == DeviceActionType.Download) {
                    Toast.makeText(this@MainActivity, R.string.downloading_app, Toast.LENGTH_SHORT).show()
                    val localPath = downloadApk(app.url, "${app.title}.apk")
                    val data = AppDevice(device = androidId, appManagerId = app.id, uri = localPath, version = app.version);

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
                    } else {
                        val response = api.deleteDeviceApp(first.id ?: "")
                        if (response.status) {
                            fetchData()
                        }
                    }
                }

                if (type == DeviceActionType.Install && !app.devices.isEmpty()) {
                    val first = app.devices.first();

                    val packageName = AppUtils.getPackageNameFromApk(this@MainActivity, first.uri)

                    if (!AppUtils.isAppInstalled(packageName, this@MainActivity)) {
                        if (!installApk(first.uri)) {
                            Toast.makeText(this@MainActivity, R.string.app_not_found,
                                Toast.LENGTH_SHORT).show()
                            downloadApk(app.url, "${app.title}.apk")
                        }
                        fetchData()
                    } else {
                        fetchData()
                        Toast.makeText(this@MainActivity, R.string.app_already_installed, Toast.LENGTH_SHORT).show()
                    }
                }

                if (type == DeviceActionType.Update && !app.devices.isEmpty()) {
                    val first = app.devices.first();
                    Toast.makeText(this@MainActivity, R.string.downloading_app_update, Toast.LENGTH_SHORT).show()
                    val localPath = downloadApk(app.url, "${app.title}.apk")
                    first.uri = localPath
                    first.appManagerId = app.id

                    val resp = api.createOrUpdateDevice(first);
                    if (resp.status) {
                        if (installApk(resp.data?.uri ?: "")) {
                            first.version = app.version
                            api.createOrUpdateDevice(first);
                        } else {
                            Toast.makeText(this@MainActivity, R.string.app_not_found,
                                Toast.LENGTH_SHORT).show()
                            downloadApk(app.url, "${app.title}.apk")
                        }
                        fetchData()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun downloadApk(url: String, fileName: String): String = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()

        NetworkModule.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("${getString(R.string.error)}: ${response.code}")

            val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(storageDir, fileName)

            val sink = file.sink().buffer()
            val source = response.body?.source() ?: throw Exception(getString(R.string.empty))

            try {
                sink.writeAll(source)
            } finally {
                sink.close()
                source.close()
            }

            return@withContext file.absolutePath
        }
    }

    private fun installApk(path: String): Boolean {
        if (path.isEmpty()) return false

        val file = File(path)

        if (file.exists() && file.canRead()) {
            val contentUri = FileProvider.getUriForFile(
                this@MainActivity,
                "br.com.sequor.launcher.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            this@MainActivity.startActivity(intent)

            return true
        } else {
            return false
        }
    }

    private fun verifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, R.string.authorize_installation, Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun fetchData() {
        lifecycleScope.launch {
            try {
                println("1")
                val api = NetworkModule.getRetrofitInstance(AppConfig.getBaseUrl(this@MainActivity)).create(AppManagerApi::class.java)
                println("2")
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                println("3.")
                val response = api.listApps(androidId)
                println("4")

                if (response.status) {
                    println("5")
                    val list = response.data ?: emptyList()
                    println("6")

                    if (list.isEmpty()) {
                        println("7.1")
                        rvAppList.visibility = View.GONE
                        cardEmpty.visibility = View.VISIBLE
                    } else {
                        println("7.2")
                        rvAppList.visibility = View.VISIBLE
                        cardEmpty.visibility = View.GONE
                        adapter.updateData(list)
                    }

                    println("8")
                    adapter.updateData(list)
                    println("9")
                    swipeRefreshLayout.isRefreshing = false
                    println("10")
                } else {
                    println("11")
                    swipeRefreshLayout.isRefreshing = false
                    println("${getString(R.string.server_error)}: ${response.message}")
                }
            } catch (e: Exception) {
                swipeRefreshLayout.isRefreshing = false
                println(e.printStackTrace())
                println("${getString(R.string.connection_error)}: ${e.message}")
            }
        }
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val etIpAddress = dialogView.findViewById<EditText>(R.id.etIpAddress)
        val etPort = dialogView.findViewById<EditText>(R.id.etPort)
        val prefsIp = AppConfig.getIp(this@MainActivity)
        val prefsPort = AppConfig.getPort(this@MainActivity)

        etIpAddress.setText(prefsIp)
        etPort.setText(prefsPort)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val ip = etIpAddress.text.toString()
                val port = etPort.text.toString()

                lifecycleScope.launch {
                    val status = AppConfig.isReachable(ip, port)

                    if (status) {
                        AppConfig.saveIp(this@MainActivity, ip)
                        AppConfig.savePort(this@MainActivity, port)

                        findViewById<View>(R.id.statusIndicator).backgroundTintList =
                            ColorStateList.valueOf(Color.GREEN)
                        fetchData()
                    } else {
                        Toast.makeText(this@MainActivity, R.string.offline_server, Toast.LENGTH_SHORT).show()
                        validateDefaultServer()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun validateDefaultServer() {
        val prefsIp = AppConfig.getIp(this@MainActivity)
        val prefsPort = AppConfig.getPort(this@MainActivity)

        lifecycleScope.launch {
            val status = AppConfig.isReachable(prefsIp, prefsPort)

            if (status) {
                findViewById<View>(R.id.statusIndicator).backgroundTintList =
                    ColorStateList.valueOf(Color.GREEN)
                fetchData()
            } else {
                swipeRefreshLayout.isRefreshing = false
                findViewById<View>(R.id.statusIndicator).backgroundTintList =
                    ColorStateList.valueOf(Color.RED)
                rvAppList.visibility = View.GONE
                cardEmpty.visibility = View.VISIBLE
                adapter.updateData(mutableListOf())
                Toast.makeText(this@MainActivity, R.string.offline_server, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUpdateWorker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "CheckAppUpdates",
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest)

        //To Test Notification
//        val updateRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
//            .setConstraints(constraints)
//            .build()
//
//        WorkManager.getInstance(applicationContext).enqueue(updateRequest)


    }
}