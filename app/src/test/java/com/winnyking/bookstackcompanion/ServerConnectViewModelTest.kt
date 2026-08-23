package com.winnyking.bookstackcompanion

import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.ui.screens.ServerConnectUiState
import com.winnyking.bookstackcompanion.ui.screens.ServerConnectViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerConnectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var serverRepository: ServerRepository
    private lateinit var viewModel: ServerConnectViewModel

    private val httpErrorMsg = "Les connexions HTTP ne sont pas autorisées"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        serverRepository = mockk()
        viewModel = ServerConnectViewModel(serverRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `testConnection rejects http url when http not allowed`() = runTest {
        viewModel.testConnection(
            baseUrl = "http://192.168.1.50:6875",
            tokenId = "id",
            tokenSecret = "secret",
            allowHttp = false,
            httpNotAllowedErrorMsg = httpErrorMsg
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ServerConnectUiState.Error(httpErrorMsg), viewModel.uiState.value)
    }

    @Test
    fun `testConnection succeeds with https url`() = runTest {
        coEvery {
            serverRepository.testServerConnectionResult("https://bookstack.example.com", "id", "secret")
        } returns Result.success(Unit)

        viewModel.testConnection(
            baseUrl = "https://bookstack.example.com/api/",
            tokenId = "id",
            tokenSecret = "secret",
            allowHttp = false,
            httpNotAllowedErrorMsg = httpErrorMsg
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ServerConnectUiState.TestSuccess, viewModel.uiState.value)
    }

    @Test
    fun `testConnection reports error message on failure`() = runTest {
        coEvery {
            serverRepository.testServerConnectionResult("https://bookstack.example.com", "id", "secret")
        } returns Result.failure(java.net.UnknownHostException("host introuvable"))

        viewModel.testConnection(
            baseUrl = "https://bookstack.example.com",
            tokenId = "id",
            tokenSecret = "secret",
            allowHttp = false,
            httpNotAllowedErrorMsg = httpErrorMsg
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ServerConnectUiState.Error)
        assertEquals("host introuvable", (state as ServerConnectUiState.Error).message)
    }

    @Test
    fun `saveServer rejects http url when http not allowed`() = runTest {
        var successCalled = false

        viewModel.saveServer(
            name = "Serveur local",
            baseUrl = "http://192.168.1.50:6875",
            tokenId = "id",
            tokenSecret = "secret",
            allowHttp = false,
            httpNotAllowedErrorMsg = httpErrorMsg,
            onSuccess = { successCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ServerConnectUiState.Error(httpErrorMsg), viewModel.uiState.value)
        assertEquals(false, successCalled)
    }

    @Test
    fun `saveServer saves sanitized https url and notifies success`() = runTest {
        coEvery {
            serverRepository.saveServer("Mon serveur", "https://bookstack.example.com", "id", "secret")
        } returns Result.success(mockk())

        var successCalled = false
        viewModel.saveServer(
            name = "Mon serveur",
            baseUrl = "https://bookstack.example.com/api/",
            tokenId = "id",
            tokenSecret = "secret",
            allowHttp = false,
            httpNotAllowedErrorMsg = httpErrorMsg,
            onSuccess = { successCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ServerConnectUiState.Saved, viewModel.uiState.value)
        assertTrue(successCalled)
    }
}
