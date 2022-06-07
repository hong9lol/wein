package com.niew.bodychecker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.SeekBar.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import com.niew.bodychecker.Image.imageProxy
import com.niew.bodychecker.databinding.ActivityMainBinding
import com.niew.bodychecker.utils.BackgroundHelper.MAX_SEEKBAR_VALUE
import com.niew.bodychecker.utils.BackgroundHelper.createBackgroundLineCanvas
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias DrawListener = () -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var overlay: Bitmap
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()

        initSeekBar()


        val gestureListener = Gesture()
        val doubleListener = DoubleGesture { onDoubleTap() }
        val gestureDetector = GestureDetector(this, gestureListener)
        gestureDetector.setOnDoubleTapListener(doubleListener)
        viewBinding.root.setOnTouchListener { _, event ->
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }
    }

    private fun setSeekBarEvent() {
        seekBarProgress = seekBar.progress
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBarProgress = seekBar.progress
                seekBarValue.text = seekBarProgress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val sharedPreferences =
                    getSharedPreferences("defaultConfig", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("progress", seekBar.progress.toString())
                editor.apply()
            }
        })
    }

    private fun initSeekBar() {
        val sharedPreferences = getSharedPreferences(
            "defaultConfig",
            MODE_PRIVATE
        )

        val progressValue = sharedPreferences.getString("progress", seekBarProgress.toString())
        seekBar.max = MAX_SEEKBAR_VALUE
        seekBar.progress = progressValue!!.toInt()
        seekBarProgress = seekBar.progress
        seekBarValue.text = seekBarProgress.toString()
        setSeekBarEvent()
    }

    private fun onDoubleTap() {
        if (seekBarState) {
            seekBar.visibility = INVISIBLE
            seekBarValue.visibility = INVISIBLE
            seekBarState = false
        } else {
            seekBar.visibility = VISIBLE
            seekBarValue.visibility = VISIBLE
            seekBarState = true
        }
    }

    override fun onResume() {
        super.onResume()
        imageProxy?.close()
    }

    private fun takePhoto() {// Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val intent = Intent(applicationContext, ImageViewer::class.java)
                    imageProxy = image
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image Capture Error: ${exception.message}")
                }
            }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setJpegQuality(QUALITY_JPEG_100)
                .build()

            previewWidth = viewFinder.width
            previewHeight = viewFinder.height
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, DrawAnalyzer {
                        overlay = Bitmap.createBitmap(
                            viewFinder.width,
                            viewFinder.height,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = createBackgroundLineCanvas(overlay, Size(previewWidth, previewHeight))
                        overlay.applyCanvas { canvas }

                        runOnUiThread {
                            back_ground.setImageBitmap(overlay)
                        }
                    })
                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val QUALITY_JPEG_100 = 100
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()


        var previewWidth = 0
        var previewHeight = 0
        var seekBarProgress = 20
        var seekBarState = false
    }

    class DoubleGesture(callback: () -> Unit) : GestureDetector.OnDoubleTapListener {
        val c = callback
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            c()
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
            return true
            TODO("not implemented")
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            return true
            TODO("not implemented")
        }
    }

    class Gesture : GestureDetector.OnGestureListener {
        override fun onShowPress(e: MotionEvent?) {
            TODO("not implemented")
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return true
            TODO("not implemented")
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return true
            TODO("not implemented")
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return true
            TODO("not implemented")
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return true
            TODO("not implemented")
        }

        override fun onLongPress(e: MotionEvent?) {
            TODO("not implemented")
        }

    }
}



// Create time stamped name and MediaStore entry.
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        // Create output options object which contains file + metadata
//        val outputOptions = ImageCapture.OutputFileOptions
//            .Builder(
//                contentResolver,
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues
//            )
//            .build()
