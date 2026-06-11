package com.chemtable.interactive.feature.elementdetail

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.Isotope
import com.chemtable.interactive.core.model.StateOfMatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ElementDetailScreen(
    atomicNumber: Int,
    onOpenNotes: (Int) -> Unit,
    onAddNote: (Int) -> Unit,
    onOpenGlossaryTerm: (String) -> Unit,
    onPlayMiniGame: (Int) -> Unit,
    viewModel: ElementDetailViewModel = hiltViewModel()
) {
    val element by viewModel.element.collectAsState()
    val isotopes by viewModel.isotopes.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember {
        listOf("개요", "원자/전자", "열·물성", "결정/3D", "위험/NFPA", "존재비율", "동위원소")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (element == null) {
            item { Text("원자번호 $atomicNumber 원소를 찾을 수 없습니다.") }
        } else {
            val current = element!!
            item {
                Text("${current.nameKo} (${current.symbol})", style = MaterialTheme.typography.titleLarge)
                Text("원자번호 ${current.atomicNumber} · 원자량 ${current.molarMass.f(3)} g/mol")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { onOpenNotes(current.atomicNumber) }) { Text("메모 보기") }
                    TextButton(onClick = { onAddNote(current.atomicNumber) }) { Text("메모 작성") }
                    FilledTonalButton(onClick = { onPlayMiniGame(current.atomicNumber) }) { Text("분자 만들기") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("isotope","half_life","electronegativity","crystal_structure","nfpa_diamond","cas_registry_number").forEach { term ->
                        AssistChip(onClick = { onOpenGlossaryTerm(term) }, label = { Text(termLabel(term)) })
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                    }
                }
            }

            when (selectedTab) {
                0 -> items(current.overviewRows()) { RowItem(it, onOpenGlossaryTerm) }
                1 -> {
                    item { ElectronShellView(current) ; HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                    items(current.atomicRows()) { RowItem(it, onOpenGlossaryTerm) }
                }
                2 -> items(current.materialRows()) { RowItem(it, onOpenGlossaryTerm) }
                3 -> {
                    item { Crystal3DView(current); HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                    items(current.crystalRows()) { RowItem(it, onOpenGlossaryTerm) }
                }
                4 -> {
                    item { NfpaDiamond(current); HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                    items(current.hazardRows()) { RowItem(it, onOpenGlossaryTerm) }
                }
                5 -> item { AbundanceChart(current) }
                else -> {
                    item { IsotopeSummary(isotopes); HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                    if (isotopes.isEmpty()) item { Text("등록된 동위원소 데이터가 없습니다.") }
                    else items(isotopes) { isotope -> IsotopeRow(isotope) ; HorizontalDivider() }
                }
            }
        }
    }
}

@Composable
private fun RowItem(row: Triple<String, String, String?>, onOpenGlossaryTerm: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(row.first, style = MaterialTheme.typography.labelMedium)
        Text(row.second, style = MaterialTheme.typography.bodyMedium)
        row.third?.let { TextButton(onClick = { onOpenGlossaryTerm(it) }) { Text("용어 설명 보기") } }
    }
    HorizontalDivider()
}

@Composable
private fun ElectronShellView(element: Element) {
    val shells = remember(element.atomicNumber, element.electronShells) { parseShells(element.electronShells, element.atomicNumber) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("전자 껍질 Interactive", style = MaterialTheme.typography.titleSmall)
        Canvas(
            modifier = Modifier.fillMaxWidth().height(250.dp).pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, rot -> scale = (scale * zoom).coerceIn(0.6f, 3f); rotation += rot * 57.2958f }
            }
        ) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val base = min(size.width, size.height) * 0.12f * scale
            val step = min(size.width, size.height) * 0.08f * scale
            drawCircle(Color(0xFFFFB74D), radius = min(size.width, size.height) * 0.05f * scale, center = c)
            shells.forEachIndexed { i, count ->
                val r = base + step * (i + 1)
                drawCircle(Color(0xFF90A4AE), radius = r, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                if (count > 0) {
                    val da = 360f / count
                    repeat(count) { idx ->
                        val rad = ((rotation + idx * da) / 180f) * PI.toFloat()
                        drawCircle(Color(0xFF1565C0), 4f, Offset(c.x + cos(rad) * r, c.y + sin(rad) * r))
                    }
                }
            }
        }
        Text("껍질 분포: ${shells.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Crystal3DView(element: Element) {
    var rotX by remember { mutableFloatStateOf(20f) }
    var rotY by remember { mutableFloatStateOf(25f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var showAtoms by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("결정 구조 3D", style = MaterialTheme.typography.titleSmall)
        Text("${element.crystalStructure ?: "N/A"} · ${element.crystalSystem ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("원자 표시", style = MaterialTheme.typography.bodySmall); Switch(showAtoms, { showAtoms = it })
        }
        Canvas(
            modifier = Modifier.fillMaxWidth().height(250.dp).pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ -> rotY += pan.x * 0.25f; rotX += pan.y * 0.25f; scale = (scale * zoom).coerceIn(0.7f, 2.5f) }
            }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val s = min(size.width, size.height) * 0.22f * scale
            val pts = listOf(
                floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f), floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f),
                floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)
            )
            fun p(v: FloatArray): Offset {
                val rx = Math.toRadians(rotX.toDouble()); val ry = Math.toRadians(rotY.toDouble())
                val x0 = v[0].toDouble(); val y0 = v[1].toDouble(); val z0 = v[2].toDouble()
                val y1 = y0 * cos(rx) - z0 * sin(rx); val z1 = y0 * sin(rx) + z0 * cos(rx)
                val x2 = x0 * cos(ry) + z1 * sin(ry); val z2 = -x0 * sin(ry) + z1 * cos(ry)
                val d = (3.2 - z2).coerceAtLeast(1.2)
                return Offset(center.x + (x2 / d * s).toFloat(), center.y + (y1 / d * s).toFloat())
            }
            val pr = pts.map { p(it) }
            val edges = listOf(0 to 1,1 to 2,2 to 3,3 to 0,4 to 5,5 to 6,6 to 7,7 to 4,0 to 4,1 to 5,2 to 6,3 to 7)
            edges.forEach { (a,b) -> drawLine(Color(0xFF4FC3F7), pr[a], pr[b], strokeWidth = 4f) }
            if (showAtoms) pr.forEach { drawCircle(Color(0xFF01579B), 5f, it) }
        }
    }
}

@Composable
private fun NfpaDiamond(element: Element) {
    val health = (element.hazardHealth ?: 0).coerceIn(0, 4)
    val flame = (element.hazardFlammability ?: 0).coerceIn(0, 4)
    val react = (element.hazardReactivity ?: 0).coerceIn(0, 4)
    val special = element.hazardSpecial ?: "-"
    val white = remember { Paint().apply { color = android.graphics.Color.BLACK; textAlign = Paint.Align.CENTER; textSize = 34f; isFakeBoldText = true } }
    val black = remember { Paint().apply { color = android.graphics.Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 40f; isFakeBoldText = true } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("NFPA 다이아몬드", style = MaterialTheme.typography.titleSmall)
        Canvas(modifier = Modifier.size(190.dp).align(Alignment.CenterHorizontally)) {
            val cx = size.width / 2f; val cy = size.height / 2f; val h = min(size.width, size.height) * 0.34f
            val t = Offset(cx, cy - h); val r = Offset(cx + h, cy); val b = Offset(cx, cy + h); val l = Offset(cx - h, cy)
            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(l.x,l.y); lineTo(t.x,t.y); lineTo(cx,cy); close() }, Color(0xFF1976D2))
            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(t.x,t.y); lineTo(r.x,r.y); lineTo(cx,cy); close() }, Color(0xFFD32F2F))
            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(r.x,r.y); lineTo(b.x,b.y); lineTo(cx,cy); close() }, Color(0xFFFBC02D))
            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(b.x,b.y); lineTo(l.x,l.y); lineTo(cx,cy); close() }, Color.White)
            drawContext.canvas.nativeCanvas.apply {
                drawText(health.toString(), cx - h * 0.38f, cy, black)
                drawText(flame.toString(), cx, cy - h * 0.34f, black)
                drawText(react.toString(), cx + h * 0.38f, cy, black)
                drawText(special, cx, cy + h * 0.42f, white)
            }
        }
    }
}

@Composable
private fun AbundanceChart(element: Element) {
    var logScale by remember { mutableStateOf(true) }
    val rows = listOf(
        "우주" to element.abundanceUniverse, "태양" to element.abundanceSun, "해양" to element.abundanceOcean,
        "인체" to element.abundanceHuman, "지각" to element.abundanceCrust, "운석" to element.abundanceMeteorite
    )
    val scaled = rows.mapNotNull { (_, v) -> v?.let { if (logScale) log10(it + 1.0) else it } }
    val maxV = scaled.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("존재비율 비교", style = MaterialTheme.typography.titleSmall)
            AssistChip(onClick = { logScale = !logScale }, label = { Text(if (logScale) "로그 ON" else "로그 OFF") })
        }
        rows.forEach { (label, value) ->
            val s = value?.let { if (logScale) log10(it + 1.0) else it } ?: 0.0
            val progress = (s / maxV).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$label: ${value?.f(6) ?: "N/A"} ppm")
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun IsotopeSummary(isotopes: List<Isotope>) {
    val stable = isotopes.count { it.isStable }
    val radioactive = isotopes.size - stable
    Text("동위원소: 안정 ${stable}개 / 방사성 ${radioactive}개", style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun IsotopeRow(isotope: Isotope) {
    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${isotope.symbol} · 질량수 ${isotope.massNumber}", style = MaterialTheme.typography.titleSmall)
        Text("중성자 ${isotope.neutronCount} · 반감기 ${isotope.halfLife ?: if (isotope.isStable) "Stable" else "N/A"}")
        Text("붕괴 모드 ${isotope.decayMode ?: "N/A"} · 자연존재비 ${isotope.naturalAbundance?.f(5) ?: "N/A"}")
    }
}

private fun parseShells(text: String?, atomicNumber: Int): List<Int> {
    val parsed = text?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.filter { it > 0 }.orEmpty()
    if (parsed.isNotEmpty()) return parsed
    val caps = listOf(2,8,18,32,32,18,8,2)
    var left = atomicNumber
    val result = mutableListOf<Int>()
    for (c in caps) {
        if (left <= 0) break
        val u = min(left, c); result += u; left -= u
    }
    return result
}

private fun Element.overviewRows(): List<Triple<String, String, String?>> = listOf(
    Triple("라틴 이름", latinName ?: "N/A", "latin_name"),
    Triple("영어 발음(IPA)", englishPronunciation ?: "N/A", "ipa_pronunciation"),
    Triple("발견 연도", discoveryYear?.toString() ?: "N/A", "discovery_year"),
    Triple("발견자", discoverer ?: "N/A", "discoverer"),
    Triple("발견 국가", discoveryCountry ?: "N/A", "discovery_country"),
    Triple("100g당 비용", costPer100gUsd?.let { "$${it.f(2)} (기준 ${costReferenceDate ?: "N/A"})" } ?: "N/A", "element_cost"),
    Triple("CAS / CID / RTECS", "${casNumber ?: "N/A"} / ${pubchemCid ?: "N/A"} / ${rtecsNumber ?: "N/A"}", "cas_registry_number"),
    Triple("Block / Period / Group", "${block ?: "N/A"} / $period / $group", null),
    Triple("데이터 출처", dataSource ?: "N/A", null),
    Triple("업데이트 / 신뢰도", "${dataUpdatedAt ?: "N/A"} / ${dataConfidence?.f(2) ?: "N/A"}", null)
)

private fun Element.atomicRows(): List<Triple<String, String, String?>> = listOf(
    Triple("전자/양성자/중성자", "${electronCount ?: "N/A"} / ${protonCount ?: "N/A"} / ${neutronCount ?: "N/A"}", null),
    Triple("전자 배치", electronConfiguration.ifBlank { "N/A" }, null),
    Triple("대표 이온 전하", commonIonCharge ?: "N/A", null),
    Triple("이온화 에너지(eV)", ionizationPotential?.f(3) ?: "N/A", "ionization_energy"),
    Triple("이온화 가능성", ionizationPossibility ?: "N/A", null),
    Triple("원자/공유/vdW 반지름(pm)", "${atomicRadius?.f(2) ?: "N/A"} / ${covalentRadius?.f(2) ?: "N/A"} / ${vanDerWaalsRadius?.f(2) ?: "N/A"}", "covalent_radius"),
    Triple("전자친화도(eV)", electronAffinity?.f(3) ?: "N/A", "electron_affinity")
)

private fun Element.materialRows(): List<Triple<String, String, String?>> = listOf(
    Triple("녹는점/끓는점(K)", "${meltingPoint?.f(2) ?: "N/A"} / ${boilingPoint?.f(2) ?: "N/A"}", null),
    Triple("몰 융해열/몰 기화열(kJ/mol)", "${heatOfFusion?.f(3) ?: "N/A"} / ${heatOfVaporization?.f(3) ?: "N/A"}", "heat_of_fusion"),
    Triple("전기전도도(S/m) / 비저항(Ω·m)", "${electricalConductivity?.f(6) ?: "N/A"} / ${resistivity?.f(12) ?: "N/A"}", "electrical_conductivity"),
    Triple("전도 유형 / 초전도온도(K)", "${electricalType ?: "N/A"} / ${superconductingTemperature?.f(2) ?: "N/A"}", "superconductivity"),
    Triple("자성 / 체적·특정·몰 자화율", "${magnetism ?: "N/A"} / ${volumeMagneticSusceptibility?.f(6) ?: "N/A"} · ${massMagneticSusceptibility?.f(6) ?: "N/A"} · ${molarMagneticSusceptibility?.f(6) ?: "N/A"}", "magnetic_susceptibility"),
    Triple("액체밀도(g/cm³)", liquidDensity?.f(4) ?: "N/A", null),
    Triple("브리넬/모스/비커스 경도", "${hardnessBrinell?.f(2) ?: "N/A"} / ${hardnessMohs?.f(2) ?: "N/A"} / ${hardnessVickers?.f(2) ?: "N/A"}", null),
    Triple("체적/영률/전단/푸아송", "${bulkModulus?.f(2) ?: "N/A"} / ${youngModulus?.f(2) ?: "N/A"} / ${shearModulus?.f(2) ?: "N/A"} / ${poissonRatio?.f(3) ?: "N/A"}", null),
    Triple("소리속도(m/s) / 굴절률", "${speedOfSound?.f(2) ?: "N/A"} / ${refractiveIndex?.f(3) ?: "N/A"}", null),
    Triple("전기음성도 / 표준 전극 전위(V)", "${electronegativity?.f(3) ?: "N/A"} / ${standardElectrodePotential?.f(3) ?: "N/A"}", "electrode_potential")
)

private fun Element.crystalRows(): List<Triple<String, String, String?>> = listOf(
    Triple("결정 구조/결정계", "${crystalStructure ?: "N/A"} / ${crystalSystem ?: "N/A"}", "crystal_structure"),
    Triple("격자 매개변수 a,b,c(Å)", "${latticeA?.f(3) ?: "N/A"}, ${latticeB?.f(3) ?: "N/A"}, ${latticeC?.f(3) ?: "N/A"}", "lattice_parameter"),
    Triple("격자 각도 α,β,γ(°)", "${latticeAlpha?.f(2) ?: "N/A"}, ${latticeBeta?.f(2) ?: "N/A"}, ${latticeGamma?.f(2) ?: "N/A"}", "lattice_parameter"),
    Triple("결정 태도", crystalHabit ?: "N/A", "crystal_habit"),
    Triple("디바이 온도(K)", debyeTemperature?.f(2) ?: "N/A", "debye_temperature")
)

private fun Element.hazardRows(): List<Triple<String, String, String?>> = listOf(
    Triple("물질 상태", when (stateOfMatter) { StateOfMatter.SOLID -> "고체"; StateOfMatter.LIQUID -> "액체"; StateOfMatter.GAS -> "기체"; StateOfMatter.UNKNOWN -> "N/A" }, null),
    Triple("방사능/반응성 수준", "${radioactivityLevel ?: "N/A"} / ${reactivityLevel ?: "N/A"}", "radioactivity"),
    Triple("반감기/수명", halfLife ?: "N/A", "half_life"),
    Triple("중성자 단면적(barn)", neutronCrossSection?.f(6) ?: "N/A", "neutron_cross_section"),
    Triple("특수위험", hazardSpecial ?: "N/A", "nfpa_diamond")
)

private fun termLabel(id: String): String = when (id) {
    "isotope" -> "동위원소"; "half_life" -> "반감기"; "electronegativity" -> "전기음성도"
    "crystal_structure" -> "결정구조"; "nfpa_diamond" -> "NFPA"; "cas_registry_number" -> "CAS"
    else -> id
}

private fun Double.f(d: Int): String = "%.${d}f".format(this)
