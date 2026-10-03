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

                            AthleteExerciseLineChart(
                                data = exerciseHistory,
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
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val weights = data.map { it.weightKg }
        val min = (weights.minOrNull() ?: 0.0) * 0.8
        val max = (weights.maxOfOrNull { it } ?: 100.0) * 1.1
        val range = (max - min).coerceAtLeast(1.0)

        for (i in 0..3) {
            val y = h * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        if (weights.size < 2) {
            if (weights.isNotEmpty()) {
                val y = h / 2f
                drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(w / 2, y))
            }
            return@Canvas
        }

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
