package com.example.swipeflix

import com.bumptech.glide.Glide
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.w3c.dom.Text
import kotlin.math.abs
import com.example.swipeflix.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

@Suppress("DEPRECATION")
class SwipeActivity : AppCompatActivity() {

    private var currentMovieIndex = 0
    private lateinit var movie: Movie
    private lateinit var movies: List<Movie>
    private lateinit var movieCard: FrameLayout
    private lateinit var cardFront: LinearLayout
    private lateinit var cardBack: LinearLayout
    private lateinit var supabase: SupabaseClient

    private var isFrontVisible = true
    private var dX = 0f
    private var dY = 0f
    private var cardStartX = 0f
    private var cardStartY = 0f
    private var isDragging = false
    private var longPressed = false
    private val longPressTimeout = 600L
    private var longPressHandler: Handler? = null
    private var longPressRunnable: Runnable? = null
    private lateinit var spacerView: View
    private lateinit var likeText: TextView
    private lateinit var dislikeText: TextView
    private lateinit var roomid: String

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_swipe)

        // Retrieve the movie list from the intent
        movies = intent.getParcelableArrayListExtra("movies") ?: emptyList()
        roomid= intent.getStringExtra("roomid") ?: "000000"

        supabase = createSupabaseClient(
            supabaseUrl = "https://conuknnnccumnwejxxkc.supabase.co/",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNvbnVrbm5uY2N1bW53ZWp4eGtjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDIxMTY4NzksImV4cCI6MjA1NzY5Mjg3OX0.5I17QPKjv2-fNaBCiYx6sOcVF0zCi1Syc0Tcg6z75uk"
        ) {
            install(Postgrest)
        }

        if (movies.isNotEmpty()) {
            // Proceed with your logic to display the movie list
        } else {
            Toast.makeText(this, "No movies found", Toast.LENGTH_SHORT).show()
        }

        spacerView = findViewById<View>(R.id.spacerView)
        dislikeText = findViewById<TextView>(R.id.disliked)
        likeText = findViewById<TextView>(R.id.liked)

        if (likeText.visibility != View.GONE || dislikeText.visibility != View.GONE) {

            spacerView.visibility=View.GONE
        }
        else{
            spacerView.visibility=View.VISIBLE
        }

        movieCard = findViewById(R.id.movie_card)

        if (movies.isNotEmpty()) {
            // Proceed with your logic to display the first movie
            displayCurrentMovie()
        } else {
            Toast.makeText(this, "No movies found", Toast.LENGTH_SHORT).show()
        }

        movieCard.post {
            cardStartX = movieCard.x
            cardStartY = movieCard.y
        }
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)
        val thumbsUpButton = findViewById<ImageButton>(R.id.thumbsup)
        val thumbsDownButton = findViewById<ImageButton>(R.id.thumbsdown)

        val scale = resources.displayMetrics.density
        movieCard.cameraDistance = 8000 * scale

        cardFront.visibility = View.VISIBLE
        cardBack.visibility = View.GONE

        thumbsUpButton.setOnClickListener {
            swipeCardRight()
        }
        thumbsDownButton.setOnClickListener {
            swipeCardLeft()

        }

        movieCard.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    cardStartX = view.x
                    cardStartY = view.y
                    isDragging = false
                    longPressed = false

                    // Start long press detection
                    longPressHandler = Handler()
                    longPressRunnable = Runnable {
                        longPressed = true
                        vibrate()
                        flipCard()
                    }
                    longPressHandler?.postDelayed(longPressRunnable!!, longPressTimeout)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val moveX = event.rawX + dX
                    val moveY = event.rawY + dY
                    val deltaX = abs(moveX - cardStartX)
                    val deltaY = abs(moveY - cardStartY)

                    // If movement exceeds threshold, cancel long press
                    if (deltaX > 20 || deltaY > 20) {
                        isDragging = true
                        longPressHandler?.removeCallbacks(longPressRunnable!!)
                    }

                    if (isDragging) {
                        view.animate()
                            .x(moveX)
                            .y(moveY)
                            .setDuration(0)
                            .start()
                        view.rotation = (moveX - cardStartX) / 20
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler?.removeCallbacks(longPressRunnable!!)

                    if (!longPressed && isDragging) {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val movedX = view.x - cardStartX

                        if (movedX > screenWidth / 4) {
                            // Swiped right
                            view.animate()
                                .x(screenWidth.toFloat())
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction {
                                    updateVotesTable(currentMovieIndex)
                                    currentMovieIndex = (currentMovieIndex + 1) % movies.size
                                    displayCurrentMovie()
                                    resetCard(movieCard)
                                }
                                .start()
                            CoroutineScope(Dispatchers.IO).launch{
//                                rightSwipeMovie(currentMovieIndex)
                            }
                        } else if (movedX < -screenWidth / 4) {
                            // Swiped left
                            view.animate()
                                .x(-screenWidth.toFloat())
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction {
                                    currentMovieIndex = (currentMovieIndex + 1) % movies.size
                                    displayCurrentMovie()
                                    resetCard(movieCard)
                                }
                                .start()
                        } else {
                            // Not far enough, reset
                            view.animate()
                                .x(cardStartX)
                                .y(cardStartY)
                                .rotation(0f)
                                .setDuration(200)
                                .start()
                        }
                    }
                    isDragging = false
                    true
                }

                else -> false
            }
        }
    }

    private fun displayCurrentMovie() {
        if (currentMovieIndex < movies.size) {
            movie = movies[currentMovieIndex]
            // Update the UI with the current movie
            updateMovieDetails(movie)
        }
    }

    private fun updateMovieDetails(movie: Movie) {
        // Assuming you have a method to update your UI
        // For example:
        val movieTitle = findViewById<TextView>(R.id.movieTitle)
        val movieRuntime = findViewById<TextView>(R.id.runtime)
        val moviePoster = findViewById<ImageView>(R.id.moviePoster)
        val movieSynopsis= findViewById<TextView>(R.id.movieDescription)
        val movieType= findViewById<TextView>(R.id.showtype)
        val movieRuntimeBack= findViewById<TextView>(R.id.runtimeBack)
        val movieRating= findViewById<TextView>(R.id.imdbRating)
        val movieTitleBack= findViewById<TextView>(R.id.movieTitleBack)
        val movieRatingBack= findViewById<TextView>(R.id.imdbRatingBack)

        movieTitle.text = movie.title
        movieRuntime.text = movie.runtime
        movieSynopsis.text= movie.synopsis
        movieType.text= movie.type
        movieRating.text= movie.imdb_rating

        movieTitleBack.text= movie.title
        movieRuntimeBack.text= movie.runtime
        movieRatingBack.text= movie.imdb_rating

        Glide.with(this)
            .load(movie.poster_url)
            .into(moviePoster)
    }

    private fun swipeCardRight() {
        val screenWidth = resources.displayMetrics.widthPixels
        movieCard.animate()
            .x(screenWidth.toFloat())
            .alpha(0f)
            .rotation(20f)
            .setDuration(300)
            .withEndAction {
                updateVotesTable(currentMovieIndex)
                currentMovieIndex = (currentMovieIndex + 1) % movies.size
                displayCurrentMovie()
                resetCard(movieCard)
            }
            .start()
        CoroutineScope(Dispatchers.IO).launch{
//            rightSwipeMovie(currentMovieIndex)
        }
    }

//    private suspend fun rightSwipeMovie(index: Int) {
//        val movieTemp = movies[index]
//        val TAG = "SwipeActivity"
//        val movieIdAsString = movieTemp.id.toString()
//
//
//        try {
//            val existingVotes = supabase.from("votes")
//                .select(columns = Columns.ALL) {
//                    filter {
//                        eq("roomid", roomid)
//                        eq("movieid", movieIdAsString)
//                    }
//                    limit(1)
//                }
//                .decodeList<Map<String, String>>()
//
//            if (existingVotes.isNotEmpty()) {
//                val currentCount = existingVotes[0]["count"]?.toIntOrNull() ?: 0
//                val newCount = (currentCount + 1)
//
//                val updateResponse = supabase.from("votes")
//                    .update(mapOf("count" to newCount.toString())) {
//                        filter {
//                            eq("roomid", roomid)
//                            eq("movieid", movieIdAsString)
//                        }
//                    }
//
//                Log.d(TAG, "Updated vote count to $newCount for movieId=${movieTemp.id}")
//            } else {
//                val insertResponse = supabase.from("votes")
//                    .insert(
//                        mapOf(
//                            "roomid" to roomid,
//                            "movieid" to movieIdAsString,
//                            "count" to "1"
//                        )
//                    )
//                Log.d(TAG, "Inserted new vote entry for movieId=${movieTemp.id}")
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error while updating votes", e)
//        }
//    }

//    @Serializable
//    data class VoteCount(val count: String)
//
//    private suspend fun rightSwipeMovie(index: Int) {
//        val movieTemp = movies[index]
//        val TAG = "SwipeActivity"
//        val movieIdAsString = movieTemp.id.toString()
//
//        try {
//            // Fetch the existing vote entry (singular response or empty)
//            val existingVotes = supabase.from("votes")
//                .select(columns = Columns.list("count")) {
//                    filter {
//                        eq("roomid", roomid)
//                        eq("movieid", movieIdAsString)
//                    }
//                }
//                .decodeSingleOrNull<VoteCount>() // Decoding into a list of maps
//
//            // Check if the list is not empty
//            if (existingVotes!= null) {
//                // Safe to access the first item
//                val currentCount = existingVotes.count.toInt()
//                val newCount = currentCount + 1
//                val strnewcount= newCount.toString()
//
//                // Update the vote count
//                val updateResponse = supabase.from("votes")
//                    .update(mapOf("count" to strnewcount)) {
//                        filter {
//                            eq("roomid", roomid)
//                            eq("movieid", movieIdAsString)
//                        }
//                    }
//
//                Log.d(TAG, "Updated vote count to $newCount for movieId=${movieTemp.id}")
//            } else {
//                // If no existing vote entry, insert a new vote entry
//                val insertResponse = supabase.from("votes")
//                    .insert(
//                        mapOf(
//                            "roomid" to roomid,
//                            "movieid" to movieIdAsString,
//                            "count" to "1"
//                        )
//                    )
//                Log.d(TAG, "Inserted new vote entry for movieId=${movieTemp.id}")
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error while updating votes", e)
//        }
//    }






    private suspend fun updateVotes(index:Int){

    }
    private fun updateVotesTable(index: Int){
        CoroutineScope(Dispatchers.IO).launch {
            updateVotes(index)
        }
    }

    private fun swipeCardLeft() {
        val screenWidth = resources.displayMetrics.widthPixels
        movieCard.animate()
            .x(-screenWidth.toFloat())
            .alpha(0f)
            .rotation(-20f)
            .setDuration(300)
            .withEndAction {
                // Move to next movie in the list
                currentMovieIndex = (currentMovieIndex + 1) % movies.size
                displayCurrentMovie()
                resetCard(movieCard)
            }
            .start()

    }

    private fun flipCard() {
        movieCard.pivotX = movieCard.width / 2f
        movieCard.pivotY = movieCard.height / 2f

        val firstHalf = AnimatorInflater.loadAnimator(this, R.animator.front_flip) as AnimatorSet
        val secondHalf = AnimatorInflater.loadAnimator(this, R.animator.back_flip) as AnimatorSet

        firstHalf.setTarget(movieCard)
        secondHalf.setTarget(movieCard)

        firstHalf.interpolator = AccelerateDecelerateInterpolator()
        secondHalf.interpolator = AccelerateDecelerateInterpolator()

        firstHalf.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                if (isFrontVisible) {
                    cardFront.visibility = View.GONE
                    cardBack.visibility = View.VISIBLE
                } else {
                    cardFront.visibility = View.VISIBLE
                    cardBack.visibility = View.GONE
                }
                isFrontVisible = !isFrontVisible
                secondHalf.start()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        firstHalf.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun resetCard(view: View) {
        view.alpha = 1f
        view.x = cardStartX
        view.y = cardStartY
        view.rotation = 0f
    }
}
