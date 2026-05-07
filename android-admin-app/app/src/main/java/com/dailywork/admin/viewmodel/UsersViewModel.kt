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

    private data class UserFilterState(
        val query: String,
        val role: String?,
        val premium: Boolean?,
        val blocked: Boolean?,
        val sortOrder: SortOrder
    )

    private val filterState = combine(
        _searchQuery,
        _roleFilter,
        _premiumFilter,
        _blockedFilter,
        _sortOrder
    ) { query: String, role: String?, premium: Boolean?, blocked: Boolean?, sortOrder: SortOrder ->
        UserFilterState(
            query = query,
            role = role,
            premium = premium,
            blocked = blocked,
            sortOrder = sortOrder
        )
    }

    val filteredUsers: StateFlow<List<User>> = combine(
        _allUsers,
        filterState
    ) { users: List<User>, filters: UserFilterState ->
        val query = filters.query.trim().lowercase()

        users
            .filter { user ->
                query.isBlank() ||
                        user.name.lowercase().contains(query) ||
                        user.email.lowercase().contains(query)
            }
            .filter { user ->
                filters.role == null || user.role == filters.role
            }
            .filter { user ->
                filters.premium == null || user.isPremium == filters.premium
            }
            .filter { user ->
                filters.blocked == null || user.isBlocked == filters.blocked
            }
            .let { list ->
                when (filters.sortOrder) {
                    SortOrder.RECENT_ACTIVITY -> list.sortedByDescending { it.lastActive }
                    SortOrder.JOINED_DATE -> list.sortedByDescending { it.createdAt }
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
