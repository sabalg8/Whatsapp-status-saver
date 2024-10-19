package com.example.status_saver_app

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
//handles downloading and sharing opearation
object FileUtilsx {

    fun saveFile(context: Context, sourceFile: File, destFolderName: String, filename: String): File? {
        val saveFolder = File(Environment.getExternalStorageDirectory(), destFolderName)
        if (!saveFolder.exists()) {
            saveFolder.mkdirs()
        }
        val destFile = File(saveFolder, filename)
        try {
            val input = FileInputStream(sourceFile)
            val output = FileOutputStream(destFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (input.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
            input.close()
            output.close()
            // Scan the saved file for media
            MediaScannerConnection.scanFile(context, arrayOf(destFile.path), null, null)
            return destFile
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    fun shareFile(context: Context, destPath: String, filename: String, fileType: String) {
        val file = File(destPath, filename)
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = fileType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, "Check out this $fileType!")
            putExtra(Intent.EXTRA_SUBJECT, "Subject")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show()
        }
    }
}