package com.trainerapp.pro.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import com.trainerapp.pro.domain.calculators.FitnessCalculators

@Composable
fun FloatingRestTimerOverlay(
    remainingSeconds: Int,
    isRunning: Boolean,
    onAddTime: (Int) -> Unit,
    onStop: () -> Unit
) {
    AnimatedVisibility(
        visible = isRunning,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ОТДЫХ МЕЖДУ СЕТАМИ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val mins = remainingSeconds / 60
                        val secs = remainingSeconds % 60
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onAddTime(30) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("+30с", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Close, contentDescription = "Стоп", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun PlateCalculatorDialog(
    initialWeight: Double,
    onDismiss: () -> Unit
) {
    var targetWeightText by remember { mutableStateOf(if (initialWeight > 0) initialWeight.toString() else "60.0") }
    val targetWeight = targetWeightText.toDoubleOrNull() ?: 20.0
    val result = FitnessCalculators.calculatePlates(targetWeight)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Калькулятор блинов", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it },
                    label = { Text("Общий вес штанги (кг)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Text("Гриф: 20 кг | На каждую сторону: ${"%.1f".format(result.weightPerSide)} кг", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(10.dp))

                Text("Навеска на одну сторону:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                if (result.platesPerSide.isEmpty()) {
                    Text("Только пустой гриф (20 кг)", fontSize = 13.sp)
                } else {
                    result.platesPerSide.forEach { (plate, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Блин ${plate} кг:", fontWeight = FontWeight.Medium)
                            Text("$count шт.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun OneRepMaxCalculatorDialog(
    initialWeight: Double,
    initialReps: Int,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf(if (initialWeight > 0) initialWeight.toString() else "80.0") }
    var repsText by remember { mutableStateOf(if (initialReps > 0) initialReps.toString() else "8") }

    val weight = weightText.toDoubleOrNull() ?: 0.0
    val reps = repsText.toIntOrNull() ?: 1
    val result = FitnessCalculators.calculate1RM(weight, reps)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Калькулятор 1ПМ", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Вес (кг)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("Повторы") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Расчетный 1ПМ (100%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("${result.average} кг", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Проценты от максимума:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                result.percentages.forEach { (pct, kg) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$pct%:", fontSize = 12.sp)
                        Text("$kg кг", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}


@Composable
fun ClientAvatar(
    photoUri: String?,
    avatarBase64: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 1.5.dp,
    defaultResId: Int = com.trainerapp.pro.R.drawable.avatar_athlete,
    contentDescription: String = "Аватар"
) {
    val bitmap = remember(photoUri, avatarBase64) {
        try {
            if (!photoUri.isNullOrBlank()) {
                val file = java.io.File(photoUri)
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(photoUri)?.asImageBitmap()
                } else null
            } else if (!avatarBase64.isNullOrBlank()) {
                val cleanB64 = if (avatarBase64.contains(",")) avatarBase64.substringAfter(",") else avatarBase64
                val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.NO_WRAP)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = defaultResId),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}

@Composable
fun PullToRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThresholdDp: Float = 72f,
    maxPullDp: Float = 140f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { refreshThresholdDp.dp.toPx() }
    val maxPullPx = with(density) { maxPullDp.dp.toPx() }
    val refreshIndicatorOffsetPx = with(density) { 56.dp.toPx() }

    val scope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullOffset.animateTo(
                targetValue = refreshIndicatorOffsetPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        } else {
            pullOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If scrolling up and pullOffset > 0, consume delta to close refresh
                if (available.y < 0f && pullOffset.value > 0f) {
                    val newOffset = (pullOffset.value + available.y).coerceAtLeast(0f)
                    val consumed = pullOffset.value - newOffset
                    scope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If scrolling down and at the top of content (available.y > 0)
                if (available.y > 0f && !isRefreshing) {
                    val dragResistance = 0.5f
                    val newOffset = (pullOffset.value + available.y * dragResistance).coerceAtMost(maxPullPx)
                    val consumedY = (newOffset - pullOffset.value) / dragResistance
                    scope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value >= refreshThresholdPx && !isRefreshing) {
                    onRefresh()
                } else if (!isRefreshing) {
                    pullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content()

        // Browser-style floating indicator at top
        val currentOffset = pullOffset.value
        if (currentOffset > 0f || isRefreshing) {
            val progress = (currentOffset / refreshThresholdPx).coerceIn(0f, 1f)
            val indicatorY = with(density) { (currentOffset - 44.dp.toPx()).coerceAtLeast(8.dp.toPx()).toDp() }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = indicatorY)
                    .zIndex(10f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        val rotation = progress * 180f
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Потяните для синхронизации",
                            tint = if (progress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(rotation)
                                .scale(0.8f + 0.2f * progress)
                        )
                    }
                }
            }
        }
    }
}

