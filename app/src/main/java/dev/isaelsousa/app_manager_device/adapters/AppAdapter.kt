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

class AppAdapter(
    private var appList: List<AppManager>,
    private val onInstallClick: (AppManager) -> Unit
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

        holder.btnInstall.setOnClickListener {
            onInstallClick(app)
        }

//        if (app.uri.length == 0) {
//            holder.btnInstall.text = "Baixar"
//        } else {
//            holder.btnInstall.text = "Instalar"
//        }

        holder.itemView.alpha = if (app.isDeleted) 0.5f else 1.0f
    }

    fun updateData(list: List<AppManager>) {
        this.appList = list;
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = appList.size
}