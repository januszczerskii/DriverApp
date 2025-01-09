package com.example.driverapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LifecycleOwner
import com.example.driverapp.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * MainActivity class handles the primary user interface and camera operations for the DriverApp.
 */
class MainActivity : AppCompatActivity() {

    // UI components and bindings
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var expandButton: ImageButton
    private lateinit var collapseButton: Button
    private lateinit var locResButton: Button
    private lateinit var soundButton: ImageButton
    private lateinit var vehicleLabel: ImageView
    private lateinit var personLabel: ImageView
    private lateinit var lanesLabel: ImageView
    private lateinit var speedLimitTextView: TextView

    private lateinit var accuracyCheckbox: CheckBox
    private lateinit var pushCheckbox: CheckBox
    private lateinit var velocityCheckbox: CheckBox
    private lateinit var vehicleCheckbox: CheckBox
    private lateinit var aPCheckbox: CheckBox
    private lateinit var laneCheckbox: CheckBox
    private lateinit var driverCheckbox: CheckBox
    private lateinit var flickerCheckbox: CheckBox

    // Model and camera-related variables
    private var module: Module? = null
    private var model: List<String> = listOf("classes.txt", "model.torchscript.ptl")

    private val cameraHelper = CameraHelper(this)
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private var isSoundOn = false
    private var allowedSpeed = 50

    private lateinit var toneGen: ToneGenerator

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    /**
     * Called when the activity is first created.
     * Initializes bindings, permissions, UI components, and camera setup.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullscreenMode()
        initializeBindings()
        checkCameraPermissions()
        initializeUIComponents()
        loadModelAndClasses()
        setupCameraParameters()
    }

    /**
     * Sets the application to fullscreen mode by removing the title and status bars.
     */
    private fun setupFullscreenMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.requestFeature(Window.FEATURE_NO_TITLE)
    }

    /**
     * Initializes data binding for the activity.
     */
    private fun initializeBindings() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Checks for camera permissions. If granted, initializes the camera.
     */
    private fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            initializeCamera()
        }
    }

    /**
     * Sets up the camera and starts its execution.
     */
    private fun initializeCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCameraSetup()
        cameraHelper.startCamera(this)
    }

    /**
     * Initializes UI components such as buttons, labels, and checkboxes.
     */
    private fun initializeUIComponents() {
        setContentView(R.layout.activity_main)
        setupButtons()
        setupLabels()
        setupCheckboxes()
    }

    /**
     * Configures button click listeners.
     */
    private fun setupButtons() {
        drawerLayout = findViewById(R.id.drawer_layout)
        expandButton = findViewById(R.id.expand_button)
        collapseButton = findViewById(R.id.collapse_button)
        soundButton = findViewById(R.id.sound_button)
        locResButton = findViewById(R.id.location_reset_button)

        expandButton.setOnClickListener { toggleDrawer() }
        collapseButton.setOnClickListener { toggleDrawer() }
        soundButton.setOnClickListener { toggleSound() }
        locResButton.setOnClickListener { resetLocation() }
    }

    /**
     * Initializes labels for vehicle, person, and lane detection icons.
     */
    private fun setupLabels() {
        vehicleLabel = findViewById(R.id.vehicle_icon)
        personLabel = findViewById(R.id.person_icon)
        lanesLabel = findViewById(R.id.lanes_icon)
        speedLimitTextView = findViewById(R.id.speedLimitTextView)
    }

    /**
     * Sets up the checkboxes and their click listeners.
     */
    private fun setupCheckboxes() {
        accuracyCheckbox = findViewById(R.id.accuracy_checkbox)
        pushCheckbox = findViewById(R.id.push_notifications_checkbox)
        velocityCheckbox = findViewById(R.id.velocity_control_checkbox)
        vehicleCheckbox = findViewById(R.id.vehicle_control_checkbox)
        aPCheckbox = findViewById(R.id.animal_person_detection_checkbox)
        laneCheckbox = findViewById(R.id.lane_control_checkbox)
        driverCheckbox = findViewById(R.id.driver_control_checkbox)
        flickerCheckbox = findViewById(R.id.screen_flickering_checkbox)

        accuracyCheckbox.setOnClickListener { changeAccuracy() }
    }

    /**
     * Loads the model and its corresponding class labels.
     */
    private fun loadModelAndClasses() {
        PrePostProcessor.mClasses = FileLoader.loadClasses(applicationContext, model[0]).toTypedArray()
        module = FileLoader.loadModel(applicationContext, model[1])
    }

    /**
     * Configures camera parameters such as sensor size and focal length.
     */
    private fun setupCameraParameters() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList
        if (cameraIdList.isNotEmpty()) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraIdList[0])
            val sensorInfo = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
            if (sensorInfo != null && focalLength != null) {
                RoadGuard.setCameraParams(sensorInfo.width, focalLength)
            }
        }
        toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }

    /**
     * Handles the result of a permission request.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        handlePermissionsResult(requestCode, grantResults)
    }

    /**
     * Processes the result of a permission request, initializing the camera if granted.
     */
    private fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Starts setting up the camera use cases such as image capture.
     */
    private fun startCameraSetup() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            cameraHelper.bindCameraUseCases(cameraProvider, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Cleans up resources when the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * Processes a bitmap to generate predictions using the loaded model.
     * @param image The input image.
     * @return A list of predictions as results.
     */
    private fun getPredictions(image: Bitmap): ArrayList<Result>? {
        val resizedBitmap = Bitmap.createScaledBitmap(image, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.noMeanRGB, PrePostProcessor.noStdRGB)
        val outputTuple = module?.forward(IValue.from(inputTensor))?.toTuple()
        val outputTensor = outputTuple?.get(0)?.toTensor()
        val outputs = outputTensor?.dataAsFloatArray
        val imgSizeX = image.width.toFloat()
        val imgSizeY = image.height.toFloat()
        return outputs?.let { PrePostProcessor.outputsToNMSPredictions(it, imgSizeX, imgSizeY) }
    }

    /**
     * Captures an image asynchronously from the camera.
     * @param cameraHelper The camera helper to capture the image.
     * @return A bitmap of the captured image.
     */
    private suspend fun captureImage(cameraHelper: CameraHelper): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            cameraHelper.takePhotoAsBitmap { bitmap ->
                continuation.resume(bitmap)
            }
        }
    }

    /**
     * Toggles the drawer layout open or closed.
     */
    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.drawer))) {
            drawerLayout.closeDrawer(findViewById(R.id.drawer))
        } else {
            drawerLayout.openDrawer(findViewById(R.id.drawer))
        }
    }

    /**
     * Toggles sound on or off, updating the UI and playing a tone if enabled.
     */
    private fun toggleSound() {
        isSoundOn = !isSoundOn
        soundButton.setImageResource(if (isSoundOn) R.drawable.sound_on else R.drawable.sound_off)
        if (isSoundOn) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        }
    }

    /**
     * Resets the location-related settings in the application.
     */
    private fun resetLocation() {
        RoadGuard.urbanArea = true
        RoadGuard.allowedSpeed = 50
    }

    /**
     * Changes the detection accuracy based on the state of the accuracy checkbox.
     */
    private fun changeAccuracy() {
        PrePostProcessor.changeAccuracy(accuracyCheckbox.isChecked)
    }

    /**
     * Processes a captured image for detection results.
     * @param image The captured bitmap image.
     */
    private fun processCapturedImage(image: Bitmap) {
        val matrix = Matrix().apply { postRotate(90F) }
        val rotatedImage = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
        val results = getPredictions(rotatedImage)
        RoadGuard.resetVehicleAndPerson()
        results?.let { processDetectionResults(it) }
    }

    /**
     * Processes detection results, updating UI elements and system states.
     * @param results The list of detection results.
     */
    private fun processDetectionResults(results: ArrayList<Result>) {
        results.forEach { RoadGuard.processOutput(it) }
        updateUIForDetections()
    }

    /**
     * Updates the UI components based on detection results.
     */
    private fun updateUIForDetections() {
        vehicleLabel.setImageResource(if (RoadGuard.detectedVehicle && vehicleCheckbox.isChecked) R.drawable.vehicle_en else R.drawable.vehicle_dis)
        personLabel.setImageResource(if (RoadGuard.detectedPerson && aPCheckbox.isChecked) R.drawable.person_en else R.drawable.person_dis)
        lanesLabel.setImageResource(if (RoadGuard.outOfLane && laneCheckbox.isChecked) R.drawable.lanes_en else R.drawable.lanes_dis)

        if (RoadGuard.allowedSpeed != allowedSpeed && velocityCheckbox.isChecked) {
            if (isSoundOn) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
            speedLimitTextView.text = "${RoadGuard.allowedSpeed}"
            allowedSpeed = RoadGuard.allowedSpeed
        }
    }

    /**
     * Streams feedback by capturing images and processing them repeatedly.
     * @param lifecycleOwner The lifecycle owner for managing coroutines.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun streamFeedback(lifecycleOwner: LifecycleOwner) {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val image = withContext(Dispatchers.IO) { captureImage(cameraHelper) }
                image?.let { processCapturedImage(it) } ?: break
            }
        }
    }
}
