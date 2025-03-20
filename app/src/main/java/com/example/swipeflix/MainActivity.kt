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
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import kotlin.random.Random

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

        joinCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
        joinCodeEditText.filters = arrayOf(InputFilter.LengthFilter(6))

        hostSessionButton.setOnClickListener {
            var nickname = nicknameEditText.text.toString().trim()

            if (nickname.length > 1) {
                nickname = nickname.replaceFirstChar { it.uppercase() }

                val roomCode = generateUniqueRoomCode()
                Toast.makeText(this, "Hosting session as $nickname!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, RoomActivity::class.java)
                intent.putExtra("roomCode", roomCode)
                intent.putExtra("nickname", nickname)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            }
        }

        joinSessionButton.setOnClickListener {
            var nickname = nicknameEditText.text.toString().trim()

            if (nickname.length < 2) {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            } else {
                nickname = nickname.replaceFirstChar { it.uppercase() }
                val enteredCode = joinCodeEditText.text.toString().trim()

                if (enteredCode.isEmpty()) {
                    Toast.makeText(this, "Enter Room Code", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, RoomActivity::class.java)
                    intent.putExtra("roomCode", enteredCode)
                    intent.putExtra("nickname", nickname)
                    startActivity(intent)
                }
            }
        }

        joinByQRButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()

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

    @Deprecated("This method has been deprecated in favor of using the Activity Result API...")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val scannedRoomCode = result.contents
                var nickname = findViewById<EditText>(R.id.nickname).text.toString().trim()

                nickname = nickname.replaceFirstChar { it.uppercase() }

                if (nickname.length <= 2) {
                    Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, RoomActivity::class.java)
                    intent.putExtra("roomCode", scannedRoomCode)
                    intent.putExtra("nickname", nickname)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun generateUniqueRoomCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}
