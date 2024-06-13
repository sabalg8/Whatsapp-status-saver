package com.example.status_saver_app_all

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.status_saver_app_all.databinding.ItemLayoutBinding
import com.example.status_saver_app_all.utils.FileUtilsx
import java.io.File
import java.io.IOException

class Adapter(private val context: Context, private val filesList: ArrayList<ModelClass>) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    private val selectedItems = mutableListOf<ModelClass>()
    private var actionMode: ActionMode? = null
    private var showCheckboxes = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = filesList[position]

        if (model.uri.toString().endsWith(".mp4")) {
            holder.binding.play.visibility = View.VISIBLE
        } else {
            holder.binding.play.visibility = View.INVISIBLE
        }

        // Load the image using Glide
        Glide.with(context)
            .load(model.uri)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.binding.progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.binding.progressBar.visibility = View.GONE
                    return false
                }
            })
            .into(holder.binding.thumbnailofstatus)

        holder.binding.checkBox.visibility = if (showCheckboxes) View.VISIBLE else View.GONE
        holder.binding.checkBox.setOnCheckedChangeListener(null)
        holder.binding.checkBox.isChecked = selectedItems.contains(model)

        holder.binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(model)
            } else {
                selectedItems.remove(model)
            }

            if (selectedItems.isEmpty()) {
                actionMode?.finish()
            } else {
                actionMode?.title = "${selectedItems.size} selected"
            }
        }

        holder.itemView.setOnLongClickListener {
            if (actionMode == null) {
                showCheckboxes = true
                notifyDataSetChanged()
                actionMode = (context as AppCompatActivity).startSupportActionMode(actionModeCallback)
            }

            holder.binding.checkBox.isChecked = true
            true
        }

        holder.itemView.setOnClickListener {
            if (actionMode != null) {
                holder.binding.checkBox.isChecked = !holder.binding.checkBox.isChecked
            } else {
                openStatus(model)
            }
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: android.view.Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu2, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: android.view.Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: android.view.MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_select_all -> {
                    selectAllItems()
                    true
                }
                R.id.action_delete -> {
                    deleteSelectedStatuses()
                    true
                }
                R.id.action_save -> {
                    saveSelectedStatuses()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            selectedItems.clear()
            showCheckboxes = false
            notifyDataSetChanged()
            actionMode = null
        }
    }

    private fun selectAllItems() {
        selectedItems.clear()
        selectedItems.addAll(filesList)
        notifyDataSetChanged()
        actionMode?.title = "${selectedItems.size} selected"
    }

    private fun deleteSelectedStatuses() {
        selectedItems.forEach { model ->
            val file = File(model.path)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    val position = filesList.indexOf(model)
                    filesList.remove(model)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, filesList.size)
                }
            }
        }
        selectedItems.clear()
        Toast.makeText(context, "Selected statuses deleted", Toast.LENGTH_SHORT).show()
        actionMode?.finish()
    }

    private fun saveSelectedStatuses() {
        SaveStatusAsyncTask().execute(*selectedItems.toTypedArray())
    }
    private inner class SaveStatusAsyncTask : AsyncTask<ModelClass, Void, Boolean>() {
        override fun doInBackground(vararg models: ModelClass): Boolean {
            models.forEach { model ->
                val sourceFile = File(model.path)
                val destPath = Environment.getExternalStorageDirectory().absolutePath + Constant.SAVE_FOLDER
                val destDir = File(destPath)

                if (!destDir.exists()) {
                    destDir.mkdirs()
                }

                val destFileName = model.filename
                val destFile = File(destDir, destFileName)

                try {
                    FileUtilsx.saveFile(context, sourceFile, destDir.absolutePath, destFileName)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return false
                }
            }
            return true
        }

        override fun onPostExecute(success: Boolean) {
            if (success) {
                Toast.makeText(context, "Selected statuses saved", Toast.LENGTH_SHORT).show()
                NotificationUtils.showNotification(
                    context,
                    "Status Saver",
                    "All selected statuses were saved successfully!"
                )
            } else {
                Toast.makeText(context, "Failed to save selected statuses", Toast.LENGTH_SHORT).show()
            }
            selectedItems.clear()
            actionMode?.finish()
        }
    }


    private fun openStatus(model: ModelClass) {
        if (model.uri.toString().endsWith(".mp4")) {
            playVideo(model)
        } else {
            openImageStatus(model)
        }
    }

    private fun openImageStatus(model: ModelClass) {
        val path = model.path
        val destPath = Environment.getExternalStorageDirectory().absolutePath + "/" + Constant.SAVE_FOLDER

        val intent = Intent(context, Picture::class.java)
        intent.putExtra("DEST_PATH", destPath)
        intent.putExtra("FILE", path)
        intent.putExtra("FILENAME", model.filename)
        context.startActivity(intent)
    }

    private fun playVideo(model: ModelClass) {
        val path = model.path
        val destPath = Environment.getExternalStorageDirectory().absolutePath + Constant.SAVE_FOLDER
        val intent = Intent(context, Video::class.java)
        intent.putExtra("DEST_PATH_VIDEO", destPath)
        intent.putExtra("FILE_VIDEO", path)
        intent.putExtra("FILENAME_VIDEO", model.filename)
        intent.putExtra("URI_VIDEO", model.uri.toString())
        context.startActivity(intent)
    }

    override fun getItemCount(): Int {
        return filesList.size
    }

    fun addItem(item: ModelClass) {
        filesList.add(item)
        notifyItemInserted(filesList.size - 1)
    }

    inner class ViewHolder(val binding: ItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)
}
