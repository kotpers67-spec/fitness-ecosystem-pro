package com.trainerapp.pro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainerapp.pro.R
import com.trainerapp.pro.ui.MainViewModel
import com.trainerapp.pro.ui.components.ClientAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToWorkout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val activeClient by viewModel.activeClient.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showClientSelectorDialog by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var clientSearchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClientAvatar(
                            photoUri = viewModel.trainerPhotoUri,
                            avatarBase64 = viewModel.trainerAvatarBase64,
                            size = 38.dp,
                            borderWidth = 1.5.dp,
                            defaultResId = R.drawable.avatar_coach,
                            contentDescription = "Тренер"
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "TRAINER PRO",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.primary
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Селектор подопечного с быстрым поиском
            Text(
                "ТЕКУЩИЙ ПОДОПЕЧНЫЙ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 0.5.sp
            )

            // Поле выбора клиента (кликабельное, открывает диалог поиска)
            OutlinedCard(
                onClick = {
                    clientSearchQuery = ""
                    showClientSelectorDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = activeClient?.fullName ?: "Выберите подопечного",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (!activeClient?.phone.isNullOrBlank()) {
                                Text(
                                    text = activeClient?.phone ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Кнопка добавления по коду прямо на HomeScreen
            OutlinedButton(
                onClick = { showPairingDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Добавить подопечного по коду",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 2. Client Summary Card
            activeClient?.let { client ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ClientAvatar(
                                    photoUri = client.photoUri,
                                    avatarBase64 = client.avatarBase64,
                                    size = 36.dp,
                                    borderWidth = 1.dp,
                                    defaultResId = R.drawable.avatar_athlete,
                                    contentDescription = client.fullName
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = client.fullName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "КАРТОЧКА АТЛЕТА",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (client.membershipStatus == "Активен") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = client.membershipStatus,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (client.membershipStatus == "Активен") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (client.goal.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(6.dp))
                                Text("Цель: ${client.goal}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (client.notes.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(6.dp))
                                Text("Заметки: ${client.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Главные кнопки действий
            Button(
                onClick = onNavigateToWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "ТРЕНИРОВКА",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "ИСТОРИЯ И ГРАФИКИ",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // Диалог быстрого поиска и выбора подопечного (SearchField по ФИО и номеру телефона)
    if (showClientSelectorDialog) {
        val filteredClients = remember(clients, clientSearchQuery) {
            val q = clientSearchQuery.trim().lowercase()
            if (q.isEmpty()) clients
            else {
                val digitsOnlyQ = q.filter { it.isDigit() }
                clients.filter { client ->
                    client.fullName.lowercase().contains(q) ||
                    client.phone.lowercase().contains(q) ||
                    (digitsOnlyQ.isNotEmpty() && client.phone.filter { it.isDigit() }.contains(digitsOnlyQ))
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showClientSelectorDialog = false },
            title = {
                Text("Выбор подопечного", fontWeight = FontWeight.Black)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Строка быстрого поиска (SearchField)
                    OutlinedTextField(
                        value = clientSearchQuery,
                        onValueChange = { clientSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Поиск по ФИО или телефону...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (clientSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { clientSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Список отфильтрованных клиентов
                    if (filteredClients.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Подопечные не найдены",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredClients) { client ->
                                val isSelected = client.id == activeClient?.id
                                Surface(
                                    onClick = {
                                        viewModel.selectClient(client.id)
                                        showClientSelectorDialog = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.fillMaxWidth()
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
                                                text = client.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            if (client.phone.isNotBlank()) {
                                                Text(
                                                    text = client.phone,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            if (client.goal.isNotBlank()) {
                                                Text(
                                                    text = client.goal,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Выбран",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Кнопка добавить по коду прямо из диалога поиска
                    Button(
                        onClick = {
                            showClientSelectorDialog = false
                            showPairingDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить по коду", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClientSelectorDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Диалог привязки по 6-значному PIN-коду или QR-коду
    if (showPairingDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var pairingCodeInput by remember { mutableStateOf("") }
        var isPairing by remember { mutableStateOf(false) }
        var pairingErrorMessage by remember { mutableStateOf<String?>(null) }

        val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                val decoded = com.trainerapp.pro.util.QrCodeScannerHelper.decodeFromBitmap(bitmap)
                if (!decoded.isNullOrBlank()) {
                    val codeToUse = if (decoded.trim().startsWith("{")) decoded else extractPairingCode(decoded).ifBlank { decoded }
                    pairingCodeInput = codeToUse
                    pairingErrorMessage = null
                    isPairing = true
                    viewModel.pairClientByCode(codeToUse) { success, msg ->
                        isPairing = false
                        if (success) {
                            showPairingDialog = false
                        } else {
                            pairingErrorMessage = msg
                        }
                    }
                } else {
                    pairingErrorMessage = "QR-код не распознан. Наведите камеру ближе или введите 6 цифр."
                }
            }
        }

        val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                pairingErrorMessage = "Для сканирования QR-кода требуется доступ к камере. Предоставьте разрешение или введите 6 цифр вручную."
            }
        }

        val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                val decoded = com.trainerapp.pro.util.QrCodeScannerHelper.decodeFromUri(context, uri)
                if (!decoded.isNullOrBlank()) {
                    val codeToUse = if (decoded.trim().startsWith("{")) decoded else extractPairingCode(decoded).ifBlank { decoded }
                    pairingCodeInput = codeToUse
                    pairingErrorMessage = null
                    isPairing = true
                    viewModel.pairClientByCode(codeToUse) { success, msg ->
                        isPairing = false
                        if (success) {
                            showPairingDialog = false
                        } else {
                            pairingErrorMessage = msg
                        }
                    }
                } else {
                    pairingErrorMessage = "QR-код на изображении не найден."
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!isPairing) showPairingDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Привязать подопечного", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Кнопки быстрого сканирования QR-кода
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isPairing
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("Сканировать QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isPairing
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Из фото QR", fontSize = 12.sp)
                        }
                    }

                    Text(
                        "Или введите 6 цифр кода подопечного (слитно, без дефиса):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = {
                            pairingCodeInput = extractPairingCode(it)
                            pairingErrorMessage = null
                        },
                        label = { Text("Код подопечного (6 цифр)") },
                        placeholder = { Text("739102 (без тире)") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isPairing
                    )

                    if (pairingErrorMessage != null) {
                        Text(
                            text = pairingErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (isPairing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Связывание и синхронизация данных...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPairing = true
                        pairingErrorMessage = null
                        val codeToPair = extractPairingCode(pairingCodeInput)
                        viewModel.pairClientByCode(codeToPair) { success, msg ->
                            isPairing = false
                            if (success) {
                                showPairingDialog = false
                            } else {
                                pairingErrorMessage = msg
                            }
                        }
                    },
                    enabled = !isPairing && pairingCodeInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Привязать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPairingDialog = false },
                    enabled = !isPairing
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

internal fun extractPairingCode(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    val linkMatch = Regex("""[?&](?:code|pin)=(\d{6})""").find(trimmed)
        ?: Regex("""(?:pair|code|pin)[/=](\d{6})""").find(trimmed)
    if (linkMatch != null) {
        return linkMatch.groupValues[1]
    }
    val jsonMatch = Regex("""\"(?:pin|code)\"\s*:\s*\"(\d{6})\"""").find(trimmed)
    if (jsonMatch != null) {
        return jsonMatch.groupValues[1]
    }
    val cleanCode = trimmed.filter { it.isDigit() }
    return if (cleanCode.length > 6) cleanCode.take(6) else cleanCode
}
