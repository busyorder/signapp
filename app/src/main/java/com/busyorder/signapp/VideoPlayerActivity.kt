package com.busyorder.signapp

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoView = findViewById(R.id.videoView)

        val videoName = intent.getStringExtra("VIDEO_NAME")

        if (videoName.isNullOrEmpty()) {
            Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val videoResId = resources.getIdentifier(
            videoName,
            "raw",
            packageName
        )

        if (videoResId == 0) {
            Toast.makeText(this, "Video file missing in res/raw", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val uri = Uri.parse("android.resource://$packageName/$videoResId")

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(uri)

        videoView.setOnPreparedListener {
            videoView.start()
        }

        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
    }
}
