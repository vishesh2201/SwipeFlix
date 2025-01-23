package com.example.swipeflix

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Suppress("SameParameterValue")
class RoomActivity : AppCompatActivity() {
    private lateinit var membersTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_room)

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        val codeButton: Button = findViewById(R.id.codeButton)
        membersTextView = findViewById(R.id.members)

        // Get room code passed via Intent
        val roomCode = intent.getStringExtra("roomCode") ?: "Unknown Room"
        codeButton.text = roomCode

        // Set up the copy-to-clipboard functionality
        codeButton.setOnClickListener {
            copyToClipboard("Session Code", codeButton.text.toString())
        }
    }

    // Copy text to clipboard
    private fun copyToClipboard(label: String, text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        showToast("Code copied to clipboard!")
    }


    // Show a Toast message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
