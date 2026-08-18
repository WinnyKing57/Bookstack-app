package com.winnyking.bookstackcompanion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winnyking.bookstackcompanion.R
import com.winnyking.bookstackcompanion.domain.model.ServerConfig

@Composable
fun ServerSelectorDropdown(
    servers: List<ServerConfig>,
    selectedServer: ServerConfig?,
    onServerSelected: (ServerConfig) -> Unit,
    onAddNewServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = selectedServer?.name ?: stringResource(R.string.server_selector_no_server),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, end = 4.dp)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.server_selector_change)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            servers.forEach { server ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = server.name,
                            fontWeight = if (server.id == selectedServer?.id) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onServerSelected(server)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.server_selector_add), color = MaterialTheme.colorScheme.primary) },
                onClick = {
                    expanded = false
                    onAddNewServer()
                }
            )
        }
    }
}
