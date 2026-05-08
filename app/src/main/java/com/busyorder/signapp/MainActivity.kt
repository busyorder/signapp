package com.busyorder.signapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Size
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.busyorder.signapp.utils.GestureStorage
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var result: TextView
    private lateinit var accuracy: TextView
    private lateinit var upload: Button
    private lateinit var speak: Button
    private lateinit var tamil: Button
    private lateinit var english: Button
    private lateinit var logout: Button
    private lateinit var previewView: PreviewView

    // System
    private lateinit var tts: TextToSpeech
    private lateinit var auth: FirebaseAuth
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var handHelper: HandLandmarkHelper


    // State
    private var currentLanguage = Locale.ENGLISH
    private var lastSpokenGesture = ""
    private var lastSpeakTime = 0L

    // Motion frames
    private var prevGray: Bitmap? = null

    // Gesture phase
    private var gesturePhase: String? = null
    private var phaseTime = 0L

    companion object {
        private const val MIN_HAND_PIXELS = 90
        private const val MOTION_THRESHOLD = 30
        private const val PHASE_TIMEOUT = 1200L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        handHelper = HandLandmarkHelper(this)

        setContentView(R.layout.activity_main)

        result = findViewById(R.id.resultText)
        accuracy = findViewById(R.id.accuracyText)
        upload = findViewById(R.id.uploadBtn)
        speak = findViewById(R.id.speakBtn)
        tamil = findViewById(R.id.tamilBtn)
        english = findViewById(R.id.englishBtn)
        logout = findViewById(R.id.logoutBtn)
        previewView = findViewById(R.id.previewView)

        result.text = "Detected: -"
        accuracy.text = "Accuracy: -"

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = currentLanguage
            }
        }

        speak.setOnClickListener {
            val text = result.text.toString().replace("Detected: ", "")
            if (text != "-" && text.isNotBlank()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        tamil.setOnClickListener {
            currentLanguage = Locale("ta", "IN")
            Toast.makeText(this, "மொழி: தமிழ்", Toast.LENGTH_SHORT).show()
        }

        english.setOnClickListener {
            currentLanguage = Locale.ENGLISH
            Toast.makeText(this, "Language: English", Toast.LENGTH_SHORT).show()
        }

        upload.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }

        logout.setOnClickListener {
            auth.signOut()
            val i = Intent(this, LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }

        val historyBtn = findViewById<Button>(R.id.historyBtn)
        val learnBtn = findViewById<Button>(R.id.learnBtn)

        historyBtn.setOnClickListener {
            startActivity(Intent(this, GestureHistoryActivity::class.java))
        }

        learnBtn.setOnClickListener {
            startActivity(Intent(this, LearnSignActivity::class.java))
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            startCamera()
        }
    }

    // ================= CAMERA =================

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(320, 240))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                try {
                    processImage(imageProxy)
                } finally {
                    imageProxy.close()
                }
            }


            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    // ================= CORE LOGIC =================

    private fun processFrame(image: ImageProxy) {

        var bmp = image.toBitmap() ?: return

        // Un-mirror front camera
        bmp = Bitmap.createBitmap(
            bmp, 0, 0, bmp.width, bmp.height,
            Matrix().apply { preScale(-1f, 1f) }, true
        )

        val gray = toGray(bmp)

        if (prevGray == null) {
            prevGray = gray
            return
        }

        val diff = frameDiff(prevGray!!, gray)
        prevGray = gray

        val motion = analyzeMotion(diff)
        if (!motion.valid) {
            gesturePhase = null
            return
        }

        val now = System.currentTimeMillis()

        // HELLO → horizontal near face
        if (motion.region == "UPPER" && abs(motion.dx) > MOTION_THRESHOLD) {
            speakGesture("Hello")
            return
        }

        // YES → upward
        if (motion.dy < -MOTION_THRESHOLD) {
            speakGesture("Yes")
            return
        }

        // THANKS → face → down
        if (motion.region == "UPPER" && motion.dy > MOTION_THRESHOLD) {
            speakGesture("Thanks")
            return
        }

        // GOOD MORNING → down then up
        if (motion.dy > MOTION_THRESHOLD) {
            gesturePhase = "DOWN"
            phaseTime = now
            return
        }

        if (
            gesturePhase == "DOWN" &&
            now - phaseTime < PHASE_TIMEOUT &&
            motion.dy < -MOTION_THRESHOLD
        ) {
            gesturePhase = null
            speakGesture("Good Morning")
        }
    }

    // ================= SPEECH =================

    private fun speakGesture(gesture: String) {

        val now = System.currentTimeMillis()

        if (gesture == lastSpokenGesture && now - lastSpeakTime < 3000) return

        lastSpokenGesture = gesture
        lastSpeakTime = now

        // 🔥 SAVE TO HISTORY HERE
        GestureStorage.saveGesture(this, gesture, 1.0f)

        val text =
            if (currentLanguage.language == "ta")
                translateTamil(gesture)
            else
                gesture

        runOnUiThread {
            result.text = "Detected: $text"
            accuracy.text = "Accuracy: 100%"
            tts.language = currentLanguage
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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

    // ================= IMAGE UTILS =================

    data class Motion(val dx: Float, val dy: Float, val region: String, val valid: Boolean)

    private fun analyzeMotion(bitmap: Bitmap): Motion {
        val w = bitmap.width
        val h = bitmap.height

        var sumX = 0f
        var sumY = 0f
        var count = 0

        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)

        for (y in 0 until h step 10) {
            for (x in 0 until w step 10) {
                if (px[y * w + x] == Color.WHITE) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        if (count < MIN_HAND_PIXELS) return Motion(0f, 0f, "", false)

        val cx = sumX / count
        val cy = sumY / count

        val dx = cx - w / 2f
        val dy = cy - h / 2f

        val region = when {
            cy < h * 0.35 -> "UPPER"
            cy > h * 0.65 -> "LOWER"
            else -> "CENTER"
        }

        return Motion(dx, dy, region, true)
    }

    private fun toGray(bmp: Bitmap): Bitmap {
        val g = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(g)
        val p = Paint()
        p.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        c.drawBitmap(bmp, 0f, 0f, p)
        return g
    }

    private fun frameDiff(a: Bitmap, b: Bitmap): Bitmap {
        val w = b.width
        val h = b.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val p1 = IntArray(w * h)
        val p2 = IntArray(w * h)

        a.getPixels(p1, 0, w, 0, 0, w, h)
        b.getPixels(p2, 0, w, 0, 0, w, h)

        for (i in p1.indices) {
            val d = abs((p1[i] and 0xFF) - (p2[i] and 0xFF))
            out.setPixel(i % w, i / w, if (d > 25) Color.WHITE else Color.BLACK)
        }
        return out
    }

    private var lastWristY = -1f

    private fun processImage(imageProxy: ImageProxy) {

        val bitmap = imageProxy.toBitmap() ?: return

        val landmarks = handHelper.detect(bitmap) ?: return
        if (landmarks.size < 21) return

        val wrist = landmarks[0]
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val ringTip = landmarks[16]



        val wristX = wrist.x()
        val wristY = wrist.y()

        val dx = indexTip.x() - wristX
        val dy = indexTip.y() - wristY

        // 🔕 IDLE GUARD (no movement)
        if (lastWristY != -1f && abs(wristY - lastWristY) < 0.02f) {
            return
        }

        val gesture = when {

            // 👋 HELLO → forehead + sideways
            wristY < 0.30f && abs(dx) > 0.15f ->
                "Hello"

            // 👍 YES → strong upward
            lastWristY != -1f && wristY < lastWristY - 0.06f ->
                "Yes"

            // 🙏 THANKS → mouth → down
            wristY in 0.38f..0.52f &&
                    lastWristY != -1f &&
                    wristY > lastWristY + 0.05f ->
                "Thanks"

            // 🙇 SORRY → chest center + small circular/side motion + closed fingers
            wristY in 0.48f..0.62f &&
                    lastWristY != -1f &&
                    abs(dx) in 0.05f..0.12f &&                 // moderate sideways motion
                    abs(wristY - lastWristY) < 0.035f &&       // very little vertical motion
                    abs(indexTip.x() - middleTip.x()) < 0.025f &&
                    abs(middleTip.x() - ringTip.x()) < 0.025f ->
                "Sorry"

            // 🙏 PLEASE → chest + gentle circular motion + open fingers
            wristY in 0.46f..0.64f &&
                    lastWristY != -1f &&
                    abs(dx) > 0.04f &&                         // allow softer sideways motion
                    abs(wristY - lastWristY) < 0.06f &&        // allow small vertical drift
                    abs(indexTip.x() - middleTip.x()) > 0.03f &&
                    abs(middleTip.x() - ringTip.x()) > 0.03f ->
                "Please"

            // ❌ NO → strong downward motion (center region)
            lastWristY != -1f &&
                    wristY > lastWristY + 0.07f &&
                    wristY in 0.35f..0.60f ->
                "No"

            else -> null
        }

        lastWristY = wristY


        gesture?.let {
            speakGesture(it)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
