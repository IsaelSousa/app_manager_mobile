import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import dev.isaelsousa.app_manager_device.R
import dev.isaelsousa.app_manager_device.models.AppManager
import dev.isaelsousa.app_manager_device.models.DeviceActionType
import dev.isaelsousa.app_manager_device.utils.AppUtils

class AppAdapter(
    private var appList: List<AppManager>,
    private val context: Context,
    private val onInstallClick: (AppManager, type: DeviceActionType) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvVersion: TextView = view.findViewById(R.id.tvVersion)
        val tvDeviceVersion: TextView = view.findViewById(R.id.tvDeviceVersion)
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val btnInstall: Button = view.findViewById(R.id.btnInstall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_manager, parent, false)
        return AppViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]

        holder.tvTitle.text = app.title
        holder.tvVersion.text = "${context.getString(R.string.version)}: ${app.version}"
        holder.ivIcon.load(app.iconUri);

        executeType(holder, app, false)

        holder.btnInstall.setOnClickListener {
            executeType(holder, app, true)
        }

        holder.itemView.alpha = if (app.isDeleted) 0.5f else 1.0f
    }

    @SuppressLint("SetTextI18n")
    fun executeType(holder: AppViewHolder, app: AppManager, click: Boolean) {
        if (app.devices.isEmpty()) {
            holder.btnInstall.text = context.getString(R.string.download)
            if (click) onInstallClick(app, DeviceActionType.Download)
        } else {
            val first = app.devices.first();
            holder.tvDeviceVersion.text = "${context.getString(R.string.device_version)}: ${first.version}"

            val packageName = AppUtils.getPackageNameFromApk(context, first.uri);
            val appInstalled = AppUtils.isAppInstalled(packageName, context);

            if (appInstalled && app.version != first.version) {
                holder.btnInstall.text = context.getString(R.string.update)
                if (click) onInstallClick(app, DeviceActionType.Update)
            } else if (appInstalled) {
                holder.btnInstall.text = context.getString(R.string.open)
                if (click) onInstallClick(app, DeviceActionType.Open)
            } else if (app.version == first.version) {
                holder.btnInstall.text = context.getString(R.string.install)
                if (click) onInstallClick(app, DeviceActionType.Install)
            } else {
                holder.btnInstall.text = context.getString(R.string.update)
                if (click) onInstallClick(app, DeviceActionType.Update)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(list: List<AppManager>) {
        this.appList = list;
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = appList.size
}