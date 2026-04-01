package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.db.AlistFolderEntity

class AlistFolderListAdapter(
    private var folders: List<AlistFolderEntity>,
    private val onDeleteClicked: (AlistFolderEntity) -> Unit
) : RecyclerView.Adapter<AlistFolderListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.title)
        val path: TextView = view.findViewById(R.id.text)
        val icon: ImageView = view.findViewById(R.id.image)
        val deleteBtn: View = view.findViewById(R.id.menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.name.text = folder.name
        holder.path.text = folder.remotePath
        holder.icon.setImageResource(R.drawable.ic_folder)
        (holder.deleteBtn as? ImageView)?.setImageResource(R.drawable.ic_delete)
        holder.deleteBtn.setOnClickListener { onDeleteClicked(folder) }
    }

    override fun getItemCount() = folders.size

    fun updateData(newFolders: List<AlistFolderEntity>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}
