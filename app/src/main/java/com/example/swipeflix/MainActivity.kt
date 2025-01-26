@file:Suppress("DEPRECATION")

package com.example.swipeflix

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nicknameEditText: EditText = findViewById(R.id.nickname)
        val joinCodeEditText: EditText = findViewById(R.id.joincode)
        val hostSessionButton: Button = findViewById(R.id.host_a_session)
        val joinSessionButton: Button = findViewById(R.id.join_session)
        val joinByQRButton: ImageButton = findViewById(R.id.joinByQRButton)

        joinCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
        joinCodeEditText.filters = arrayOf(InputFilter.LengthFilter(6))

        hostSessionButton.setOnClickListener {
            var nickname = nicknameEditText.text.toString().trim() // Use var instead of val

            if (nickname.length > 2) {
                // Capitalize the first letter and make the rest lowercase
                nickname = nickname.replaceFirstChar { it.uppercase() }

                val roomCode = createRoom(nickname)
                Toast.makeText(this, "Hosting session as $nickname!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, RoomActivity::class.java)
                intent.putExtra("roomCode", roomCode)
                intent.putExtra("nickname", nickname) // Pass the modified nickname
                startActivity(intent)
            } else {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            }
        }

        joinSessionButton.setOnClickListener {
            var nickname = nicknameEditText.text.toString().trim() // Use var instead of val

            if (nickname.length <= 2) {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            } else {
                // Capitalize the first letter and make the rest lowercase
                nickname = nickname.replaceFirstChar { it.uppercase() }
                val enteredCode = joinCodeEditText.text.toString().trim()

                if (enteredCode.isEmpty()) {
                    Toast.makeText(this, "Enter Room Code", Toast.LENGTH_SHORT).show()
                } else {
                    joinRoom(enteredCode, nickname)
                    val intent = Intent(this, RoomActivity::class.java)
                    intent.putExtra("roomCode", enteredCode)
                    intent.putExtra("nickname", nickname) // Pass the modified nickname
                    startActivity(intent)
                }
            }
        }

        joinByQRButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim() // Use var instead of val

            if (nickname.isEmpty() || nickname.length <= 2) {
                Toast.makeText(this, "Enter a valid Nickname", Toast.LENGTH_SHORT).show()
            } else {
                val integrator = IntentIntegrator(this)
                integrator.setCaptureActivity(CaptureActivity::class.java)
                integrator.setOrientationLocked(true)
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                integrator.setPrompt("Scan a QR Code")
                integrator.initiateScan()
            }
        }


    }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val scannedRoomCode = result.contents
                var nickname = findViewById<EditText>(R.id.nickname).text.toString().trim()

                // Capitalize the first letter and make the rest lowercase
                nickname = nickname.replaceFirstChar { it.uppercase() }

                if (nickname.length <= 2) {
                    Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
                } else {
                    // Join the room using the scanned room code and the capitalized nickname
                    val intent = Intent(this, RoomActivity::class.java)
                    intent.putExtra("roomCode", scannedRoomCode)
                    intent.putExtra("nickname", nickname)
                    startActivity(intent)

                    // Add the user to the room's database members
                    joinRoom(scannedRoomCode, nickname)
                }
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }



    private fun createRoom(nickname: String): String {
        val roomCode = generateUniqueRoomCode()
        val roomData = mapOf(
            "host" to nickname,  // Save the host nickname
            "genre" to "",
            "members" to mapOf(nickname to true)
        )
        db.child("rooms").child(roomCode).setValue(roomData)
        return roomCode
    }


    private fun joinRoom(roomCode: String, nickname: String) {
        db.child("rooms").child(roomCode).child("members").child(nickname).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Joined Room: $roomCode", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to Join Room", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateUniqueRoomCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}
