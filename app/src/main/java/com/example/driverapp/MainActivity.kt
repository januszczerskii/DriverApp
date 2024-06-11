package com.example.driverapp

import android.os.Bundle
import androidx.drawerlayout.widget.DrawerLayout
import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleOwner

import com.example.driverapp.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume


class MainActivity : AppCompatActivity() { // user choices and changing the main activity components
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var expandButton: ImageButton
    private lateinit var collapsesButton: ImageButton
    private var module: Module? = null
    private var model: List<String> = listOf("classesSigns.txt", "modelSigns.torchscript.ptl")
    private val cameraHelper = CameraHelper(this)
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private var isSoundOn = true

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100 // You can choose any integer value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the main view
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.requestFeature(Window.FEATURE_NO_TITLE) // Set fullscreen

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Request CAMERA permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            cameraExecutor = Executors.newSingleThreadExecutor()
            startCameraSetup()
            cameraHelper.startCamera(this)
        }
        streamFeedback(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                // Check if the CAMERA permission has been granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraExecutor = Executors.newSingleThreadExecutor()
                    startCameraSetup()
                    cameraHelper.startCamera(this)

                } else {
                    // Permission denied, handle accordingly (e.g., show a message or disable camera functionality)
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startCameraSetup() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Set up image capture
            imageCapture = ImageCapture.Builder()
                .build()

            // Bind use cases to camera
            cameraHelper.bindCameraUseCases(cameraProvider, imageCapture)

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun getPredictions(image: Bitmap): ArrayList<Result>? {
        val resizedBitmap = Bitmap.createScaledBitmap(
            image,
            PrePostProcessor.mInputWidth,
            PrePostProcessor.mInputHeight,
            true
        )
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            PrePostProcessor.noMeanRGB,
            PrePostProcessor.noStdRGB
        )
        val outputTuple = module?.forward(IValue.from(inputTensor))?.toTuple()

        val outputTensor = outputTuple?.get(0)?.toTensor()
        val outputs = outputTensor?.dataAsFloatArray
        val imgSizeX = image.width.toFloat()
        val imgSizeY = image.height.toFloat()
        val results = outputs?.let {
            PrePostProcessor.outputsToNMSPredictions(
                it,
                imgSizeX,
                imgSizeY
            )
        }
        return results
    }

    private suspend fun captureImage(cameraHelper: CameraHelper): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            cameraHelper.takePhotoAsBitmap { bitmap ->
                continuation.resume(bitmap)
            }
        }
    }

    // Function to toggle drawer (open/close)
    private fun toggleDrawer() { // TODO Not used
        if (drawerLayout.isDrawerOpen(findViewById(R.id.drawer))) {
            drawerLayout.closeDrawer(findViewById(R.id.drawer))
        } else {
            drawerLayout.openDrawer(findViewById(R.id.drawer))
        }
    }

    // Function to toggle sound state and update button icon
    private fun toggleSound() { // TODO Not used
        isSoundOn = !isSoundOn //

        // Change button icon
        val iconResource = if (isSoundOn) {
            R.drawable.sound_on
        } else {
            R.drawable.sound_off
        }
        collapsesButton.setImageResource(iconResource)

    }

    @OptIn(DelicateCoroutinesApi::class)
    fun streamFeedback(lifecycleOwner: LifecycleOwner) {
        GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                var image: Bitmap? = null

                // Capture image from the camera
                image = captureImage(cameraHelper)
                val matrix = Matrix().apply { postRotate(90F) }
                if (image != null) {
                    image = Bitmap.createBitmap(
                        image,
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )
                }

                if (image != null) {
                    val imgAfterConv = image.copy(image.config, true)

                    val results = getPredictions(imgAfterConv)
                    if (results != null) {
                        for (result in results) {
                            PrePostProcessor.mClasses[result.classIndex] + " " + result.score + "\n"
                        }
                    }
                } else {
                    break
                }
            }
        }
    }


}