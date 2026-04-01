package code.name.monkey.retromusic.fragments.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.AlistServerAdapter
import code.name.monkey.retromusic.adapter.AlistFolderListAdapter
import code.name.monkey.retromusic.databinding.FragmentAlistSettingsBinding
import code.name.monkey.retromusic.db.AlistDao
import code.name.monkey.retromusic.db.AlistFolderEntity
import code.name.monkey.retromusic.db.AlistServerEntity
import code.name.monkey.retromusic.db.RetroDatabase
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.repository.AlistSongRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class AlistSettingsFragment : AbsMainActivityFragment(R.layout.fragment_alist_settings) {
    private var _binding: FragmentAlistSettingsBinding? = null
    private val binding get() = _binding!!
    private val alistRepo: AlistSongRepository by inject()
    private lateinit var alistDao: AlistDao

    private lateinit var serverAdapter: AlistServerAdapter
    private lateinit var folderAdapter: AlistFolderListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlistSettingsBinding.bind(view)
        alistDao = RetroDatabase.getInstance(requireContext()).alistDao()

        mainActivity.setSupportActionBar(binding.toolbar)
        mainActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = "Alist Network Storage"

        serverAdapter = AlistServerAdapter(emptyList(), { server ->
            deleteServer(server)
        }, { server ->
            showAddFolderDialog(server)
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = serverAdapter

        folderAdapter = AlistFolderListAdapter(emptyList()) { folder ->
            deleteFolder(folder)
        }
        binding.foldersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.foldersRecyclerView.adapter = folderAdapter

        binding.addServerFab.setOnClickListener {
            showAddServerDialog()
        }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val servers = alistDao.getAllServers()
            val folders = alistDao.getAllFolders()
            
            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    binding.emptyState.isVisible = servers.isEmpty()
                    serverAdapter.updateData(servers)
                    
                    binding.emptyFoldersState.isVisible = folders.isEmpty()
                    folderAdapter.updateData(folders)
                }
            }
        }
    }

    private fun deleteServer(server: AlistServerEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            alistDao.deleteServer(server)
            alistDao.deleteSongsByServer(server.id)
            alistDao.getAllFolders().filter { it.serverId == server.id }.forEach {
                alistDao.deleteFolder(it)
            }
            loadData()
        }
    }

    private fun deleteFolder(folder: AlistFolderEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            alistDao.deleteFolder(folder)
            alistDao.deleteSongsByPath(folder.serverId, folder.remotePath)
            loadData()
        }
    }

    private fun startFullScan() {
        showToast("Scanning library...")
        lifecycleScope.launch(Dispatchers.IO) {
            val folders = alistDao.getAllFolders()
            for (folder in folders) {
                alistRepo.scanFolder(folder.serverId, folder.remotePath)
            }
            withContext(Dispatchers.Main) {
                libraryViewModel.forceReload(ReloadType.Playlists)
                showToast("Library scan complete")
            }
        }
    }

    private fun showAddFolderDialog(server: AlistServerEntity) {
        val bundle = Bundle()
        bundle.putLong("serverId", server.id)
        findNavController().navigate(R.id.action_alistSettingsFragment_to_alistFolderBrowserFragment, bundle)
    }

    private fun showAddServerDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_alist_server, null)
        val nameInput = view.findViewById<TextInputEditText>(R.id.serverName)
        val urlInput = view.findViewById<TextInputEditText>(R.id.serverUrl)
        val userInput = view.findViewById<TextInputEditText>(R.id.username)
        val passInput = view.findViewById<TextInputEditText>(R.id.password)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Alist Server")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val server = AlistServerEntity(
                    name = nameInput.text.toString(),
                    url = urlInput.text.toString(),
                    username = userInput.text.toString(),
                    password = passInput.text.toString()
                )
                saveServer(server)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveServer(server: AlistServerEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            alistDao.insertServer(server)
            loadData()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_alist_settings, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            findNavController().popBackStack()
            return true
        }
        if (menuItem.itemId == R.id.action_scan) {
            startFullScan()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
