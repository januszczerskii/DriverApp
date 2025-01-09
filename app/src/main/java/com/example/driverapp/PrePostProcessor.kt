package com.example.driverapp

import android.graphics.Rect
import java.util.Arrays

/**
 * Represents a detection result, including class index, confidence score,
 * bounding box, and additional image dimensions.
 *
 * @param classIndex The index of the detected class.
 * @param score The confidence score of the detection.
 * @param rect The bounding box of the detection.
 * @param width The normalized width of the bounding box.
 * @param height The normalized height of the bounding box.
 * @param imgSizeX The width of the input image.
 * @param imgSizeY The height of the input image.
 */
class Result(
    var classIndex: Int,
    var score: Float,
    var rect: Rect,
    var width: Float,
    var height: Float,
    var imgSizeX: Float,
    var imgSizeY: Float
)

/**
 * A utility object for pre-processing and post-processing image data for object detection models.
 * Includes functionality for Non-Max Suppression (NMS) and Intersection over Union (IoU) calculations.
 */
object PrePostProcessor {
    var noMeanRGB = floatArrayOf(0.0f, 0.0f, 0.0f) // RGB mean normalization values
    var noStdRGB = floatArrayOf(1.0f, 1.0f, 1.0f) // RGB standard deviation values

    // Model input image size
    var mInputWidth = 640
    var mInputHeight = 640

    private const val mOutputRow = 25200 // Output rows as determined by YOLOv5 for 640x640 input
    private var mOutputColumn = 6 // Columns include bounding box, score, and class probabilities
    private var mThreshold = 0.05f // Confidence threshold for detections
    private const val mNmsLimit = 15 // Maximum number of bounding boxes after NMS
    lateinit var mClasses: Array<String> // Array of class labels

    /**
     * Adjusts the detection threshold for accuracy.
     *
     * @param higher Whether to increase (true) or decrease (false) the threshold.
     */
    fun changeAccuracy(higher: Boolean) {
        mThreshold = if (higher) 0.9f else 0.05f
    }

    /**
     * Applies Non-Max Suppression (NMS) to reduce overlapping bounding boxes.
     *
     * @param boxes List of detection results to filter.
     * @return A filtered list of detection results.
     */
    private fun nonMaxSuppression(boxes: ArrayList<Result>): ArrayList<Result> {
        // Sort boxes by score in ascending order
        boxes.sortWith(java.util.Comparator { o1, o2 -> o1.score.compareTo(o2.score) })
        val selected = ArrayList<Result>()
        val active = BooleanArray(boxes.size)
        Arrays.fill(active, true)
        var numActive = active.size

        var done = false
        var i = 0
        while (i < boxes.size && !done) {
            if (active[i]) {
                val boxA = boxes[i]
                selected.add(boxA)
                if (selected.size >= mNmsLimit) break
                for (j in i + 1 until boxes.size) {
                    if (active[j]) {
                        val boxB = boxes[j]
                        if (iou(boxA.rect, boxB.rect) > mThreshold) {
                            active[j] = false
                            numActive -= 1
                            if (numActive <= 0) {
                                done = true
                                break
                            }
                        }
                    }
                }
            }
            i++
        }
        return selected
    }

    /**
     * Calculates the Intersection-over-Union (IoU) for two bounding boxes.
     *
     * @param a The first bounding box.
     * @param b The second bounding box.
     * @return The IoU value as a float.
     */
    private fun iou(a: Rect, b: Rect): Float {
        val areaA = ((a.right - a.left) * (a.bottom - a.top)).toFloat()
        if (areaA <= 0.0) return 0.0f
        val areaB = ((b.right - b.left) * (b.bottom - b.top)).toFloat()
        if (areaB <= 0.0) return 0.0f
        val intersectionMinX = Math.max(a.left, b.left).toFloat()
        val intersectionMinY = Math.max(a.top, b.top).toFloat()
        val intersectionMaxX = Math.min(a.right, b.right).toFloat()
        val intersectionMaxY = Math.min(a.bottom, b.bottom).toFloat()
        val intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0f) *
                Math.max(intersectionMaxX - intersectionMinX, 0f)
        return intersectionArea / (areaA + areaB - intersectionArea)
    }

    /**
     * Converts model outputs to a list of detection results after applying NMS.
     *
     * @param outputs The raw output array from the model.
     * @param imgSizeX The width of the input image.
     * @param imgSizeY The height of the input image.
     * @return A filtered list of detection results.
     */
    fun outputsToNMSPredictions(
        outputs: FloatArray,
        imgSizeX: Float,
        imgSizeY: Float
    ): ArrayList<Result> {
        val results = ArrayList<Result>()
        for (i in 0 until mOutputRow) {
            if (outputs[i * mOutputColumn + 4] > mThreshold) {
                val x = outputs[i * mOutputColumn]
                val y = outputs[i * mOutputColumn + 1]
                val w = outputs[i * mOutputColumn + 2]
                val h = outputs[i * mOutputColumn + 3]
                val left = imgSizeX / mInputWidth * (x - w / 2)
                val top = imgSizeY / mInputHeight * (y - h / 2)
                val right = imgSizeX / mInputWidth * (x + w / 2)
                val bottom = imgSizeY / mInputHeight * (y + h / 2)
                var max = outputs[i * mOutputColumn + 5]
                var cls = 0
                for (j in 0 until mOutputColumn - 5) {
                    if (outputs[i * mOutputColumn + 5 + j] > max) {
                        max = outputs[i * mOutputColumn + 5 + j]
                        cls = j
                    }
                }
                val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                val result = Result(cls, outputs[i * mOutputColumn + 4], rect, w / mInputWidth, h / mInputHeight, imgSizeX, imgSizeY)
                results.add(result)
            }
        }
        return nonMaxSuppression(results)
    }

    /**
     * Assigns class labels and updates the output column count.
     *
     * @param classes Array of class labels.
     */
    fun assignClasses(classes: Array<String>) {
        mClasses = classes
        mOutputColumn = mClasses.size + 5
    }
}
