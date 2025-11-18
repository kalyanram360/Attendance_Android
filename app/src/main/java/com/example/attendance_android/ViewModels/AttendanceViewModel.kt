package com.example.attendance_android.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance_android.data.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UserRole {
    STUDENT,
    TEACHER,
    NONE
}

data class OnboardingState(
    val selectedInstitute: String = "",
    val selectedRole: UserRole = UserRole.NONE,
    val name: String = "",
    val email: String = "",
    val activationCode: String = "",
    val isOnboardingComplete: Boolean = false
)

class OnboardingViewModel(
    private val dataStore: DataStoreManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    fun updateInstitute(institute: String) {
        _uiState.value = _uiState.value.copy(selectedInstitute = institute)
    }

    fun updateRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(selectedRole = role)
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updateActivationCode(code: String) {
        if (code.length <= 8) {
            _uiState.value = _uiState.value.copy(activationCode = code)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.setOnboardingComplete(true)
            dataStore.setLoggedIn(true)
            // Save onboarding data to SharedPreferences or DataStore
            // For now, just mark as complete
            _uiState.value = _uiState.value.copy(isOnboardingComplete = true)
        }
    }

    fun isPageValid(page: Int): Boolean {
        return when (page) {
            0 -> _uiState.value.selectedInstitute.isNotEmpty()
            1 -> _uiState.value.selectedRole != UserRole.NONE
            2 -> _uiState.value.name.isNotEmpty() && _uiState.value.email.isNotEmpty()
            3 -> _uiState.value.activationCode.length == 8
            else -> false
        }
    }
}