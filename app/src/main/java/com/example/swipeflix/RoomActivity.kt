package com.example.swipeflix

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import java.io.File
import java.io.FileOutputStream

@Suppress("SameParameterValue")
class RoomActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var membersTextView: TextView
    private lateinit var roomStatusListener: ValueEventListener
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
        val startSwipingButton: Button = findViewById(R.id.startSwiping)
        membersTextView = findViewById(R.id.members)

        startSwipingButton.setOnClickListener{
            val intent = Intent(this, SwipeActivity::class.java)
            startActivity(intent)
        }

        db = FirebaseDatabase.getInstance().reference

        val roomCode = intent.getStringExtra("roomCode") ?: "Unknown Room"
        generateQRAsPNG(roomCode)
        codeButton.text = roomCode

        codeButton.setOnClickListener {
            copyToClipboard("Session Code", codeButton.text.toString())
        }

        // Listen for changes in members and update the TextView
        listenForMemberUpdates(roomCode)


        roomStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "closed") {
                    val intent = Intent(this@RoomActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to check room status.")
            }
        }

        // Attach the listener to the database reference
        db.child("rooms").child(roomCode).child("status")
            .addValueEventListener(roomStatusListener)

    }

    override fun onDestroy() {
        super.onDestroy()
        val roomCode = intent.getStringExtra("roomCode") ?: return

        // Remove Firebase listeners to avoid multiple triggers
        db.child("rooms").child(roomCode).child("status").removeEventListener(roomStatusListener)

        // Set the room status to "closed" and delete the room
        db.child("rooms").child(roomCode).child("status").setValue("closed")
            .addOnCompleteListener {
                deleteRoom()
            }
    }

    private fun generateQRAsPNG(roomCode: String) {
        try {
            // Generate QR Code bitmap
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(roomCode, BarcodeFormat.QR_CODE, 500, 500)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) // ARGB_8888 supports transparency

            for (x in 0 until width) {
                for (y in 0 until height) {
                    // Set the pixel color: Transparent for black, White for QR code
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }

            // Save the bitmap as a PNG file
            val file = File(this.filesDir, "QRCode_$roomCode.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Display the saved QR Code in the ImageView
            val qrCodeImageView: ImageView = findViewById(R.id.qrCode)
            qrCodeImageView.setImageBitmap(bitmap)

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

                    // Update TextView with the member names only
                    val membersText = membersList.joinToString("\n")
                    membersTextView.text = membersText
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RoomActivity, "Failed to load members.", Toast.LENGTH_SHORT)
                        .show()
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

    private fun deleteRoom() {
        val roomCode = intent.getStringExtra("roomCode") ?: return
        // Remove the room node from Firebase
        db.child("rooms").child(roomCode).removeValue()
    }


}

