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

    fun processOutput(classNum: Int, objW: Float){
        val check = calcDist(1.8, objW)
        when (classNum) {
            // Person Detected
            0 -> {
                if (calcDist(0.45, objW) < safeDist){
                    detectedPerson = true
                }
            }

            // Single-track vehicle Detected
            1 -> {
                if (calcDist(0.8, objW) < safeDist){
                    detectedVehicle = true
                }
            }

            // Car Detected
            2 -> {
                if (calcDist(1.7, objW) < safeDist){
                    detectedVehicle = true
                }
            }

            // Bus Detected
            3 -> {
                if (calcDist(2.55, objW) < safeDist){
                    detectedVehicle = true
                }
            }

            // Train Detected
            4 -> {
                detectedVehicle = true
            }

            // Truck Detected
            5 -> {
                if (calcDist(2.55, objW) < safeDist){
                    detectedVehicle = true
                }
            }

            // Traffic Light Detected
            6 -> { }

            // Stop Sign Detected
            7 -> { }

            // Animal Detected
            8 -> { }

            // Sports Ball Detected
            9 -> { }
            10 -> allowedSpeed = 20
            11 -> allowedSpeed = 30
            12 -> allowedSpeed = 40
            13 -> allowedSpeed = 50
            14 -> allowedSpeed = 60
            15 -> allowedSpeed = 70
            16 -> allowedSpeed = 80
            17 -> allowedSpeed = 90
            18 -> allowedSpeed = 100
            19 -> allowedSpeed = 110
            20 -> allowedSpeed = 120
            21 -> allowedSpeed = 140
            // 22 -> other signs
            // Urban area
            23 -> {
                urbanArea = true
                allowedSpeed = 50
            }
            // End of urban area
            24 -> {
                urbanArea = false
                allowedSpeed = 90
            }
            // Highway
            25 -> {
                urbanArea = false
                allowedSpeed = 140
            }
            // Expressway
            26 -> {
                urbanArea = false
                allowedSpeed = 120
            }
            // Priority change
            27 -> {
                allowedSpeed = if(urbanArea) 50 else 90
            }
            // Highway end
            28 -> allowedSpeed = 90
            // Expressway end
            29 -> allowedSpeed = 90
        }
    }

    private fun calcDist(realObjectWidth: Double, imageObjectWidth: Float): Double{
        return realObjectWidth * focalLength / (sensorWidth * imageObjectWidth)
    }
}