package com.example.status_saver_app
import android.app.Dialog

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.status_saver_app.databinding.ActivityPictureBinding
import java.io.File
class Picture : AppCompatActivity() {
    private lateinit var binding: ActivityPictureBinding
    private lateinit var pictureUri: Uri
    private lateinit var destPath: String
    private lateinit var filename: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setTitle(R.string.photo_status)

        val intent = intent
        destPath = intent.getStringExtra("DEST_PATH") ?: ""
        val file = intent.getStringExtra("FILE") ?: ""
        filename = intent.getStringExtra("FILENAME") ?: ""

        val imageFile = File(file)
        if (!imageFile.exists()) {
            return
        }

        pictureUri = Uri.fromFile(imageFile)
        Glide.with(this)
            .load(pictureUri)
            .error(R.drawable.error_logo)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.particularimage)

        binding.particularimage.isZoomable = true

        binding.sharebtn2.setOnClickListener {
            FileUtilsx.shareFile(this, destPath, filename, "image/*")
        }

        binding.download12.setOnClickListener {
            FileUtilsx.saveFile(this, imageFile, "SavedStatus", filename)
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_dialog)
        dialog.show()
        val button = dialog.findViewById<Button>(R.id.okbutton)
        button.setOnClickListener { dialog.dismiss() }
    }
}
