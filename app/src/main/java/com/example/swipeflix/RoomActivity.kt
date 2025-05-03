package com.example.swipeflix

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
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
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream

class RoomActivity : AppCompatActivity() {

    @Serializable
    data class GenreParam(val selected_genre: String)

    @Serializable
    data class UserNickname(val nickname: String)

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
    private lateinit var pollingJob: Job // To manage the polling coroutine
    private lateinit var pollingJobSwipingButton: Job
    private var startSwiping: Boolean = false
    private var genre: String= "Horror"

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
        isHost = intent.getBooleanExtra("isHost", false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Supabase client
        supabase = createSupabaseClient(
            supabaseUrl = "https://conuknnnccumnwejxxkc.supabase.co/",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNvbnVrbm5uY2N1bW53ZWp4eGtjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDIxMTY4NzksImV4cCI6MjA1NzY5Mjg3OX0.5I17QPKjv2-fNaBCiYx6sOcVF0zCi1Syc0Tcg6z75uk"
        ) {
            install(Postgrest)
        }

        val genres = listOf("Action & Adventure", "Animation", "Crime", "Drama", "Documentary", "Fantasy", "Horror", "Mystery and Thriller", "Romance", "Sport", "War and Military")
        val autoComplete: AutoCompleteTextView = findViewById(R.id.auto_complete)
        val autoCompleteLayout: View = autoComplete.parent as View
        val adapter = ArrayAdapter(this, R.layout.list_item, genres)
        autoComplete.setAdapter(adapter)

        var selectedGenre = ""
        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ ->
            selectedGenre = adapterView.getItemAtPosition(i).toString()
            Toast.makeText(this, "Genre: $selectedGenre", Toast.LENGTH_SHORT).show()
        }

        membersTextView = findViewById(R.id.members)

        generateQRAsPNG(roomCode)
        codeButton.text = roomCode

        codeButton.setOnClickListener {
            copyToClipboard("Session Code", codeButton.text.toString())
        }

        // Set visibility based on isHost
        if (isHost) {
            startSwipingButton.visibility = View.VISIBLE
            autoCompleteLayout.visibility = View.VISIBLE
            selectedGenreTextView.visibility = View.GONE
        } else {
            startSwipingButton.visibility = View.GONE
            autoCompleteLayout.visibility = View.GONE
            selectedGenreTextView.visibility = View.VISIBLE
            startPollingStartSwiping()
        }

        startSwipingButton.setOnClickListener {
            if (selectedGenre == "") {
                Toast.makeText(this, "Please select a genre first", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    addgenre(roomCode, selectedGenre)

                    if (::movies.isInitialized && movies.isNotEmpty()) {
                        val intent = Intent(this@RoomActivity, SwipeActivity::class.java)
                        intent.putParcelableArrayListExtra("movies", ArrayList(movies))
                        intent.putExtra("roomid", roomCode)
                        callupdateSwiping()
                        startActivity(intent)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@RoomActivity, "Failed to load movies. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Initialize members list and start polling
        membersList.add(nickname)
        CoroutineScope(Dispatchers.IO).launch {
            updateMembersView()
        }
        startPollingMembers()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel polling coroutine
        pollingJob.cancel()
        pollingJobSwipingButton.cancel()
        if (isHost) {
            callDeleteRoom(roomCode)
            showToast("Room closed.")
        } else {
            callDeleteUser(nickname, roomCode)
            showToast("You left the room.")
        }
    }

    private fun startPollingMembers() {
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                updateMembersView()
                delay(5000) // Update every 5 seconds
            }
        }
    }

    private fun startPollingStartSwiping(){
        pollingJobSwipingButton= CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                swipeCheck()
                delay(5000)
            }
        }

    }

    private fun callupdateSwiping(){

        CoroutineScope(Dispatchers.IO).launch {
            updateStartSwiping()
        }
    }

    private suspend fun updateStartSwiping(){
        try {
            Log.e("Supabase", "Trying to update room with code: $roomCode")
            val response = supabase.from("rooms").update(
                mapOf("start_swiping" to true)
            ) {
                select()
                filter {
                    eq("roomid", roomCode)
                }
            }

            Log.e("Supabase", "Update response for start_swiping: $response")

            val updatedData = response.data

            if (!updatedData.isNullOrEmpty()) {
                Log.e("Supabase", "start_swiping updated successfully.")
                startSwiping= true;
            } else {
                Log.e("Supabase", "No matching room found or update for start_swiping failed.")
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Error updating start_swiping: ${e.message}")
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

    private suspend fun updateMembersView() {
        try {
            val nicknames = supabase.from("users")
                .select(columns = Columns.list("nickname")) {
                    filter {
                        eq("roomid", roomCode)
                    }
                }
                .decodeList<UserNickname>()

            // Clear and update membersList to avoid duplicates
            membersList.clear()
            membersList.addAll(nicknames.map { it.nickname })

            val membersText = membersList.joinToString("\n")
            withContext(Dispatchers.Main) {
                membersTextView.text = membersText
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Error updating members view: ${e.message}")
        }
    }

    private suspend fun swipeCheck(){
        try{
//            val startSwiping = supabase.from("rooms").select(columns = Columns.list("start_swiping")).decodeSingle<Boolean>()
            val result = supabase.from("rooms")
                .select(columns = Columns.list("start_swiping")) {
                    filter {
                        eq("roomid", roomCode)
                    }
                }
                .decodeList<Map<String, Boolean>>()
            val startSwiping = result.firstOrNull()?.get("start_swiping") ?: false
            if (startSwiping){
                delay(2000)
                val result2 = supabase.from("rooms")
                    .select(columns = Columns.list("genre")) {
                        filter {
                            eq("roomid", roomCode)
                        }
                    }
                    .decodeList<Map<String, String>>()
                genre= result2.firstOrNull()?.get("genre") ?: "Drama"
                movies= callFetchMoviesByGenre(genre)
                val intent = Intent(this@RoomActivity, SwipeActivity::class.java)
                intent.putParcelableArrayListExtra("movies", ArrayList(movies))
                intent.putExtra("roomid", roomCode)
                pollingJobSwipingButton.cancel()
                startActivity(intent)
            }
        }
        catch (e: Exception){
            Log.e("Supabase", "Error fetching start_swiping: ${e.message}")
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

    private suspend fun addgenre(roomCode: String, genre: String) {
        try {
            Log.e("Supabase", "Trying to update room with code: $roomCode")
            val response = supabase.from("rooms").update(
                mapOf("genre" to genre)
            ) {
                select()
                filter {
                    eq("roomid", roomCode)
                }
            }

            Log.e("Supabase", "Update response: $response")

            val updatedData = response.data

            if (!updatedData.isNullOrEmpty()) {
                Log.e("Supabase", "Genre updated successfully.")
                movies = callFetchMoviesByGenre(genre)
                Log.e("Fetch", "Movies for genre $genre are $movies")
            } else {
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

    private fun callDeleteRoom(roomCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            deleteroom(roomCode)
        }
    }

    internal fun callDeleteUser(nickname: String, roomCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            deleteuser(nickname, roomCode)
        }
    }

    private suspend fun deleteroom(roomCode: String) {
        try {
            supabase.from("users").delete {
                filter {
                    eq("roomid", roomCode)
                }
            }

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

    private suspend fun deleteuser(nickname: String, roomCode: String) {
        try {
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