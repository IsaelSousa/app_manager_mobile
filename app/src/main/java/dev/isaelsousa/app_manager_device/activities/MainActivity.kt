package dev.isaelsousa.app_manager_device.activities

import AppAdapter
import android.content.Intent
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
import dev.isaelsousa.app_manager_device.R
import dev.isaelsousa.app_manager_device.models.AppManager
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson
import dev.isaelsousa.app_manager_device.data.network.client
import dev.isaelsousa.app_manager_device.data.network.retrofit
import dev.isaelsousa.app_manager_device.data.remote.AppManagerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private val api = retrofit.create(AppManagerApi::class.java)
    private lateinit var adapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recycler();
    }

    fun recycler() {
        val rvAppList = findViewById<RecyclerView>(R.id.rvAppList)

        adapter = AppAdapter(mutableListOf()) { app ->
            executeInstall(app);
        }

        rvAppList.layoutManager = LinearLayoutManager(this)
        rvAppList.adapter = adapter

        fetchData();
    }

    private fun executeInstall(app: AppManager) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            verifyPermission()
            return
        }

        lifecycleScope.launch {
            try {
                if (app.uri.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, "Baixando APK...", Toast.LENGTH_SHORT).show()
                    val localPath = downloadApk(app.url, "${app.title}.apk")
                    app.uri = localPath

                    val resp = api.createOrUpdate(app);
                    if (resp.status) {
                        fetchData();
                    }
                } else {
                    installApk(app.uri)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun downloadApk(url: String, fileName: String): String = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Falha ao baixar arquivo")

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        response.body?.byteStream()?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return@withContext file.absolutePath
    }

    private fun installApk(path: String) {
        val file = File(path)
        if (!file.exists()) throw Exception("Arquivo não encontrado")

        val contentUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }

    private fun verifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                // Se não tem permissão, manda o usuário para a tela de configurações
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, "Por favor, autorize a instalação para continuar.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchData() {
        lifecycleScope.launch {
            try {
                val response = api.listApps()

                if (response.status) {
                    val list = response.data ?: emptyList()
                    adapter.updateData(list)
                } else {
                    println("Erro do servidor: ${response.message}")
                }
            } catch (e: Exception) {
                println("Erro de conexão: ${e.message}")
            }
        }
    }
}