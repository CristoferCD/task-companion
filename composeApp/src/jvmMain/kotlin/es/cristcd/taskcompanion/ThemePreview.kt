package es.cristcd.taskcompanion

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun ThemePreview() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.primary)) {
            Text("primary")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onPrimary)) {
            Text("onPrimary")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.primaryContainer)) {
            Text("primaryContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onPrimaryContainer)) {
            Text("onPrimaryContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.inversePrimary)) {
            Text("inversePrimary")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.secondary)) {
            Text("secondary")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onSecondary)) {
            Text("onSecondary")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.secondaryContainer)) {
            Text("secondaryContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onSecondaryContainer)) {
            Text("onSecondaryContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.tertiary)) {
            Text("tertiary")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onTertiary)) {
            Text("onTertiary")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.tertiaryContainer)) {
            Text("tertiaryContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onTertiaryContainer)) {
            Text("onTertiaryContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.background)) {
            Text("background")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onBackground)) {
            Text("onBackground")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surface)) {
            Text("surface")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onSurface)) {
            Text("onSurface")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Text("surfaceVariant")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onSurfaceVariant)) {
            Text("onSurfaceVariant")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceTint)) {
            Text("surfaceTint")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.inverseSurface)) {
            Text("inverseSurface")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.inverseOnSurface)) {
            Text("inverseOnSurface")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.error)) {
            Text("error")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onError)) {
            Text("onError")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.errorContainer)) {
            Text("errorContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.onErrorContainer)) {
            Text("onErrorContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.outline)) {
            Text("outline")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.outlineVariant)) {
            Text("outlineVariant")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.scrim)) {
            Text("scrim")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceBright)) {
            Text("surfaceBright")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceDim)) {
            Text("surfaceDim")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceContainer)) {
            Text("surfaceContainer")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Text("surfaceContainerHigh")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Text("surfaceContainerHighest")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            Text("surfaceContainerLow")
        }
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Text("surfaceContainerLowest")
        }
    }
}

@Preview
@Composable
fun TextPreview() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("displayLarge", style = MaterialTheme.typography.displayLarge)
        Text("displayMedium", style = MaterialTheme.typography.displayMedium)
        Text("displaySmall", style = MaterialTheme.typography.displaySmall)
        Text("headlineLarge", style = MaterialTheme.typography.headlineLarge)
        Text("headlineMedium", style = MaterialTheme.typography.headlineMedium)
        Text("headlineSmall", style = MaterialTheme.typography.headlineSmall)
        Text("titleLarge", style = MaterialTheme.typography.titleLarge)
        Text("titleMedium", style = MaterialTheme.typography.titleMedium)
        Text("titleSmall", style = MaterialTheme.typography.titleSmall)
        Text("bodyLarge", style = MaterialTheme.typography.bodyLarge)
        Text("bodyMedium", style = MaterialTheme.typography.bodyMedium)
        Text("bodySmall", style = MaterialTheme.typography.bodySmall)
        Text("labelLarge", style = MaterialTheme.typography.labelLarge)
        Text("labelMedium", style = MaterialTheme.typography.labelMedium)
        Text("labelSmall", style = MaterialTheme.typography.labelSmall)
    }
}