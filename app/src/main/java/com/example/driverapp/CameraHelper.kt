package com.example.driverapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A helper class for managing camera operations, such as capturing images,
 * binding camera use cases, and rotating images.
 *
 * @param context The context of the calling component.
 */
class CameraHelper(private val context: Context) {

    // Executor for running background tasks
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    // ImageCapture use case for taking photos
    private var imageCapture: ImageCapture? = null

    /**
     * Starts the camera and binds the preview to the lifecycle of the provided owner.
     *
     * @param lifecycleOwner The lifecycle owner for the camera binding.
     */
    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider, lifecycleOwner)
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Binds the camera use cases, such as image capture, to the lifecycle of the application.
     *
     * @param cameraProvider The ProcessCameraProvider instance.
     * @param imageCapture The ImageCapture use case to bind.
     */
    fun bindCameraUseCases(cameraProvider: ProcessCameraProvider, imageCapture: ImageCapture) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        // Unbind any existing use cases before binding new ones
        cameraProvider.unbindAll()

        // Bind the lifecycle owner, camera selector, and use cases
        cameraProvider.bindToLifecycle(
            context as LifecycleOwner, // Assuming 'context' is a LifecycleOwner
            cameraSelector,
            imageCapture
        )
    }

    /**
     * Rotates a bitmap image by a specified angle.
     *
     * @param source The source bitmap to rotate.
     * @param angle The angle in degrees to rotate the bitmap.
     * @return A new rotated bitmap.
     */
    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    /**
     * Binds the preview use case to the camera provider and lifecycle owner.
     *
     * @param cameraProvider The ProcessCameraProvider instance.
     * @param lifecycleOwner The lifecycle owner for the camera binding.
     */
    private fun bindPreview(cameraProvider: ProcessCameraProvider, lifecycleOwner: LifecycleOwner) {
        imageCapture = ImageCapture.Builder().build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(imageCapture!!)
            .build()

        // Unbind any existing use cases before binding new ones
        cameraProvider.unbindAll()

        // Bind the lifecycle owner, camera selector, and use cases
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            imageCapture
        )
    }

    /**
     * Captures a photo and returns it as a bitmap via the callback function.
     *
     * @param callback A lambda function to handle the resulting bitmap.
     */
    fun takePhotoAsBitmap(callback: (Bitmap?) -> Unit) {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(createTempFile()).build()
        imageCapture?.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                if (savedUri != null) {
                    val bitmap = BitmapFactory.decodeFile(savedUri.path)
                    callback(bitmap)
                } else {
                    callback(null)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                callback(null)
            }
        })
    }

    /**
     * Creates a temporary file for storing captured photos.
     *
     * @return A File object representing the temporary file.
     */
    private fun createTempFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.getExternalFilesDir(null), "temp")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    companion object {
        private const val TAG = "CameraHelper" // Tag for logging
    }
}
