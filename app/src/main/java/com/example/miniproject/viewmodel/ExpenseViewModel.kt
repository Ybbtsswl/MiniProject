package com.example.miniproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.miniproject.data.Category
import com.example.miniproject.data.Expense
import com.example.miniproject.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalExpense: Double = 0.0,
    val selectedExpense: Expense? = null
)

class ExpenseViewModel : ViewModel() {
    private val repository = ExpenseRepository()

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var currentMonth: String? = null

    // ✅ Categories State
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _isLoadingCategories = MutableStateFlow(false)
    val isLoadingCategories: StateFlow<Boolean> = _isLoadingCategories.asStateFlow()

    private fun updateTotal() {
        val total = _uiState.value.expenses.sumOf { it.amount }
        _uiState.value = _uiState.value.copy(totalExpense = total)
    }

    // ✅ Load ALL expenses
    fun loadExpenses(userId: String) {
        currentUserId = userId
        currentMonth = null
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.getExpenses(userId)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        expenses = list,
                        isLoading = false,
                        selectedExpense = null
                    )
                    updateTotal()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to load expenses",
                        isLoading = false
                    )
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
                    _uiState.value = _uiState.value.copy(
                        expenses = list,
                        isLoading = false,
                        selectedExpense = null
                    )
                    updateTotal()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Failed to load expenses",
                        isLoading = false
                    )
                }
        }
    }

    // ✅ FILTER expenses
    fun filterExpenses(
        userId: String,
        startDate: String? = null,
        endDate: String? = null,
        categoryId: String? = null
    ) {
        currentUserId = userId
        currentMonth = null
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.filterExpenses(userId, startDate, endDate, categoryId)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        expenses = list,
                        isLoading = false,
                        selectedExpense = null
                    )
                    updateTotal()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        error = err.message ?: "Filter failed",
                        isLoading = false
                    )
                }
        }
    }

    // ✅ ADD expense
    fun addExpense(expense: Expense, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.addExpense(expense)
                .onSuccess {
                    onComplete(true, "Expense added successfully!")
                    refresh()
                }
                .onFailure {
                    onComplete(false, it.message ?: "Failed to add expense")
                }
        }
    }

    // ✅ SELECT expense
    fun selectExpense(expense: Expense?) {
        _uiState.value = _uiState.value.copy(selectedExpense = expense)
    }

    // ✅ UPDATE expense
    fun updateExpense(expense: Expense, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.updateExpense(expense)
                .onSuccess {
                    onComplete(true, "Expense updated successfully!")
                    _uiState.value = _uiState.value.copy(selectedExpense = null)
                    refresh()
                }
                .onFailure {
                    onComplete(false, it.message ?: "Failed to update expense")
                }
        }
    }

    // ✅ DELETE expense
    fun deleteExpense(expenseId: String, onComplete: (Boolean, String, Boolean) -> Unit) {
        viewModelScope.launch {
            val expense = _uiState.value.expenses.find { it.expenseId == expenseId }
            val wasFromSplit = expense?.isFromSplit == true && !expense.splitBillId.isNullOrEmpty()
            val splitBillId = expense?.splitBillId

            if (wasFromSplit && splitBillId != null) {
                repository.clearSplitBillReference(splitBillId)
                    .onFailure {
                        onComplete(false, "Failed to clear split bill reference", false)
                        return@launch
                    }
            }

            repository.deleteExpense(expenseId, expense?.userId ?: currentUserId)
                .onSuccess {
                    onComplete(true, "Expense deleted successfully!", wasFromSplit)
                    _uiState.value = _uiState.value.copy(selectedExpense = null)
                    refresh()
                }
                .onFailure {
                    onComplete(false, it.message ?: "Failed to delete expense", false)
                }
        }
    }

    // ✅ Clear error
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ✅ Refresh
    fun refresh() {
        currentUserId?.let { id ->
            if (currentMonth != null) {
                loadExpensesByMonth(id, currentMonth!!)
            } else {
                loadExpenses(id)
            }
        }
    }

    // ✅ Load Categories — FIXED
    fun loadCategories(userId: String?) {
        viewModelScope.launch {
            _isLoadingCategories.value = true
            val result = repository.getCategories(userId)
            _isLoadingCategories.value = false

            if (result.isSuccess) {
                _categories.value = result.getOrNull() ?: emptyList()
            }
        }
    }

}
