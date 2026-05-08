package com.busyorder.signapp

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.util.*
import kotlin.math.abs

class UploadActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // UI
    private lateinit var imageBtn: Button
    private lateinit var videoBtn: Button
    private lateinit var englishBtn: Button
    private lateinit var tamilBtn: Button
    private lateinit var imagePreview: ImageView
    private lateinit var videoView: VideoView
    private lateinit var progress: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var accuracyText: TextView

    private var currentLanguage = Locale.ENGLISH

    // Gesture counters
    private var yesFrames = 0
    private var thanksFrames = 0
    private var helloFrames = 0
    private var noFrames = 0
    private var sorryFrames = 0
    private var pleaseFrames = 0
    private var cooldownFrames = 0

    // ===== Motion smoothing (EMA) =====
    private var smoothedMotion = 0f
    private val motionAlpha = 0.6f   // 0.5–0.7 is ideal for video


    // Helpers
    private lateinit var tts: TextToSpeech
    private lateinit var handHelper: HandLandmarkHelper

    private var lastSpokenGesture = ""
    private var speakTamil = false

    private val storageRef = FirebaseStorage.getInstance().reference

    // ================= PICKERS =================

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                showImage(it)
                uploadToFirebase(it)
                detectFromImage(it)
            }
        }

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                showVideo(it)
                uploadToFirebase(it)
                detectFromVideo(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        imageBtn = findViewById(R.id.imageBtn)
        videoBtn = findViewById(R.id.videoBtn)
        englishBtn = findViewById(R.id.englishBtn)
        tamilBtn = findViewById(R.id.tamilBtn)
        imagePreview = findViewById(R.id.imagePreview)
        videoView = findViewById(R.id.videoView)
        progress = findViewById(R.id.uploadProgress)
        resultText = findViewById(R.id.resultText)
        accuracyText = findViewById(R.id.accuracyText)

        tts = TextToSpeech(this, this)
        handHelper = HandLandmarkHelper(this)

        imageBtn.setOnClickListener { imagePicker.launch("image/*") }
        videoBtn.setOnClickListener { videoPicker.launch("video/*") }

        englishBtn.setOnClickListener {
            speakTamil = false
            setTtsLanguage()
            updateLangUI()
        }

        tamilBtn.setOnClickListener {
            speakTamil = true
            setTtsLanguage()
            updateLangUI()
        }

        updateLangUI()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) setTtsLanguage()
    }

    private fun setTtsLanguage() {
        val locale = if (speakTamil) Locale("ta", "IN") else Locale.ENGLISH
        tts.language = locale
    }

    private fun updateLangUI() {
        if (speakTamil) {
            tamilBtn.setBackgroundResource(R.drawable.btn_purple_gradient)
            tamilBtn.setTextColor(getColor(android.R.color.white))
            englishBtn.setBackgroundResource(R.drawable.btn_outline)
            englishBtn.setTextColor(getColor(R.color.purple_500))
        } else {
            englishBtn.setBackgroundResource(R.drawable.btn_purple_gradient)
            englishBtn.setTextColor(getColor(android.R.color.white))
            tamilBtn.setBackgroundResource(R.drawable.btn_outline)
            tamilBtn.setTextColor(getColor(R.color.purple_500))
        }
    }

    // ================= PREVIEW =================

    private fun showImage(uri: Uri) {
        imagePreview.visibility = View.VISIBLE
        videoView.visibility = View.GONE
        imagePreview.setImageURI(uri)
    }

    private fun showVideo(uri: Uri) {
        imagePreview.visibility = View.GONE
        videoView.visibility = View.VISIBLE
        videoView.setVideoURI(uri)
        videoView.start()
    }

    private fun uploadToFirebase(uri: Uri) {
        progress.visibility = View.VISIBLE
        storageRef.child("uploads/${System.currentTimeMillis()}")
            .putFile(uri)
            .addOnCompleteListener {
                progress.visibility = View.GONE
            }
    }

    // ================= IMAGE =================

    private fun detectFromImage(uri: Uri) {
        val bitmap =
            android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)

        val lm = handHelper.detect(bitmap)
        if (lm == null) {
            showResult("No hand detected", 0)
            return
        }

        val gesture = detectStaticGesture(lm)
        showResult(gesture ?: "Gesture unclear", if (gesture == null) 0 else 100)
    }

    private fun detectStaticGesture(lm: List<NormalizedLandmark>): String? {

        val wrist = lm[0]
        val index = lm[8]
        val middle = lm[12]
        val thumb = lm[4]

        val wristY = wrist.y()
        val dx = abs(index.x() - wrist.x())
        val palmFlat = abs(index.y() - middle.y()) < 0.03f
        val thumbUp = thumb.y() < wrist.y() - 0.08f

        val open = abs(index.x() - middle.x()) > 0.04f
        val closed = abs(index.x() - middle.x()) < 0.03f

        return when {
            wristY < 0.35f && dx > 0.15f -> "Hello"
            wristY in 0.38f..0.55f && dx < 0.12f -> "Yes"
            thumbUp && wristY > 0.45f -> "Thanks"

            /* ===== PLEASE ===== */
            open && wrist.y() in 0.50f..0.65f ->
                "Please"

            /* ===== SORRY ===== */
            closed && wrist.y() in 0.45f..0.65f ->
                "Sorry"

            /* ===== GOOD MORNING ===== */
            open && wrist.y() in 0.60f..0.75f ->
                "Good Morning"

            else -> null
        }
    }

    // ================= VIDEO =================

    private fun detectFromVideo(uri: Uri) {

        progress.visibility = View.VISIBLE

        Thread {
            try {
                val retriever = MediaMetadataRetriever()

                contentResolver.openFileDescriptor(uri, "r")?.use {
                    retriever.setDataSource(it.fileDescriptor)
                } ?: throw IllegalStateException("Cannot open video")

                val durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLong() ?: 0L

                val rotation =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                    )?.toInt() ?: 0

                val samples = 12
                val votes = mutableMapOf<String, Int>()
                var lastWristY: Float? = null
                var successFrames = 0

                for (i in 1..samples) {

                    val timeUs = (durationMs * i / (samples + 1)) * 1000

                    val rawFrame = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: continue

                    val frame = Bitmap.createBitmap(
                        rawFrame,
                        0,
                        0,
                        rawFrame.width,
                        rawFrame.height,
                        Matrix().apply {
                            if (rotation != 0) postRotate(rotation.toFloat())
                        },
                        true
                    )
                    rawFrame.recycle()

                    val safeFrame = toARGB8888(frame)
                    val lm = handHelper.detect(safeFrame) ?: continue

                    successFrames++

                    val wristY = lm[0].y()

                    if (lastWristY != null) {
                        val rawMotion = wristY - lastWristY!!
                        smoothedMotion =
                            motionAlpha * rawMotion + (1 - motionAlpha) * smoothedMotion

                        val gesture = detectMotionGesture(lm)
                        if (gesture != null) {
                            votes[gesture] = (votes[gesture] ?: 0) + 1
                        }
                    }

                    lastWristY = wristY   // ✅ MUST BE INSIDE LOOP
                }

                retriever.release()

                runOnUiThread {
                    progress.visibility = View.GONE

                    if (successFrames < 2 || votes.isEmpty()) {
                        showResult("Gesture unclear", 0)
                        return@runOnUiThread
                    }

                    val best = votes.maxByOrNull { it.value }!!
                    val confidence =
                        ((best.value * 100) / votes.values.sum()).coerceAtMost(100)

                    if (confidence < 30) {
                        showResult("Gesture unclear", confidence)
                    } else {
                        showResult(best.key, confidence)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progress.visibility = View.GONE
                    Toast.makeText(this, "Video error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun toARGB8888(src: Bitmap): Bitmap {
        return if (src.config == Bitmap.Config.ARGB_8888)
            src else src.copy(Bitmap.Config.ARGB_8888, false)
    }

    // ================= MOTION =================
    private fun detectMotionGesture(
        lm: List<NormalizedLandmark>
    ): String? {

        if (cooldownFrames-- > 0) return null

        val wrist = lm[0]
        val index = lm[8]
        val middle = lm[12]

        val wristY = wrist.y()

        val open =
            index.y() < wristY &&
                    middle.y() < wristY

        val closed =
            index.y() > wristY - 0.05f &&
                    middle.y() > wristY - 0.05f

        /* ===== Yes ===== */
        if (open && wristY < 0.35f) {
            yesFrames++
            if (yesFrames >= 2) {
                return reset("Yes")
            }
        } else yesFrames = 0

        /* ===== THANKS ===== */
        if (open && wristY in 0.35f..0.55f) {
            thanksFrames++
            if (thanksFrames >= 2) {
                return reset("Thanks")
            }
        } else thanksFrames = 0

        /* ===== Hello ===== */
        if (closed && wristY in 0.40f..0.65f) {
            helloFrames++
            if (helloFrames >= 2) {
                return reset("Hello")
            }
        } else helloFrames = 0

        /* ===== NO ===== */
        if (closed && wristY > 0.60f) {
            noFrames++
            if (noFrames >= 2) {
                return reset("No")
            }
        } else noFrames = 0

        /* ===== SORRY ===== */
        if (closed && wristY in 0.45f..0.60f) {
            sorryFrames++
            if (sorryFrames >= 2) {
                return reset("Sorry")
            }
        } else sorryFrames = 0

        /* ===== PLEASE ===== */
        if (open && wristY in 0.45f..0.65f) {
            pleaseFrames++
            if (pleaseFrames >= 2) {
                return reset("Please")
            }
        } else pleaseFrames = 0

        return null

    }


    private fun reset(result: String): String {
    yesFrames = 0
    thanksFrames = 0
    helloFrames = 0
    cooldownFrames = 6
    smoothedMotion = 0f   // 🔑 RESET EMA
    return result
}


// ================= UI + TTS =================
private fun showResult(gesture: String, accuracy: Int) {

    // Convert for display
    val displayText =
        if (speakTamil) translateTamil(gesture)
        else gesture

    resultText.text = "Detected: $displayText"
    accuracyText.text = "Accuracy: $accuracy%"

    if (accuracy >= 30 && gesture != lastSpokenGesture) {

        lastSpokenGesture = gesture

        val speakText =
            if (speakTamil) translateTamil(gesture)
            else gesture

        tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}

    private fun translateTamil(text: String): String {
        return when (text.trim().lowercase()) {

            "hello" -> "வணக்கம்"

            "yes" -> "ஆம்"

            "no" -> "இல்லை"

            "thanks" -> "நன்றி"

            "please" -> "தயவு செய்து"

            "sorry" -> "மன்னிக்கவும்"

            "good morning" -> "காலை வணக்கம்"

            else -> text
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
