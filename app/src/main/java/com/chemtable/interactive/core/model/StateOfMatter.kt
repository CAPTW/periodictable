package com.chemtable.interactive.core.model

enum class StateOfMatter {
    SOLID,
    LIQUID,
    GAS,
    UNKNOWN;

    companion object {
        fun from(value: String?): StateOfMatter =
            when (value?.trim()?.uppercase()) {
                "SOLID" -> SOLID
                "LIQUID" -> LIQUID
                "GAS" -> GAS
                else -> UNKNOWN
            }
    }
}
