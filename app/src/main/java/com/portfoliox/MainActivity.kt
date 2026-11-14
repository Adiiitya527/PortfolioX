package com.portfoliox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.portfoliox.ui.PortfolioXApp
import com.portfoliox.ui.theme.PortfolioXTheme

/**
 * The app's only Activity: the "host page" that the whole Compose UI renders into.
 * Web analogy: index.html with a single root <div>, where React (here: Compose) takes over.
 * Moving between screens happens inside Compose (see AppNavHost), not between Activities.
 */
class MainActivity : ComponentActivity() {

    // onCreate is a lifecycle callback: Android calls it when the screen is first created.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Draw behind the status and navigation bars, like a full-bleed web page.
        setContent {
            PortfolioXTheme {
                PortfolioXApp()
            }
        }
    }
}
