package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.CalculationHistory
import com.example.data.parser.ExpressionParser
import com.example.data.repository.CalculationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = CalculationRepository(database.calculationHistoryDao())

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    // Real-time preview of the current expression
    val previewResult: StateFlow<String?> = _expression
        .combine(MutableStateFlow(Unit)) { expr, _ ->
            ExpressionParser.getPreview(expr)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val historyList: StateFlow<List<CalculationHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    fun onKeyPress(key: String) {
        when (key) {
            "C" -> clear()
            "⌫" -> backspace()
            "=" -> evaluate()
            "+/-" -> toggleSign()
            "()" -> handleParentheses()
            else -> append(key)
        }
    }

    private fun clear() {
        _expression.value = ""
    }

    private fun backspace() {
        val current = _expression.value
        if (current.isNotEmpty()) {
            _expression.value = current.dropLast(1)
        }
    }

    private fun evaluate() {
        val current = _expression.value
        if (current.isEmpty()) return

        // Skip evaluation if it's already just showing double/error/limit
        if (current == "Format Error" || current == "Divide by zero" || current == "Error" || current == "Value too large") {
            _expression.value = ""
            return
        }

        val result = ExpressionParser.evaluate(current)
        
        if (result != "Format Error" && result != "Divide by zero" && result != "Error" && result != "Value too large") {
            viewModelScope.launch {
                repository.insert(CalculationHistory(expression = current, result = result))
            }
        }

        _expression.value = result
    }

    private fun handleParentheses() {
        val current = _expression.value
        if (current.isEmpty()) {
            _expression.value = "("
            return
        }

        val openCount = current.count { it == '(' }
        val closeCount = current.count { it == ')' }
        val lastChar = current.last()

        if (openCount > closeCount && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) {
            _expression.value = current + ")"
        } else {
            if (lastChar.isDigit() || lastChar == ')' || lastChar == '%') {
                _expression.value = current + "×("
            } else {
                _expression.value = current + "("
            }
        }
    }

    private fun toggleSign() {
        val current = _expression.value
        if (current.isEmpty()) {
            _expression.value = "(-"
            return
        }

        val pattern = Regex("\\(-\\d+(\\.\\d*)?\\)$")
        val matchResult = pattern.find(current)
        
        if (matchResult != null) {
            val originalMatch = matchResult.value
            val numberOnly = originalMatch.substring(2, originalMatch.length - 1)
            _expression.value = current.substring(0, matchResult.range.first) + numberOnly
        } else {
            val lastNumTokenRange = Regex("\\d+(\\.\\d*)?$").find(current)?.range
            if (lastNumTokenRange != null) {
                val lastNum = current.substring(lastNumTokenRange)
                val prefix = current.substring(0, lastNumTokenRange.first)
                
                if (prefix.endsWith("(-")) {
                    _expression.value = prefix.dropLast(2) + lastNum
                } else {
                    _expression.value = prefix + "(-" + lastNum + ")"
                }
            } else {
                if (current.endsWith("(-")) {
                    _expression.value = current.dropLast(2)
                } else {
                    _expression.value = current + "(-"
                }
            }
        }
    }

    private fun append(key: String) {
        val current = _expression.value
        
        // If screen is showing error string, replace it
        if (current == "Format Error" || current == "Divide by zero" || current == "Error" || current == "Value too large") {
            if (isOperator(key.first())) {
                _expression.value = "0" + key
            } else {
                _expression.value = key
            }
            return
        }

        if (key == ".") {
            if (current.isEmpty()) {
                _expression.value = "0."
                return
            }
            val lastChar = current.last()
            if (isOperator(lastChar) || lastChar == '(') {
                _expression.value = current + "0."
                return
            }
            
            val lastNumToken = current.split(Regex("[+\\-×÷()]")).lastOrNull() ?: ""
            if (!lastNumToken.contains(".")) {
                _expression.value = current + "."
            }
            return
        }

        if (isOperator(key.first())) {
            if (current.isEmpty()) {
                if (key == "-") {
                    _expression.value = "-"
                } else {
                    _expression.value = "0$key"
                }
                return
            }
            val lastChar = current.last()
            if (isOperator(lastChar)) {
                if (lastChar != '%') {
                    _expression.value = current.dropLast(1) + key
                } else {
                    _expression.value = current + key
                }
                return
            }
            if (lastChar == '(') {
                if (key == "-") {
                    _expression.value = current + "-"
                }
                return
            }
            _expression.value = current + key
            return
        }

        if (current == "0") {
            _expression.value = key
        } else {
            _expression.value = current + key
        }
    }

    private fun isOperator(c: Char): Boolean {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '%'
    }

    fun selectHistoryItem(history: CalculationHistory) {
        _expression.value = history.expression
        _showHistory.value = false
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteHistoryItem(item: CalculationHistory) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun setShowHistory(show: Boolean) {
        _showHistory.value = show
    }

    fun toggleVibration() {
        _vibrationEnabled.value = !_vibrationEnabled.value
    }
}
