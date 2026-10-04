package com.athleteapp.pro.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.athleteapp.pro.R
import com.athleteapp.pro.domain.timer.RestTimerState
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AthleteAvatar(
    avatarPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderWidth: Dp = 1.5.dp,
    defaultResId: Int = R.drawable.avatar_athlete,
    contentDescription: String = "Аватар"
) {
    val bitmap = remember(avatarPath) {
        try {
            if (!avatarPath.isNullOrBlank()) {
                val file = File(avatarPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
                } else {
                    val cleanB64 = if (avatarPath.contains(",")) avatarPath.substringAfter(",") else avatarPath
                    val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
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
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = defaultResId),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512
) {
    val qrBitmap = remember(content) {
        try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bmp.asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

    if (qrBitmap != null) {
        Image(
            bitmap = qrBitmap,
            contentDescription = "QR-код",
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Ошибка создания QR", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RestTimerFloatingBanner(
    state: RestTimerState,
    onAddSeconds: (Int) -> Unit,
    onReset: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isRunning && state.remainingSeconds <= 0) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ОТДЫХ МЕЖДУ ПОДХОДАМИ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть таймер",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatTimerTime(state.remainingSeconds),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = if (state.remainingSeconds <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onAddSeconds(30) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+30с", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = { onAddSeconds(-15) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("-15с", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Сброс таймера",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatTimerTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
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
                    // Apply resistance dampening factor
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

