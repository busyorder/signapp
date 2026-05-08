package com.busyorder.signapp

import android.content.Context
import com.google.mediapipe.tasks.vision.handlandmarker.*
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode

class HandLandmarkHelper(context: Context) {

    private val landmarker: HandLandmarker

    init {
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(
                com.google.mediapipe.tasks.core.BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task")
                    .build()
            )
            .setNumHands(1)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        landmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detect(bitmap: android.graphics.Bitmap): List<NormalizedLandmark>? {
        val image: MPImage = BitmapImageBuilder(bitmap).build()
        val result = landmarker.detect(image)
        return result.landmarks().firstOrNull()
    }
}
