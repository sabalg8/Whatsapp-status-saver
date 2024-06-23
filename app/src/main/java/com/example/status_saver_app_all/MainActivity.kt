package com.example.status_saver_app_all

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.status_saver_app_all.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val BACK_PRESS_TIME_INTERVAL = 2000
        private const val PREFS_NAME = "PREFS_NAME"
        private const val FIRST_TIME_KEY = "FIRST_TIME_KEY"
        private const val REQUEST_STORAGE_PERMISSION = 1
        private const val PAGE_SIZE = 20
        private const val BUSINESS_STATUS_FOLDER_NAME = "Business Statuses"
    }

    private var backPressedOnce = false
    private lateinit var fileObserver: FileObserver
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Adapter
    private var allStatusFiles: MutableList<File> = ArrayList()
    private lateinit var statusPath: String
    private lateinit var businessStatusPath: String
    private lateinit var actionBar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFileObserver()
        val settings = getSharedPreferences(PREFS_NAME, 0)
        val isFirstTime = settings.getBoolean(FIRST_TIME_KEY, true)
        if (isFirstTime) {
            showHowToUseDialog()
            val editor = settings.edit()
            editor.putBoolean(FIRST_TIME_KEY, false)
            editor.apply()
        }

        actionBar = supportActionBar!!
        actionBar.title = getString(R.string.app_name)
        binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        adapter = Adapter(this, ArrayList())
        binding.recyclerView.adapter = adapter
        binding.howToUse.setOnClickListener { showHowToUseDialog() }
        binding.swipe.setOnRefreshListener {
            binding.swipe.isRefreshing = true
            Handler(Looper.getMainLooper()).postDelayed({
                binding.swipe.isRefreshing = false
                setupLayout()
            }, 1000)
        }

        checkStoragePermission()
        NotificationUtils.createNotificationChannel(this)
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            setupLayout()
        } else {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLayout()
            } else {
                showToast("Permission denied. App cannot function without storage permission.")
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.permission_explanation_dialog, null)
        val descriptionTextView = dialogView.findViewById<TextView>(R.id.text_permission_description)
        val openSettingsButton = dialogView.findViewById<Button>(R.id.btn_open_settings)
        descriptionTextView.text = getString(R.string.why_dialogue)
        openSettingsButton.setOnClickListener { openAppSettings() }
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()
    }

    private fun showStoragePermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
        builder.setMessage("This app needs access to manage all files on your device.")
        builder.setPositiveButton("Grant") { _, _ ->
            requestStoragePermission()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        fileObserver.stopWatching()
    }

    override fun onResume() {
        super.onResume()
        fileObserver.startWatching()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadStatusFiles()
        }
    }

    private fun setupFileObserver() {
        statusPath = getStatusPath()
        businessStatusPath = getBusinessStatusPath()
        Thread {
            fileObserver = object : FileObserver(statusPath, CREATE or DELETE) {
                override fun onEvent(event: Int, path: String?) {
                    if (event == CREATE || event == DELETE) {
                        runOnUiThread { loadStatusFiles() }
                    }
                }
            }
            fileObserver.startWatching()

            val businessFileObserver = object : FileObserver(businessStatusPath, CREATE or DELETE) {
                override fun onEvent(event: Int, path: String?) {
                    if (event == CREATE || event == DELETE) {
                        runOnUiThread { loadStatusFiles() }
                    }
                }
            }
            businessFileObserver.startWatching()
        }.start()
    }

    private fun loadStatusFiles() {
        CoroutineScope(Dispatchers.Main).launch {
            val newStatusFiles = withContext(Dispatchers.IO) {
                val personalStatusFiles = getStatusFiles(statusPath)
                val businessStatusFiles = getStatusFiles(businessStatusPath)
                val allFiles = mutableListOf<File>()
                personalStatusFiles?.let { allFiles.addAll(it) }
                businessStatusFiles?.let { allFiles.addAll(it) }
                allFiles.filterNot { it.isDirectory || it.name.endsWith(".nomedia") }.toMutableList()
            }

            allStatusFiles.clear()
            allStatusFiles.addAll(newStatusFiles)
            val startIndex = adapter.itemCount
            val endIndex = (startIndex + PAGE_SIZE).coerceAtMost(allStatusFiles.size)
            for (i in startIndex until endIndex) {
                val file = allStatusFiles[i]
                val modelClass = ModelClass().apply {
                    uri = Uri.fromFile(file)
                    path = file.absolutePath
                    filename = file.name
                }
                adapter.addItem(modelClass)
            }
            adapter.notifyDataSetChanged()
            if (startIndex == 0 && adapter.itemCount > 0) {
                binding.idTVMsg.visibility = View.GONE
                binding.howToUse.visibility = View.GONE
            } else {
                binding.idTVMsg.visibility = View.VISIBLE
                binding.howToUse.visibility = View.VISIBLE
            }
            updateStatusCount()
        }
    }

    private fun updateStatusCount() {
        val statsFound = getString(R.string.status_found_text)
        actionBar.subtitle = "$statsFound${allStatusFiles.size}"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupLayout() {
        val spanCount = 2
        val staggeredGridLayoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.layoutManager = staggeredGridLayoutManager
        loadStatusFiles()
    }

    private fun getStatusFiles(path: String): Array<File>? {
        val statusFolder = File(path)
        return if (statusFolder.exists() && statusFolder.isDirectory) {
            statusFolder.listFiles { file ->
                !file.isDirectory && !file.name.endsWith(".nomedia")
            }
        } else {
            null
        }
    }

    private fun getStatusPath(): String {
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val statusFolder = File(externalStorageDir, Constant.STATUS_FOLDER_NAME)
        return statusFolder.absolutePath
    }

    private fun getBusinessStatusPath(): String {
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val businessStatusFolder = File(externalStorageDir, BUSINESS_STATUS_FOLDER_NAME)
        return businessStatusFolder.absolutePath
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                openSettings()
                return true
            }
            R.id.how_use -> {
                showHowToUseDialog()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun showHowToUseDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.how_to_use, null)
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onBackPressed() {
        if (backPressedOnce) {
            super.onBackPressed()
            return
        }
        this.backPressedOnce = true
        showToast("Press back again to exit")
        Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, BACK_PRESS_TIME_INTERVAL.toLong())
    }
}
