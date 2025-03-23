@file:Suppress("DEPRECATION")

package com.example.swipeflix

import kotlinx.coroutines.delay
import android.util.Log
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
import io.github.jan.supabase.*
import io.github.jan.supabase.postgrest.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient

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

        // Initialize Supabase
        supabase = createSupabaseClient(
            supabaseUrl = "https://conuknnnccumnwejxxkc.supabase.co/",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNvbnVrbm5uY2N1bW53ZWp4eGtjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDIxMTY4NzksImV4cCI6MjA1NzY5Mjg3OX0.5I17QPKjv2-fNaBCiYx6sOcVF0zCi1Syc0Tcg6z75uk"
        ) {

            //...

            install(Postgrest) {
                // settings
            }

        }

        val nicknameEditText: EditText = findViewById(R.id.nickname)
        val joinCodeEditText: EditText = findViewById(R.id.joincode)
        val hostSessionButton: Button = findViewById(R.id.host_a_session)
        val joinSessionButton: Button = findViewById(R.id.join_session)
        val joinByQRButton: ImageButton = findViewById(R.id.joinByQRButton)

        joinCodeEditText.inputType = InputType.TYPE_CLASS_NUMBER
        joinCodeEditText.filters = arrayOf(InputFilter.LengthFilter(6))

        hostSessionButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()

            if (nickname.length > 1) {
                val formattedNickname = nickname.replaceFirstChar { it.uppercase() }
                val roomCode = generateUniqueRoomCode()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val userid= createUser(formattedNickname, roomCode)
                        Log.e("Supabase","User id returned from the users table on supabase: ${userid}")
                        delay(500)
                        if (userid != null) {
                            val population= createRoom(roomCode, userid)
                        } // Create Room First
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Hosting session as $formattedNickname!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@MainActivity, RoomActivity::class.java)
                            intent.putExtra("roomCode", roomCode)
                            intent.putExtra("nickname", formattedNickname)
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Log.e("Supabase", "Error: ${e.message}")
                    }
                }
            } else {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            }
        }


        joinSessionButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()

            if (nickname.length < 2) {
                Toast.makeText(this, "Enter Nickname", Toast.LENGTH_SHORT).show()
            } else {
                val formattedNickname = nickname.replaceFirstChar { it.uppercase() }
                val enteredCode = joinCodeEditText.text.toString().trim()

                if (enteredCode.isEmpty()) {
                    Toast.makeText(this, "Enter Room Code", Toast.LENGTH_SHORT).show()
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        val roomId = enteredCode
                        if (roomId != null) {
                            addUserToRoom(formattedNickname, roomId)
                            withContext(Dispatchers.Main) {
                                val intent = Intent(this@MainActivity, RoomActivity::class.java)
                                intent.putExtra("roomCode", enteredCode)
                                intent.putExtra("nickname", formattedNickname)
                                startActivity(intent)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Invalid Room Code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateUniqueRoomCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    private suspend fun createUser(nickname: String, roomId: String): String? {
        val user = mapOf("nickname" to nickname, "roomid" to roomId)
        return supabase.from("users")
            .insert(user){select()}
            .decodeSingle<Map<String, String>>()["userid"]
    }
    // Create a new room in the rooms table
    private suspend fun createRoom(roomCode: String, leaderUserid: String): String? {
        Log.e("FuncCall", "Inside createRoom function with roomCode: $roomCode, leaderUserid: $leaderUserid")
        try {
            // Create a data object directly as a map
            val roomData = mapOf(
                "roomid" to roomCode,
                "population" to "1",
                "leader_user_id" to leaderUserid,
                "is_active" to "true",
                "genre" to "Anime"
            )

            // Insert the data directly
            return supabase.from("rooms").insert(roomData){select()}.decodeSingle<Map<String, String>>()["population"]
        } catch (e: Exception) {
            Log.e("Supabase", "Error inserting into rooms table: ${e.message}")
            return "0"
        }
    }





    // Add a user to the users table and associate with a room
    private suspend fun addUserToRoom(nickname: String, roomId: String) {
        try {
            supabase.from("users").insert(
                mapOf("nickname" to nickname, "room_id" to roomId)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
