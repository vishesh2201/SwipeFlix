package com.example.swipeflix

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast


class RoomActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_room)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        animate1()

        val codeButton: Button = findViewById(R.id.codeButton)
        val swipeLogo: ImageView = findViewById(R.id.swipeFlixS)
        val info: ImageButton = findViewById(R.id.infoButton)
        val qrCode: ImageView = findViewById(R.id.qrCode)
        val copyCode: TextView = findViewById(R.id.CopyCode)
        val selectGenre: TextView = findViewById(R.id.selectGenre)
        val startSwiping: Button = findViewById(R.id.startSwiping)

        // Set up the copy-to-clipboard functionality
        codeButton.setOnClickListener {
            val codeText = codeButton.text.toString() // Get the code from the button

            // Access the ClipboardManager
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Session Code", codeText)
            clipboardManager.setPrimaryClip(clip)

            // Show a confirmation Toast
            Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animate1(vararg views: android.view.View) {

        val codeButton: Button = findViewById(R.id.codeButton)
        val swipeLogo: ImageView = findViewById(R.id.swipeFlixS)
        val info: ImageButton = findViewById(R.id.infoButton)
        val qrCode: ImageView = findViewById(R.id.qrCode)
        val copyCode: TextView = findViewById(R.id.CopyCode)
        val selectGenre: TextView = findViewById(R.id.selectGenre)
        val startSwiping: Button = findViewById(R.id.startSwiping)

    }
}

