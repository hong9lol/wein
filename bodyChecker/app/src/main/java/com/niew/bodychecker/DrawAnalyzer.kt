package com.niew.bodychecker

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class DrawAnalyzer(private val listener: DrawListener) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        listener()
        image.close()
    }
}