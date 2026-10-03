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
                        if (completedSets.size >= 2) {
                            val weights = completedSets.map { it.weightKg }
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
