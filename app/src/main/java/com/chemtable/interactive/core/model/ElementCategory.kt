package com.chemtable.interactive.core.model

enum class ElementCategory {
    ALKALI_METAL,
    ALKALINE_EARTH,
    TRANSITION_METAL,
    POST_TRANSITION_METAL,
    METALLOID,
    NONMETAL,
    HALOGEN,
    NOBLE_GAS,
    LANTHANIDE,
    ACTINIDE,
    UNKNOWN;

    companion object {
        fun from(value: String?): ElementCategory =
            when (value?.trim()?.uppercase()) {
                "ALKALI_METAL" -> ALKALI_METAL
                "ALKALINE_EARTH" -> ALKALINE_EARTH
                "TRANSITION_METAL" -> TRANSITION_METAL
                "POST_TRANSITION_METAL" -> POST_TRANSITION_METAL
                "METALLOID" -> METALLOID
                "NONMETAL" -> NONMETAL
                "HALOGEN" -> HALOGEN
                "NOBLE_GAS" -> NOBLE_GAS
                "LANTHANIDE" -> LANTHANIDE
                "ACTINIDE" -> ACTINIDE
                else -> UNKNOWN
            }
    }
}
