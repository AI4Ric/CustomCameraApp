package com.example.cameraapplication

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.widget.CheckBox


class MainActivity : AppCompatActivity() {

    private lateinit var startCamera: ActivityResultLauncher<Intent>
    private lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {

        Log.d("OnCreate", "onCreate started")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestCameraPermission()

        startCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Log.d("CameraApp", "Camera took the photo")
                val file = File(currentPhotoPath)
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val filename = file.name
                saveImageToGallery(bitmap, filename)
                // Increment here?
            } else {
                Log.d("CameraApp", "Failed to take photo")
            }
        }


        val buttonOpenCamera: Button = findViewById(R.id.button2)
        buttonOpenCamera.setOnClickListener { openCamera() }

    }


    private fun openCamera() {
        // Intent to take a picture and return control to the calling application
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra("android.intent.extra.quickCapture", true)
        try {
            // Create the File where the photo should go
            val photoFile: File = createImageFile()
            photoFile.also {
                // Save the file: path for use with ACTION_VIEW intents
                currentPhotoPath = it.absolutePath
                // Get the content URI for the image file
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.example.cameraapplication.fileprovider",
                    it
                )
                // Pass the content URI where the photo should be saved
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                // Launch the camera activity
                startCamera.launch(takePictureIntent)
            }
        } catch (ex: IOException) {
            // Error occurred while creating the File
            Log.e("CameraApp", "Error occurred while creating the file", ex)
        }
    }



    @Throws(IOException::class)
    private fun createImageFile(): File {
        Log.d("CameraApp", "create image file running")
        // Create an image file name
        val autoIncrementCheckBox: CheckBox = findViewById(R.id.autoIncrementCheckBox)
        val userInput = findViewById<EditText>(R.id.inputText).text.toString().trim()
        var vendorNumber = if (userInput.isEmpty()) "9999" else userInput
        val lotNumberEditText: EditText = findViewById(R.id.LotNumber)
        val lotNumber = if (lotNumberEditText.text.toString().isEmpty()) 0 else lotNumberEditText.text.toString().toInt()
        //val userInputLotNumber = findViewById<EditText>(R.id.LotNumber).text.toString().trim()
        //var lotNumber = if (userInputLotNumber.isEmpty()) "0" else userInputLotNumber
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (autoIncrementCheckBox.isChecked) {
            val newTextValue = lotNumber + 1
            lotNumberEditText.setText(newTextValue.toString())
            // Log.d("MyDebug", "checkbox is True")
        }
        // Log.d("MyDebug", "Checkbox state: ${autoIncrementCheckBox.isChecked}")

        return File.createTempFile(
            "${lotNumber}_${vendorNumber}_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }

    }


    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
        private const val REQUEST_TAKE_PHOTO = 1
    }

    private fun saveImageToGallery(bitmap: Bitmap, filename: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                // Notify gallery
                MediaScannerConnection.scanFile(this, arrayOf(uri.toString()), null, null)
            } ?: run {
                Log.e("CameraApp", "Failed to open output stream")
            }
        }

    }




    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, you can open the camera here if you need to.
        }
    }



}
