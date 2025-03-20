package com.example.swipeflix

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

class RoomActivity : AppCompatActivity() {

    private lateinit var membersTextView: TextView
    private var membersList = mutableSetOf<String>()
    private var isHost = false

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

        val genres = listOf("Anime", "Action & adventure films", "Action Sci-Fi and Fantasy", "Comedies", "Dramas", "Documentaries", "Indian movies", "Horror movies", "Mysteries", "Reality TV", "TV Dramas")
        val autoComplete: AutoCompleteTextView = findViewById(R.id.auto_complete)
        val autoCompleteLayout: View = autoComplete.parent as View
        val adapter = ArrayAdapter(this, R.layout.list_item, genres)
        autoComplete.setAdapter(adapter)

        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ ->
            val selectedGenre = adapterView.getItemAtPosition(i).toString()
            Toast.makeText(this, "Genre: $selectedGenre", Toast.LENGTH_SHORT).show()
            // Placeholder: store genre locally or for further use
        }

        val selectedGenreTextView: TextView = findViewById(R.id.selected_genre)
        val codeButton: Button = findViewById(R.id.codeButton)
        val startSwipingButton: Button = findViewById(R.id.startSwiping)
        val nickname = intent.getStringExtra("nickname") ?: "Unknown"
        val roomCode = intent.getStringExtra("roomCode") ?: "Unknown Room"
        membersTextView = findViewById(R.id.members)

        generateQRAsPNG(roomCode)
        codeButton.text = roomCode

        codeButton.setOnClickListener {
            copyToClipboard("Session Code", codeButton.text.toString())
        }

        // Simulate host check
        isHost = true // Assume the user is the host for now. Replace with logic if needed.

        if (isHost) {
            startSwipingButton.visibility = View.VISIBLE
            autoCompleteLayout.visibility = View.VISIBLE
            selectedGenreTextView.visibility = View.GONE
        } else {
            startSwipingButton.visibility = View.GONE
            autoCompleteLayout.visibility = View.GONE
            selectedGenreTextView.visibility = View.VISIBLE
        }

        startSwipingButton.setOnClickListener {
            val intent = Intent(this, SwipeActivity::class.java)
            startActivity(intent)
        }

        // Simulate member join (just display current nickname)
        membersList.add(nickname)
        updateMembersView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHost) {
            // Placeholder for closing the room or clean-up logic
            showToast("Room closed.")
        } else {
            showToast("You left the room.")
        }
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

    private fun updateMembersView() {
        val membersText = membersList.joinToString("\n")
        membersTextView.text = membersText
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
