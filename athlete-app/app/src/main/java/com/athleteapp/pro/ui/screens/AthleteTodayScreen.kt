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

    // Group sets by exercise
    val setsByExercise = remember(sets) {
        sets.groupBy { it.exerciseId }
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
                                text = profile?.fullName ?: "Атлет",
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

            // 3. Neuro-Adaptive Engine Readiness & Tonnage Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "НЕЙРО-АДАПТИВНАЯ ГОТОВНОСТЬ",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (readiness.fatigueLevel) {
                                    FatigueLevel.FRESH -> MaterialTheme.colorScheme.primaryContainer
                                    FatigueLevel.OPTIMAL -> MaterialTheme.colorScheme.secondaryContainer
                                    FatigueLevel.ELEVATED -> MaterialTheme.colorScheme.tertiaryContainer
                                    FatigueLevel.EXHAUSTED -> MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Text(
                                    text = readiness.fatigueLevel.titleRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Readiness Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Индекс готовности: ${readiness.readinessPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Усталость: ${readiness.fatiguePercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (readiness.readinessPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Общий тоннаж",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${readiness.totalTonnageKg} кг",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Выполнено сетов",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${readiness.completedSetsCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Средний RPE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (readiness.averageRpe > 0.0) "${readiness.averageRpe}" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // 3. Client Goal Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "ЦЕЛЬ НА СЕЗОН",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profile?.goal ?: "Регулярные тренировки и прогресс весов",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 4. Exercise Matrix Cards
            if (setsByExercise.isEmpty()) {
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
                                text = "На сегодня тренировка не запланирована",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddExerciseDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Добавить упражнение")
                            }
                        }
                    }
                }
            } else {
                items(setsByExercise.entries.toList(), key = { it.key }) { (exerciseId, exerciseSets) ->
                    val exerciseName = exerciseSets.firstOrNull()?.exerciseName ?: "Упражнение"
                    val muscleGroup = exerciseSets.firstOrNull()?.muscleGroup ?: ""
                    val matchedExercise = exercises.find { it.id == exerciseId }
                    val recommendation = viewModel.getRecommendationForExercise(exerciseId)

                    AthleteExerciseCard(
                        exerciseName = exerciseName,
                        muscleGroup = muscleGroup,
                        sets = exerciseSets,
                        recommendation = recommendation,
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

                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        enabled = isSelfAllowed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSelfAllowed) "Добавить еще упражнение" else "Добавление упражнений заблокировано")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Выбор упражнения") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exercises, key = { it.id }) { ex ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addSetToExercise(ex)
                                    showAddExerciseDialog = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ex.muscleGroup,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("Закрыть")
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
    recommendation: NeuroRecommendation,
    isSelfAllowed: Boolean,
    onToggleSet: (MyWorkoutSetEntity) -> Unit,
    onUpdateSet: (MyWorkoutSetEntity, Double, Int) -> Unit,
    onUpdateRpe: (MyWorkoutSetEntity, Double) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (MyWorkoutSetEntity) -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = muscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onAddSet,
                    enabled = isSelfAllowed,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Сет", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Neuro-Adaptive Suggestion Tip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Цель: ${recommendation.recommendedWeightKg} кг × ${recommendation.recommendedReps} (RPE ${recommendation.targetRpe})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = recommendation.recommendationReason,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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

            // RPE Tag Button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isSelfAllowed) {
                            Modifier.clickable {
                                val currentRpe = set.rpe ?: 8.0
                                val nextRpe = when (currentRpe) {
                                    7.0 -> 8.0
                                    8.0 -> 8.5
                                    8.5 -> 9.0
                                    9.0 -> 9.5
                                    9.5 -> 10.0
                                    else -> 7.0
                                }
                                onUpdateRpe(nextRpe)
                            }
                        } else Modifier
                    ),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "RPE ${(set.rpe ?: 8.0)}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelfAllowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

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
        }
    }
}
