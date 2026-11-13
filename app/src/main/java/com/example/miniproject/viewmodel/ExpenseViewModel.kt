package com.example.miniproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.miniproject.data.Expense
import com.example.miniproject.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ExpenseViewModel : ViewModel() {
    private val repository = ExpenseRepository()

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState

    private var currentUserId: String? = null
    private var currentMonth: String? = null

    // ✅ Load ALL expenses
    fun loadExpenses(userId: String) {
        currentUserId = userId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.getExpenses(userId)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(expenses = list, isLoading = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(error = err.message, isLoading = false)
                }
        }
    }

    // ✅ Load expenses BY MONTH
    fun loadExpensesByMonth(userId: String, month: String) {
        currentUserId = userId
        currentMonth = month
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.getExpensesByMonth(userId, month)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(expenses = list, isLoading = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(error = err.message, isLoading = false)
                }
        }
    }

    // ✅ ADD expense
    fun addExpense(expense: Expense, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.addExpense(expense)
            result.onSuccess {
                onComplete(true, "Expense added successfully!")
                currentUserId?.let { loadExpenses(it) } // Refresh list
            }.onFailure {
                onComplete(false, it.message ?: "Failed to add expense")
            }
        }
    }

    // ✅ UPDATE expense
    fun updateExpense(expense: Expense, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateExpense(expense)
            result.onSuccess {
                onComplete(true, "Expense updated successfully!")
                currentUserId?.let {
                    if (currentMonth != null) loadExpensesByMonth(it, currentMonth!!)
                    else loadExpenses(it)
                }
            }.onFailure {
                onComplete(false, it.message ?: "Failed to update expense")
            }
        }
    }

    // ✅ DELETE expense
    fun deleteExpense(expenseId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteExpense(expenseId)
            result.onSuccess {
                onComplete(true, "Expense deleted successfully!")
                currentUserId?.let { loadExpenses(it) } // Refresh list
            }.onFailure {
                onComplete(false, it.message ?: "Failed to delete expense")
            }
        }
    }

    // ✅ Refresh current list
    fun refresh() {
        currentUserId?.let {
            if (currentMonth != null) loadExpensesByMonth(it, currentMonth!!)
            else loadExpenses(it)
        }
    }
}
