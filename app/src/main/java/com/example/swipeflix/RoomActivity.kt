package com.example.swipeflix

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.*
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

        // Apply system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Genre Selection
        val items = listOf("Horror", "Comedy", "Romance", "Drama", "Random")
        val autoComplete: AutoCompleteTextView = findViewById(R.id.auto_complete)
        val autoCompleteLayout: View = autoComplete.parent as View // Get the layout container
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        autoComplete.setAdapter(adapter)

        autoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, _, i, _ ->
                val itemSelected = adapterView.getItemAtPosition(i)
                updateGenreInFirebase(itemSelected.toString())
                Toast.makeText(this, "Genre: $itemSelected", Toast.LENGTH_SHORT).show()
            }

        // Selected Genre TextView
        val selectedGenreTextView: TextView = findViewById(R.id.selected_genre)

        // Initialize UI elements
        val codeButton: Button = findViewById(R.id.codeButton)
        val startSwipingButton: Button = findViewById(R.id.startSwiping)
        val nickname = intent.getStringExtra("nickname") ?: "Unknown"
        val roomCode = intent.getStringExtra("roomCode") ?: "Unknown Room"
        membersTextView = findViewById(R.id.members)

        // Set initial button visibility for the host
        startSwipingButton.visibility = Button.GONE
        autoCompleteLayout.visibility = View.GONE
        selectedGenreTextView.visibility = View.GONE

        // Initialize Firebase Database reference
        db = FirebaseDatabase.getInstance().reference

        // Generate QR Code and display room code
        generateQRAsPNG(roomCode)
        codeButton.text = roomCode

        // Copy room code to clipboard when button clicked
        codeButton.setOnClickListener { copyToClipboard("Session Code", codeButton.text.toString()) }

        // Listen for member updates in the room
        listenForMemberUpdates(roomCode)

        // Check if current user is the host and show buttons accordingly
        checkIfHost(roomCode, nickname, startSwipingButton, autoCompleteLayout, selectedGenreTextView)

        // Handle start swiping button click
        startSwipingButton.setOnClickListener {
            val intent = Intent(this, SwipeActivity::class.java)
            startActivity(intent)
        }

        // Listen for room status changes (closed or open)
        listenForRoomStatus(roomCode)
    }

    override fun onDestroy() {
        super.onDestroy()
        val roomCode = intent.getStringExtra("roomCode") ?: return
        val nickname = intent.getStringExtra("nickname") ?: return

        db.child("rooms").child(roomCode).child("host").get().addOnSuccessListener { snapshot ->
            val host = snapshot.getValue(String::class.java)

            if (nickname == host) {
                db.child("rooms").child(roomCode).child("status").removeEventListener(roomStatusListener)
                db.child("rooms").child(roomCode).child("status").setValue("closed")
                    .addOnCompleteListener {
                        deleteRoom()
                    }
            } else {
                db.child("rooms").child(roomCode).child("members").child(nickname).removeValue()
                    .addOnCompleteListener {
                        showToast("$nickname left the room")
                    }
            }
        }
    }

    private fun updateGenreInFirebase(genre: String) {
        val roomCode = intent.getStringExtra("roomCode") ?: return
        db.child("rooms").child(roomCode).child("genre").setValue(genre)
    }

    private fun generateQRAsPNG(roomCode: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(roomCode, BarcodeFormat.QR_CODE, 500, 500)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }

            val file = File(this.filesDir, "QRCode_$roomCode.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

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
                                showToast("$it Joined")
                            }
                        }
                    }

                    val membersText = membersList.joinToString("\n")
                    membersTextView.text = membersText
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RoomActivity, "Failed to load members.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkIfHost(
        roomCode: String,
        nickname: String,
        startSwipingButton: Button,
        autoCompleteLayout: View,
        selectedGenreTextView: TextView
    ) {
        db.child("rooms").child(roomCode).child("host").get().addOnSuccessListener { snapshot ->
            val host = snapshot.getValue(String::class.java)
            if (nickname == host) {
                startSwipingButton.visibility = View.VISIBLE
                autoCompleteLayout.visibility = View.VISIBLE
                selectedGenreTextView.visibility = View.GONE
            } else {
                startSwipingButton.visibility = View.GONE
                autoCompleteLayout.visibility = View.GONE
                selectedGenreTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun listenForRoomStatus(roomCode: String) {
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

        db.child("rooms").child(roomCode).child("status").addValueEventListener(roomStatusListener)
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
        db.child("rooms").child(roomCode).removeValue()
    }
}
