package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.User
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UsersViewModel(
    private val repository: AdminFirestoreRepository = AdminFirestoreRepository()
) : ViewModel() {

    private val _contractors = MutableStateFlow<List<User>>(emptyList())
    val contractors: StateFlow<List<User>> = _contractors

    private val _personalUsers = MutableStateFlow<List<User>>(emptyList())
    val personalUsers: StateFlow<List<User>> = _personalUsers

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            repository.getUsers("contractor").collectLatest {
                _contractors.value = it
            }
        }
        viewModelScope.launch {
            repository.getUsers("personal").collectLatest {
                _personalUsers.value = it
            }
        }
    }

    fun toggleBlockStatus(user: User) {
        viewModelScope.launch {
            repository.setUserBlockedStatus(user.uid, !user.isBlocked)
        }
    }
}
