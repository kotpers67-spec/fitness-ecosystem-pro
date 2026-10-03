package com.trainerapp.pro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.trainerapp.pro.data.local.entities.ClientEntity
import com.trainerapp.pro.data.local.entities.ExerciseEntity
import com.trainerapp.pro.data.update.UpdateCheckResult
import com.trainerapp.pro.ui.MainViewModel
import com.trainerapp.pro.ui.i18n.AppLanguage
import com.trainerapp.pro.ui.i18n.AppStrings
import com.trainerapp.pro.ui.theme.AppThemePreset
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

    fun exportAndShareFile() {
        scope.launch {
            try {
                val jsonText = viewModel.backupManager.exportToJson()
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "TrainerPro_Backup_$dateStr.json"
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
                    putExtra(Intent.EXTRA_SUBJECT, "Резервная копия Trainer Pro")
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

    var showAddClientDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<ClientEntity?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }

    // Exercise search & filter
    var exerciseSearch by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf("Все") }

    var isSyncing by remember { mutableStateOf(false) }

    // Auto-update state
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val muscleGroups = listOf("Все", "Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс/Кор", "Кардио")

    val filteredExercises = remember(exercises, exerciseSearch, selectedMuscleGroup) {
        exercises.filter { ex ->
            val matchesSearch = exerciseSearch.isBlank() || ex.name.contains(exerciseSearch, ignoreCase = true)
            val matchesGroup = selectedMuscleGroup == "Все" || ex.muscleGroup.equals(selectedMuscleGroup, ignoreCase = true)
            matchesSearch && matchesGroup
        }
    }

    var trainerFirstName by remember { mutableStateOf(viewModel.trainerFirstName) }
    var trainerLastName by remember { mutableStateOf(viewModel.trainerLastName) }
    var trainerPhone by remember { mutableStateOf(viewModel.trainerPhone) }
    var selectedTrainerPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val trainerPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedTrainerPhotoUri = uri
            viewModel.saveTrainerProfile(context, trainerFirstName, trainerLastName, trainerPhone, uri)
            Toast.makeText(context, "Фото тренера обновлено!", Toast.LENGTH_SHORT).show()
        }
    }

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

            // 0. КАРТОЧКА И ПРОФИЛЬ ТРЕНЕРА
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "ПРОФИЛЬ И КАРТОЧКА ТРЕНЕРА",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            com.trainerapp.pro.ui.components.ClientAvatar(
                                photoUri = selectedTrainerPhotoUri?.toString() ?: viewModel.trainerPhotoUri,
                                avatarBase64 = viewModel.trainerAvatarBase64,
                                size = 64.dp,
                                defaultResId = com.trainerapp.pro.R.drawable.avatar_coach
                            )

                            OutlinedButton(
                                onClick = { trainerPhotoLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Изменить фото", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = trainerFirstName,
                            onValueChange = { trainerFirstName = it },
                            label = { Text("Имя") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = trainerLastName,
                            onValueChange = { trainerLastName = it },
                            label = { Text("Фамилия") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = trainerPhone,
                            onValueChange = { trainerPhone = it },
                            label = { Text("Телефон для связи") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                viewModel.saveTrainerProfile(context, trainerFirstName, trainerLastName, trainerPhone, selectedTrainerPhotoUri)
                                Toast.makeText(context, "Профиль тренера сохранен!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("СОХРАНИТЬ ПРОФИЛЬ ТРЕНЕРА", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

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

            // 2. ЦВЕТОВАЯ ТЕМА (5 ПАЛИТР)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get("color_theme", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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

            // 3. ОБЛАЧНАЯ СИНХРОНИЗАЦИЯ ДАННЫХ (ZERO-LOGIN & ШИФРОВАНИЕ)
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
                                    text = "СИНХРОНИЗАЦИЯ ДАННЫХ",
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
                                    text = "АКТИВНО",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (activeClient != null) "Активный подопечный: ${activeClient!!.fullName}" else "Подопечный не выбран",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Двусторонняя синхронизация: отправка назначенного плана подопечному и получение фактических подходов и замеров.",
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
                                Text("Синхронизировать данные", fontSize = 13.sp)
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

            // 3.5. ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ (AUTO-UPDATE)
            item {
                var isCheckingUpdate by remember { mutableStateOf(false) }
                var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
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
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ",
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
                                    text = "v$currentVersionName",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Автоматическая проверка и загрузка новых релизов приложения.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Автоматическая установка обновлений", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Фоновое скачивание и запуск установки в фоне", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
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
                                            updateStatusText = "Доступна новая версия: v${data.latestVersion}!"
                                        } else {
                                            updateStatusText = "У вас уже установлена актуальная версия Trainer Pro (v1.0.5)."
                                        }
                                    } else {
                                        updateStatusText = "У вас установлена актуальная версия (v1.0.5)."
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Проверка...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Проверить обновления", fontSize = 13.sp)
                            }
                        }

                        if (updateResult?.isUpdateAvailable == true && updateResult?.downloadUrl != null) {
                            Button(
                                onClick = {
                                    updateStatusText = "Загрузка и установка обновления..."
                                    viewModel.updateService.downloadAndInstallApk(updateResult!!.downloadUrl!!)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
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

            // 4. СПИСОК ПОДОПЕЧНЫХ (CRUD)
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
                                Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.get("clients_list", lang),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                clientToEdit = null
                                showAddClientDialog = true
                            }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (clients.isEmpty()) {
                            Text(
                                text = "Список подопечных пуст. Добавьте первого клиента по кнопке '+' или привяжите по PIN/QR-коду на главном экране.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            clients.forEachIndexed { index, client ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("[${index + 1}] ${client.fullName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (client.phone.isNotBlank()) {
                                            Text(client.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
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

            // 5. КАТАЛОГ УПРАЖНЕНИЙ (ПОЛНЫЙ CRUD: ДОБАВЛЕНИЕ, РЕДАКТИРОВАНИЕ, ПОИСК, УДАЛЕНИЕ)
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
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "КАТАЛОГ УПРАЖНЕНИЙ (${filteredExercises.size}/${exercises.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                exerciseToEdit = null
                                showAddExerciseDialog = true
                            }) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Добавить упражнение", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Search Field
                        OutlinedTextField(
                            value = exerciseSearch,
                            onValueChange = { exerciseSearch = it },
                            placeholder = { Text("Поиск упражнения...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (exerciseSearch.isNotBlank()) {
                                    IconButton(onClick = { exerciseSearch = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        // Muscle Group Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(muscleGroups) { group ->
                                FilterChip(
                                    selected = selectedMuscleGroup == group,
                                    onClick = { selectedMuscleGroup = group },
                                    label = { Text(group, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        if (filteredExercises.isEmpty()) {
                            Text(
                                text = "Упражнения не найдены. Нажмите '+' для добавления нового упражнения.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredExercises.forEach { ex ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ex.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${ex.muscleGroup} • Отдых ${ex.defaultRestSeconds}с",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        exerciseToEdit = ex
                                                        showAddExerciseDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteExercise(ex) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. АВТООБНОВЛЕНИЕ ПРИЛОЖЕНИЯ
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

            // 7. ЛОКАЛЬНЫЙ JSON ЭКСПОРТ/ИМПОРТ & ФАЙЛЫ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "РЕЗЕРВНОЕ КОПИРОВАНИЕ И РЕЗЕРВНЫЕ ФАЙЛЫ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Вы можете сохранить базу данных в файл и отправить её через Telegram, WhatsApp, Email или сохранить в память телефона.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { exportAndShareFile() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Файл в Мессенджер", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Импорт из файла", fontSize = 11.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        backupJsonText = viewModel.backupManager.exportToJson()
                                        showBackupDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Текст JSON", fontSize = 11.sp)
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
                                Text("Вставить JSON", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // Exercise Add/Edit Dialog
    if (showAddExerciseDialog) {
        var exName by remember(exerciseToEdit) { mutableStateOf(exerciseToEdit?.name ?: "") }
        var exGroup by remember(exerciseToEdit) { mutableStateOf(exerciseToEdit?.muscleGroup ?: "Грудь") }
        var exRest by remember(exerciseToEdit) { mutableStateOf((exerciseToEdit?.defaultRestSeconds ?: 90).toString()) }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text(if (exerciseToEdit == null) "Новое упражнение" else "Редактировать упражнение", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exName,
                        onValueChange = { exName = it },
                        label = { Text("Название упражнения *") },
                        placeholder = { Text("Например: Жим лежа на брусьях") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exGroup,
                        onValueChange = { exGroup = it },
                        label = { Text("Группа мышц") },
                        placeholder = { Text("Грудь, Спина, Ноги, Плечи, Руки, Пресс...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exRest,
                        onValueChange = { exRest = it },
                        label = { Text("Время отдыха (секунды)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exName.isNotBlank()) {
                            val restSec = exRest.toIntOrNull() ?: 90
                            val ex = exerciseToEdit?.copy(name = exName.trim(), muscleGroup = exGroup.trim(), defaultRestSeconds = restSec)
                                ?: ExerciseEntity(name = exName.trim(), muscleGroup = exGroup.trim(), defaultRestSeconds = restSec, isCustom = true)
                            viewModel.saveExercise(ex)
                            showAddExerciseDialog = false
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAddExerciseDialog = false }) { Text("Отмена") } }
        )
    }

    // Client Add/Edit Dialog
    if (showAddClientDialog) {
        var name by remember(clientToEdit) { mutableStateOf(clientToEdit?.fullName ?: "") }
        var phone by remember(clientToEdit) { mutableStateOf(clientToEdit?.phone ?: "") }
        var goal by remember(clientToEdit) { mutableStateOf(clientToEdit?.goal ?: "") }
        var notes by remember(clientToEdit) { mutableStateOf(clientToEdit?.notes ?: "") }

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
                            val client = clientToEdit?.copy(fullName = name.trim(), phone = phone.trim(), goal = goal.trim(), notes = notes.trim())
                                ?: ClientEntity(fullName = name.trim(), phone = phone.trim(), goal = goal.trim(), notes = notes.trim())
                            viewModel.saveClient(client)
                            showAddClientDialog = false
                        }
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAddClientDialog = false }) { Text("Отмена") } }
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
