package com.example.status_saver_app_all
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.status_saver_app_all.databinding.ActivityVideoBinding
import com.example.status_saver_app_all.utils.FileUtilsx
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import java.io.File


class Video : AppCompatActivity() {
    private lateinit var binding: ActivityVideoBinding
    private lateinit var destPath: String
    private lateinit var filename: String
    private lateinit var file: File
    private lateinit var player: SimpleExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setTitle(R.string.video_status)
        setupViews()
        binding.download12.setOnClickListener {
            if (file.exists()) {
                FileUtilsx.saveFile(this, file, "SavedStatus", filename)
                Toast.makeText(this, "Video Downloaded in $destPath", Toast.LENGTH_SHORT).show()
            }
        }
        binding.sharebtn2.setOnClickListener {
            if (file.exists()) {
                FileUtilsx.shareFile(this, destPath, filename, "video/*")
            }
        }
    }

    private fun setupViews() {
        destPath = intent.getStringExtra("DEST_PATH_VIDEO")!!
        val filePath = intent.getStringExtra("FILE_VIDEO")!!
        val uri = intent.getStringExtra("URI_VIDEO")!!
        filename = intent.getStringExtra("FILENAME_VIDEO")!!
        file = File(filePath)
        val playerView = findViewById<PlayerView>(R.id.playerView)
        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player
        val mediaItem = MediaItem.fromUri(Uri.parse(uri))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
