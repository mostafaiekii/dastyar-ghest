package ir.dastyar.ghest.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.dastyar.ghest.util.Farsi

@Composable
fun LockScreen(verify: (String) -> Boolean, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var shaking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val offset = remember { Animatable(0f) }

    LaunchedEffect(shaking) {
        if (shaking) {
            repeat(3) {
                offset.animateTo(12f, tween(55))
                offset.animateTo(-12f, tween(55))
            }
            offset.animateTo(0f, tween(55))
            shaking = false
            error = false
            pin = ""
        }
    }

    val submit: (Int) -> Unit = { d ->
        if (!shaking && pin.length < 4) {
            val next = pin + d.toString()
            pin = next
            if (next.length == 4) {
                if (verify(next)) onUnlock() else { error = true; shaking = true }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Rounded.Lock, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(18.dp).size(32.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("رمز را وارد کن", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (error) {
            Text(
                "رمز اشتباه است",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(24.dp))

        Row(Modifier.graphicsLayer { translationX = offset.value }) {
            repeat(4) { idx ->
                val filled = idx < pin.length
                Surface(
                    shape = CircleShape,
                    color = if (filled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(8.dp).size(16.dp)
                ) {}
            }
        }

        Spacer(Modifier.height(32.dp))

        val keys = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        keys.chunked(3).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                rowKeys.forEach { k ->
                    KeyButton(Farsi.digit(k)) { submit(k) }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(72.dp))
            KeyButton(Farsi.digit(0)) { submit(0) }
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (pin.isNotEmpty()) {
                    FilledTonalButton(onClick = { pin = pin.dropLast(1) }) {
                        Icon(Icons.Rounded.Backspace, contentDescription = "پاک کردن")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
