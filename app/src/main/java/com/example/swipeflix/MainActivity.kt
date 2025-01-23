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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
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

        // Restrict the "Join by Code" EditText to only 6 digits
        joinCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
        joinCodeEditText.filters = arrayOf(InputFilter.LengthFilter(6))

        // Host session button logic
        hostSessionButton.setOnClickListener {
            lifecycleScope.launch {
                val nickname = nicknameEditText.text.toString().trim()


                if (nickname.length > 2) {
                    // Create the room
                    val roomCode = createRoom()

                    // Proceed to host session
                    Toast.makeText(this@MainActivity, "Hosting session as $nickname!", Toast.LENGTH_SHORT).show()
                    // Pass the roomCode to RoomActivity
                    val intent = Intent(this@MainActivity, RoomActivity::class.java)
                    intent.putExtra("roomCode", roomCode)
                    startActivity(intent)
                } else {
                    // Show an error message if the nickname is invalid
                    Toast.makeText(this@MainActivity, "Enter Nickname", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Join session button logic
        joinSessionButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()
            val enteredCode = joinCodeEditText.text.toString().trim()

            if (nickname.length <= 2) {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            } else if (enteredCode.isEmpty()) {
                Toast.makeText(this, "Enter Room Code", Toast.LENGTH_SHORT).show()
            } else {
                // Join the room and add user to memberIDs
                lifecycleScope.launch {
                    joinRoom(enteredCode, nickname) // Call the joinRoom function
                    val intent = Intent(this@MainActivity, RoomActivity::class.java)
                    intent.putExtra("roomCode", enteredCode)
                    startActivity(intent)
                }
            }
        }

        joinByQRButton.setOnClickListener{
            val integrator = IntentIntegrator(this)
            integrator.setCaptureActivity(CaptureActivity::class.java)
            integrator.setOrientationLocked(true)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan a QR Code")
            integrator.initiateScan()
        }
    }





    private val db = FirebaseFirestore.getInstance()

    // Function to create a room in Firestore and return the room code
    private suspend fun createRoom(): String {
        val nicknameEditText: EditText = findViewById(R.id.nickname)

        val roomCode = generateUniqueRoomCode() // Generate a unique 6-digit code

        val nickname = nicknameEditText.text.toString().trim()

        val currentTimestamp = System.currentTimeMillis()

        // Create a new room document in the "rooms" collection
        val roomData = hashMapOf(
            "genre" to "", // Genre will be selected later
            "currentMovie" to "", // Placeholder for current movie
            "memberIDs" to mutableListOf(nickname), // List of member IDs
            "hostTimestamp" to currentTimestamp
        )
        db.collection("rooms").document(roomCode).set(roomData)
            .addOnSuccessListener { _ ->
                // Room created successfully
            }
            .addOnFailureListener { e ->
                // Handle room creation failure
                Toast.makeText(this, "Error creating room: $e", Toast.LENGTH_SHORT).show()
            }

        return roomCode
    }

    private suspend fun joinRoom(roomCode: String, memberName: String) {
        val roomDoc = db.collection("rooms").document(roomCode).get().await()
        if (roomDoc.exists()) {
            val currentMembers = roomDoc.data?.get("memberIDs") as MutableList<String>
            currentMembers.add(memberName)
            db.collection("rooms").document(roomCode).update("memberIDs", currentMembers)
        } else {
            // Handle scenario where the room code is invalid
            Toast.makeText(this@MainActivity, "Invalid Room Code", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to generate a unique 6-digit room code
    private suspend fun generateUniqueRoomCode(): String {
        // Implement logic to generate a random 6-digit code and check for uniqueness in Firestore
        // Here's a basic example (replace with a more robust approach)
        var code = ((Math.random() * 900000) + 100000).toInt().toString()
        while (db.collection("rooms").document(code).get().await().exists()) {
            code = generateUniqueRoomCode()
        }
        return code
    }

}