package com.example.attendance_android.ViewModels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TeacherClassViewModel : ViewModel() {

    // Year (1, 2, 3, 4)
    private val _year = MutableStateFlow(0)
    val year: StateFlow<Int> = _year.asStateFlow()

    // Branch (CSE, ECE, ME...)
    private val _branch = MutableStateFlow("")
    val branch: StateFlow<String> = _branch.asStateFlow()

    // Section (A, B, C...)
    private val _section = MutableStateFlow("")
    val section: StateFlow<String> = _section.asStateFlow()

    // Subject
    private val _subject = MutableStateFlow("")
    val subject: StateFlow<String> = _subject.asStateFlow()

    // ----------- Update Methods -------------

    fun updateYear(value: Int) {
        _year.value = value
    }

    fun updateBranch(value: String) {
        _branch.value = value
    }

    fun updateSection(value: String) {
        _section.value = value
    }
    fun updateSubject(value: String) {
        _subject.value = value
    }

    // Convenience: all three at once
    fun updateAll(year: Int, branch: String, section: String, subject: String) {
        _year.value = year
        _branch.value = branch
        _section.value = section
        _subject.value = subject
    }
}
