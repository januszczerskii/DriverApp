package com.example.driverapp

import android.content.Context
import org.pytorch.LiteModuleLoader
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import org.pytorch.Module

/**
 * Utility object for handling file operations related to loading PyTorch Lite models
 * and associated class labels from the app's assets.
 */
object FileLoader {

    /**
     * Copies a file from the app's assets to the app's private storage and returns its file path.
     * If the file already exists and is non-empty, the existing path is returned.
     *
     * @param appContext The application context.
     * @param assetName The name of the asset file to be copied.
     * @return The absolute path to the copied file in the app's private storage.
     * @throws IOException If an I/O error occurs during the file operation.
     */
    @Throws(IOException::class)
    fun assetFilePath(appContext: Context, assetName: String): String {
        val file = File(appContext.filesDir, assetName)

        // Return the file path if it already exists and is not empty
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        // Copy the file from the assets directory to the app's private storage
        appContext.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024) // 4KB buffer
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        return file.absolutePath
    }

    /**
     * Loads a PyTorch Lite model from the app's assets.
     *
     * @param appContext The application context.
     * @param modelFileName The name of the model file in the assets directory.
     * @return The loaded PyTorch Module instance, or `null` if loading fails.
     */
    fun loadModel(appContext: Context, modelFileName: String): Module? {
        return LiteModuleLoader.load(this.assetFilePath(appContext, modelFileName))
    }

    /**
     * Loads class labels from a text file in the app's assets directory.
     * Each line in the file corresponds to a class label.
     *
     * @param appContext The application context.
     * @param classFileName The name of the class label file in the assets directory.
     * @return A list of class labels loaded from the file.
     */
    fun loadClasses(appContext: Context, classFileName: String): MutableList<String> {
        val bufferedReader = BufferedReader(InputStreamReader(appContext.assets.open(classFileName)))
        val iterator = bufferedReader.lineSequence().iterator()
        val classes: MutableList<String> = ArrayList()

        // Read each line and add it to the classes list
        while (iterator.hasNext()) {
            classes.add(iterator.next())
        }

        // Assign the loaded classes to the PrePostProcessor
        PrePostProcessor.assignClasses(classes.toTypedArray())
        bufferedReader.close()

        return classes
    }
}
