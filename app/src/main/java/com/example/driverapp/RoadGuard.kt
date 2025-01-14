package com.example.driverapp

/**
 * The `RoadGuard` object processes outputs from a detection model to manage vehicle speed,
 * detect objects on the road, and provide safety alerts. It uses camera parameters to calculate
 * distances and determine object proximity.
 */
object RoadGuard {
    // Camera parameters
    private var sensorWidth: Float = 0.0f
    private var focalLength: Float = 0.0f

    // State variables
    var allowedSpeed: Int = 50
    var detectedVehicle: Boolean = false
    var detectedPerson: Boolean = false
    var outOfLane: Boolean = false
    var driverStateAlert: Boolean = false
    var urbanArea: Boolean = true
    private val safeDist: Double = 10.0 // Safe distance threshold in meters

    /**
     * Sets the camera parameters for distance calculations.
     *
     * @param sensorW The width of the camera sensor in millimeters.
     * @param focalL The focal length of the camera in millimeters.
     */
    fun setCameraParams(sensorW: Float, focalL: Float) {
        sensorWidth = sensorW
        focalLength = focalL
    }

    /**
     * Resets the detection flags for vehicles and persons.
     */
    fun resetVehicleAndPerson() {
        detectedPerson = false
        detectedVehicle = false
    }

    fun resetLocation() {
        allowedSpeed = if (urbanArea) 50 else allowedSpeed
    }

    /**
     * Processes a single detection result and updates the state variables
     * (e.g., allowed speed, detected objects, and lane status).
     *
     * @param result A `Result` object representing a single detection.
     */
    fun processOutput(result: Result) {
        when (result.classIndex) {
            // Speed limit detections
            in 0..11 -> allowedSpeed = when (result.classIndex) {
                0 -> 20; 1 -> 30; 2 -> 40; 3 -> 50
                4 -> 60; 5 -> 70; 6 -> 80; 7 -> 90
                8 -> 100; 9 -> 110; 10 -> 120; 11 -> 140
                else -> allowedSpeed
            }

            // Urban area start
            13 -> {
                urbanArea = true
                allowedSpeed = 50
            }

            // Urban area end
            14 -> {
                urbanArea = false
                allowedSpeed = 90
            }

            // Highways and expressways
            15 -> {
                urbanArea = false
                allowedSpeed = 140 // Highway
            }
            16 -> {
                urbanArea = false
                allowedSpeed = 120 // Expressway
            }

            // Person detected
            20 -> {
                if (calcDist(0.45, result.width) < safeDist) {
                    detectedPerson = true
                }
            }

            // Vehicle detections
            21 -> { // Single-track vehicle
                if (calcDist(0.8, result.width) < safeDist) {
                    detectedVehicle = true
                }
            }
            22 -> { // Car
                if (calcDist(1.7, result.width) < safeDist) {
                    detectedVehicle = true
                }
            }
            23, 25 -> { // Bus or Truck
                if (calcDist(2.55, result.width) < safeDist) {
                    detectedVehicle = true
                }
            }
            24 -> detectedVehicle = true // Train

            // Lane detection
            30 -> {
                val imXHalf = result.imgSizeX / 2
                outOfLane = result.rect.right > imXHalf && result.rect.left < imXHalf
            }

            // Default cases (e.g., Stop sign, animal, etc.)
            else -> { /* No additional actions required */ }
        }
    }

    /**
     * Calculates the distance between the camera and an object based on the object's real-world
     * width and its width in the captured image.
     *
     * @param realObjectWidth The real-world width of the object in meters.
     * @param imageObjectWidth The object's width in the image (normalized).
     * @return The calculated distance in meters.
     */
    private fun calcDist(realObjectWidth: Double, imageObjectWidth: Float): Double {
        return realObjectWidth * focalLength / (sensorWidth * imageObjectWidth)
    }
}
