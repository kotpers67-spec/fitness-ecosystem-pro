package com.athleteapp.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.athleteapp.pro.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athleteapp.pro.data.local.entities.AssignedExerciseEntity
import com.athleteapp.pro.data.local.entities.MyWorkoutSetEntity
import com.athleteapp.pro.domain.calculators.FatigueLevel
import com.athleteapp.pro.domain.calculators.NeuroRecommendation
import com.athleteapp.pro.ui.AthleteViewModel
import com.athleteapp.pro.ui.components.AthleteAvatar
import com.athleteapp.pro.ui.components.RestTimerFloatingBanner
import com.athleteapp.pro.ui.i18n.AthleteStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteTodayScreen(
    viewModel: AthleteViewModel,
    modifier: Modifier = Modifier
) {
    val date by viewModel.selectedDate.collectAsState()
    val sets by viewModel.currentSets.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val timerState by viewModel.timerManager.timerState.collectAsState()
    val readiness by viewModel.sessionReadiness.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val isSelfAllowed = currentSession?.isSelfWorkoutAllowed == true

    val lang = settings?.language ?: "ru"
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    // Group sets by exercise and sort: incomplete at top, completed at bottom
    val sortedExerciseGroups = remember(sets) {
        sets.groupBy { it.exerciseId }.entries.sortedWith(
            compareBy<Map.Entry<Long, List<MyWorkoutSetEntity>>> { (_, exerciseSets) ->
                if (exerciseSets.isNotEmpty() && exerciseSets.all { it.isCompleted }) 1 else 0
            }.thenBy { it.value.firstOrNull()?.setNumber ?: 0 }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AthleteAvatar(
                            avatarPath = profile?.avatarPath ?: profile?.photoUri,
                            size = 40.dp,
                            contentDescription = "Атлет"
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = AthleteStrings.get("my_workout", lang),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (profile?.fullName.isNullOrBlank()) "Атлет" else profile?.fullName!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            RestTimerFloatingBanner(
                state = timerState,
                onAddSeconds = { viewModel.timerManager.addSeconds(it) },
                onReset = { viewModel.timerManager.resetTimer() },
                onStop = { viewModel.timerManager.stopTimer() }
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
            // 1. Date Switcher (< Date >)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.changeDate(-1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Вчера")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { viewModel.changeDate(1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Завтра")
                        }
                    }
                }
            }

            // 2. Permission & Access Mode Banner (Read-Only vs Self-Workout)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelfAllowed)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelfAllowed)
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    else
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelfAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSelfAllowed) Icons.Default.EditNote else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isSelfAllowed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSelfAllowed) "Самостоятельная тренировка разрешена" else "Тренировка с тренером (Только чтение)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelfAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSelfAllowed)
                                    "Тренер разрешил отметку подходов. Вводите фактические веса и отмечайте сеты."
                                else
                                    "В зале тренировку ведёт и отмечает тренер. Поля ввода заблокированы.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }



            if (isSelfAllowed) {
                item {
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("+ Добавить упражнение", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Exercise Matrix Cards
            if (sortedExerciseGroups.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isSelfAllowed) "Упражнений пока нет. Нажмите «+ Добавить упражнение» выше." else "На сегодня тренер не назначил упражнений",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sortedExerciseGroups, key = { it.key }) { (exerciseId, exerciseSets) ->
                    val exerciseName = exerciseSets.firstOrNull()?.exerciseName ?: "Упражнение"
                    val muscleGroup = exerciseSets.firstOrNull()?.muscleGroup ?: ""
                    val matchedExercise = exercises.find { it.id == exerciseId }

                    AthleteExerciseCard(
                        exerciseName = exerciseName,
                        muscleGroup = muscleGroup,
                        sets = exerciseSets,
                        isSelfAllowed = isSelfAllowed,
                        onToggleSet = { viewModel.toggleSetCompletion(it) },
                        onUpdateSet = { set, w, r -> viewModel.updateSetValues(set, w, r) },
                        onUpdateRpe = { set, rpe -> viewModel.updateSetRpe(set, rpe) },
                        onAddSet = {
                            matchedExercise?.let { viewModel.addSetToExercise(it) }
                        },
                        onDeleteSet = { viewModel.deleteSet(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showAddExerciseDialog) {
        var newExName by remember { mutableStateOf("") }
        var newMuscleGroup by remember { mutableStateOf("Грудь") }
        var newWeight by remember { mutableStateOf("60") }
        var newReps by remember { mutableStateOf("10") }
        val muscleGroups = listOf("Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс/Кор", "Кардио")

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Добавить упражнение") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newExName,
                        onValueChange = { newExName = it },
                        label = { Text("Название упражнения") },
                        placeholder = { Text("Например: Жим гантелей") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Группа мышц:", style = MaterialTheme.typography.labelSmall)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(muscleGroups) { group ->
                            FilterChip(
                                selected = newMuscleGroup == group,
                                onClick = { newMuscleGroup = group },
                                label = { Text(group, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newWeight,
                            onValueChange = { newWeight = it },
                            label = { Text("Вес (кг)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newReps,
                            onValueChange = { newReps = it },
                            label = { Text("Повторы") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = newWeight.toDoubleOrNull() ?: 0.0
                        val r = newReps.toIntOrNull() ?: 10
                        if (newExName.isNotBlank()) {
                            viewModel.createSelfExercise(newExName, newMuscleGroup, w, r)
                            showAddExerciseDialog = false
                        }
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun AthleteExerciseCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<MyWorkoutSetEntity>,
    isSelfAllowed: Boolean,
    onToggleSet: (MyWorkoutSetEntity) -> Unit,
    onUpdateSet: (MyWorkoutSetEntity, Double, Int) -> Unit,
    onUpdateRpe: (MyWorkoutSetEntity, Double) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (MyWorkoutSetEntity) -> Unit
) {
    val isAllCompleted = sets.isNotEmpty() && sets.all { it.isCompleted }
    var isCollapsed by remember(exerciseName, isAllCompleted) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAllCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isAllCompleted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllCompleted) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isAllCompleted) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "✓ СДЕЛАНО",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                    Text(
                        text = "$muscleGroup • ${sets.size} подходов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAllCompleted) {
                        TextButton(
                            onClick = { isCollapsed = !isCollapsed },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (isCollapsed) "▶ Развернуть" else "▼ Свернуть",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isSelfAllowed) {
                        FilledTonalButton(
                            onClick = onAddSet,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Подход", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (!isCollapsed) {
                Spacer(modifier = Modifier.height(12.dp))

                sets.forEach { set ->
                    AthleteSetRow(
                        set = set,
                        isSelfAllowed = isSelfAllowed,
                        onToggle = { onToggleSet(set) },
                        onUpdate = { w, r -> onUpdateSet(set, w, r) },
                        onUpdateRpe = { rpe -> onUpdateRpe(set, rpe) },
                        onDelete = { onDeleteSet(set) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AthleteSetRow(
    set: MyWorkoutSetEntity,
    isSelfAllowed: Boolean,
    onToggle: () -> Unit,
    onUpdate: (Double, Int) -> Unit,
    onUpdateRpe: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var weightText by remember(set.actualWeightKg) { mutableStateOf(if (set.actualWeightKg == 0.0) "" else set.actualWeightKg.toString()) }
    var repsText by remember(set.actualReps) { mutableStateOf(set.actualReps.toString()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (set.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Set Number Badge
            Surface(
                shape = CircleShape,
                color = if (set.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${set.setNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (set.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Weight Input
            OutlinedTextField(
                value = weightText,
                enabled = isSelfAllowed,
                onValueChange = {
                    weightText = it
                    val w = it.toDoubleOrNull() ?: 0.0
                    val r = repsText.toIntOrNull() ?: set.actualReps
                    onUpdate(w, r)
                },
                label = { Text("Кг", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )

            // Reps Input
            OutlinedTextField(
                value = repsText,
                enabled = isSelfAllowed,
                onValueChange = {
                    repsText = it
                    val w = weightText.toDoubleOrNull() ?: set.actualWeightKg
                    val r = it.toIntOrNull() ?: 0
                    onUpdate(w, r)
                },
                label = { Text("Повт", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )

            // Checkbox / Complete Button (Minimum 48.dp touch target)
            FilledIconToggleButton(
                checked = set.isCompleted,
                enabled = isSelfAllowed,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = if (set.isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Отметить выполненным",
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isSelfAllowed) {
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить подход",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
