package com.example.driverapp

class roadGuard {
    private lateinit var imageSize: Array<Int>

    private var allowedSpeed: Int = 50
    private var detectedVehicle: Boolean = false
    private var detectedPerson: Boolean = false
    private var outOfLane: Boolean = false
    private var driverStateAlert: Boolean = false

    fun setCameraParams(imgSize: Array<Int>){
        imageSize = imgSize
    }

    fun processOutput(classNum: Int, imgObjW: Int){
        when (classNum) {
            // Person Detected
            0 -> {
                calcDist(1.8, imgObjW)
            }
            // Single-track vehicle Detected
            1 -> {
                calcDist(1.8, 200)
            }

            // Car Detected
            2 -> {
                calcDist(1.8, 200)
            }

            // Bus Detected
            3 -> {
                calcDist(1.8, 200)
            }

            // Train Detected
            4 -> {
                calcDist(1.8, 200)
            }

            // Truck Detected
            5 -> {
                calcDist(1.8, 200)
            }

            // Traffic Light Detected
            6 -> {  }

            // Stop Sign Detected
            7 -> { }

            // Animal Detected
            8 -> {
                calcDist(1.8, 200)
            }

            // Sports Ball Detected
            9 -> {}
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
            23 -> allowedSpeed = 50
            24 -> allowedSpeed = 90
            25 -> allowedSpeed = 140
            26 -> allowedSpeed = 120
            //27 ->  If urban area then 50
            28 -> allowedSpeed = 90
            29 -> allowedSpeed = 90
        }
    }

    fun calcDist(realObjectWidth: Double, imageObjectWidth: Int): Int{
        return 1
    }
}