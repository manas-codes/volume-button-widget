package com.example.volumewidget

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkOverlayPermission()) {
            startWidgetService()
            finish()
        } else {
            requestOverlayPermission()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        // Using deprecated startActivityForResult for simplicity without androidx.activity
        startActivityForResult(intent, 123)
        Toast.makeText(this, "Please grant 'Display over other apps' permission", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            if (checkOverlayPermission()) {
                startWidgetService()
                finish()
            } else {
                Toast.makeText(this, "Permission denied. App cannot function.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startWidgetService() {
        val intent = Intent(this, VolumeWidgetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
