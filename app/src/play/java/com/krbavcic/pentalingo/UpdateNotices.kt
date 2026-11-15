package com.krbavcic.pentalingo

import android.content.Context

/**
 * Per Ankh from Google Play never looks for releases itself: Google Play keeps it up to date, and
 * allows no other way (the Device and Network Abuse policy). The setting is not offered here.
 */
object UpdateNotices {
    const val AVAILABLE = false

    fun set(context: Context, on: Boolean) = Unit

    fun allowed(context: Context): Boolean = false
}
