package com.palich.layouts

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import okhttp3.*
import java.io.IOException
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        lateinit var context: Context
            private set
    }
}

class MainActivity : Activity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSIONS_REQUEST_CODE = 77

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPerm()

        val callButton: Button = findViewById(R.id.buttonCall)
        callButton.setOnClickListener {
            val phoneNumber = "+380502377111"
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            startActivity(intent)
        }

        val msgButton: Button = findViewById(R.id.buttonTelegram)
        msgButton.setOnClickListener {

            val message = if (!isPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
                "Hello from Android on ${Build.MODEL}!"
            } else {
                "Hello from Android!"
            }
            TelegramSender().sendMessage(message)
        }

        val mailButton: Button = findViewById(R.id.buttonMail)
        mailButton.setOnClickListener {
            val recipientEmail = "y.palich.t@gmai.com"
            val subject = "Test from Android"

            val message = if (!isPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
                "Hello from Android on ${Build.MODEL}!"
            } else {
                "Hello from Android!"
            }

            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, message)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    private fun requestPerm() {
        val permissions = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_PHONE_STATE,
        )
        Log.i(TAG, "Requesting permissions: $permissions")

        requestPermissions(permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)

        if (permissions.any { !isPermissionGranted(it) }) {
            Log.e(TAG, "Permission not granted")
        } else {
            Log.e(TAG, "All permission granted")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.e(TAG, "onRequestPermissionsResult")
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (i in permissions.indices) {
                Log.e(TAG, "Permission ${permissions[i]} granted: ${grantResults[i]}")
            }
        }
    }

}

fun Context.isPermissionGranted(permissionName: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permissionName
    ) == PackageManager.PERMISSION_GRANTED
}

class TelegramSender(val context: Context = App.context) {
    companion object {
        private const val TAG = "TelegramSender"
        private const val botToken =
            "5880318868:AAGgL3Z21BUZT0TsQhRuyK5bhZIIrnbVeeU" //one of my bots
        private const val chatId = "264822129" //my user id
    }

    fun sendMessage(message: String) {

        if (!context.isPermissionGranted(Manifest.permission.INTERNET)) {
            Log.e(TAG, "Permission not granted")
            Toast.makeText(context, "Permission INTERNET not granted", Toast.LENGTH_LONG).show()
            return
        }

        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val requestBody = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", message)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error sending message", e)
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.close()
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    Toast.makeText(context, "Message sent", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}