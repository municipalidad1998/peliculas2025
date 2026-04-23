package com.streamflixreborn.streamflix

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.streamflixreborn.streamflix.backup.BackupService

class BackupActivity : AppCompatActivity() {
    
    private lateinit var tokenEditText: EditText
    private lateinit var chatIdEditText: EditText
    private lateinit var backupButton: Button
    
    companion object {
        private const val REQUEST_PERMISSIONS = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        
        tokenEditText = findViewById(R.id.tokenEditText)
        chatIdEditText = findViewById(R.id.chatIdEditText)
        backupButton = findViewById(R.id.backupButton)
        
        backupButton.setOnClickListener {
            val token = tokenEditText.text.toString().trim()
            val chatId = chatIdEditText.text.toString().trim()
            
            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa el token y el ID del chat", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (checkPermissions()) {
                startBackup(token, chatId)
            } else {
                requestPermissions()
            }
        }
    }
    
    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                val token = tokenEditText.text.toString().trim()
                val chatId = chatIdEditText.text.toString().trim()
                startBackup(token, chatId)
            } else {
                Toast.makeText(this, "Permisos necesarios para realizar el backup", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startBackup(token: String, chatId: String) {
        Toast.makeText(this, "Iniciando backup...", Toast.LENGTH_SHORT).show()
        
        val backupService = BackupService(this, token, chatId)
        backupService.startBackup()
    }
}