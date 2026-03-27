package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dailywork.attedance.data.SyncRepository
import com.dailywork.attedance.data.UserPreferencesRepository

class ViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val syncRepository: SyncRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, syncRepository!!) as T
        }
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, syncRepository!!) as T
        }
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository, syncRepository!!) as T
        }
        if (modelClass.isAssignableFrom(PassbookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PassbookViewModel(repository, syncRepository!!) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(WorkersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkersViewModel(repository, syncRepository!!) as T
        }
        if (modelClass.isAssignableFrom(WorkerDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerDetailViewModel(syncRepository!!) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
