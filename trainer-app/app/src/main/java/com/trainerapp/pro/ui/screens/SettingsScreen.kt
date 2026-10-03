package com.trainerapp.pro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainerapp.pro.data.local.entities.ClientEntity
import com.trainerapp.pro.data.local.entities.ExerciseEntity
import com.trainerapp.pro.data.sync.GitHubSyncConfig
import com.trainerapp.pro.data.update.UpdateCheckResult
import com.trainerapp.pro.ui.MainViewModel
import com.trainerapp.pro.ui.i18n.AppLanguage
import com.trainerapp.pro.ui.i18n.AppStrings
import com.trainerapp.pro.ui.theme.AppThemePreset
import com.trainerapp.pro.ui.theme.LayoutStylePreset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clients by viewModel.clients.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeClient by viewModel.activeClient.collectAsState()

    val lang = settings.language

    var showAddClientDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<ClientEntity?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }

    // GitHub Sync state
    var showGitHubConfigDialog by remember { mutableStateOf(false) }
    var githubTokenInput by remember(settings.githubToken) { mutableStateOf(settings.githubToken) }
    var githubRepoInput by remember(settings.githubRepo) { mutableStateOf(settings.githubRepo) }
    var isSyncing by remember { mutableStateOf(false) }

    // Auto-update state
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppStrings.get("settings_title", lang), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. ЯЗЫК ИНТЕРФЕЙСА (RU / EN)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get("language", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.values().forEach { l ->
                                FilterChip(
                                    selected = settings.language == l.code,
                                    onClick = { viewModel.updateLanguage(l.code) },
                                    label = { Text(l.title, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. АВТООБНОВЛЕНИЕ ПРИЛОЖЕНИЯ (GITHUB RELEASES API)
            item {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.get("auto_update", lang),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text("v1.0.0", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isCheckingUpdate = true
                                scope.launch {
                                    val res = viewModel.updateService.checkForUpdates()
                                    isCheckingUpdate = false
                                    res.onSuccess {
                                        updateResult = it
                                        showUpdateDialog = true
                                    }.onFailure {
                                        Toast.makeText(context, "Ошибка проверки: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(AppStrings.get("check_update", lang))
                            }
                        }
                    }
                }
            }

            // 3. ОБЛАЧНАЯ СИНХРОНИЗАЦИЯ GOOGLE ДИСК (БЕЗОПАСНО БЕЗ ВХОДА В АККАУНТ)
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
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ОБЛАКО GOOGLE ДИСК",
                                    fontSize = 12.sp,
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
                            text = if (activeClient != null) "Активный подопечный: " else "Подопечный не выбран",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Двусторонняя синхронизация: передача плана подопечному и получение выполненных подходов и замеров.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Button(
                            onClick = {
                                if (activeClient == null) {
                                    Toast.makeText(context, "Выберите подопечного для синхронизации", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSyncing = true
                                scope.launch {
                                    viewModel.syncActiveClientWithGoogleDrive()
                                    isSyncing = false
                                }
                            },
                            enabled = !isSyncing && activeClient != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Синхронизация...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Синхронизировать с Google Диском", fontSize = 13.sp)
                            }
                        }

                        val syncStatusMsg by viewModel.syncStatus.collectAsState()
                        if (syncStatusMsg != null) {
                            Text(
                                text = syncStatusMsg!!,
                                fontSize = 11.sp,
                                color = if (syncStatusMsg!!.startsWith("Ошибка")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            // 4. ЦВЕТОВАЯ ТЕМА (5 ПАЛИТР)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = AppStrings.get("color_theme", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        AppThemePreset.values().forEach { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.displayName, fontWeight = FontWeight.Medium)
                                RadioButton(
                                    selected = settings.currentThemeName == preset.displayName,
                                    onClick = { viewModel.updateTheme(preset.displayName) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. ДИЗАЙН И КОМПОНОВКА ЭКРАНА (5 СТИЛЕЙ)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = AppStrings.get("layout_style", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        LayoutStylePreset.values().forEach { style ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(style.displayName, fontWeight = FontWeight.Bold)
                                        Text(style.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    RadioButton(
                                        selected = settings.currentLayoutStyleName == style.displayName,
                                        onClick = { viewModel.updateLayoutStyle(style.displayName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. СПИСОК ИМЕН / КЛИЕНТОВ (CRUD)
            item {
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
                            Text(
                                text = AppStrings.get("clients_list", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = {
                                clientToEdit = null
                                showAddClientDialog = true
                            }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (clients.isEmpty()) {
                            Text("Список пуст", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        } else {
                            clients.forEachIndexed { index, client ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("[${index + 1}] ${client.fullName}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Row {
                                        IconButton(onClick = {
                                            clientToEdit = client
                                            showAddClientDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteClient(client) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. СПИСОК УПРАЖНЕНИЙ
            item {
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
                            Text(
                                text = "${AppStrings.get("exercises_list", lang)} (${exercises.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { showAddExerciseDialog = true }) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        exercises.take(6).forEachIndexed { index, ex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("[${index + 1}] ${ex.name}", fontSize = 13.sp)
                                Text(ex.muscleGroup, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // 8. ЛОКАЛЬНЫЙ JSON ЭКСПОРТ/ИМПОРТ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ЛОКАЛЬНЫЙ JSON ЭКСПОРТ / ИМПОРТ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        backupJsonText = viewModel.backupManager.exportToJson()
                                        showBackupDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Экспорт JSON", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    backupJsonText = ""
                                    showBackupDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Импорт JSON", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // GitHub Config Dialog
    if (showGitHubConfigDialog) {
        AlertDialog(
            onDismissRequest = { showGitHubConfigDialog = false },
            title = { Text("Настройка GitHub Sync", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Для синхронизации через закрытый репозиторий GitHub создайте Personal Access Token (PAT) с правами repo.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(value = githubTokenInput, onValueChange = { githubTokenInput = it }, label = { Text("GitHub Token (ghp_...)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = githubRepoInput, onValueChange = { githubRepoInput = it }, label = { Text("Имя репозитория") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateGitHubConfig(githubTokenInput.trim(), githubRepoInput.trim())
                        showGitHubConfigDialog = false
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showGitHubConfigDialog = false }) { Text("Отмена") } }
        )
    }

    // Auto-update Result Dialog
    if (showUpdateDialog && updateResult != null) {
        val update = updateResult!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (update.isUpdateAvailable) "Доступно обновление!" else "Версия актуальна", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Текущая: ${update.currentVersion} | Доступная: ${update.latestVersion}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(update.releaseNotes, fontSize = 13.sp)
                }
            },
            confirmButton = {
                if (update.isUpdateAvailable && update.downloadUrl != null) {
                    Button(
                        onClick = {
                            viewModel.updateService.downloadAndInstallApk(update.downloadUrl)
                            showUpdateDialog = false
                        }
                    ) { Text("Скачать и обновить") }
                } else {
                    TextButton(onClick = { showUpdateDialog = false }) { Text("OK") }
                }
            },
            dismissButton = {
                if (update.isUpdateAvailable) {
                    TextButton(onClick = { showUpdateDialog = false }) { Text("Позже") }
                }
            }
        )
    }

    // Client Add/Edit Dialog
    if (showAddClientDialog) {
        var name by remember { mutableStateOf(clientToEdit?.fullName ?: "") }
        var phone by remember { mutableStateOf(clientToEdit?.phone ?: "") }
        var goal by remember { mutableStateOf(clientToEdit?.goal ?: "") }
        var notes by remember { mutableStateOf(clientToEdit?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text(if (clientToEdit == null) "Новый подопечный" else "Редактировать подопечного", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ФИО *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Цель тренировок") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Ограничения / Заметки") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val client = clientToEdit?.copy(fullName = name, phone = phone, goal = goal, notes = notes)
                                ?: ClientEntity(fullName = name, phone = phone, goal = goal, notes = notes)
                            viewModel.saveClient(client)
                            showAddClientDialog = false
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAddClientDialog = false }) { Text("Отмена") } }
        )
    }

    // Custom Exercise Dialog
    if (showAddExerciseDialog) {
        var exName by remember { mutableStateOf("") }
        var exGroup by remember { mutableStateOf("Грудь") }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Новое упражнение", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = exName, onValueChange = { exName = it }, label = { Text("Название упражнения *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = exGroup, onValueChange = { exGroup = it }, label = { Text("Группа мышц") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exName.isNotBlank()) {
                            viewModel.saveExercise(ExerciseEntity(name = exName, muscleGroup = exGroup, isCustom = true))
                            showAddExerciseDialog = false
                        }
                    }
                ) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddExerciseDialog = false }) { Text("Отмена") } }
        )
    }

    // Backup JSON Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Данные резервной копии", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = backupJsonText,
                    onValueChange = { backupJsonText = it },
                    label = { Text("JSON данные") },
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupJsonText.isNotBlank()) {
                            scope.launch {
                                val res = viewModel.backupManager.importFromJson(backupJsonText)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Импортировано успешно!", Toast.LENGTH_SHORT).show()
                                    showBackupDialog = false
                                } else {
                                    Toast.makeText(context, "Ошибка формата JSON", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) { Text("Импортировать") }
            },
            dismissButton = { TextButton(onClick = { showBackupDialog = false }) { Text("Закрыть") } }
        )
    }
}
