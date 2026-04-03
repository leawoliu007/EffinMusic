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
    private val onScanClicked: (AlistFolderEntity) -> Unit,
    private val onDeleteClicked: (AlistFolderEntity) -> Unit
) : RecyclerView.Adapter<AlistFolderListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.title)
        val path: TextView = view.findViewById(R.id.text)
        val scanBtn: View = view.findViewById(R.id.scan_btn)
        val deleteBtn: View = view.findViewById(R.id.delete_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alist_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.name.text = folder.name
        holder.path.text = folder.remotePath
        
        holder.scanBtn.setOnClickListener { onScanClicked(folder) }
        holder.deleteBtn.setOnClickListener { onDeleteClicked(folder) }
    }

    override fun getItemCount() = folders.size

    fun updateData(newFolders: List<AlistFolderEntity>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}
