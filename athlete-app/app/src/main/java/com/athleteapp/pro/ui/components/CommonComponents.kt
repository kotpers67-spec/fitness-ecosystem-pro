package com.athleteapp.pro.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.athleteapp.pro.R
import com.athleteapp.pro.domain.timer.RestTimerState
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

@Composable
fun AthleteAvatar(
    avatarPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderWidth: Dp = 1.5.dp,
    contentDescription: String = "Аватар атлета"
) {
    val bitmap = remember(avatarPath) {
        if (!avatarPath.isNullOrBlank()) {
            val file = File(avatarPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
            } else null
        } else null
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
                painter = painterResource(id = R.drawable.avatar_athlete),
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
        } catch (e: Exception) {
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
