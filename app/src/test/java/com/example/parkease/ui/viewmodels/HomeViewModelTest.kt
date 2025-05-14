package com.example.parkease.ui.viewmodels

import com.example.parkease.data.ParkingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel
    private lateinit var repository: ParkingRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = HomeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when cancelReservation is called, repository cancelReservation is called`() = runTest {
        // Given
        coEvery { repository.cancelReservation(any(), any()) } just Runs

        // When
        viewModel.cancelReservation()

        // Then
        coVerify { repository.cancelReservation(any(), any()) }
    }

    @Test
    fun `when cancelReservation succeeds, uiState is updated correctly`() = runTest {
        // Given
        coEvery { repository.cancelReservation(any(), any()) } answers {
            firstArg<() -> Unit>().invoke()
        }

        // When
        viewModel.cancelReservation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assert(viewModel.uiState.value.reservationData == null)
        assert(!viewModel.uiState.value.isLoading)
        assert(viewModel.uiState.value.error == null)
    }

    @Test
    fun `when cancelReservation fails, error is set in uiState`() = runTest {
        // Given
        val errorMessage = "Test error"
        coEvery { repository.cancelReservation(any(), any()) } answers {
            secondArg<(String) -> Unit>().invoke(errorMessage)
        }

        // When
        viewModel.cancelReservation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assert(viewModel.uiState.value.error == errorMessage)
        assert(!viewModel.uiState.value.isLoading)
    }
} 