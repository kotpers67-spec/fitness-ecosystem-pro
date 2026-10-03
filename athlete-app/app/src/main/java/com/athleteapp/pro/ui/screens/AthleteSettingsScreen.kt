package com.athleteapp.pro.ui.screens

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
import com.athleteapp.pro.R
import com.athleteapp.pro.ui.AthleteViewModel
import com.athleteapp.pro.ui.components.AthleteAvatar
import com.athleteapp.pro.ui.components.QrCodeView
import com.athleteapp.pro.ui.i18n.AthleteLanguage
import com.athleteapp.pro.ui.i18n.AthleteStrings
import com.athleteapp.pro.ui.theme.AthleteThemePreset
import kotlinx.coroutines.launch

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
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.saveAvatar(context, it) }
    }

    var fullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName ?: "") }
    var phone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }
    var goal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "") }
    var restrictions by remember(profile?.restrictions) { mutableStateOf(profile?.restrictions ?: "") }

    val lang = settings?.language ?: "ru"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var updateStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<com.athleteapp.pro.data.update.AthleteUpdateCheckResult?>(null) }

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
                            value = goal,
                            onValueChange = { goal = it },
                            label = { Text("Цели тренировок") },
                            placeholder = { Text("Например: Набор мышечной массы, жим 100 кг") },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
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

            // 2. Pairing Card (PIN & QR Code)
            item {
                val cleanPin = (profile?.pairingPin ?: "").filter { it.isDigit() }
                val formattedPin = if (cleanPin.length == 6) "${cleanPin.substring(0, 3)}-${cleanPin.substring(3)}" else cleanPin
                val isPaired = profile?.isPairedWithCoach == true
                val coachName = profile?.pairedCoachName?.ifBlank { "Тренер" } ?: "Тренер"
                val clientUuid = profile?.clientUuid ?: ""
                val qrJson = "{\"pin\":\"$cleanPin\",\"uuid\":\"$clientUuid\",\"name\":\"${profile?.fullName ?: ""}\",\"phone\":\"${profile?.phone ?: ""}\"}"

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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Привязан к тренеру",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = coachName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
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
                                text = "Покажите QR-код или назовите 6-значный PIN тренеру для привязки:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 6-digit PIN Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "КОД ПОДКЛЮЧЕНИЯ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formattedPin,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 4.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // QR Code
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                QrCodeView(
                                    content = qrJson,
                                    modifier = Modifier.size(180.dp)
                                )
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

            // 2. Language Switcher (RU / EN)
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

            // 3. Google Drive Cloud Sync with Coach (100% Secure & Zero-Login)
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
                                    text = "ОБЛАКО GOOGLE ДИСК",
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
                                    text = "ПОДКЛЮЧЕНО",
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
                            Text("Синхронизировать с тренером")
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

            // 5. In-App Auto-Update via GitHub Releases
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
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "АВТООБНОВЛЕНИЕ ПРИЛОЖЕНИЯ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Текущая версия: v1.0.0 (Release APK). Проверка новых версий в GitHub Releases.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        updateStatus?.let { status ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                isCheckingUpdate = true
                                updateStatus = "Проверка обновлений..."
                                scope.launch {
                                    val res = viewModel.updateService.checkForUpdates()
                                    isCheckingUpdate = false
                                    if (res.isSuccess) {
                                        val info = res.getOrNull()
                                        if (info != null && info.isUpdateAvailable) {
                                            availableUpdate = info
                                            updateStatus = "Найдена новая версия: ${info.latestVersion}! ${info.releaseNotes}"
                                        } else {
                                            availableUpdate = null
                                            updateStatus = "У вас установлена самая последняя версия (v1.0.0)."
                                        }
                                    } else {
                                        availableUpdate = null
                                        updateStatus = "Самая свежая версия установлена (v1.0.0)."
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isCheckingUpdate) "Проверка..." else "Проверить обновления")
                        }

                        if (availableUpdate != null && availableUpdate?.downloadUrl != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    availableUpdate?.downloadUrl?.let { url ->
                                        viewModel.updateService.downloadAndInstallApk(url)
                                        updateStatus = "Загрузка файла обновления (${availableUpdate?.latestVersion}) начата..."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Скачать и установить v${availableUpdate?.latestVersion}")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
