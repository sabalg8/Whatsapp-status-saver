package com.example.status_saver_app
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.status_saver_app.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var fileObserver: FileObserver
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Adapter
    private var allStatusFiles: MutableList<File> = ArrayList()
    private lateinit var statusPath: String
    private lateinit var actionBar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFileObserver()
        val settings = getSharedPreferences(OBJ.PREFS_NAME, 0)
        val isFirstTime = settings.getBoolean(OBJ.FIRST_TIME_KEY, true)
        if (isFirstTime) {
            showHowToUseDialog()
            val editor = settings.edit()
            editor.putBoolean(OBJ.FIRST_TIME_KEY, false)
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

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                setupLayout()
            } else {
                requestStoragePermission()
            }
        }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            OBJ.REQUEST_STORAGE_PERMISSION
        )
    }




    override fun onPause() {
        super.onPause()
        fileObserver.stopWatching()
    }

    override fun onResume() {
        super.onResume()
        fileObserver.startWatching()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadStatusFiles()
        }
    }

    private fun setupFileObserver() {
        statusPath = getStatusPath()
        Thread {
            fileObserver = object : FileObserver(statusPath, CREATE or DELETE) {
                override fun onEvent(event: Int, path: String?) {
                    if (event == CREATE || event == DELETE) {
                        runOnUiThread { loadStatusFiles() }
                    }
                }
            }
            fileObserver.startWatching()


        }.start()
    }


    private fun loadStatusFiles() {
        CoroutineScope(Dispatchers.Main).launch {
            val newStatusFiles = withContext(Dispatchers.IO) {
                val personalStatusFiles = getStatusFiles(statusPath)
                val allFiles = mutableListOf<File>()
                personalStatusFiles?.let { allFiles.addAll(it) }
                allFiles.filterNot { it.isDirectory || it.name.endsWith(".nomedia") }
                    .toMutableList()
            }

            allStatusFiles.clear()
            allStatusFiles.addAll(newStatusFiles)
            val startIndex = adapter.itemCount
            val endIndex = (startIndex + Constant.PAGE_SIZE).coerceAtMost(allStatusFiles.size)
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
            if (adapter.itemCount > 0) {
                binding.idTVMsg.visibility = View.GONE
                binding.howToUse.visibility = View.GONE
            } else {
                binding.idTVMsg.visibility = View.VISIBLE
                binding.howToUse.visibility = View.VISIBLE
            }
        }
    }


    private fun setupLayout() {
        val spanCount = 2
        val staggeredGridLayoutManager =
            StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
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
        val whatsappDir = File(externalStorageDir, Constant.STATUS_FOLDER_NAME)
        return whatsappDir.absolutePath
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
        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()

        // Change the positive button color
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.divider_color))

    }

}