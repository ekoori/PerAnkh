package com.krbavcic.pentalingo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.krbavcic.pentalingo.core.ui.PentalingoTheme

/**
 * Portrait, edge to edge, on papyrus. The system bars stay visible — this is a reading app used
 * on a commute, not an immersive game — but are drawn light so they sit on the sheet.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
