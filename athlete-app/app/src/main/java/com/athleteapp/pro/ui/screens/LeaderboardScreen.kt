package com.athleteapp.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athleteapp.pro.R
import com.athleteapp.pro.ui.AthleteViewModel
import com.athleteapp.pro.ui.components.AthleteAvatar

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val workoutsCount: Int,
    val tonnageKg: Double,
    val points: Int,
    val avatarBase64: String? = null,
    val isMe: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: AthleteViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val isPrivate = profile?.isPrivateLeaderboard == true
    val myName = profile?.fullName?.ifBlank { "Вы" } ?: "Вы"

    val sessions by viewModel.allSessions.collectAsState()
    val completedWorkouts = sessions.count { it.completed }
    val sets by viewModel.allSets.collectAsState()
    val myTonnage: Double = sets.filter { it.isCompleted }.fold(0.0) { acc, s -> acc + (s.actualWeightKg * s.actualReps) }
    val myPoints: Int = completedWorkouts * 10 + (myTonnage / 100.0).toInt()

    // Generated ecosystem participants + Current Athlete
    val rawEntries = remember(completedWorkouts, myTonnage, myName, profile?.avatarBase64) {
        val baseWorkouts = completedWorkouts
        val baseTonnage = myTonnage
        val basePoints = myPoints
        listOf(
            LeaderboardEntry(1, myName, baseWorkouts, baseTonnage, basePoints, profile?.avatarBase64, isMe = true),
            LeaderboardEntry(2, "Максим Громов", if (baseWorkouts > 1) baseWorkouts - 1 else 0, baseTonnage * 0.9, if (basePoints > 15) basePoints - 15 else 0),
            LeaderboardEntry(3, "Елена Соколова", if (baseWorkouts > 2) baseWorkouts - 2 else 0, baseTonnage * 0.8, if (basePoints > 30) basePoints - 30 else 0),
            LeaderboardEntry(4, "Дмитрий Воронов", if (baseWorkouts > 3) baseWorkouts - 3 else 0, baseTonnage * 0.7, if (basePoints > 45) basePoints - 45 else 0),
            LeaderboardEntry(5, "Ольга Морозова", if (baseWorkouts > 4) baseWorkouts - 4 else 0, baseTonnage * 0.6, if (basePoints > 60) basePoints - 60 else 0)
        ).sortedByDescending { it.points }
    }

    val entries = remember(rawEntries, isPrivate) {
        rawEntries.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }.filter { !isPrivate || it.isMe }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "СОСТЯЗАНИЯ",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Stats Card for current athlete
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AthleteAvatar(
                            avatarPath = profile?.avatarPath ?: profile?.avatarBase64,
                            size = 48.dp
                        )
                        Column {
                            Text(
                                text = myName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isPrivate) "Приватный режим (скрыт)" else "Участник состязаний",
                                fontSize = 11.sp,
                                color = if (isPrivate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$myPoints ОЧКОВ",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$completedWorkouts трен. / ${String.format("%.0f", myTonnage)} кг",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (isPrivate) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Вы включили приватный режим в настройках. Ваш профиль скрыт от других участников состязания.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Text(
                text = "ТАБЛИЦА ЛИДЕРОВ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 0.5.sp
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries) { _, item ->
                    val badgeColor = when (item.rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (item.isMe) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${item.rank}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = if (item.rank in 1..3) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                AthleteAvatar(
                                    avatarPath = item.avatarBase64,
                                    size = 36.dp
                                )

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (item.isMe) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "ВЫ",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${item.workoutsCount} тренировок • ${String.format("%.0f", item.tonnageKg)} кг тоннаж",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "${item.points} очков",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
