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
import com.google.firebase.database.*
import android.graphics.Bitmap
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Suppress("SameParameterValue")
class RoomActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var membersTextView: TextView
    private var previousMembers = mutableSetOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_room)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val codeButton: Button = findViewById(R.id.codeButton)
        membersTextView = findViewById(R.id.members)

        db = FirebaseDatabase.getInstance().reference

        val roomCode = intent.getStringExtra("roomCode") ?: "Unknown Room"
        generateQRCode(roomCode)
        codeButton.text = roomCode

        codeButton.setOnClickListener {
            copyToClipboard("Session Code", codeButton.text.toString())
        }

        // Listen for changes in members and update the TextView
        listenForMemberUpdates(roomCode)
    }

    private fun generateQRCode(roomCode: String) {
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix = qrCodeWriter.encode(roomCode, BarcodeFormat.QR_CODE, 300, 300)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            val qrImageView: ImageView = findViewById(R.id.qrCode)
            qrImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to generate QR Code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenForMemberUpdates(roomCode: String) {
        db.child("rooms").child(roomCode).child("members")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val membersList = mutableListOf<String>()
                    for (memberSnapshot in snapshot.children) {
                        val memberName = memberSnapshot.key
                        memberName?.let {
                            membersList.add(it)
                            if (it !in previousMembers) {
                                previousMembers.add(it)
                                // Show a toast when a new member joins
                                showToast("$it Joined")
                            }
                        }
                    }

                    // Update TextView with "Members:" followed by member names
                    val membersText = "Members:\n" + membersList.joinToString("\n")
                    membersTextView.text = membersText
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RoomActivity, "Failed to load members.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        showToast("Code copied to clipboard!")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
