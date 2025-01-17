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
        val hostSessionButton: Button = findViewById(R.id.host_session)
        val joinSessionButton: Button = findViewById(R.id.join_session)
        val joinByQRButton: ImageButton = findViewById(R.id.joinByQRButton)

        // Restrict the "Join by Code" EditText to only 6 digits
        joinCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
        joinCodeEditText.filters = arrayOf(InputFilter.LengthFilter(6))

        // Host session button logic
        hostSessionButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim() // Get and trim the nickname input

            // Check if the nickname is valid
            if (nickname.length > 2) {
                // Proceed to host session (replace with your desired action)
                Toast.makeText(this, "Hosting session as $nickname!", Toast.LENGTH_SHORT).show()
                // Code to start the host session or navigate to another activity can go here
                val intent = Intent(this, RoomActivity::class.java)
                startActivity(intent)
            } else {
                // Show an error message if the nickname is invalid
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            }
        }

        // Join session button logic
        joinSessionButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()
            val enteredCode = joinCodeEditText.text.toString().trim()

            if (nickname.length <= 2) {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            } else if (enteredCode != "420690") {
                Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, RoomActivity::class.java)
                startActivity(intent)
            }
        }

        joinByQRButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()

            if (nickname.length <= 2) {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            } else {
                // Initiate the QR scan if the nickname is valid
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
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Scanned: ${result.contents}", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
