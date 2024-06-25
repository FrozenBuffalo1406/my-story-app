package com.dicoding.mystoryapp.view.upload

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dicoding.mystoryapp.ViewModelFactory
import com.dicoding.mystoryapp.data.preference.UserPreference
import com.dicoding.mystoryapp.data.response.FileUploadResponse
import com.dicoding.mystoryapp.databinding.ActivityUploadBinding
import com.dicoding.mystoryapp.getImageUri
import com.dicoding.mystoryapp.reduceFileImage
import com.dicoding.mystoryapp.uriToFile
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class UploadActivity : AppCompatActivity() {

    private val viewModel by viewModels<UploadViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var binding: ActivityUploadBinding
    private var imageUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {isGranted: Boolean ->
            if (isGranted) {
                showDialog("Memberikan Izin")
            } else {
                showDialog("Menolak Izin")
            }
        }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            showImage()
        } else {
            Log.d(PHOTO_PICKER, "Tolong pilih gambar")
        }
    }

    private fun showImage() {
        imageUri?.let {
            binding.ivPreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        binding.btnGallery.setOnClickListener { startGallery() }
        binding.btnCamera.setOnClickListener { intentCamera() }
        binding.btnUpload.setOnClickListener { upload() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun upload() {
        imageUri?.let { uri->
            val imageFile = uriToFile(uri, this).reduceFileImage()
            val desc = binding.etDesc.text.toString()
            showLoading(true)
            val requestBody = desc.toRequestBody("text/plain".toMediaType())
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multiPartBody = MultipartBody.Part.createFormData(
                "photo",
                imageFile.name,
                requestImageFile
            )
            lifecycleScope.launch {
                try {
                    val pref = UserPreference.getInstance(this@UploadActivity)
                    val token = pref.getSession().first().token
                    viewModel.saveImage(token, multiPartBody, requestBody)
                    showDialog("Berhasil mengupload Gambar")
                    clearInputField()
                    finish()
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    val errorResult = Gson().fromJson(errorBody, FileUploadResponse::class.java)
                    showDialog("Gagal mengupload gambar e: $errorResult" )
                    showLoading(false)
                }
            }
        } ?: showDialog("Anda belum memasukkan gambar")
    }

    override fun onResume() {
        super.onResume()
        viewModel.getStories()
    }


    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun intentCamera() {
        imageUri = getImageUri(this)
        launcherIntentCamera.launch(imageUri!!)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun showDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
            .setTitle("Information")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun clearInputField(){
        binding.etDesc.text = null
        binding.ivPreview.setImageURI(null)
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
        private const val PHOTO_PICKER = "Photo"
    }
}