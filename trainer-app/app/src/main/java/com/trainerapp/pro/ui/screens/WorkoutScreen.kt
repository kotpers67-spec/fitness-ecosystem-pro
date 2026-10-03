package com.trainerapp.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainerapp.pro.data.local.entities.ExerciseEntity
import com.trainerapp.pro.data.local.entities.WorkoutSetEntity
import com.trainerapp.pro.ui.MainViewModel
import com.trainerapp.pro.ui.components.OneRepMaxCalculatorDialog
import com.trainerapp.pro.ui.components.PlateCalculatorDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val currentDate by viewModel.currentDate.collectAsState()
    val activeClient by viewModel.activeClient.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val currentSets by viewModel.currentSets.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val selectedOrder by viewModel.selectedExerciseOrder.collectAsState()

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showPlateDialog by remember { mutableStateOf(false) }
    var show1RMDialog by remember { mutableStateOf(false) }
    var showExerciseStatsDialog by remember { mutableStateOf(false) }
    var statsSummary by remember { mutableStateOf<MainViewModel.LastExerciseStatsSummary?>(null) }
    var selectedWeightForCalc by remember { mutableStateOf(0.0) }
    var selectedRepsForCalc by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Distinct exercise orders present in this session (max 8)
    val sessionExercises = remember(currentSets, exercises) {
        currentSets.groupBy { it.exerciseOrder }.toSortedMap().mapNotNull { (order, sets) ->
            val exId = sets.firstOrNull()?.exerciseId ?: return@mapNotNull null
            val exercise = exercises.find { it.id == exId } ?: return@mapNotNull null
            Triple(order, exercise, sets)
        }
    }

    val activeExerciseTriple = sessionExercises.find { it.first == selectedOrder }
        ?: sessionExercises.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeClient?.fullName ?: "Тренировка",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "ДНЕВНИК ТРЕНИРОВОК",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
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
                    IconButton(onClick = {
                        selectedWeightForCalc = activeExerciseTriple?.third?.lastOrNull()?.weightKg ?: 60.0
                        showPlateDialog = true
                    }) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = "Блины", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val lastSet = activeExerciseTriple?.third?.lastOrNull()
                        selectedWeightForCalc = lastSet?.weightKg ?: 80.0
                        selectedRepsForCalc = lastSet?.reps ?: 8
                        show1RMDialog = true
                    }) {
                        Icon(Icons.Default.Calculate, contentDescription = "1ПМ", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // 1. Навигатор даты: < Дата > (строго по эскизу 2)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeDateByDays(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий день", tint = MaterialTheme.colorScheme.primary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentDate,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }

                    IconButton(onClick = { viewModel.changeDateByDays(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Следующий день", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Тумблер допуска: Разрешить самостоятельное выполнение на дату
            val isSelfAllowed = currentSession?.isSelfWorkoutAllowed == true
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelfAllowed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isSelfAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Разрешить самостоятельное выполнение на $currentDate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelfAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isSelfAllowed) "Атлет может отмечать подходы самостоятельно"
                                   else "Режим «Только чтение» (тренировка с тренером в зале)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isSelfAllowed,
                        onCheckedChange = { viewModel.setSelfWorkoutAllowed(it) }
                    )
                }
            }

            // 2. Горизонтальная лента плиток упражнений (Max 8 плиток, строго по эскизу 2)
            Text(
                text = "УПРАЖНЕНИЯ НА СЕГОДНЯ (MAX 8)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessionExercises) { (order, exercise, sets) ->
                    val isSelected = order == (activeExerciseTriple?.first ?: 1)
                    val completedCount = sets.count { it.isCompleted }

                    Card(
                        onClick = { viewModel.setSelectedExerciseOrder(order) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .height(68.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[$order] ${exercise.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${exercise.muscleGroup}",
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "$completedCount/${sets.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (sessionExercises.size < 8) {
                    item {
                        Card(
                            onClick = { showAddExerciseDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                            ),
                            modifier = Modifier
                                .width(90.dp)
                                .height(68.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                                Text("+ Упр.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3. Таблица подходов для выбранного упражнения (строго по эскизу 2)
            if (activeExerciseTriple != null) {
                val (order, exercise, sets) = activeExerciseTriple

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Header active exercise
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = exercise.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${exercise.muscleGroup} • Отдых: ${exercise.defaultRestSeconds}с",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { viewModel.removeExerciseFromSession(exercise.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Table Column Headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("СЕТ", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                            Text("ВЕС (КГ)", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("ПОВТОРЫ", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("ГОТОВО", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sets) { set ->
                                SetRowItem(
                                    set = set,
                                    onUpdate = { updatedSet -> viewModel.updateSet(updatedSet) },
                                    onDelete = { viewModel.deleteSet(set) }
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.addSetToCurrentExercise(exercise.id, order) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("+ ПОДХОД", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                statsSummary = viewModel.getLastExerciseStats(exercise.id)
                                                showExerciseStatsDialog = true
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.BarChart, contentDescription = "Статистика", modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("СТАТИСТИКА", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(10.dp))
                        Text("Нет упражнений на сегодня", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showAddExerciseDialog = true }) {
                            Text("+ Добавить упражнение")
                        }
                    }
                }
            }
        }
    }

    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        AddExerciseToSessionDialog(
            exercises = exercises,
            onDismiss = { showAddExerciseDialog = false },
            onSelect = { exerciseId ->
                viewModel.addExerciseToSession(exerciseId)
                showAddExerciseDialog = false
            }
        )
    }

    // Plate Calculator
    if (showPlateDialog) {
        PlateCalculatorDialog(
            initialWeight = selectedWeightForCalc,
            onDismiss = { showPlateDialog = false }
        )
    }

    // 1RM Calculator
    if (show1RMDialog) {
        OneRepMaxCalculatorDialog(
            initialWeight = selectedWeightForCalc,
            initialReps = selectedRepsForCalc,
            onDismiss = { show1RMDialog = false }
        )
    }

    // Exercise History / Statistics Dialog
    if (showExerciseStatsDialog && activeExerciseTriple != null) {
        val exercise = activeExerciseTriple.second
        val summary = statsSummary

        AlertDialog(
            onDismissRequest = { showExerciseStatsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Статистика: ${exercise.name}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (summary == null || summary.lastDate == null) {
                        Text(
                            text = "Ранее история выполнения этого упражнения для данного подопечного не найдена.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Дата последней тренировки:", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text(summary.lastDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Максимальный вес:", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text("${summary.maxWeightKg} кг", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Количество подходов:", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text("${summary.setsCount} (всего ${summary.totalReps} повт.)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "ДЕТАЛИЗАЦИЯ ПОДХОДОВ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            summary.sets.forEach { set ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Подход ${set.setNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${set.weightKg} кг × ${set.reps} повт.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExerciseStatsDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
fun SetRowItem(
    set: WorkoutSetEntity,
    onUpdate: (WorkoutSetEntity) -> Unit,
    onDelete: () -> Unit
) {
    var weightStr by remember(set.weightKg) { mutableStateOf(set.weightKg.toString()) }
    var repsStr by remember(set.reps) { mutableStateOf(set.reps.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (set.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [1], [2], [3]
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (set.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.setNumber}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (set.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.width(8.dp))

        // Weight Input
        OutlinedTextField(
            value = weightStr,
            onValueChange = {
                weightStr = it
                it.toDoubleOrNull()?.let { w -> onUpdate(set.copy(weightKg = w)) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.width(8.dp))

        // Reps Input
        OutlinedTextField(
            value = repsStr,
            onValueChange = {
                repsStr = it
                it.toIntOrNull()?.let { r -> onUpdate(set.copy(reps = r)) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.width(8.dp))

        // Complete Checkbox / Button (48.dp touch target)
        IconButton(
            onClick = { onUpdate(set.copy(isCompleted = !set.isCompleted)) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (set.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Готово",
                tint = if (set.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun AddExerciseToSessionDialog(
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf("Все") }

    val muscleGroups = listOf("Все", "Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс/Кор")

    val filtered = exercises.filter {
        (selectedMuscleGroup == "Все" || it.muscleGroup == selectedMuscleGroup) &&
        (it.name.contains(searchQuery, ignoreCase = true))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить упражнение", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск упражнения...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(muscleGroups) { group ->
                        FilterChip(
                            selected = selectedMuscleGroup == group,
                            onClick = { selectedMuscleGroup = group },
                            label = { Text(group, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered) { ex ->
                        Card(
                            onClick = { onSelect(ex.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(ex.muscleGroup, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
