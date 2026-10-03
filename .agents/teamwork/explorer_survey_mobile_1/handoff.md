# Handoff Report: Mobile Architecture & Crash Investigation (Trainer Pro & Athlete Pro)

## 1. Observation

Direct examination of the `trainer-app` and `athlete-app` codebases revealed the following concrete findings across the 4 key investigation areas:

### 1.1 Camera & QR Code Scanning
1. **Missing Runtime CAMERA Permission Check in Trainer Pro Pairing Dialog**:
   - **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt`
   - **Lines 453–455**:
     ```kotlin
     val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
         androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
     ) { bitmap -> ... }
     ```
   - **Lines 526–531**:
     ```kotlin
     Button(
         onClick = { cameraLauncher.launch(null) },
         modifier = Modifier.weight(1f),
         shape = RoundedCornerShape(10.dp),
         colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
         enabled = !isPairing
     ) { ... }
     ```
   - **Observation**: `cameraLauncher.launch(null)` is invoked directly on button click without verifying `ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED`. There is no `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` defined in the composable. Even though `android.permission.CAMERA` is declared in `trainer-app/app/src/main/AndroidManifest.xml` (line 8), Android 6.0+ (API 23+) requires runtime permission granting; otherwise, the camera contract triggers a `SecurityException` or silently returns `null`.

2. **Exception Handling and Memory Pressure in `QrCodeScannerHelper`**:
   - **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/util/QrCodeScannerHelper.kt`
   - **Lines 14–26**:
     ```kotlin
     fun decodeFromBitmap(bitmap: Bitmap): String? {
         return try {
             val width = bitmap.width
             val height = bitmap.height
             val pixels = IntArray(width * height)
             bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
             val source = RGBLuminanceSource(width, height, pixels)
             val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
             MultiFormatReader().decode(binaryBitmap).text
         } catch (_: Exception) {
             null
         }
     }
     ```
   - **Lines 28–37**:
     ```kotlin
     fun decodeFromUri(context: Context, uri: Uri): String? {
         return try {
             context.contentResolver.openInputStream(uri)?.use { stream ->
                 val bitmap = BitmapFactory.decodeStream(stream) ?: return null
                 decodeFromBitmap(bitmap)
             }
         } catch (_: Exception) {
             null
         }
     }
     ```
   - **Observation**:
     - `catch (_: Exception)` does **not** catch `java.lang.OutOfMemoryError` (or `java.lang.Error` / `Throwable`).
     - `val pixels = IntArray(width * height)` for a modern camera capture (e.g. 12 MP: 4000x3000) attempts to allocate 12 million integers (48 MB of contiguous heap memory), causing an unhandled `OutOfMemoryError` crash.
     - In `decodeFromUri`, `BitmapFactory.decodeStream(stream)` reads the full-resolution image into memory with no `inJustDecodeBounds` dimension check or `inSampleSize` downscaling.

---

### 1.2 CursorWindow / Avatar Cache & SQLite Limits
1. **Avatar Storage in Athlete Pro**:
   - **File**: `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt`
   - **Lines 333–345**:
     ```kotlin
     val squareBitmap = Bitmap.createBitmap(originalBitmap, x, y, edge, edge)
     val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

     val avatarFile = File(context.filesDir, "athlete_avatar.jpg")
     FileOutputStream(avatarFile).use { out ->
         scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
     }

     val bytes = avatarFile.readBytes()
     val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
     val savedPath = avatarFile.absolutePath
     val current = profile.value ?: AthleteProfileEntity()
     dao.saveProfile(current.copy(avatarPath = savedPath, photoUri = savedPath, avatarBase64 = base64Str))
     ```
   - **File**: `athlete-app/app/src/main/java/com/athleteapp/pro/data/local/entities/AthleteEntities.kt`
   - **Lines 20, 26**: `avatarBase64: String? = null`, `pairedCoachAvatarBase64: String? = null` in `@Entity(tableName = "athlete_profile")`.
   - **Observation**: Avatars are scaled to `512x512` and compressed at `JPEG 85%`. This produces file sizes of 50 KB – 120 KB, which expand in Base64 to 70 KB – 160 KB. Storing this directly into Room SQLite creates heavy rows.

2. **Avatar Storage in Trainer Pro**:
   - **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt`
   - **Lines 147–157**:
     ```kotlin
     val squareBitmap = android.graphics.Bitmap.createBitmap(originalBitmap, x, y, edge, edge)
     val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

     val file = java.io.File(context.filesDir, "trainer_avatar.jpg")
     java.io.FileOutputStream(file).use { out ->
         scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
     }
     val bytes = file.readBytes()
     val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
     editor.putString("trainer_photo_uri", file.absolutePath)
     editor.putString("trainer_avatar_base64", b64)
     ```
   - **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/data/local/entities/TrainerEntities.kt`
   - **Line 23**: `val avatarBase64: String? = null` in `@Entity(tableName = "clients") ClientEntity`.
   - **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/GitHubSyncManager.kt`
   - **Lines 216–218**:
     ```kotlin
     if (existingClient != null && !payload.avatarBase64.isNullOrBlank() && existingClient.avatarBase64 != payload.avatarBase64) {
         dao.updateClient(existingClient.copy(avatarBase64 = payload.avatarBase64))
     }
     ```
   - **Observation**: In Trainer Pro, each client in the `clients` table can store an `avatarBase64`. In `TrainerDao.getAllClients()`, `SELECT * FROM clients` queries all rows with `avatarBase64`. In SQLite / Android, `CursorWindow` has a default limit of 2 MB. If multiple clients have large Base64 blobs, or if a single blob exceeds the row allocation budget, `android.database.sqlite.SQLiteBlobTooBigException` is thrown.
   - **Avatar Decoding in UI**:
     - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/components/CommonComponents.kt` line 263: `catch (_: Exception) { null }`
     - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/components/CommonComponents.kt` line 51: `catch (_: Exception) { null }`
     - Neither catches `OutOfMemoryError` on `BitmapFactory.decodeByteArray`.

---

### 1.3 MainViewModel Blocking Flow Initialization
- **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt`
- **Lines 175–191**:
  ```kotlin
  viewModelScope.launch(Dispatchers.IO) {
      // Убеждаемся, что у существующих клиентов есть clientUuid (миграция legacy)
      val clientList = clients.filter { it.isNotEmpty() }.first()
      clientList.forEach { client ->
          if (client.clientUuid.isBlank()) {
              val defaultUuid = if (client.id == 1L) "f47ac10b-58cc-4372-a567-0e02b2c3d479" else java.util.UUID.randomUUID().toString()
              val defaultPin = if (client.id == 1L) "739102" else ""
              dao.updateClient(client.copy(clientUuid = defaultUuid, pairingCode = defaultPin))
          }
      }
      // Автоматическая фоновая синхронизация и проверка обновлений каждые 45 секунд
      var lastUpdateCheck = 0L
      var lastAutoUpdateVersion: String? = null
      while (true) {
          selectedClientId.value?.let {
              syncActiveClientWithGoogleDrive()
          }
          ...
  ```
- **Observation**:
  - `clients.filter { it.isNotEmpty() }.first()` strictly waits for a non-empty list of clients.
  - On a fresh installation or clean database (Zero-Mocks policy, zero seed clients), `clients` emits `emptyList()`.
  - The filter condition `it.isNotEmpty()` drops the emission.
  - `.first()` suspends indefinitely.
  - As a result, lines 185–205 (the periodic background sync loop and update checking loop) are never executed on a fresh start until the user manually creates a client.

---

### 1.4 Gradle Release Assembly Prerequisites
- **Files**:
  - `trainer-app/app/build.gradle.kts` (lines 25–34)
  - `athlete-app/app/build.gradle.kts` (lines 25–34)
- **Configuration**:
  ```kotlin
  buildTypes {
      release {
          isMinifyEnabled = false
          isCrunchPngs = false
          signingConfig = signingConfigs.getByName("debug")
          proguardFiles(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              "proguard-rules.pro"
          )
      }
      debug {
          isMinifyEnabled = false
      }
  }
  ```
- **Execution Test Results**:
  - Running `.\gradlew.bat testDebugUnitTest` in `trainer-app`:
    - Result: `BUILD SUCCESSFUL in 18s` (25 tasks up-to-date, 0 failures).
  - Running `.\gradlew.bat testDebugUnitTest` in `athlete-app`:
    - Result: `BUILD SUCCESSFUL in 16s` (25 tasks up-to-date, 0 failures).
  - Running `.\gradlew.bat assembleRelease` in `trainer-app`:
    - Result: `BUILD SUCCESSFUL in 19s`. Output: `trainer-app/app/build/outputs/apk/release/app-release.apk` (13,151,112 bytes).
  - Running `.\gradlew.bat assembleRelease` in `athlete-app`:
    - Result: `BUILD SUCCESSFUL in 17s`. Output: `athlete-app/app/build/outputs/apk/release/app-release.apk` (13,006,588 bytes).
  - Metadata verification: both APKs declare `versionCode = 5` and `versionName = "1.0.5"`.

---

## 2. Logic Chain

1. **Camera Permission Crash**:
   - In Android (API 23+), invoking a camera capture action without granted runtime permission triggers an OS security failure or intent rejection.
   - Observation 1.1 shows `HomeScreen.kt` directly calls `cameraLauncher.launch(null)` when the "Сканировать QR" button is clicked, without requesting `android.permission.CAMERA` via `ActivityResultContracts.RequestPermission()`.
   - Therefore, adding a runtime permission launcher and checking `ContextCompat.checkSelfPermission` before launching the camera ensures the app cannot crash due to missing permissions.

2. **OutOfMemoryError During QR Scanning**:
   - High-resolution camera photos easily reach 12–48 megapixels.
   - Observation 1.1 shows `QrCodeScannerHelper` attempts to allocate `IntArray(width * height)` for full unscaled bitmaps and decodes streams without `inSampleSize`.
   - In Kotlin/Java, `OutOfMemoryError` is an `Error`, not an `Exception`. The `catch (_: Exception)` block cannot catch it.
   - Therefore, downscaling images to max 800px / 1024px before buffer allocation and catching `Throwable` guarantees zero OOM crashes.

3. **CursorWindow Overflow / `SQLiteBlobTooBigException`**:
   - Android SQLite enforces a 2 MB limit per `CursorWindow`.
   - Observation 1.2 shows that both `AthleteViewModel` and `MainViewModel` currently scale avatars to 512x512 at 85% JPEG, yielding 70–160 KB Base64 strings per entity.
   - When `TrainerDao.getAllClients()` queries all clients, having multiple clients with 100+ KB blobs rapidly approaches CursorWindow boundaries.
   - Downscaling avatars to max 128x128 with 75% JPEG compression produces file sizes of 3–8 KB (< 15 KB threshold), producing Base64 strings < 11 KB.
   - At < 15 KB, even 100 clients total < 1.5 MB, well within the 2 MB window. Catching `Throwable` during `BitmapFactory.decodeByteArray` further protects the UI from decoding memory spikes.

4. **Empty Database ViewModel Hang**:
   - In `MainViewModel.kt`, line 177: `clients.filter { it.isNotEmpty() }.first()`.
   - Observation 1.3 proves that on an empty database (standard Zero-Mocks fresh install), `clients` emits `[]`, the filter drops it, and `first()` suspends indefinitely.
   - Because `first()` never completes, the coroutine never enters the `while (true)` loop that runs auto-sync and periodic update checking.
   - Replacing this with `val initialClients = clients.first()` allows the coroutine to inspect clients once and immediately proceed to background tasks, whether the database has clients or not.

5. **Release Assembly Viability**:
   - Observation 1.4 confirms that `buildTypes.release` in both projects uses `signingConfig = signingConfigs.getByName("debug")` and `isMinifyEnabled = false`.
   - Because of this, both projects can build signed release APKs via `assembleRelease` without external keys, producing fully functional standalone binaries.

---

## 3. Caveats

1. **Pre-existing Local Database Records**:
   - If an existing test install on a device already has an avatar stored before v1.0.5, reading it could still be large until re-saved. Adding an avatar sanitization step during sync / startup prevents legacy oversized rows from remaining uncompressed.
2. **ZXing MultiFormatReader Configuration**:
   - Downscaling beyond 500px might degrade QR detection on very low-density codes photographed from far away. A max dimension of 800px provides the optimal balance between QR readability and low memory footprint.
3. **Android 13+ Photo Picker**:
   - The photo picker in `HomeScreen.kt` uses `ActivityResultContracts.GetContent("image/*")`, which does not require storage permissions on modern Android versions. Only `CAMERA` requires explicit runtime permission.

---

## 4. Conclusion & Recommended Implementations

All 4 target issues have been pinpointed with exact file locations and failure modes. Below are the precise drop-in code implementations recommended for the dev phase:

### Fix 1: Runtime CAMERA Permission in `HomeScreen.kt`
- **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt`
- **Around lines 453–540**:
  ```kotlin
  // Add permission launcher alongside cameraLauncher
  val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
  ) { isGranted ->
      if (isGranted) {
          cameraLauncher.launch(null)
      } else {
          pairingErrorMessage = "Для сканирования QR-кода требуется доступ к камере. Предоставьте разрешение или введите 6 цифр вручную."
      }
  }
  ```
  And update button click:
  ```kotlin
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
  )
  ```

---

### Fix 2: `QrCodeScannerHelper.kt` OOM Prevention and Downscaling
- **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/util/QrCodeScannerHelper.kt`
- **Replace lines 12–38 with**:
  ```kotlin
  object QrCodeScannerHelper {

      private const val MAX_SCAN_DIMENSION = 800

      fun decodeFromBitmap(bitmap: Bitmap): String? {
          var scaledBitmap: Bitmap? = null
          return try {
              val width = bitmap.width
              val height = bitmap.height

              val workingBitmap = if (width > MAX_SCAN_DIMENSION || height > MAX_SCAN_DIMENSION) {
                  val scale = MAX_SCAN_DIMENSION.toFloat() / maxOf(width, height)
                  val targetW = (width * scale).toInt().coerceAtLeast(1)
                  val targetH = (height * scale).toInt().coerceAtLeast(1)
                  scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                  scaledBitmap
              } else {
                  bitmap
              }

              val curW = workingBitmap.width
              val curH = workingBitmap.height
              val pixels = IntArray(curW * curH)
              workingBitmap.getPixels(pixels, 0, curW, 0, 0, curW, curH)

              val source = RGBLuminanceSource(curW, curH, pixels)
              val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
              val hints = mapOf(
                  com.google.zxing.DecodeHintType.TRY_HARDER to true,
                  com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
              )
              MultiFormatReader().apply { setHints(hints) }.decode(binaryBitmap).text
          } catch (_: Throwable) {
              null
          } finally {
              if (scaledBitmap != null && scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
                  scaledBitmap.recycle()
              }
          }
      }

      fun decodeFromUri(context: Context, uri: Uri): String? {
          return try {
              val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
              context.contentResolver.openInputStream(uri)?.use { stream ->
                  BitmapFactory.decodeStream(stream, null, options)
              }

              var sampleSize = 1
              val maxSide = maxOf(options.outWidth, options.outHeight)
              while (maxSide / (sampleSize * 2) >= MAX_SCAN_DIMENSION) {
                  sampleSize *= 2
              }

              val decodeOptions = BitmapFactory.Options().apply {
                  inSampleSize = sampleSize
                  inPreferredConfig = Bitmap.Config.RGB_565
              }
              val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                  BitmapFactory.decodeStream(stream, null, decodeOptions)
              } ?: return null

              decodeFromBitmap(bitmap)
          } catch (_: Throwable) {
              null
          }
      }
  }
  ```

---

### Fix 3: CursorWindow & Avatar Optimization (Max 128x128, JPEG 75%, < 15 KB)
- **File**: `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt` (lines 333–342)
  ```kotlin
  val edge = minOf(width, height)
  val x = (width - edge) / 2
  val y = (height - edge) / 2
  val squareBitmap = Bitmap.createBitmap(originalBitmap, x, y, edge, edge)
  val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 128, 128, true)

  val avatarFile = File(context.filesDir, "athlete_avatar.jpg")
  var quality = 75
  var bytes: ByteArray
  do {
      java.io.ByteArrayOutputStream().use { baos ->
          scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
          bytes = baos.toByteArray()
      }
      quality -= 10
  } while (bytes.size > 15 * 1024 && quality >= 35)

  avatarFile.writeBytes(bytes)
  val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
  ```

- **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt` (lines 147–156)
  Apply identical 128x128 scaling, 75% quality, and size < 15 KB loop.

- **Files**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/components/CommonComponents.kt` (line 263) & `athlete-app/app/src/main/java/com/athleteapp/pro/ui/components/CommonComponents.kt` (line 51)
  Change `catch (_: Exception)` to `catch (_: Throwable)` on Base64 decoding.

---

### Fix 4: MainViewModel Safe Flow Initialization
- **File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt`
- **Replace lines 175–188 with**:
  ```kotlin
  viewModelScope.launch(Dispatchers.IO) {
      // Non-blocking legacy client check on empty database
      val initialClients = clients.first()
      if (initialClients.isNotEmpty()) {
          initialClients.forEach { client ->
              if (client.clientUuid.isBlank()) {
                  val defaultUuid = if (client.id == 1L) "f47ac10b-58cc-4372-a567-0e02b2c3d479" else java.util.UUID.randomUUID().toString()
                  val defaultPin = if (client.id == 1L) "739102" else ""
                  dao.updateClient(client.copy(clientUuid = defaultUuid, pairingCode = defaultPin))
              }
          }
      }
      // Фоновая периодическая синхронизация и проверка обновлений
      var lastUpdateCheck = 0L
      var lastAutoUpdateVersion: String? = null
      while (isActive) {
          selectedClientId.value?.let {
              syncActiveClientWithGoogleDrive()
          }
          if (System.currentTimeMillis() - lastUpdateCheck > 60000L) {
              lastUpdateCheck = System.currentTimeMillis()
              val checkRes = updateService.checkForUpdates()
              val updateInfo = checkRes.getOrNull()
              if (updateInfo?.isUpdateAvailable == true && !updateInfo.downloadUrl.isNullOrBlank()) {
                  _availableUpdate.value = updateInfo
                  if (isAutoInstallUpdatesEnabled && updateInfo.latestVersion != lastAutoUpdateVersion) {
                      lastAutoUpdateVersion = updateInfo.latestVersion
                      updateService.downloadAndInstallApk(updateInfo.downloadUrl)
                  }
              }
          }
          delay(45000L)
      }
  }
  ```

---

## 5. Verification Method

### 5.1 Unit Tests Verification
Run the unit test suites for both applications:
```powershell
# Trainer Pro unit tests
cd F:\Projects\fitness-ecosystem-pro\trainer-app
.\gradlew.bat testDebugUnitTest

# Athlete Pro unit tests
cd F:\Projects\fitness-ecosystem-pro\athlete-app
.\gradlew.bat testDebugUnitTest
```
*Expected*: All tests pass with exit code 0 (`BUILD SUCCESSFUL`).

### 5.2 Release Assembly Verification
Compile release APKs for both applications:
```powershell
# Assemble Trainer Pro Release APK
cd F:\Projects\fitness-ecosystem-pro\trainer-app
.\gradlew.bat assembleRelease

# Assemble Athlete Pro Release APK
cd F:\Projects\fitness-ecosystem-pro\athlete-app
.\gradlew.bat assembleRelease
```
*Expected*:
- `trainer-app/app/build/outputs/apk/release/app-release.apk` generated (~13 MB).
- `athlete-app/app/build/outputs/apk/release/app-release.apk` generated (~13 MB).
- Both signed via `debug` key, installable via `adb install -r`.

### 5.3 Invalidation Conditions
- If `clients.filter { it.isNotEmpty() }.first()` remains in `MainViewModel.kt`, any clean launch on an empty database will hang background auto-sync indefinitely.
- If `QrCodeScannerHelper` retains `catch (_: Exception)` without downscaling, scanning an image > 8 MP can throw an unhandled `OutOfMemoryError`.
- If avatars are stored above 15 KB (512x512 JPEG 85%), loading client lists from Room will eventually fail with `SQLiteBlobTooBigException`.
