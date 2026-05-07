package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.User
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    RECENT_ACTIVITY, JOINED_DATE
}

class UsersViewModel(
    private val repository: AdminFirestoreRepository = AdminFirestoreRepository()
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _roleFilter = MutableStateFlow<String?>("contractor") // Default to contractor as per old UI
    val roleFilter: StateFlow<String?> = _roleFilter

    private val _premiumFilter = MutableStateFlow<Boolean?>(null)
    val premiumFilter: StateFlow<Boolean?> = _premiumFilter

    private val _blockedFilter = MutableStateFlow<Boolean?>(null)
    val blockedFilter: StateFlow<Boolean?> = _blockedFilter

    private val _sortOrder = MutableStateFlow(SortOrder.RECENT_ACTIVITY)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    val filteredUsers: StateFlow<List<User>> = combine(
        _allUsers, _searchQuery, _roleFilter, _premiumFilter, _blockedFilter, _sortOrder
    ) { users, query, role, premium, blocked, sort ->
        users.filter { user ->
            val matchesQuery = query.isEmpty() || user.name.contains(query, ignoreCase = true) || user.email.contains(query, ignoreCase = true)
            val matchesRole = role == null || user.role == role
            val matchesPremium = premium == null || user.isPremium == premium
            val matchesBlocked = blocked == null || user.isBlocked == blocked
            matchesQuery && matchesRole && matchesPremium && matchesBlocked
        }.sortedWith { u1, u2 ->
            when (sort) {
                SortOrder.RECENT_ACTIVITY -> u2.lastActive.compareTo(u1.lastActive)
                SortOrder.JOINED_DATE -> u2.createdAt.compareTo(u1.createdAt)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            repository.getAllUsers().collectLatest {
                _allUsers.value = it
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRoleFilter(role: String?) {
        _roleFilter.value = role
    }

    fun setPremiumFilter(premium: Boolean?) {
        _premiumFilter.value = premium
    }

    fun setBlockedFilter(blocked: Boolean?) {
        _blockedFilter.value = blocked
    }

    fun setSortOrder(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun toggleBlockStatus(user: User) {
        viewModelScope.launch {
            repository.setUserBlockedStatus(user.uid, !user.isBlocked)
        }
    }

    fun togglePremiumStatus(user: User) {
        viewModelScope.launch {
            repository.setUserPremiumStatus(user.uid, !user.isPremium)
        }
    }

    fun getUser(userId: String): Flow<User?> = repository.getUser(userId)
}
