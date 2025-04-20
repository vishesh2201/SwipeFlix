package com.example.swipeflix

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.swipeflix.MainActivity.RoomPopulation
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import java.io.File
import java.io.FileOutputStream
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID
import com.example.swipeflix.Movie



class RoomActivity : AppCompatActivity() {



    @Serializable
    data class GenreParam(val selected_genre: String)

    private lateinit var supabase: SupabaseClient
    private lateinit var membersTextView: TextView
    private var membersList = mutableSetOf<String>()
    private var isHost = false
    lateinit var selectedGenreTextView: TextView
    lateinit var codeButton: Button
    lateinit var startSwipingButton: Button
    lateinit var nickname: String
    lateinit var roomCode: String
    lateinit var movies: List<Movie>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_room)


        selectedGenreTextView = findViewById(R.id.selected_genre)
        codeButton = findViewById(R.id.codeButton)
        startSwipingButton = findViewById(R.id.startSwiping)

        nickname = intent.getStringExtra("nickname") ?: "Unknown"
        roomCode = intent.getStringExtra("roomCode") ?: "Unknown Room"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supabase = createSupabaseClient(
            supabaseUrl = "https://conuknnnccumnwejxxkc.supabase.co/",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNvbnVrbm5uY2N1bW53ZWp4eGtjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDIxMTY4NzksImV4cCI6MjA1NzY5Mjg3OX0.5I17QPKjv2-fNaBCiYx6sOcVF0zCi1Syc0Tcg6z75uk"
        ) {

            //...

            install(Postgrest) {
                // settings
            }

        }

        val genres = listOf("Action & Adventure", "Animation", "Crime", "Drama", "Documentary","Fantasy", "Horror", "Mystery and Thriller", "Romance", "Sport", "War and Military")
        val autoComplete: AutoCompleteTextView = findViewById(R.id.auto_complete)
        val autoCompleteLayout: View = autoComplete.parent as View
        val adapter = ArrayAdapter(this, R.layout.list_item, genres)
        autoComplete.setAdapter(adapter)

        var selectedGenre= ""
        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ ->
            selectedGenre = adapterView.getItemAtPosition(i).toString()
            Toast.makeText(this, "Genre: $selectedGenre", Toast.LENGTH_SHORT).show()
            // Placeholder: store genre locally or for further use
        }


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
            if (selectedGenre == "") {
                Toast.makeText(this, "Please select a genre first", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    addgenre(roomCode, selectedGenre)

                    if (::movies.isInitialized && movies.isNotEmpty()) {
                        val intent = Intent(this@RoomActivity, SwipeActivity::class.java)
                        // Ensure you are using putParcelableArrayListExtra to pass a list of Parcelable objects
                        intent.putParcelableArrayListExtra("movies", ArrayList(movies))
                        startActivity(intent)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@RoomActivity, "Failed to load movies. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        }


        // Simulate member join (just display current nickname)
        membersList.add(nickname)
        CoroutineScope(Dispatchers.IO).launch {
            updateMembersView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHost) {
            callDeleteRoom(roomCode)
            showToast("Room closed.")
        } else {
            callDeleteUser(nickname, roomCode)
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

    @Serializable
    data class UserNickname(val nickname: String)

    private suspend fun updateMembersView() {

        val nicknames = supabase.from("users")
            .select(columns = Columns.list("nickname")) {
                filter {
                    eq("roomid", roomCode)
                }
            }
            .decodeList<UserNickname>()

        membersList.addAll(nicknames.map { it.nickname })

        val membersText = membersList.joinToString("\n")
        runOnUiThread {
            membersTextView.text = membersText
        }
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

    // A suspend function to update the room genre
    private suspend fun addgenre(roomCode: String, genre: String) {
        try {
            Log.e("Supabase", "Trying to update room with code: $roomCode")
            val response = supabase.from("rooms").update(
                mapOf("genre" to genre)  // Set the genre to the provided genre
            ) {
                select()
                filter {
                    eq("roomid", roomCode)  // Filter by roomid
                }

            }

            // Log the response
            Log.e("Supabase", "Update response: $response")

            val updatedData = response.data

            if (!updatedData.isNullOrEmpty()) {
                // The update was successful
                Log.e("Supabase", "Genre updated successfully.")
                movies= callFetchMoviesByGenre(genre)
                Log.e("Fetch", "Movies for genre $genre are $movies")
            } else {
                // No rows were updated
                Log.e("Supabase", "No matching room found or update failed.")
            }

        } catch (e: Exception) {
            Log.e("Supabase", "Error updating room genre: ${e.message}")
        }
    }

    private suspend fun callFetchMoviesByGenre(genre: String): List<Movie> {
        return try {
            val rpcParams = buildJsonObject {
                put("selected_genre", genre)
            }

            supabase.postgrest
                .rpc("fetch_movies_by_genre", rpcParams)
                .decodeList<Movie>()
        } catch (e: Exception) {
            Log.e("Supabase", "Error fetching movies: ${e.message}")
            emptyList()
        }
    }

    private fun callDeleteRoom(roomCode: String){
        CoroutineScope(Dispatchers.IO).launch {
            deleteroom(roomCode)
        }
    }

    internal fun callDeleteUser(nickname: String, roomCode: String){
        CoroutineScope(Dispatchers.IO).launch {
            deleteuser(nickname, roomCode)
        }
    }

    private suspend fun deleteroom(roomCode: String) {
        try {
            // Delete all users with the given roomCode from the users table
            supabase.from("users").delete {
                filter {
                    eq("roomid", roomCode)
                }
            }

            // Delete the room itself from the rooms table
            supabase.from("rooms").delete {
                filter {
                    eq("roomid", roomCode)
                }
            }

            Log.e("Supabase", "Room and all users deleted successfully.")

        } catch (e: Exception) {
            Log.e("Supabase", "Error deleting room and users: ${e.message}")
        }
    }

    // Function to delete the user from the users table without altering the rooms table
    private suspend fun deleteuser(nickname: String, roomCode: String) {
        try {
            // Delete the user from the users table with the given roomCode and nickname
            supabase.from("users").delete {
                filter {
                    eq("roomid", roomCode)
                    eq("nickname", nickname)
                }
            }

            Log.e("Supabase", "$nickname has been removed from the room $roomCode.")

        } catch (e: Exception) {
            Log.e("Supabase", "Error deleting user: ${e.message}")
        }
    }

}