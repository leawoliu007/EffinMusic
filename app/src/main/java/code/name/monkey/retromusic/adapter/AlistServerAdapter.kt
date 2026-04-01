package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.db.AlistServerEntity

class AlistServerAdapter(
    private var servers: List<AlistServerEntity>,
    private val onDeleteClicked: (AlistServerEntity) -> Unit,
    private val onAddFolderClicked: (AlistServerEntity) -> Unit
) : RecyclerView.Adapter<AlistServerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.title)
        val url: TextView = view.findViewById(R.id.text)
        val image: android.widget.ImageView = view.findViewById(R.id.image)
        val deleteBtn: View = view.findViewById(R.id.menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val server = servers[position]
        holder.name.text = server.name
        holder.url.text = server.url
        holder.image.setImageResource(R.drawable.ic_cloud)
        (holder.deleteBtn as? android.widget.ImageView)?.setImageResource(R.drawable.ic_delete)
        holder.deleteBtn.setOnClickListener { onDeleteClicked(server) }
        holder.itemView.setOnClickListener { onAddFolderClicked(server) }
    }

    override fun getItemCount() = servers.size

    fun updateData(newServers: List<AlistServerEntity>) {
        servers = newServers
        notifyDataSetChanged()
    }
}
