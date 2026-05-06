package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.AdminStats
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: AdminFirestoreRepository = AdminFirestoreRepository()
) : ViewModel() {

    private val _stats = MutableStateFlow(AdminStats())
    val stats: StateFlow<AdminStats> = _stats

    init {
        viewModelScope.launch {
            repository.getAdminStats().collectLatest {
                _stats.value = it
            }
        }
    }
}
