package com.example.data.parser

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ExpressionParser {

    /**
     * Checks if the expression is syntactically valid and completely evaluatable.
     * Returns the formatted result, or null if it cannot be fully parsed.
     */
    fun getPreview(expression: String): String? {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        if (sanitized.isEmpty()) return null

        // Check if expression ends with an operator or open parenthesis, in which case we shouldn't show partial preview
        val lastChar = sanitized.lastOrNull()
        if (lastChar != null && (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/' || lastChar == '(')) {
            // Try to parse the expression without the last trailing operator
            val subExpr = sanitized.dropLast(1)
            if (subExpr.isEmpty()) return null
            return tryEvaluate(subExpr)
        }

        return tryEvaluate(sanitized)
    }

    private fun tryEvaluate(expr: String): String? {
        try {
            val parser = Parser(expr)
            val result = parser.parse()
            if (result.isInfinite() || result.isNaN()) return null
            return formatResult(result)
        } catch (e: Exception) {
            return null
        }
    }

    fun evaluate(expression: String): String {
        try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace(" ", "")

            if (sanitized.isEmpty()) return ""

            val parser = Parser(sanitized)
            val result = parser.parse()
            
            return formatResult(result)
        } catch (e: ArithmeticException) {
            return "Divide by zero"
        } catch (e: Exception) {
            return "Format Error"
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite()) return "Value too large"
        if (value.isNaN()) return "Error"
        
        val bd = try {
            BigDecimal(value.toString()).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros()
        } catch (e: Exception) {
            BigDecimal(value).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros()
        }
        
        val doubleVal = bd.toDouble()
        
        if (doubleVal == Math.floor(doubleVal) && !doubleVal.isInfinite() && doubleVal <= Long.MAX_VALUE && doubleVal >= Long.MIN_VALUE) {
            return doubleVal.toLong().toString()
        }
        
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,##0.##########", symbols)
        df.maximumFractionDigits = 10
        return df.format(doubleVal)
    }

    private class Parser(private val input: String) {
        private var pos = 0

        fun parse(): Double {
            val result = parseExpression()
            if (pos < input.length) {
                throw IllegalArgumentException("Unexpected character at position $pos")
            }
            return result
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                if (consume('+')) {
                    value += parseTerm()
                } else if (consume('-')) {
                    value -= parseTerm()
                } else {
                    break
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                if (consume('*')) {
                    value *= parseFactor()
                } else if (consume('/')) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) {
                        throw ArithmeticException("Division by zero")
                    }
                    value /= divisor
                } else {
                    break
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            var value = parsePrimary()
            while (consume('%')) {
                value /= 100.0
            }
            return value
        }

        private fun parsePrimary(): Double {
            if (consume('+')) {
                return parsePrimary()
            }
            if (consume('-')) {
                return -parsePrimary()
            }
            if (consume('(')) {
                val value = parseExpression()
                consume(')') // consume closed parenthesis if present, or continue
                return value
            }

            val start = pos
            if (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                var hasDot = false
                while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                    if (input[pos] == '.') {
                        if (hasDot) break // Stop parsing number if multiple decimals
                        hasDot = true
                    }
                    pos++
                }
                val numStr = input.substring(start, pos)
                return numStr.toDoubleOrNull() ?: 0.0
            }

            throw IllegalArgumentException("Unexpected token at pos $pos")
        }

        private fun consume(char: Char): Boolean {
            if (pos < input.length && input[pos] == char) {
                pos++
                return true
            }
            return false
        }
    }
}
