package com.example.driverapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LifecycleOwner
import com.example.driverapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import android.widget.TextView


class MainActivity : AppCompatActivity() { // user choices and changing the main activity components
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

    private lateinit var accuracyCheckbox : CheckBox
    private lateinit var pushCheckbox : CheckBox
    private lateinit var velocityCheckbox : CheckBox
    private lateinit var vehicleCheckbox : CheckBox
    private lateinit var aPCheckbox : CheckBox
    private lateinit var laneCheckbox : CheckBox
    private lateinit var driverCheckbox : CheckBox
    private lateinit var flickerCheckbox : CheckBox


    private var module: Module? = null
    //private var model: List<String> = listOf("classes.txt", "model.torchscript.ptl")
    private var model: List<String> = listOf("classes.txt", "model.torchscript.ptl")

    private val cameraHelper = CameraHelper(this)
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private var isSoundOn = false
    private var allowedSpeed = 50
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
        setContentView(R.layout.activity_main)
        speedLimitTextView = findViewById(R.id.speedLimitTextView)
        //initialize buttons
        drawerLayout = findViewById(R.id.drawer_layout)
        expandButton = findViewById(R.id.expand_button)
        collapseButton = findViewById(R.id.collapse_button)
        soundButton = findViewById(R.id.sound_button)
        locResButton = findViewById(R.id.location_reset_button)

        expandButton.setOnClickListener{toggleDrawer()}
        collapseButton.setOnClickListener{toggleDrawer()}
        soundButton.setOnClickListener{toggleSound()}
        locResButton.setOnClickListener{resetLocation()}

        //initialize labels
        //velocityLabel = findViewById(R.id.velocity_icon)
        vehicleLabel = findViewById(R.id.vehicle_icon)
        personLabel = findViewById(R.id.person_icon)
        lanesLabel = findViewById(R.id.lanes_icon)


        //Initialize checkboxes
        accuracyCheckbox = findViewById(R.id.accuracy_checkbox)
        pushCheckbox = findViewById(R.id.push_notifications_checkbox)
        velocityCheckbox = findViewById(R.id.velocity_control_checkbox)
        vehicleCheckbox = findViewById(R.id.vehicle_control_checkbox)
        aPCheckbox = findViewById(R.id.animal_person_detection_checkbox)
        laneCheckbox = findViewById(R.id.lane_control_checkbox)
        driverCheckbox = findViewById(R.id.driver_control_checkbox)
        flickerCheckbox = findViewById(R.id.screen_flickering_checkbox)

        accuracyCheckbox.setOnClickListener{changeAccuracy()}

        PrePostProcessor.mClasses =
            FileLoader.loadClasses(applicationContext, model[0]).toTypedArray()
        module = FileLoader.loadModel(applicationContext, model[1])

        streamFeedback(this)

        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList
        if (cameraIdList.isNotEmpty()){
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraIdList[0])
            val sensorInfo = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val focalLength =
                cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.firstOrNull()
            if (sensorInfo != null && focalLength != null){
                RoadGuard.setCameraParams(sensorInfo.width, focalLength)
            }
        }
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

    private fun changeAccuracy(){
        PrePostProcessor.change_accuracy(accuracyCheckbox.isChecked)
    }

    // Function to toggle drawer (open/close)
    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.drawer))) {
            drawerLayout.closeDrawer(findViewById(R.id.drawer))
        } else {
            drawerLayout.openDrawer(findViewById(R.id.drawer))
        }
    }

    // Function to toggle sound state and update button icon
    private fun toggleSound() {
        isSoundOn = !isSoundOn //

        // Change button icon
        val iconResource = if (isSoundOn) {
            R.drawable.sound_on
        } else {
            R.drawable.sound_off
        }
        soundButton.setImageResource(iconResource)

    }

    private fun resetLocation() {
        RoadGuard.urbanArea = true
        RoadGuard.allowedSpeed = 50
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun streamFeedback(lifecycleOwner: LifecycleOwner) {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                var image: Bitmap? = null

                // Capture image from the camera
                image = withContext(Dispatchers.IO) {return@withContext captureImage(cameraHelper) }

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

//                    val results = getPredictions(imgAfterConv)
                    val results = withContext(Dispatchers.IO) {return@withContext getPredictions(imgAfterConv) }
                    RoadGuard.resetVehicleAndPerson()
                    if (results != null) {
                        for (result in results) {
                            RoadGuard.processOutput(result.classIndex, result.width)
                        }

                        vehicleLabel.setImageResource(R.drawable.vehicle_dis)
                        personLabel.setImageResource(R.drawable.person_dis)
                        lanesLabel.setImageResource(R.drawable.lanes_dis)


                       /* if (RoadGuard.allowedSpeed != allowedSpeed && velocityCheckbox.isChecked) {
                            val veloIconID = veloDict[RoadGuard.allowedSpeed]
                            if (veloIconID != null) {
                                velocityLabel.setImageResource(veloIconID)
                            }
                        }*/

                        if (RoadGuard.allowedSpeed != allowedSpeed && velocityCheckbox.isChecked) {
                            // Update the speed limit text dynamically
                            speedLimitTextView.text = "${RoadGuard.allowedSpeed}"

                            // Update the local allowedSpeed variable
                            allowedSpeed = RoadGuard.allowedSpeed
                        }


                        if (RoadGuard.detectedVehicle && vehicleCheckbox.isChecked) {
                            vehicleLabel.setImageResource(R.drawable.vehicle_en)
                        }

                        if (RoadGuard.detectedPerson && aPCheckbox.isChecked) {
                            personLabel.setImageResource(R.drawable.person_en)
                        }

                        if ((RoadGuard.outOfLane && laneCheckbox.isChecked())) {
                            lanesLabel.setImageResource(R.drawable.lanes_en);
                        }



                    }
                } else {
                    break
                }
            }
        }
    }


}