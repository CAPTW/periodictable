package com.chemtable.interactive.feature.search

enum class SearchSortOption(val label: String) {
    AtomicNumberAsc("원자번호 ↑"),
    AtomicNumberDesc("원자번호 ↓"),
    MolarMassAsc("몰질량 ↑"),
    MolarMassDesc("몰질량 ↓"),
    ElectronegativityDesc("전기음성도 ↓"),
    ThermalConductivityDesc("열전도도 ↓")
}
