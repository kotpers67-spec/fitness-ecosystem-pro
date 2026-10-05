package com.athleteapp.pro.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athleteapp.pro.data.auth.AthleteRemoteAuthResult
import com.athleteapp.pro.data.auth.TelegramSessionStatusResult
import com.athleteapp.pro.ui.AthleteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AthleteAuthScreen(viewModel: AthleteViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    var show2FaDialog by remember { mutableStateOf(false) }
    var twoFaUserId by remember { mutableLongStateOf(0L) }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var otpTimerSeconds by remember { mutableIntStateOf(300) }
    var isVerifying2Fa by remember { mutableStateOf(false) }

    var isPollingTgSession by remember { mutableStateOf(false) }
    var tgStatusText by remember { mutableStateOf<String?>(null) }

    var showTgCodeDialog by remember { mutableStateOf(false) }
    var tgCodeInput by remember { mutableStateOf("") }
    var tgCodeError by remember { mutableStateOf<String?>(null) }
    var tgCodeTimerSeconds by remember { mutableIntStateOf(300) }
    var isVerifyingTgOtp by remember { mutableStateOf(false) }

    LaunchedEffect(showTgCodeDialog) {
        if (showTgCodeDialog) {
            tgCodeTimerSeconds = 300
            while (tgCodeTimerSeconds > 0) {
                delay(1000L)
                tgCodeTimerSeconds--
            }
        }
    }

    LaunchedEffect(show2FaDialog) {
        if (show2FaDialog) {
            otpTimerSeconds = 300
            while (otpTimerSeconds > 0) {
                delay(1000L)
                otpTimerSeconds--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ATHLETE PRO",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isLoginMode) "Вход в аккаунт атлета" else "Регистрация нового атлета",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Telegram 1-Click Fast Login Button
            Button(
                onClick = {
                    scope.launch {
                        errorMessage = null
                        isPollingTgSession = true
                        tgStatusText = "Подключение к Telegram..."

                        val session = viewModel.remoteAuthManager.initTelegramSession()
                        val sessionId = session?.first ?: ""
                        val authParam = if (sessionId.isNotBlank()) {
                            if (sessionId.startsWith("auth_")) sessionId else "auth_$sessionId"
                        } else "login"

                        val tgScheme = "tg://resolve?domain=fitnessecosystemBOT&start=$authParam"
                        val webFallback = if (session != null) session.second else "https://t.me/fitnessecosystemBOT?start=$authParam"

                        try {
                            val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tgScheme)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(tgIntent)
                        } catch (_: Exception) {
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            } catch (_: Exception) {}
                        }

                        if (session != null) {
                            val sessionId = session.first
                            tgStatusText = "Ожидание нажатия кнопки СТАРТ в Telegram боте..."
                            var elapsed = 0
                            while (elapsed < 300 && isPollingTgSession) {
                                delay(1500L)
                                elapsed += 2
                                val status = viewModel.remoteAuthManager.pollTelegramSession(sessionId)
                                if (status is TelegramSessionStatusResult.Authorized) {
                                    viewModel.completeRemoteLogin(status.user)
                                    isPollingTgSession = false
                                    tgStatusText = null
                                    break
                                } else if (status is TelegramSessionStatusResult.Require2Fa) {
                                    errorMessage = "Включена 2FA аутентификация: вход в 1 клик заблокирован политикой безопасности. Введите 6-значный код из Telegram"
                                    isPollingTgSession = false
                                    tgStatusText = null
                                    tgCodeInput = ""
                                    tgCodeError = null
                                    showTgCodeDialog = true
                                    break
                                } else if (status is TelegramSessionStatusResult.Expired) {
                                    errorMessage = "Срок действия сессии Telegram истёк"
                                    isPollingTgSession = false
                                    tgStatusText = null
                                    break
                                }
                            }
                        } else {
                            tgStatusText = null
                            isPollingTgSession = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2AABEE),
                    contentColor = Color.White
                )
            ) {
                if (isPollingTgSession) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ожидание подтверждения...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = "✈ Войти через Telegram (в 1 клик)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (tgStatusText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tgStatusText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telegram Login by 6-digit Bot Code Button
            OutlinedButton(
                onClick = {
                    tgCodeInput = ""
                    tgCodeError = null
                    showTgCodeDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "🔑 Войти по коду из Telegram бота",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "  или по логину  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switcher Tabs
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Button(
                        onClick = { isLoginMode = true; errorMessage = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoginMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isLoginMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Вход", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { isLoginMode = false; errorMessage = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isLoginMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (!isLoginMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Регистрация", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fields Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Имя и Фамилия") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Номер телефона") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Логин") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        enabled = !isLoggingIn,
                        onClick = {
                            errorMessage = null
                            if (isLoginMode) {
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Введите логин и пароль"
                                } else {
                                    scope.launch {
                                        isLoggingIn = true
                                        val res = viewModel.remoteLogin(username, password)
                                        when (res) {
                                            is AthleteRemoteAuthResult.Success -> {
                                                // Logged in successfully
                                            }
                                            is AthleteRemoteAuthResult.Require2Fa -> {
                                                twoFaUserId = res.userId
                                                otpTimerSeconds = res.expiresInSeconds
                                                otpInput = ""
                                                otpError = null
                                                show2FaDialog = true
                                            }
                                            is AthleteRemoteAuthResult.InvalidCredentials -> {
                                                // Check local fallback
                                                if (viewModel.checkCredentials(username, password)) {
                                                    if (viewModel.is2FaEnabled) {
                                                        show2FaDialog = true
                                                        otpInput = ""
                                                        otpError = null
                                                        otpTimerSeconds = 300
                                                    } else {
                                                        viewModel.completeLogin()
                                                    }
                                                } else {
                                                    errorMessage = res.message
                                                }
                                            }
                                            is AthleteRemoteAuthResult.Error -> {
                                                errorMessage = res.message
                                            }
                                            is AthleteRemoteAuthResult.OfflineFallback -> {
                                                if (viewModel.checkCredentials(username, password)) {
                                                    if (viewModel.is2FaEnabled) {
                                                        show2FaDialog = true
                                                        otpInput = ""
                                                        otpError = null
                                                        otpTimerSeconds = 300
                                                    } else {
                                                        viewModel.completeLogin()
                                                    }
                                                } else {
                                                    errorMessage = "Неверный логин или пароль (офлайн режим)"
                                                }
                                            }
                                        }
                                        isLoggingIn = false
                                    }
                                }
                            } else {
                                if (fullName.isBlank()) {
                                    errorMessage = "Укажите имя и фамилию"
                                } else if (username.isBlank() || username.length < 3) {
                                    errorMessage = "Логин должен быть от 3 символов"
                                } else if (password.length < 4) {
                                    errorMessage = "Пароль должен быть от 4 символов"
                                } else {
                                    scope.launch {
                                        isLoggingIn = true
                                        val regRes = viewModel.remoteRegister(
                                            fullName = fullName,
                                            username = username,
                                            password = password,
                                            phone = phone
                                        )
                                        if (regRes is AthleteRemoteAuthResult.Error) {
                                            errorMessage = regRes.message
                                        }
                                        isLoggingIn = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Войти" else "Зарегистрироваться",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Контакты владельцев проекта
            Text(
                text = "Связь с владельцами проекта:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SantiLA213"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("@SantiLA213", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Spirit5449"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("@Spirit5449", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }

    if (show2FaDialog) {
        val mm = otpTimerSeconds / 60
        val ss = otpTimerSeconds % 60
        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", mm, ss)

        AlertDialog(
            onDismissRequest = { show2FaDialog = false },
            title = { Text("Двухфакторная аутентификация (2FA)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Введите 6-значный код подтверждения из Telegram бота:")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "⏱ Действует: $timeStr",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                otpInput = it
                                otpError = null
                            }
                        },
                        label = { Text("Код 2FA (6 цифр)") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (otpError != null) {
                        Text(
                            text = otpError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isVerifying2Fa,
                    onClick = {
                        if (otpTimerSeconds <= 0) {
                            otpError = "Срок действия кода истёк (5 минут)"
                        } else if (otpInput.length != 6) {
                            otpError = "Введите ровно 6 цифр"
                        } else {
                            scope.launch {
                                isVerifying2Fa = true
                                if (twoFaUserId > 0L) {
                                    val res = viewModel.remoteVerify2Fa(twoFaUserId, otpInput)
                                    if (res is AthleteRemoteAuthResult.Success) {
                                        show2FaDialog = false
                                    } else if (res is AthleteRemoteAuthResult.Error) {
                                        otpError = res.message
                                    } else {
                                        val ok = viewModel.verify2FaOtp(otpInput)
                                        if (ok) {
                                            show2FaDialog = false
                                        } else {
                                            otpError = "Неверный код подтверждения"
                                        }
                                    }
                                } else {
                                    val ok = viewModel.verify2FaOtp(otpInput)
                                    if (ok) {
                                        show2FaDialog = false
                                    } else {
                                        otpError = "Неверный код подтверждения"
                                    }
                                }
                                isVerifying2Fa = false
                            }
                        }
                    }
                ) {
                    if (isVerifying2Fa) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Подтвердить")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { show2FaDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showTgCodeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isVerifyingTgOtp) {
                    showTgCodeDialog = false
                }
            },
            title = {
                Text(
                    text = "Вход по 6-значному коду",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Введите 6-значный код из Telegram бота (@fitnessecosystemBOT) или код подключения:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val mm = tgCodeTimerSeconds / 60
                    val ss = tgCodeTimerSeconds % 60
                    val timeStr = String.format(java.util.Locale.US, "%02d:%02d", mm, ss)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "⏱ Действует: $timeStr",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }

                    OutlinedTextField(
                        value = tgCodeInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                tgCodeInput = it
                                tgCodeError = null
                            }
                        },
                        label = { Text("6-значный код") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (tgCodeError != null) {
                        Text(
                            text = tgCodeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isVerifyingTgOtp && tgCodeInput.length == 6,
                    onClick = {
                        if (tgCodeTimerSeconds <= 0) {
                            tgCodeError = "Срок действия кода истёк (5 минут)"
                        } else {
                            scope.launch {
                                isVerifyingTgOtp = true
                                tgCodeError = null
                                val res = viewModel.remoteAuthManager.verifyTelegramOtp(tgCodeInput)
                                if (res is AthleteRemoteAuthResult.Success) {
                                    viewModel.completeRemoteLogin(res.user)
                                    showTgCodeDialog = false
                                } else if (res is AthleteRemoteAuthResult.Error) {
                                    tgCodeError = res.message
                                } else {
                                    val ok = viewModel.verify2FaOtp(tgCodeInput)
                                    if (ok) {
                                        showTgCodeDialog = false
                                    } else {
                                        tgCodeError = "Неверный код авторизации"
                                    }
                                }
                                isVerifyingTgOtp = false
                            }
                        }
                    }
                ) {
                    if (isVerifyingTgOtp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Войти")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTgCodeDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
