package com.example.driverapp

object RoadGuard {
    private var sensorWidth: Float = 0.0f
    private var focalLength: Float = 0.0f

    var allowedSpeed: Int = 50
    var detectedVehicle: Boolean = false
    var detectedPerson: Boolean = false
    var outOfLane: Boolean = false
    var driverStateAlert: Boolean = false
    var urbanArea: Boolean = true
    private val safeDist: Double = 10.0

    fun setCameraParams(sensorW: Float, focalL: Float){
        sensorWidth = sensorW
        focalLength = focalL
    }

    fun resetVehicleAndPerson(){
        detectedPerson = false
        detectedVehicle = false
    }

    fun processOutput(result: Result){
        when (result.classIndex) {
            0 -> allowedSpeed = 20
            1 -> allowedSpeed = 30
            2 -> allowedSpeed = 40
            3 -> allowedSpeed = 50
            4 -> allowedSpeed = 60
            5 -> allowedSpeed = 70
            6 -> allowedSpeed = 80
            7 -> allowedSpeed = 90
            8 -> allowedSpeed = 100
            9 -> allowedSpeed = 110
            10 -> allowedSpeed = 120
            11 -> allowedSpeed = 140
            12 -> allowedSpeed = if(urbanArea) 50 else 90
            // Urban area
            13 -> {
                urbanArea = true
                allowedSpeed = 50
            }


            // End of urban area
            14 -> {
                urbanArea = false
                allowedSpeed = 90
            }
            // Highway
            15 -> {
                urbanArea = false
                allowedSpeed = 140
            }
            // Expressway
            16 -> {
                urbanArea = false
                allowedSpeed = 120
            }
            // Priority change
            17 -> {
                allowedSpeed = if(urbanArea) 50 else 90
            }
            // Highway end
            18 -> allowedSpeed = 90
            // Expressway end
            19 -> allowedSpeed = 90
            // Person Detected
            20 -> {
                if (calcDist(0.45, result.width) < safeDist){
                    detectedPerson = true
                }
            }

            // Single-track vehicle Detected
            21 -> {
                if (calcDist(0.8, result.width) < safeDist){
                    detectedVehicle = true
                }
            }

            // Car Detected
            22 -> {
                if (calcDist(1.7, result.width) < safeDist){
                    detectedVehicle = true
                }
            }

            // Bus Detected
            23 -> {
                if (calcDist(2.55, result.width) < safeDist){
                    detectedVehicle = true
                }
            }

            // Train Detected
            24 -> {
                detectedVehicle = true
            }

            // Truck Detected
            25 -> {
                if (calcDist(2.55, result.width) < safeDist){
                    detectedVehicle = true
                }
            }

            // Traffic Light Detected
            26 -> allowedSpeed = if(urbanArea) 50 else 90

            // Stop Sign Detected
            27 -> { }

            // Animal Detected
            28 -> { }

            // Sports Ball Detected
            29 -> detectedPerson = true

            // Lane detected
            30 -> {
                val imXHalf = result.imgSizeX / 2
                outOfLane = result.rect.right > imXHalf && result.rect.left < imXHalf
            }
        }
    }

    private fun calcDist(realObjectWidth: Double, imageObjectWidth: Float): Double{
        return realObjectWidth * focalLength / (sensorWidth * imageObjectWidth)
    }
}