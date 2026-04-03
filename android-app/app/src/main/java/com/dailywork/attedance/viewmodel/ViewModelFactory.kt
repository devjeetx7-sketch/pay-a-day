package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.data.FirestoreRepository

class ViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(PassbookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PassbookViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(WorkersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkersViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(WorkerDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerDetailViewModel(firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
