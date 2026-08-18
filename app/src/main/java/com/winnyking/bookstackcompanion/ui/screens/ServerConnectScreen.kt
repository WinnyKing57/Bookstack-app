package com.winnyking.bookstackcompanion.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnyking.bookstackcompanion.R
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ServerConnectUiState {
    object Idle : ServerConnectUiState()
    object Testing : ServerConnectUiState()
    object TestSuccess : ServerConnectUiState()
    object Saving : ServerConnectUiState()
    object Saved : ServerConnectUiState()
    data class Error(val message: String) : ServerConnectUiState()
}

@HiltViewModel
class ServerConnectViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServerConnectUiState>(ServerConnectUiState.Idle)
    val uiState: StateFlow<ServerConnectUiState> = _uiState.asStateFlow()

    fun testConnection(name: String, baseUrl: String, tokenId: String, tokenSecret: String) {
        viewModelScope.launch {
            _uiState.value = ServerConnectUiState.Testing
            val success = serverRepository.testServerConnection(baseUrl, tokenId, tokenSecret)
            if (success) {
                _uiState.value = ServerConnectUiState.TestSuccess
            } else {
                _uiState.value = ServerConnectUiState.Error("error_connect_failed")
            }
        }
    }

    fun saveServer(name: String, baseUrl: String, tokenId: String, tokenSecret: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = ServerConnectUiState.Saving
            val result = serverRepository.saveServer(name, baseUrl, tokenId, tokenSecret)
            if (result.isSuccess) {
                _uiState.value = ServerConnectUiState.Saved
                onSuccess()
            } else {
                _uiState.value = ServerConnectUiState.Error(result.exceptionOrNull()?.message ?: "error_save_failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConnectScreen(
    onServerConnected: () -> Unit,
    viewModel: ServerConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var tokenId by remember { mutableStateOf("") }
    var tokenSecret by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.server_connect_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.server_connect_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.server_name_label)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.server_url_label)) },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tokenId,
                onValueChange = { tokenId = it },
                label = { Text(stringResource(R.string.server_token_id_label)) },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tokenSecret,
                onValueChange = { tokenSecret = it },
                label = { Text(stringResource(R.string.server_token_secret_label)) },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState) {
                is ServerConnectUiState.Testing, is ServerConnectUiState.Saving -> {
                    CircularProgressIndicator()
                }
                is ServerConnectUiState.Error -> {
                    val errorMsg = when ((uiState as ServerConnectUiState.Error).message) {
                        "error_connect_failed" -> stringResource(R.string.server_connect_error)
                        "error_save_failed" -> stringResource(R.string.server_save_error)
                        else -> stringResource(R.string.error_generic)
                    }
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                is ServerConnectUiState.TestSuccess -> {
                    Text(
                        text = stringResource(R.string.server_connect_success),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                else -> {}
            }

            Row {
                OutlinedButton(
                    onClick = { viewModel.testConnection(name, baseUrl, tokenId, tokenSecret) },
                    enabled = name.isNotBlank() && baseUrl.isNotBlank() && tokenId.isNotBlank() && tokenSecret.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(stringResource(R.string.server_connect_test))
                }

                Button(
                    onClick = { viewModel.saveServer(name, baseUrl, tokenId, tokenSecret, onServerConnected) },
                    enabled = name.isNotBlank() && baseUrl.isNotBlank() && tokenId.isNotBlank() && tokenSecret.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.server_connect_save))
                }
            }
        }
    }
}
