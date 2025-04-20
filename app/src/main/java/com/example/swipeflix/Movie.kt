package com.example.swipeflix

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Movie(
    val id: String,
    val title: String,
    val poster_url: String,
    val type: String?,
    val imdb_rating: String?,
    val runtime: String?,
    val synopsis: String?
) : Parcelable
