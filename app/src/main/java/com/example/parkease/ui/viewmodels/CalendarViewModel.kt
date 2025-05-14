package com.example.parkease.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class CalendarUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: String = "",
    val startTime: String = "",
    val endTime: String = ""
)

class CalendarViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    fun updateDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun updateStartTime(time: String) {
        _uiState.value = _uiState.value.copy(startTime = time)
    }

    fun updateEndTime(time: String) {
        _uiState.value = _uiState.value.copy(endTime = time)
    }

    fun validateAndProceed(onValid: () -> Unit) {
        val state = _uiState.value
        if (state.selectedDate.isEmpty()) {
            _uiState.value = state.copy(error = "Please select a date")
            return
        }
        if (state.startTime.isEmpty()) {
            _uiState.value = state.copy(error = "Please select a start time")
            return
        }
        if (state.endTime.isEmpty()) {
            _uiState.value = state.copy(error = "Please select an end time")
            return
        }

        // Validate time range
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val start = sdf.parse(state.startTime)
            val end = sdf.parse(state.endTime)
            if (start != null && end != null && start.after(end)) {
                _uiState.value = state.copy(error = "End time must be after start time")
                return
            }
        } catch (e: Exception) {
            _uiState.value = state.copy(error = "Invalid time format")
            return
        }

        // Validate date is not in the past
        val today = Calendar.getInstance()
        val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(state.selectedDate)
        if (selectedDate != null && selectedDate.before(today.time)) {
            _uiState.value = state.copy(error = "Cannot select a date in the past")
            return
        }

        onValid()
    }
} 