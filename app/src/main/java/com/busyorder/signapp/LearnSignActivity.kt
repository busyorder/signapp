package com.busyorder.signapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LearnSignActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn_sign)

        setupButton(R.id.btnAlphabet, "asl_alphabet_abc")
        setupButton(R.id.btnHello, "hello")
        setupButton(R.id.btnYes, "yes")
        setupButton(R.id.btnNo, "no")
        setupButton(R.id.btnThankYou, "thank_you")
        setupButton(R.id.btnWelcome, "welcome")
        setupButton(R.id.btnPlease, "please")
        setupButton(R.id.btnSorry, "sorry")
        setupButton(R.id.btnGoodMorning, "good_morning")
        setupButton(R.id.btnGoodAfternoon, "good_afternoon")
        setupButton(R.id.btnGoodNight, "good_night")
        setupButton(R.id.btnHelp, "help")
        setupButton(R.id.btnWhat, "what")
        setupButton(R.id.btnWhere, "where")
        setupButton(R.id.btnHowAreYou, "how_are_you")
        setupButton(R.id.btnIAmSorry, "i_am_sorry")
    }

    private fun setupButton(buttonId: Int, videoName: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("VIDEO_NAME", videoName)
            startActivity(intent)
        }
    }
}
