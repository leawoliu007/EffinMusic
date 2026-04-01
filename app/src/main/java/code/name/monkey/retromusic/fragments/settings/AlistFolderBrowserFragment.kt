package code.name.monkey.retromusic.fragments.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.AlistFolderAdapter
import code.name.monkey.retromusic.alist.network.AlistClient
import code.name.monkey.retromusic.databinding.FragmentAlistFolderBrowserBinding
import code.name.monkey.retromusic.db.RetroDatabase
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.views.BreadCrumbLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import org.koin.android.ext.android.inject
import code.name.monkey.retromusic.repository.AlistSongRepository
import code.name.monkey.retromusic.alist.model.AlistFsListRequest
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.fragments.ReloadType

class AlistFolderBrowserFragment : AbsMainActivityFragment(R.layout.fragment_alist_folder_browser), BreadCrumbLayout.SelectionCallback {
    private var _binding: FragmentAlistFolderBrowserBinding? = null
    private val binding get() = _binding!!
    private val alistRepo: AlistSongRepository by inject()
    
    private var serverId: Long = -1
    private var currentPath: String = "/"
    private val selectedPaths = mutableSetOf<String>()
    
    private lateinit var adapter: AlistFolderAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlistFolderBrowserBinding.bind(view)
        serverId = arguments?.getLong("serverId") ?: -1

        setupRecyclerView()
        setupBreadcrumbs()
        
        binding.doneFab.setOnClickListener {
            confirmSelection()
        }

        loadPath(currentPath)
    }

    private fun setupRecyclerView() {
        adapter = AlistFolderAdapter(emptyList(), { folder ->
            // On Click: Navigate deeper
            val nextPath = if (currentPath.endsWith("/")) currentPath + folder.name else "$currentPath/${folder.name}"
            loadPath(nextPath)
        }, { folder, isChecked ->
            // On Pick: Toggle selection
            val fullPath = if (currentPath.endsWith("/")) currentPath + folder.name else "$currentPath/${folder.name}"
            if (isChecked) selectedPaths.add(fullPath)
            else selectedPaths.remove(fullPath)
            updateFab()
        }, { currentPath })
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupBreadcrumbs() {
        binding.breadCrumbs.setCallback(this)
    }

    private fun loadPath(path: String) {
        currentPath = path
        binding.progressBar.isVisible = true
        
        // Update Breadcrumbs
        binding.breadCrumbs.setActiveOrAdd(BreadCrumbLayout.Crumb(File(path)), false)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val db = RetroDatabase.getInstance(requireContext())
            val server = db.alistDao().getServerById(serverId)
            if (server != null) {
                val service = AlistClient.create(server.url)
                try {
                    val response = service.listFiles(AlistFsListRequest(path))
                    if (response.code == 200) {
                        val folders = response.data?.content?.filter { it.isDir } ?: emptyList()
                        withContext(Dispatchers.Main) {
                            adapter.updateData(folders, selectedPaths)
                            binding.progressBar.isVisible = false
                            updateFab()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("Error loading Alist: ${e.message}")
                        binding.progressBar.isVisible = false
                        updateFab()
                    }
                }
            }
        }
    }

    private fun updateFab() {
        binding.doneFab.text = "Select (${selectedPaths.size})"
        binding.doneFab.isVisible = selectedPaths.isNotEmpty()
    }

    private fun confirmSelection() {
        val bundle = Bundle()
        bundle.putStringArray("selectedPaths", selectedPaths.toTypedArray())
        bundle.putLong("serverId", serverId)
        
        // Return to AlistSettingsFragment with result
        // Since we don't have Fragment Results API easily available in this old project style, 
        // We'll just save them to DB here.
        lifecycleScope.launch(Dispatchers.IO) {
            val db = RetroDatabase.getInstance(requireContext())
            selectedPaths.forEach { path ->
                val folder = code.name.monkey.retromusic.db.AlistFolderEntity(
                    serverId = serverId,
                    remotePath = path,
                    name = path.substringAfterLast('/')
                )
                db.alistDao().insertFolder(folder)
                
                // Trigger scan for each
                alistRepo.scanFolder(serverId, path)
            }
            withContext(Dispatchers.Main) {
                libraryViewModel.forceReload(ReloadType.Playlists)
                findNavController().popBackStack()
            }
        }
    }

    override fun onCrumbSelection(crumb: BreadCrumbLayout.Crumb, index: Int) {
        loadPath(crumb.file.path)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            findNavController().popBackStack()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
