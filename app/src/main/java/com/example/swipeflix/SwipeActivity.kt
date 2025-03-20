package com.example.swipeflix

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SwipeActivity : AppCompatActivity() {

    private lateinit var movieCard: FrameLayout
    private lateinit var cardFront: LinearLayout
    private lateinit var cardBack: LinearLayout
    private var isFrontVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_swipe)

        movieCard = findViewById(R.id.movie_card)
        cardFront = findViewById(R.id.cardFront)
        cardBack = findViewById(R.id.cardBack)

        // Set camera distance to fix perspective distortion
        val scale = resources.displayMetrics.density
        movieCard.cameraDistance = 8000 * scale

        // ✅ Initialize the visibility properly to prevent first-click glitch
        cardFront.visibility = View.VISIBLE
        cardBack.visibility = View.GONE

        // Flip on card click
        movieCard.setOnClickListener {
            flipCard()
        }
    }

    private fun flipCard() {
        // Ensure pivot is set before flipping
        movieCard.pivotX = movieCard.width / 2f
        movieCard.pivotY = movieCard.height / 2f

        val firstHalf = AnimatorInflater.loadAnimator(this, R.animator.front_flip) as AnimatorSet
        val secondHalf = AnimatorInflater.loadAnimator(this, R.animator.back_flip) as AnimatorSet

        firstHalf.setTarget(movieCard)
        firstHalf.interpolator = AccelerateDecelerateInterpolator()

        secondHalf.setTarget(movieCard)
        secondHalf.interpolator = AccelerateDecelerateInterpolator()

        firstHalf.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (isFrontVisible) {
                    cardFront.visibility = View.GONE
                    cardBack.visibility = View.VISIBLE
                } else {
                    cardFront.visibility = View.VISIBLE
                    cardBack.visibility = View.GONE
                }
                secondHalf.start()
                isFrontVisible = !isFrontVisible
            }
        })

        firstHalf.start()

    }
}
