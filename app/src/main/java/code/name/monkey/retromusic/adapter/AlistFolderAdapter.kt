package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.alist.model.AlistFile
import com.google.android.material.checkbox.MaterialCheckBox

class AlistFolderAdapter(
    private var folders: List<AlistFile>,
    private val onFolderClicked: (AlistFile) -> Unit,
    private val onPickChanged: (AlistFile, Boolean) -> Unit,
    private val currentPathProvider: () -> String
) : RecyclerView.Adapter<AlistFolderAdapter.ViewHolder>() {

    private val selectedPaths = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val checkBox: MaterialCheckBox = view.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alist_folder_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.name.text = folder.name
        
        val currentPath = currentPathProvider()
        val fullPath = if (currentPath.endsWith("/")) currentPath + folder.name else "$currentPath/${folder.name}"
        
        holder.checkBox.setOnCheckedChangeListener(null) // Prevents recycling effects
        holder.checkBox.isChecked = selectedPaths.contains(fullPath)
        
        holder.itemView.setOnClickListener { onFolderClicked(folder) }
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onPickChanged(folder, isChecked)
            if (isChecked) selectedPaths.add(fullPath) 
            else selectedPaths.remove(fullPath)
        }
    }

    override fun getItemCount() = folders.size

    fun updateData(newFolders: List<AlistFile>, currentSelected: Set<String>) {
        folders = newFolders
        selectedPaths.clear()
        selectedPaths.addAll(currentSelected)
        notifyDataSetChanged()
    }
}
