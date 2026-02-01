package com.almex.asistente.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.almex.asistente.databinding.ActivityMainBinding
import com.almex.asistente.utils.PreferencesManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class MainViewModel : ViewModel() {
    
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    
    private val _backgroundImage = MutableLiveData<Uri?>()
    val backgroundImage: LiveData<Uri?> = _backgroundImage
    
    private val _accentColor = MutableLiveData<Int>()
    val accentColor: LiveData<Int> = _accentColor
    
    private var imagePickerLauncher: ActivityResultLauncher<Intent>? = null
    
    fun selectBackgroundImage(activity: AppCompatActivity) {
        if (imagePickerLauncher == null) {
            imagePickerLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        PreferencesManager.saveBackgroundImageUri(activity, uri.toString())
                        _backgroundImage.value = uri
                    }
                }
            }
        }
        
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher?.launch(intent)
    }
    
    fun showColorPicker(context: Context) {
        // Implementación simplificada - en una app real usarías un color picker
        val colors = arrayOf(
            Color.parseColor("#2196F3"), // Azul
            Color.parseColor("#4CAF50"), // Verde
            Color.parseColor("#FF9800"), // Naranja
            Color.parseColor("#9C27B0"), // Púrpura
            Color.parseColor("#F44336")  // Rojo
        )
        
        val currentIndex = PreferencesManager.getAccentColorIndex(context)
        val newIndex = (currentIndex + 1) % colors.size
        val newColor = colors[newIndex]
        
        PreferencesManager.saveAccentColor(context, newColor, newIndex)
        _accentColor.value = newColor
    }
    
    fun applyCurrentSettings(context: Context, binding: ActivityMainBinding) {
        // Aplicar color de acento guardado
        val savedColor = PreferencesManager.getAccentColor(context)
        _accentColor.value = savedColor
        applyAccentColor(binding, savedColor)
        
        // Aplicar imagen de fondo guardada
        val savedImageUri = PreferencesManager.getBackgroundImageUri(context)
        if (savedImageUri.isNotEmpty()) {
            val uri = Uri.parse(savedImageUri)
            _backgroundImage.value = uri
            applyBackgroundImage(binding.backgroundImage, uri)
        }
    }
    
    fun applyBackgroundImage(imageView: ImageView, uri: Uri) {
        Glide.with(imageView.context)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val processedBitmap = applyDarkOverlay(resource)
                    imageView.background = BitmapDrawable(imageView.resources, processedBitmap)
                }
                
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // No-op
                }
            })
    }
    
    fun applyAccentColor(binding: ActivityMainBinding, color: Int) {
        binding.apply {
            // Aplicar color de acento a elementos UI
            backgroundButton.setBackgroundColor(color)
            accentColorButton.setBackgroundColor(color)
            statusText.setTextColor(color)
        }
    }
    
    private fun applyDarkOverlay(original: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(original.width, original.height, original.config)
        val canvas = Canvas(result)
        
        // Dibujar imagen original
        canvas.drawBitmap(original, 0f, 0f, null)
        
        // Aplicar overlay oscuro para legibilidad
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#80000000") // Negro con 50% transparencia
        }
        canvas.drawRect(0f, 0f, original.width.toFloat(), original.height.toFloat(), overlayPaint)
        
        return result
    }
}