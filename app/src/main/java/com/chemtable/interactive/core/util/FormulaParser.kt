package com.chemtable.interactive.core.util

import com.chemtable.interactive.core.model.ParsedFormulaComponent
import com.chemtable.interactive.core.model.ParsedFormulaResult

import javax.inject.Inject

class FormulaParser @Inject constructor() {

    fun parse(formula: String): ParsedFormulaResult {
        val normalized = normalizeFormula(formula)
        if (normalized.isEmpty()) return ParsedFormulaResult(formula = "", components = emptyList())

        val merged = mutableMapOf<String, Int>()
        val terms = splitTopLevelTerms(normalized)

        for (term in terms) {
            val trimmed = term.trim()
            if (trimmed.isBlank()) continue
            val termMap = parseTerm(trimmed)
            termMap.forEach { (symbol, count) ->
                merged[symbol] = (merged[symbol] ?: 0) + count
            }
        }

        if (merged.isEmpty()) {
            return ParsedFormulaResult(formula = normalized, components = emptyList())
        }

        val components = merged.entries
            .sortedBy { it.key }
            .map { (symbol, count) -> ParsedFormulaComponent(symbol = symbol, count = count) }
        return ParsedFormulaResult(formula = normalized, components = components)
    }

    private fun parseTerm(term: String): Map<String, Int> {
        val termMap = linkedMapOf<String, Int>()
        var index = 0

        val leading = parseCount(term, index)
        val multiplier = leading.value
        index = leading.nextIndex

        parseGroup(
            formula = term,
            start = index,
            multiplier = multiplier,
            result = termMap
        )
        return termMap
    }

    private fun parseGroup(
        formula: String,
        start: Int,
        multiplier: Int,
        result: MutableMap<String, Int>
    ): Int {
        var index = start
        while (index < formula.length) {
            val char = formula[index]
            when {
                char == '(' -> {
                    val nested = linkedMapOf<String, Int>()
                    index = parseGroup(formula, index + 1, 1, nested)
                    val count = parseCount(formula, index)
                    index = count.nextIndex
                    nested.forEach { (symbol, countPerGroup) ->
                        val total = (result[symbol] ?: 0) + (countPerGroup * count.value * multiplier)
                        result[symbol] = total
                    }
                }

                char == ')' -> return index + 1

                char.isUpperCase() -> {
                    val readSymbol = parseElementSymbol(formula, index)
                    val symbol = readSymbol.value
                    index = readSymbol.nextIndex

                    val count = parseCount(formula, index)
                    index = count.nextIndex

                    val total = (result[symbol] ?: 0) + (count.value * multiplier)
                    result[symbol] = total
                }

                else -> index++
            }
        }
        return index
    }

    private fun parseElementSymbol(formula: String, start: Int): ReadResult<String> {
        if (start >= formula.length || !formula[start].isUpperCase()) {
            return ReadResult("", start)
        }

        var end = start + 1
        while (end < formula.length && formula[end].isLowerCase()) {
            end++
        }
        return ReadResult(formula.substring(start, end), end)
    }

    private fun parseCount(formula: String, index: Int): ReadResult<Int> {
        if (index >= formula.length) return ReadResult(1, index)

        var current = index
        var value = 0
        while (current < formula.length) {
            val ch = formula[current]
            val digit = toDigit(ch)
            if (digit == null) break
            value = (value * 10) + digit
            current++
        }
        return ReadResult(if (value == 0) 1 else value, current)
    }

    private fun splitTopLevelTerms(formula: String): List<String> {
        val normalized = formula
            .replace("→", "+")
            .replace("->", "+")
            .replace("·", "+")

        val terms = mutableListOf<String>()
        val builder = StringBuilder()
        var depth = 0

        var index = 0
        while (index < normalized.length) {
            val ch = normalized[index]
            when (ch) {
                '(' -> {
                    depth++
                    builder.append(ch)
                }

                ')' -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    builder.append(ch)
                }

                '+' -> {
                    if (depth == 0) {
                        terms.add(builder.toString())
                        builder.clear()
                    } else {
                        builder.append(ch)
                    }
                }

                else -> builder.append(ch)
            }
            index++
        }
        if (builder.isNotBlank()) terms.add(builder.toString())
        return terms
    }

    private fun normalizeFormula(formula: String): String =
        formula
            .replace(" ", "")
            .replace("[", "(")
            .replace("]", ")")
            .trim()

    private fun toDigit(ch: Char): Int? = when (ch) {
        in '0'..'9' -> ch - '0'
        '₀' -> 0
        '₁' -> 1
        '₂' -> 2
        '₃' -> 3
        '₄' -> 4
        '₅' -> 5
        '₆' -> 6
        '₇' -> 7
        '₈' -> 8
        '₉' -> 9
        else -> null
    }

    private data class ReadResult<T>(val value: T, val nextIndex: Int)
}

