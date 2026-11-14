package com.portfoliox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portfoliox.ui.theme.PortfolioXTheme
import com.portfoliox.ui.theme.financialColors

/**
 * The app's only Activity: the "host page" that the whole Compose UI renders into.
 * Web analogy: index.html with a single root <div>, where React (here: Compose) takes over.
 */
class MainActivity : ComponentActivity() {

    // onCreate is a lifecycle callback: Android calls it when the screen is first created.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Draw behind the status and navigation bars, like a full-bleed web page.
        setContent { // From here on, the UI is described with @Composable functions.
            PortfolioXTheme {
                // Placeholder until Phase 3 replaces it with the navigation host.
                SetupCompleteScreen()
            }
        }
    }
}

@Composable
private fun SetupCompleteScreen() {
    // Scaffold provides the standard screen layout slots (top bar, bottom bar, FAB, content).
    // innerPadding keeps content clear of the system bars.
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "PortfolioX",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Phase 0: project setup complete",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            ThemeCheckCard()
        }
    }
}

/** Shows the custom gain/loss colours, to confirm the theme works in light and dark mode. */
@Composable
private fun ThemeCheckCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Theme check", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Gain", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "+$1,234.56 (+2.45%)",
                    color = MaterialTheme.financialColors.gain,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Loss", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "−$321.09 (−0.87%)",
                    color = MaterialTheme.financialColors.loss,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

// @Preview renders a composable inside Android Studio without running the app,
// similar to Storybook for React components. Open this file and choose "Split" view.
@Preview(name = "Light", showBackground = true)
@Composable
private fun SetupCompleteLightPreview() {
    PortfolioXTheme(darkTheme = false) { SetupCompleteScreen() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SetupCompleteDarkPreview() {
    PortfolioXTheme(darkTheme = true) { SetupCompleteScreen() }
}
