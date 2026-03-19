package br.com.sequor.launcher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import br.com.sequor.launcher.R

class AppUtils {
    companion object {
        fun isAppInstalled(packageName: String?, context: Context): Boolean {
            if (packageName.isNullOrBlank()) return false

            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(packageName, 0)
                }
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun getPackageNameFromApk(context: Context, apkPath: String): String? {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
            return packageInfo?.packageName
        }

        fun openApp(packageName: String, context: Context) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, R.string.could_not_open_the_app, Toast.LENGTH_SHORT).show()
            }
        }
    }
}