package com.flightmaximizer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AirportChip(
    iata: String,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    InputChip(
        selected = false,
        onClick = { onRemove?.invoke() },
        label = { Text(iata.uppercase()) },
        trailingIcon = if (onRemove != null) {
            { Icon(Icons.Default.Close, contentDescription = "Remove $iata") }
        } else null,
        modifier = modifier
    )
}
