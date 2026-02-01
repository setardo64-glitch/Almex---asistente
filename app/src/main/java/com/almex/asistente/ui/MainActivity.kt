package com.almex.asistente.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.almex.asistente.databinding.ActivityMainBinding
import com.almex.asistente.service.VoiceListenerService
import com.almex.asistente.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startVoiceService()
        } else {
            // Manejar permisos denegados
            binding.statusText.text = "Permisos requeridos para funcionar"
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupUI()
        checkPermissions()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.apply {
            // Configurar listeners para personalización
            backgroundButton.setOnClickListener {
                viewModel.selectBackgroundImage(this@MainActivity)
            }
            
            accentColorButton.setOnClickListener {
                viewModel.showColorPicker(this@MainActivity)
            }
            
            // Botón para acceder a Horacio
            horacioButton.setOnClickListener {
                val intent = Intent(this@MainActivity, HoracioActivity::class.java)
                startActivity(intent)
            }
            
            // Botón para acceder a Documentación
            documentsButton.setOnClickListener {
                val intent = Intent(this@MainActivity, DocumentsActivity::class.java)
                startActivity(intent)
            }
            
            // Aplicar configuración guardada
            viewModel.applyCurrentSettings(this@MainActivity, binding)
        }
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needsPermission) {
            permissionLauncher.launch(permissions)
        } else {
            startVoiceService()
        }
    }
    
    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceListenerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        binding.statusText.text = "Almex está escuchando..."
    }
    
    private fun observeViewModel() {
        viewModel.statusMessage.observe(this) { message ->
            binding.statusText.text = message
        }
        
        viewModel.backgroundImage.observe(this) { uri ->
            if (uri != null) {
                viewModel.applyBackgroundImage(binding.backgroundImage, uri)
            }
        }
        
        viewModel.accentColor.observe(this) { color ->
            viewModel.applyAccentColor(binding, color)
        }
    }
}