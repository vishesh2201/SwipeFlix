package com.example.swipeflix

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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
        val hostSessionButton: Button = findViewById(R.id.startSwiping)
        val joinSessionButton: Button = findViewById(R.id.join_session)
        val joinByQRButton: ImageButton = findViewById(R.id.joinByQRButton)
        val swipeFlixEditText: TextView = findViewById(R.id.swipeflix_Text)

        // Restrict the "Join by Code" EditText to only 6 digits
        joinCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
        joinCodeEditText.filters = arrayOf(InputFilter.LengthFilter(6))



        // Host session button logic
        hostSessionButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim() // Get and trim the nickname input

            // Check if the nickname is valid
            if (nickname.length > 2) {
                // Run the animation and navigate after it's done
                animate1()
                    // Proceed to host session (replace with your desired action)
                    Toast.makeText(this, "Hosting session as $nickname!", Toast.LENGTH_SHORT).show()
                    //val intent = Intent(this, RoomActivity::class.java)
                    //startActivity(intent)

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
                Toast.makeText(this, "=Enter Nickname", Toast.LENGTH_SHORT).show()
            } else if (enteredCode != "420690") {
                Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show()
            } else {
                animate2()
                //val intent = Intent(this, RoomActivity::class.java)
                //startActivity(intent)
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

    override fun onResume() {
        super.onResume()
        val nicknameEditText: EditText = findViewById(R.id.nickname)
        val joinCodeEditText: EditText = findViewById(R.id.joincode)
        val hostSessionButton: Button = findViewById(R.id.startSwiping)
        val joinSessionButton: Button = findViewById(R.id.join_session)
        val joinByQRButton: ImageButton = findViewById(R.id.joinByQRButton)
        val swipeFlixEditText: TextView = findViewById(R.id.swipeflix_Text)
        resetViewProperties(hostSessionButton, nicknameEditText, joinCodeEditText, joinSessionButton, joinByQRButton, swipeFlixEditText)
    }
    private fun animate1(){
        val nicknameEditText: EditText = findViewById(R.id.nickname)
        val joinCodeEditText: EditText = findViewById(R.id.joincode)
        val hostSessionButton: Button = findViewById(R.id.startSwiping)
        val joinSessionButton: Button = findViewById(R.id.join_session)
        val joinByQRButton: ImageButton = findViewById(R.id.joinByQRButton)
        val swipeFlixEditText: TextView = findViewById(R.id.swipeflix_Text)

        swipeFlixEditText.animate()
            .alpha(0f)
            .translationY(-100f)
            .setDuration(500)
            .start()

        nicknameEditText.animate()
            .alpha(0f)
            .translationY(-100f)
            .setDuration(500)
            .start()

        joinCodeEditText.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(500)
            .start()

        joinByQRButton.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(500)
            .start()

        joinSessionButton.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(500)
            .start()

        hostSessionButton.animate()
            .alpha(0f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(500)
            .withEndAction {
                // Navigate to the next activity after animations complete
                val intent = Intent(this, RoomActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            .start()


    }

    private fun animate2(){
        val nicknameEditText: EditText = findViewById(R.id.nickname)
        val joinCodeEditText: EditText = findViewById(R.id.joincode)
        val hostSessionButton: Button = findViewById(R.id.startSwiping)
        val joinSessionButton: Button = findViewById(R.id.join_session)
        val joinByQRButton: ImageButton = findViewById(R.id.joinByQRButton)
        val swipeFlixEditText: TextView = findViewById(R.id.swipeflix_Text)

        swipeFlixEditText.animate()
            .alpha(0f)
            .translationY(-100f)
            .setDuration(500)
            .start()

        nicknameEditText.animate()
            .alpha(0f)
            .translationY(-100f)
            .setDuration(500)
            .start()

        joinCodeEditText.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(500)
            .start()

        joinByQRButton.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(500)
            .start()

        joinSessionButton.animate()
            .alpha(0f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(500)
            .start()

        hostSessionButton.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(500)
            .withEndAction {
                // Navigate to the next activity after animations complete
                val intent = Intent(this, RoomActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            .start()


    }

    private fun resetViewProperties(vararg views: android.view.View) {
        views.forEach { view ->
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start()
        }
    }
}
