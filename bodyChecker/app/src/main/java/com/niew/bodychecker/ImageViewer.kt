package com.niew.bodychecker

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.applyCanvas
import com.niew.bodychecker.Image.imageProxy
import com.niew.bodychecker.MainActivity.Companion.previewHeight
import com.niew.bodychecker.MainActivity.Companion.previewWidth
import com.niew.bodychecker.fingerpaint.DrawableOnTouchView
import com.niew.bodychecker.utils.BackgroundHelper.createBackgroundLineCanvas
import com.niew.bodychecker.utils.ImageHelper.imageProxyToBitmap
import com.niew.bodychecker.utils.ImageHelper.rotate
import kotlinx.android.synthetic.main.activity_main2.*


class ImageViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.imageview)
//


        setContentView(R.layout.activity_main2)
        // image is rotated
        val originalImgSize = Size(imageProxy!!.height, imageProxy!!.width)

        val bitmap = imageProxyToBitmap(imageProxy!!)
        val rotatedBitmap = rotate(bitmap, 90f)
        val targetWidth = (previewWidth * rotatedBitmap.height) / previewHeight
        val resizedBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            (rotatedBitmap.width - targetWidth) / 2,
            0,
            targetWidth,
            rotatedBitmap.height
        )

        val ratio = (resizedBitmap.width / previewWidth.toFloat())
        createBackgroundLineCanvas(
            resizedBitmap,
            Size(originalImgSize.width, originalImgSize.height),
            ratio
        ).also { canvas ->
            resizedBitmap.applyCanvas { canvas }
        }

        runOnUiThread {
            imageview.setImageBitmap(resizedBitmap)
        }
        try {
            val drawableOnTouchView = DrawableOnTouchView(this)
            drawableOnTouchView.setActionListener(object : DrawableOnTouchView.OnActionListener {
                override fun OnCancel() {
                    drawableOnTouchView.isClickable = false
                }

                override fun OnDone(bitmap: Bitmap?) {
                    drawableOnTouchView.makeNonClickable(false)
                }

                override fun show() {
                    drawableOnTouchView.makeNonClickable(true)
                }

                override fun killSelf() {
                    //if(listener!=null)listener.killSelf(drawableOnTouchView.getBitmap());
                }
            })

            drawableOnTouchView.setColorChangedListener(object :
                DrawableOnTouchView.OnColorChangedListener {
                override fun onColorChanged(color: Int) {}
                override fun onStrokeWidthChanged(strokeWidth: Float) {}
                override fun onBrushChanged(Brushid: Int) {}
            })
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            params.gravity = Gravity.CENTER
            main_frame.addView(drawableOnTouchView, params)
            drawableOnTouchView.attachCanvas(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "${e.message}")
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TAG = "ImageViewer"
    }
}