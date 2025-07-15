package com.example.konvo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Custom CropImageContract that handles image cropping with better error handling and fallbacks
 */
class ImprovedCropImageContract : ActivityResultContract<Uri, Uri?>() {
    // Store application context as a field
    private lateinit var appContext: Context
    private lateinit var outputFile: File
    
    override fun createIntent(context: Context, input: Uri): Intent {
        println("[ImprovedCropImageContract] Creating crop intent for URI: $input")
        // Save the context for later use
        this.appContext = context.applicationContext
        
        // Create a content:// URI for the cropped image output
        outputFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        val destinationUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
        println("[ImprovedCropImageContract] Created destination URI: $destinationUri")
        
        try {
            // Create the crop intent
            val intent = Intent("com.android.camera.action.CROP")
                .setDataAndType(input, "image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .putExtra("crop", "true")
                .putExtra("aspectX", 1)
                .putExtra("aspectY", 1)
                .putExtra("scale", true)
                .putExtra("outputX", 512)
                .putExtra("outputY", 512)
                .putExtra("return-data", false)
                .putExtra(MediaStore.EXTRA_OUTPUT, destinationUri)
                .putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
            
            // Grant permissions for all available apps that can handle this intent
            val resInfoList = context.packageManager.queryIntentActivities(
                intent, 
                PackageManager.MATCH_DEFAULT_ONLY
            )
            
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    destinationUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                println("[ImprovedCropImageContract] Granted permissions to: $packageName")
            }
            
            // Check if this intent can be handled
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            
            if (resolveInfo != null) {
                println("[ImprovedCropImageContract] Found activity to handle crop: ${resolveInfo.activityInfo.packageName}")
                return intent
            }
            
            println("[ImprovedCropImageContract] No crop activity found, using fallback")
            // Fallback: Just copy the image as-is if no crop activity is available
            try {
                val inputStream = context.contentResolver.openInputStream(input)
                if (inputStream != null) {
                    val outputStream = FileOutputStream(outputFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    println("[ImprovedCropImageContract] Copied image as fallback")
                }
            } catch (e: Exception) {
                println("[ImprovedCropImageContract] Fallback copy failed: ${e.message}")
            }
            // Return an intent that will just return the original URI
            return Intent().putExtra("output_uri", input.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            println("[ImprovedCropImageContract] Error creating crop intent: ${e.message}")
            // In case of exception, create an intent that will just return the original URI
            return Intent().putExtra("output_uri", input.toString())
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        println("[ImprovedCropImageContract] Parsing result, resultCode: $resultCode")
        if (resultCode != Activity.RESULT_OK) {
            println("[ImprovedCropImageContract] Result not OK")
            return null
        }
        
        try {
            // First try to get the destinationUri from our extras
            val outputUriStr = intent?.getStringExtra("output_uri")
            if (outputUriStr != null) {
                println("[ImprovedCropImageContract] Found output_uri in extras: $outputUriStr")
                return Uri.parse(outputUriStr)
            }
            
            // If the app used our output URI, check if file exists and has content
            if (::outputFile.isInitialized && outputFile.exists() && outputFile.length() > 0) {
                println("[ImprovedCropImageContract] Found valid output file: ${outputFile.absolutePath}, size: ${outputFile.length()}")
                return FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    outputFile
                )
            }
            
            // Fallback: search for any recently created crop file
            val cacheDir = File(appContext.cacheDir.path)
            val cropFile = cacheDir.listFiles()?.filter { 
                it.name.startsWith("cropped_") && it.length() > 0 && 
                System.currentTimeMillis() - it.lastModified() < 30000 // Less than 30 seconds old
            }?.maxByOrNull { it.lastModified() }
            
            if (cropFile != null) {
                println("[ImprovedCropImageContract] Found fallback crop file: ${cropFile.absolutePath}")
                return FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    cropFile
                )
            }
            
            println("[ImprovedCropImageContract] No valid output found")
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            println("[ImprovedCropImageContract] Error parsing result: ${e.message}")
            return null
        }
    }
} 