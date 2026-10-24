package com.krbavcic.pentalingo

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.krbavcic.pentalingo.core.ui.PentalingoTheme

/**
 * Edge to edge, on papyrus. The system bars stay visible — this is a reading app used on a
 * commute, not an immersive game — but are drawn light so they sit on the sheet.
 *
 * A phone holds the app upright. A tablet (or a foldable opened out) turns it whichever way it is
 * held, and its pages keep to a column in the middle of the screen (core-ui's PageWidth).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holdUprightOnPhones(resources.configuration)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            PentalingoTheme {
                AppHost()
            }
        }
    }

    // The manifest keeps the activity alive through these changes, so a foldable opened or closed
    // while the app runs is asked again here.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        holdUprightOnPhones(newConfig)
    }

    /** Android's own line between phones and tablets: 600 dp on the screen's shorter side. */
    private fun holdUprightOnPhones(config: Configuration) {
        val wanted = if (config.smallestScreenWidthDp >= 600) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (requestedOrientation != wanted) requestedOrientation = wanted
    }
}
