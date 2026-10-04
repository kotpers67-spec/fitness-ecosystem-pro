package com.athleteapp.pro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athleteapp.pro.data.local.dao.AthleteSetHistory
import com.athleteapp.pro.data.local.entities.MyAnthropometryEntity
import com.athleteapp.pro.ui.AthleteViewModel
import com.athleteapp.pro.ui.i18n.AthleteStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteHistoryScreen(
    viewModel: AthleteViewModel,
    modifier: Modifier = Modifier
) {
    val anthropometry by viewModel.anthropometry.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val selectedExId by viewModel.selectedExerciseId.collectAsState()
    val exerciseHistory by viewModel.exerciseHistory.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val lang = settings?.language ?: "ru"
    var showWeightDialog by remember { mutableStateOf(false) }
    var selectedPointData by remember { mutableStateOf<Triple<String, Double, List<AthleteSetHistory>>?>(null) }
    var showMetricWeight by remember { mutableStateOf(true) }
    var showMetricSets by remember { mutableStateOf(true) }
    var showMetricReps by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AthleteStrings.get("my_history", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Body Weight Progress Card + Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ДИНАМИКА ВЕСА ТЕЛА",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showWeightDialog = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Внести замер",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val latestWeight = anthropometry.lastOrNull()?.weightKg
                        val startWeight = anthropometry.firstOrNull()?.weightKg

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Текущий вес", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (latestWeight != null) "$latestWeight кг" else "—",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Старт", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (startWeight != null) "$startWeight кг" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Chart
                        if (anthropometry.size >= 2) {
                            AthleteWeightLineChart(
                                data = anthropometry,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Добавьте минимум 2 замера для графика",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Exercise Strength Curve Card + Exercise Chips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "ПРОГРЕСС В УПРАЖНЕНИЯХ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Exercise selector chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(exercises, key = { it.id }) { ex ->
                                val isSelected = ex.id == selectedExId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectExerciseForHistory(ex.id) },
                                    label = { Text(ex.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (exerciseHistory.isNotEmpty()) {
                            val maxWeight = exerciseHistory.maxOfOrNull { it.weightKg } ?: 0.0
                            Text(
                                text = "Максимальный рабочий вес: $maxWeight кг",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Metric Toggles (Weight, Sets, Reps)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = showMetricWeight,
                                    onClick = { showMetricWeight = !showMetricWeight },
                                    label = { Text("Вес (кг)", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                FilterChip(
                                    selected = showMetricSets,
                                    onClick = { showMetricSets = !showMetricSets },
                                    label = { Text("Подходы", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFF38BDF8)
                                    )
                                )
                                FilterChip(
                                    selected = showMetricReps,
                                    onClick = { showMetricReps = !showMetricReps },
                                    label = { Text("Повторы", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFF43F5E).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFFF43F5E)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            AthleteExerciseLineChart(
                                data = exerciseHistory,
                                showWeight = showMetricWeight,
                                showSets = showMetricSets,
                                showReps = showMetricReps,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                onPointClick = { date, weight, sets ->
                                    selectedPointData = Triple(date, weight, sets)
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Нет записей по этому упражнению",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (selectedPointData != null) {
        val (date, maxW, sets) = selectedPointData!!
        AlertDialog(
            onDismissRequest = { selectedPointData = null },
            title = {
                Text("Детали тренировки: $date", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Максимальный вес: $maxW кг", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Всего подходов: ${sets.size}", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    sets.forEachIndexed { i, s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Подход ${i + 1}:", fontWeight = FontWeight.Medium)
                            Text("${s.weightKg} кг × ${s.reps} повт", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedPointData = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showWeightDialog) {
        var inputWeight by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Замер веса тела") },
            text = {
                OutlinedTextField(
                    value = inputWeight,
                    onValueChange = { inputWeight = it },
                    label = { Text("Вес (кг)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val w = inputWeight.toDoubleOrNull()
                    if (w != null && w > 0) {
                        viewModel.saveAnthropometry(w)
                    }
                    showWeightDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun AthleteWeightLineChart(
    data: List<MyAnthropometryEntity>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val weights = data.map { it.weightKg }
        val min = weights.minOrNull() ?: 0.0
        val max = weights.maxOrNull() ?: 100.0
        val range = (max - min).coerceAtLeast(1.0)

        // Draw grid
        for (i in 0..3) {
            val y = h * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        if (weights.size < 2) return@Canvas

        val path = Path()
        val stepX = w / (weights.size - 1)

        weights.forEachIndexed { index, weight ->
            val x = index * stepX
            val y = h - ((weight - min) / range * (h * 0.8f) + h * 0.1f).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun AthleteExerciseLineChart(
    data: List<AthleteSetHistory>,
    showWeight: Boolean = true,
    showSets: Boolean = true,
    showReps: Boolean = true,
    modifier: Modifier = Modifier,
    onPointClick: (date: String, weight: Double, sets: List<AthleteSetHistory>) -> Unit = { _, _, _ -> }
) {
    val weightColor = MaterialTheme.colorScheme.primary
    val setsColor = Color(0xFF38BDF8) // Cyan
    val repsColor = Color(0xFFF43F5E) // Rose
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val textStyle = androidx.compose.ui.text.TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    // Group sets by date and compute metrics per day
    val setsByDate = remember(data) { data.groupBy { it.date } }
    data class DayMetric(
        val date: String,
        val maxWeight: Double,
        val setsCount: Int,
        val avgReps: Double,
        val sets: List<AthleteSetHistory>
    )

    val dayPoints = remember(setsByDate) {
        setsByDate.map { (date, sets) ->
            val maxW = sets.maxOfOrNull { it.weightKg } ?: 0.0
            val count = sets.size
            val avgR = if (count > 0) sets.map { it.reps }.average() else 0.0
            DayMetric(date, maxW, count, avgR, sets)
        }
    }

    var pointOffsets by remember { mutableStateOf<List<Pair<Offset, DayMetric>>>(emptyList()) }

    Box(
        modifier = modifier
            .pointerInput(dayPoints) {
                detectTapGestures { tapOffset ->
                    val hit = pointOffsets.find { (offset, _) ->
                        (offset - tapOffset).getDistance() <= 36f
                    }
                    if (hit != null) {
                        val (_, info) = hit
                        onPointClick(info.date, info.maxWeight, info.sets)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Draw horizontal grid lines
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            if (dayPoints.isEmpty()) return@Canvas

            val weights = dayPoints.map { it.maxWeight }
            val setsList = dayPoints.map { it.setsCount.toDouble() }
            val repsList = dayPoints.map { it.avgReps }

            val minW = (weights.minOrNull() ?: 0.0) * 0.8
            val maxW = (weights.maxOrNull() ?: 100.0) * 1.15
            val rangeW = (maxW - minW).coerceAtLeast(1.0)

            val maxSets = (setsList.maxOrNull() ?: 5.0) * 1.2
            val rangeSets = maxSets.coerceAtLeast(1.0)

            val maxReps = (repsList.maxOrNull() ?: 15.0) * 1.2
            val rangeReps = maxReps.coerceAtLeast(1.0)

            if (dayPoints.size == 1) {
                val item = dayPoints.first()
                val y = h / 2f
                val center = Offset(w / 2, y)
                if (showWeight) {
                    drawCircle(color = weightColor, radius = 6.dp.toPx(), center = center)
                    pointOffsets = listOf(Pair(center, item))
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "${item.maxWeight} кг",
                        topLeft = Offset(w / 2 - 35f, y - 24.dp.toPx()),
                        style = textStyle
                    )
                }
                return@Canvas
            }

            val stepX = w / (dayPoints.size - 1)
            val newOffsets = mutableListOf<Pair<Offset, DayMetric>>()

            // 1. Draw Sets Line (Cyan)
            if (showSets) {
                val setsPath = Path()
                dayPoints.forEachIndexed { index, item ->
                    val x = index * stepX
                    val y = h - ((item.setsCount / rangeSets) * (h * 0.75f) + h * 0.12f).toFloat()
                    if (index == 0) setsPath.moveTo(x, y) else setsPath.lineTo(x, y)
                    drawCircle(color = setsColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                drawPath(
                    path = setsPath,
                    color = setsColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 2. Draw Reps Line (Rose)
            if (showReps) {
                val repsPath = Path()
                dayPoints.forEachIndexed { index, item ->
                    val x = index * stepX
                    val y = h - ((item.avgReps / rangeReps) * (h * 0.75f) + h * 0.12f).toFloat()
                    if (index == 0) repsPath.moveTo(x, y) else repsPath.lineTo(x, y)
                    drawCircle(color = repsColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                drawPath(
                    path = repsPath,
                    color = repsColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Draw Weight Line (Primary Neon)
            if (showWeight) {
                val weightPath = Path()
                dayPoints.forEachIndexed { index, item ->
                    val x = index * stepX
                    val y = h - (((item.maxWeight - minW) / rangeW) * (h * 0.75f) + h * 0.12f).toFloat()
                    val center = Offset(x, y)
                    if (index == 0) weightPath.moveTo(x, y) else weightPath.lineTo(x, y)
                    drawCircle(color = weightColor, radius = 5.dp.toPx(), center = center)

                    val label = "${item.maxWeight} кг"
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(x - 20f, (y - 20.dp.toPx()).coerceAtLeast(2f)),
                        style = textStyle
                    )
                    newOffsets.add(Pair(center, item))
                }
                drawPath(
                    path = weightPath,
                    color = weightColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                pointOffsets = newOffsets
            } else {
                // If weight is hidden, map tap targets to middle height for dayPoints
                pointOffsets = dayPoints.mapIndexed { index, item ->
                    Pair(Offset(index * stepX, h / 2f), item)
                }
            }
        }
    }
}

