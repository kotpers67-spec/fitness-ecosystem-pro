package com.athleteapp.pro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.athleteapp.pro.R
import com.athleteapp.pro.ui.AthleteViewModel
import com.athleteapp.pro.ui.components.AthleteAvatar
import com.athleteapp.pro.ui.components.QrCodeView
import com.athleteapp.pro.ui.i18n.AthleteLanguage
import com.athleteapp.pro.ui.i18n.AthleteStrings
import com.athleteapp.pro.ui.theme.AthleteThemePreset
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteSettingsScreen(
    viewModel: AthleteViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val syncMsg by viewModel.syncMessage.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.saveAvatar(context, it) }
    }

    val backupFilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonText = context.contentResolver.openInputStream(uri)?.use { 
                        it.bufferedReader().readText() 
                    }
                    if (!jsonText.isNullOrBlank()) {
                        val res = viewModel.backupManager.importFromJson(jsonText)
                        res.onSuccess { count ->
                            Toast.makeText(context, "Импортировано из файла! Записей: $count", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(context, "Ошибка импорта файла: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Не удалось прочитать файл: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun exportAndShareAthleteFile() {
        scope.launch {
            try {
                val jsonText = viewModel.backupManager.exportToJson()
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "AthletePro_Backup_$dateStr.json"
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { os ->
                    os.write(jsonText.toByteArray(Charsets.UTF_8))
                }

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Резервная копия Athlete Pro")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(sendIntent, "Отправить бэкап в мессенджер / файл")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка экспорта файла: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var fullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName ?: "") }
    var phone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }
    var goal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "") }
    var restrictions by remember(profile?.restrictions) { mutableStateOf(profile?.restrictions ?: "") }

    val lang = settings?.language ?: "ru"
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMsg) {
        syncMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AthleteStrings.get("my_profile", lang),
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
            // 1. Profile Editing & Photo Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clickable { photoPickerLauncher.launch("image/*") }
                            ) {
                                AthleteAvatar(
                                    avatarPath = profile?.avatarPath ?: profile?.photoUri,
                                    size = 68.dp,
                                    borderWidth = 2.dp,
                                    contentDescription = "Аватар атлета"
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Сменить фото",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (fullName.isNotBlank()) fullName else "Атлет",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (phone.isNotBlank()) phone else "Номер не указан",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Изменить фото", fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("ФИО атлета") },
                            placeholder = { Text("Например: Иван Иванов") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Номер телефона") },
                            placeholder = { Text("+7 999 000-00-00") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )


                        OutlinedTextField(
                            value = restrictions,
                            onValueChange = { restrictions = it },
                            label = { Text("Ограничения и травмы") },
                            placeholder = { Text("Например: Без осевых нагрузок") },
                            leadingIcon = { Icon(Icons.Default.WarningAmber, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Button(
                            onClick = {
                                viewModel.updateProfile(fullName, phone, goal, restrictions)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сохранить профиль")
                        }
                    }
                }
            }

            // 2. Pairing Card (PIN & Link)
            item {
                val cleanPin = (profile?.pairingPin ?: "").filter { it.isDigit() }
                val formattedPin = if (cleanPin.length == 6) "${cleanPin.substring(0, 3)}-${cleanPin.substring(3)}" else cleanPin
                val isPaired = profile?.isPairedWithCoach == true
                val coachName = profile?.pairedCoachName?.ifBlank { "Тренер" } ?: "Тренер"
                val clientUuid = profile?.clientUuid ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ПРИВЯЗКА К ТРЕНЕРУ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaired) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (isPaired) "СВЯЗАН" else "ОЖИДАНИЕ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPaired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isPaired) {
                            val coachPhone = profile?.pairedCoachPhone ?: ""
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AthleteAvatar(
                                            avatarPath = profile?.pairedCoachPhotoUri ?: profile?.pairedCoachAvatarBase64,
                                            size = 52.dp,
                                            defaultResId = R.drawable.avatar_coach
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Привязан к тренеру",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = coachName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Black
                                            )
                                            if (coachPhone.isNotBlank()) {
                                                Text(
                                                    text = coachPhone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        if (coachPhone.isNotBlank()) {
                                            IconButton(onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$coachPhone"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }) {
                                                Icon(Icons.Default.Phone, contentDescription = "Позвонить", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.unpairFromCoach() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Отвязаться от тренера")
                            }
                        } else {
                            Text(
                                text = "Покажите QR-код тренеру или назовите 6-значный код:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Карточка с PIN-кодом (слитно, без дефиса) и таймером 05:00
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "КОД ПОДКЛЮЧЕНИЯ (6 ЦИФР)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = cleanPin,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 6.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Вводится тренером слитно, без дефиса",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    val secondsLeft by viewModel.pinSecondsRemaining.collectAsState()
                                    val isExpiringSoon = secondsLeft < 60
                                    val timerFormatted = String.format(Locale.US, "%02d:%02d", secondsLeft / 60, secondsLeft % 60)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = if (isExpiringSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Код действителен: $timerFormatted",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isExpiringSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { secondsLeft / 300f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (isExpiringSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Кнопки копирования и отправки ссылки тренеру
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Pairing Link", "https://fitnessapp.pro/pair?code=$cleanPin")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Ссылка для тренера скопирована!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Скопировать", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Мой код подключения в Fitness Pro: $cleanPin\nСсылка для подключения: https://fitnessapp.pro/pair?code=$cleanPin")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Отправить тренеру"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Отправить", fontSize = 11.sp)
                                }
                            }

                            // QR-код для быстрого сканирования тренером
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    QrCodeView(
                                        content = cleanPin,
                                        modifier = Modifier
                                            .size(200.dp)
                                            .padding(12.dp)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.regeneratePairingPin() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Сгенерировать новый код")
                            }
                        }
                    }
                }
            }

            // 2b. Telegram & 2FA Security Card
            item {
                val is2FaActive by viewModel.is2FaEnabledFlow.collectAsState()
                val tgUsername by viewModel.telegramUsernameFlow.collectAsState()
                val clientUuid = profile?.clientUuid ?: ""
                var showTgDialog by remember { mutableStateOf(false) }
                var tempTgInput by remember { mutableStateOf(tgUsername) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TELEGRAM & БЕЗОПАСНОСТЬ 2FA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (tgUsername.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (tgUsername.isNotBlank()) "@$tgUsername" else "НЕ ПРИВЯЗАН",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tgUsername.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Привяжите Telegram-бота для получения 6-значных кодов 2FA при входе и уведомлений.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                tempTgInput = tgUsername
                                showTgDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (tgUsername.isNotBlank()) "Изменить привязку Telegram" else "Привязать Telegram", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Двухфакторная аутентификация (2FA)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Запрашивать 6-значный OTP код из Telegram при входе в аккаунт",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = is2FaActive,
                                onCheckedChange = { checked ->
                                    if (checked && tgUsername.isBlank()) {
                                        Toast.makeText(context, "Сначала привяжите Telegram для получения кодов!", Toast.LENGTH_LONG).show()
                                        tempTgInput = ""
                                        showTgDialog = true
                                    } else {
                                        viewModel.update2FaEnabled(checked)
                                    }
                                }
                            )
                        }
                    }
                }

                if (showTgDialog) {
                    AlertDialog(
                        onDismissRequest = { showTgDialog = false },
                        title = { Text("Привязка Telegram", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Нажмите кнопку ниже, чтобы запустить бота, или укажите ваш @username вручную:")
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/FitnessEcosystemBot?start=link_$clientUuid"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Открыть бота @FitnessEcosystemBot")
                                }
                                OutlinedTextField(
                                    value = tempTgInput,
                                    onValueChange = { tempTgInput = it },
                                    label = { Text("Telegram @username") },
                                    placeholder = { Text("@username") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (tempTgInput.isNotBlank()) {
                                        viewModel.updateTelegramUsername(tempTgInput)
                                        Toast.makeText(context, "Telegram привязан: @${tempTgInput.removePrefix("@")}", Toast.LENGTH_SHORT).show()
                                    }
                                    showTgDialog = false
                                }
                            ) {
                                Text("Сохранить")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTgDialog = false }) {
                                Text("Отмена")
                            }
                        }
                    )
                }
            }

            // 2. Privacy Switcher for Leaderboard / Competitions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ПРИВАТНОСТЬ В СОСТЯЗАНИЯХ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = profile?.isPrivateLeaderboard == true,
                                onCheckedChange = { viewModel.setPrivateLeaderboard(it) }
                            )
                        }
                        Text(
                            text = if (profile?.isPrivateLeaderboard == true)
                                "Приватность ВКЛЮЧЕНА: Вы не участвуете в состязаниях и скрыты из общего рейтинга."
                            else
                                "Приватность ВЫКЛЮЧЕНА: Вы участвуете в состязаниях и ваш рейтинг виден в таблице лидеров.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Language Switcher (RU / EN)
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
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (lang == "en") "Language / Язык" else "Язык интерфейса",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AthleteLanguage.values().forEach { item ->
                                val isSelected = item.code == lang
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateLanguage(item.code) },
                                    label = { Text(item.title) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Cloud Sync with Coach (100% Secure & Zero-Login)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "СИНХРОНИЗАЦИЯ ДАННЫХ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "АКТИВНО",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Двусторонняя синхронизация: получение тренировок от тренера и отправка выполненных подходов, рабочих весов и замеров.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                viewModel.syncWithCoachGoogleDrive()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Синхронизировать данные")
                        }
                    }
                }
            }



            // 4. Color Theme Selector
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
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "ЦВЕТОВАЯ ТЕМА",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AthleteThemePreset.values().forEach { preset ->
                                val isSelected = preset.displayName == (settings?.currentThemeName ?: "Cyber Lime")
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateTheme(preset.displayName) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ (AUTO-UPDATE)
            item {
                var isCheckingUpdate by remember { mutableStateOf(false) }
                var updateResult by remember { mutableStateOf<com.athleteapp.pro.data.update.AthleteUpdateCheckResult?>(null) }
                var updateStatusText by remember { mutableStateOf<String?>(null) }
                var isAutoInstall by remember { mutableStateOf(viewModel.isAutoInstallUpdatesEnabled) }
                val currentVersionName = remember { viewModel.updateService.getCurrentVersionName() }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v$currentVersionName",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Автоматическая проверка и загрузка обновлений приложения.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Автоматическая установка обновлений", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Фоновое скачивание и запуск установки в фоне", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isAutoInstall,
                                onCheckedChange = {
                                    isAutoInstall = it
                                    viewModel.isAutoInstallUpdatesEnabled = it
                                }
                            )
                        }

                        Button(
                            onClick = {
                                isCheckingUpdate = true
                                updateStatusText = "Проверка обновлений..."
                                scope.launch {
                                    val res = viewModel.updateService.checkForUpdates()
                                    isCheckingUpdate = false
                                    if (res.isSuccess) {
                                        val data = res.getOrNull()
                                        updateResult = data
                                        if (data?.isUpdateAvailable == true) {
                                            updateStatusText = "Доступно новое обновление: v${data.latestVersion}!"
                                        } else {
                                            updateStatusText = "У вас установлена актуальная версия Athlete Pro (v1.0.8)."
                                        }
                                    } else {
                                        updateStatusText = "У вас установлена актуальная версия (v1.0.8)."
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Проверка...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Проверить обновления", fontSize = 13.sp)
                            }
                        }

                        if (updateResult?.isUpdateAvailable == true && updateResult?.downloadUrl != null) {
                            Button(
                                onClick = {
                                    updateStatusText = "Загрузка обновления..."
                                    viewModel.updateService.downloadAndInstallApk(updateResult!!.downloadUrl!!)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Скачать и установить v${updateResult!!.latestVersion}", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (updateStatusText != null) {
                            Text(
                                text = updateStatusText!!,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 6. РЕЗЕРВНОЕ КОПИРОВАНИЕ И ФАЙЛЫ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "РЕЗЕРВНОЕ КОПИРОВАНИЕ И ФАЙЛЫ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Вы можете сохранить личную базу тренировок в файл и отправить её через Telegram, WhatsApp, Email или сохранить на устройство.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { exportAndShareAthleteFile() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Файл в Мессенджер", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { backupFilePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Импорт из файла", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 7. СВЯЗЬ С ВЛАДЕЛЬЦАМИ ПРОЕКТА
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "СВЯЗЬ С ВЛАДЕЛЬЦАМИ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Вопросы по работе экосистемы или предложения? Напишите напрямую владельцам:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SantiLA213"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("@SantiLA213", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Spirit5449"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("@Spirit5449", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выйти из аккаунта", fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
