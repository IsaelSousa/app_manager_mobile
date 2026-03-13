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
        val tvUri: TextView = view.findViewById(R.id.tvUri)
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val btnInstall: Button = view.findViewById(R.id.btnInstall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_manager, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]

        holder.tvTitle.text = app.title
        holder.tvVersion.text = "Versão: ${app.version}"
        holder.ivIcon.load(app.iconUri);

        executeType(holder, app, false)

        holder.btnInstall.setOnClickListener {
            executeType(holder, app, true)
        }

        holder.itemView.alpha = if (app.isDeleted) 0.5f else 1.0f
    }

    fun executeType(holder: AppViewHolder, app: AppManager, click: Boolean) {
        if (app.devices.isEmpty()) {
            holder.btnInstall.text = "Baixar"
            if (click) onInstallClick(app, DeviceActionType.Download)
        } else {
            val first = app.devices.first();
            holder.tvUri.text = first.uri

            val packageName = AppUtils.getPackageNameFromApk(context, first.uri);
            val appInstalled = AppUtils.isAppInstalled(packageName, context);
            if (appInstalled) {
                holder.btnInstall.text = "Abrir"
                if (click) onInstallClick(app, DeviceActionType.Open)
            } else if (app.version == first.version) {
                holder.btnInstall.text = "Instalar"
                if (click) onInstallClick(app, DeviceActionType.Install)
            } else {
                holder.btnInstall.text = "Atualizar"
                if (click) onInstallClick(app, DeviceActionType.Update)
            }
        }
    }

    fun updateData(list: List<AppManager>) {
        this.appList = list;
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = appList.size
}