package com.trainerapp.pro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainerapp.pro.data.local.dao.SetHistoryItem
import com.trainerapp.pro.data.local.entities.AnthropometryEntity
import com.trainerapp.pro.data.sync.SyncState
import com.trainerapp.pro.ui.MainViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val activeClient by viewModel.activeClient.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val anthropometryHistory by viewModel.anthropometryHistory.collectAsState()
    val exerciseHistory by viewModel.exerciseHistory.collectAsState()
    val selectedChartExerciseId by viewModel.selectedChartExerciseId.collectAsState()
    val syncState by viewModel.syncEngine.syncState.collectAsState()
    val discoveredDevices by viewModel.syncEngine.discoveredDevices.collectAsState()
    val syncStatusMessage by viewModel.syncEngine.statusMessage.collectAsState()

    var showAddAnthroDialog by remember { mutableStateOf(false) }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }
    var showChartWeight by remember { mutableStateOf(true) }
    var showChartSets by remember { mutableStateOf(true) }
    var showChartReps by remember { mutableStateOf(true) }

    val selectedExercise = exercises.find { it.id == selectedChartExerciseId }
        ?: exercises.firstOrNull()

    LaunchedEffect(exercises) {
        if (selectedChartExerciseId == null && exercises.isNotEmpty()) {
            viewModel.selectChartExercise(exercises.first().id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ИСТОРИЯ И АНАЛИТИКА",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = activeClient?.fullName ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddAnthroDialog = true }) {
                        Icon(Icons.Default.AddChart, contentDescription = "Замер", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. График динамики веса тела подопечного (строго по эскизу 3)
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
                            Text(
                                text = "ДИНАМИКА ВЕСА ТЕЛА (КГ)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val lastWeight = anthropometryHistory.lastOrNull()?.weightKg
                            Text(
                                text = if (lastWeight != null) "$lastWeight кг" else "--",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        if (anthropometryHistory.size >= 2) {
                            val weights = anthropometryHistory.map { it.weightKg }
                            SimpleLineChart(
                                dataPoints = weights,
                                lineColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Недостаточно замеров для графика", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // 2. График роста силовых показателей по упражнению (строго по эскизу 3)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ПРОГРЕСС СИЛОВЫХ ПО УПРАЖНЕНИЮ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(10.dp))

                        // Селектор упражнения
                        ExposedDropdownMenuBox(
                            expanded = exerciseDropdownExpanded,
                            onExpandedChange = { exerciseDropdownExpanded = !exerciseDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedExercise?.name ?: "Выберите упражнение",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exerciseDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = exerciseDropdownExpanded,
                                onDismissRequest = { exerciseDropdownExpanded = false }
                            ) {
                                exercises.forEach { ex ->
                                    DropdownMenuItem(
                                        text = { Text(ex.name) },
                                        onClick = {
                                            viewModel.selectChartExercise(ex.id)
                                            exerciseDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        val completedSets = exerciseHistory.filter { it.isCompleted }
                        if (completedSets.isNotEmpty()) {
                            // Interactive Metric Toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = showChartWeight,
                                    onClick = { showChartWeight = !showChartWeight },
                                    label = { Text("Вес (кг)", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                FilterChip(
                                    selected = showChartSets,
                                    onClick = { showChartSets = !showChartSets },
                                    label = { Text("Подходы", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFF38BDF8)
                                    )
                                )
                                FilterChip(
                                    selected = showChartReps,
                                    onClick = { showChartReps = !showChartReps },
                                    label = { Text("Повторы", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFF43F5E).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFFF43F5E)
                                    )
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            TrainerExerciseLineChart(
                                history = completedSets,
                                showWeight = showChartWeight,
                                showSets = showChartSets,
                                showReps = showChartReps,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Нет истории подходов по этому упражнению", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // Add Anthropometry Dialog
    if (showAddAnthroDialog && activeClient != null) {
        var weightStr by remember { mutableStateOf("") }
        var chestStr by remember { mutableStateOf("") }
        var waistStr by remember { mutableStateOf("") }
        var bicepsStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAnthroDialog = false },
            title = { Text("Новый замер тела", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Вес (кг) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = chestStr,
                        onValueChange = { chestStr = it },
                        label = { Text("Обхват груди (см)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = waistStr,
                        onValueChange = { waistStr = it },
                        label = { Text("Обхват талии (см)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bicepsStr,
                        onValueChange = { bicepsStr = it },
                        label = { Text("Обхват бицепса (см)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightStr.toDoubleOrNull() ?: 0.0
                        if (w > 0) {
                            viewModel.addAnthropometry(
                                AnthropometryEntity(
                                    clientId = activeClient!!.id,
                                    date = LocalDate.now().toString(),
                                    weightKg = w,
                                    chestCm = chestStr.toDoubleOrNull(),
                                    waistCm = waistStr.toDoubleOrNull(),
                                    bicepsCm = bicepsStr.toDoubleOrNull()
                                )
                            )
                            showAddAnthroDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAnthroDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun TrainerExerciseLineChart(
    history: List<SetHistoryItem>,
    showWeight: Boolean = true,
    showSets: Boolean = true,
    showReps: Boolean = true,
    modifier: Modifier = Modifier
) {
    val weightColor = MaterialTheme.colorScheme.primary
    val setsColor = Color(0xFF38BDF8) // Cyan
    val repsColor = Color(0xFFF43F5E) // Rose
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    data class TrainerDayMetric(
        val date: String,
        val maxWeight: Double,
        val setsCount: Int,
        val avgReps: Double
    )

    val setsByDate = remember(history) { history.groupBy { it.date } }
    val dayPoints = remember(setsByDate) {
        setsByDate.map { (date, sets) ->
            val maxW = sets.maxOfOrNull { it.weightKg } ?: 0.0
            val count = sets.size
            val avgR = if (count > 0) sets.map { it.reps }.average() else 0.0
            TrainerDayMetric(date, maxW, count, avgR)
        }
    }

    Canvas(modifier = modifier) {
        if (dayPoints.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height

        for (i in 0..3) {
            val y = h * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

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
            if (showWeight) {
                drawCircle(color = weightColor, radius = 5.dp.toPx(), center = Offset(w / 2, y))
            }
            return@Canvas
        }

        val stepX = w / (dayPoints.size - 1)

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
                style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
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
                style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }

        // 3. Draw Weight Line (Primary)
        if (showWeight) {
            val weightPath = Path()
            dayPoints.forEachIndexed { index, item ->
                val x = index * stepX
                val y = h - (((item.maxWeight - minW) / rangeW) * (h * 0.75f) + h * 0.12f).toFloat()
                if (index == 0) weightPath.moveTo(x, y) else weightPath.lineTo(x, y)
                drawCircle(color = weightColor, radius = 5.dp.toPx(), center = Offset(x, y))
            }
            drawPath(
                path = weightPath,
                color = weightColor,
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

@Composable
fun SimpleLineChart(
    dataPoints: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas

        val minVal = (dataPoints.minOrNull() ?: 0.0) * 0.95
        val maxVal = (dataPoints.maxOrNull() ?: 100.0) * 1.05
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = ((value - minVal) / range).toFloat()
            val y = size.height - (normalizedY * size.height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Draw point
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
