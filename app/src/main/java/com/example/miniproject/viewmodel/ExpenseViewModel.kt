package com.example.miniproject.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.miniproject.data.Expense
import com.example.miniproject.repository.ExpenseRepository
import kotlinx.coroutines.launch

class ExpenseViewModel : ViewModel() {
    private val repo = ExpenseRepository()

    private val _expenses = MutableLiveData<List<Expense>>()
    val expenses: LiveData<List<Expense>> = _expenses

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // OCR extracted data holder
    val scannedAmount = MutableLiveData<String>()
    val scannedDate = MutableLiveData<String>()

    fun loadExpenses(userId: String) {
        viewModelScope.launch {
            _loading.value = true
            repo.getExpenses(userId).onSuccess { list ->
                _expenses.value = list
            }.onFailure {
                _message.value = "Failed to load expenses"
            }
            _loading.value = false
        }
    }

    fun saveExpense(expense: Expense, userId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            repo.addExpense(expense.copy(userId = userId))
                .onSuccess {
                    _message.value = "Expense saved"
                    loadExpenses(userId)
                    onResult(true)
                }
                .onFailure {
                    _message.value = "Save failed: ${it.message}"
                    onResult(false)
                }
            _loading.value = false
        }
    }

    fun clearMessage() { _message.value = null }

    // Simulate OCR processing — replace with actual OCR (ML Kit)
    fun processReceiptImage(imagePath: String) {
        _loading.value = true
        // Simulate delay
        scannedAmount.value = "15.90"
        scannedDate.value = "24 Apr 2025"
        _loading.value = false
    }
}
