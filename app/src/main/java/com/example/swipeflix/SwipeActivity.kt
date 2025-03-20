package com.example.swipeflix

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SwipeActivity : AppCompatActivity() {

    private lateinit var movieImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_swipe)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val cardFront = findViewById<LinearLayout>(R.id.cardFront)
        val cardBack = findViewById<LinearLayout>(R.id.cardBack)
        val flipButton = findViewById<Button>(R.id.flipButton)
        movieImage = findViewById(R.id.moviePoster)
        val movieCard = findViewById<FrameLayout>(R.id.movie_card)



        var isFrontVisible = true


        flipButton.setOnClickListener{
            val frontAnim = AnimatorInflater.loadAnimator(this, R.animator.front_flip) as AnimatorSet
            val backAnim = AnimatorInflater.loadAnimator(this, R.animator.back_flip) as AnimatorSet

            if (isFrontVisible) {
                frontAnim.setTarget(movieCard)
                backAnim.setTarget(movieCard)

                frontAnim.interpolator = AccelerateDecelerateInterpolator()
                backAnim.interpolator = AccelerateDecelerateInterpolator()

                frontAnim.start()
                frontAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        cardFront.visibility = View.GONE
                        cardBack.visibility = View.VISIBLE
                        backAnim.start()
                    }
                })
            } else {
                frontAnim.setTarget(movieCard)
                backAnim.setTarget(movieCard)

                frontAnim.interpolator = AccelerateDecelerateInterpolator()
                backAnim.interpolator = AccelerateDecelerateInterpolator()

                frontAnim.start()
                frontAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        cardFront.visibility = View.VISIBLE
                        cardBack.visibility = View.GONE
                        backAnim.start()
                    }
                })
            }
            isFrontVisible = !isFrontVisible

        }
    }

}
