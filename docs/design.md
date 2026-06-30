# Android后台相机录像应用设计文档

## 项目概述

**应用名称：** BackCam Recorder（暂定）

**目标平台：** Android 13+ (API 33+)

**核心功能：** 支持后台运行的相机录像应用，用于个人活动记录（骑行、跑步等），录制时长1-3小时，支持隐私相册功能。

---

## 需求总结

| 需求项 | 描述 |
|-------|------|
| **使用场景** | 个人活动记录（骑行、跑步等） |
| **录制时长** | 1-3小时 |
| **视频质量** | 默认4K 60fps，用户可配置 |
| **预览模式** | 可选预览，支持完全后台录制 |
| **隐私相册** | 默认开启，视频存入隐私相册；可关闭直接存入系统相册 |
| **目标版本** | Android 13+ |

---

## 技术方案

### 技术选型

| 技术 | 选择 | 理由 |
|-----|------|------|
| **相机API** | CameraX | Google官方推荐，生命周期感知，设备兼容性好 |
| **编码方式** | MediaRecorder | 简单高效，CameraX原生支持 |
| **后台服务** | Foreground Service | Android 13+后台录制必须 |
| **UI框架** | Jetpack Compose | 现代、声明式、易于状态管理 |
| **数据库** | Room | Jetpack组件，类型安全 |
| **依赖注入** | Hilt | Google推荐，与Jetpack集成良好 |
| **架构模式** | MVVM + Repository | 清晰分层，易于测试 |

### 核心依赖

```groovy
// build.gradle.kts (app)
dependencies {
    // CameraX
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // MediaStore for system gallery
    implementation("androidx.media:media:1.7.0")

    // ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
}
```

---

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer                             │
├─────────────────────────────────────────────────────────┤
│  MainActivity (@Composable)                             │
│  ├── PreviewScreen (相机预览界面)                       │
│  │   ├── CameraPreview                                  │
│  │   ├── RecordingControls                              │
│  │   └── RecordingIndicator                             │
│  ├── GalleryScreen (隐私相册/系统相册)                  │
│  │   ├── VideoGrid                                      │
│  │   └── VideoPlayerScreen                              │
│  ├── SettingsScreen (设置界面)                          │
│  │   ├── ResolutionSelector                             │
│  │   ├── FrameRateSelector                              │
│  │   └── PrivacyAlbumToggle                             │
│  └── MainNavigation                                     │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  ViewModel Layer                        │
├─────────────────────────────────────────────────────────┤
│  RecordViewModel                                        │
│  ├── uiState: StateFlow<RecordUiState>                 │
│  ├── startRecording()                                   │
│  ├── stopRecording()                                    │
│  └── togglePreview()                                    │
│                                                         │
│  GalleryViewModel                                       │
│  ├── videos: StateFlow<List<Recording>>                │
│  ├── deleteVideo(id: String)                            │
│  └── exportToGallery(id: String)                       │
│                                                         │
│  SettingsViewModel                                      │
│  ├── settings: StateFlow<AppSettings>                  │
│  └── updateSettings(settings: AppSettings)              │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  Service Layer                          │
├─────────────────────────────────────────────────────────┤
│  RecordForegroundService                                │
│  ├── 前台通知管理 (Notification)                        │
│  ├── CameraX 生命周期管理                                │
│  ├── MediaRecorder 录制控制                             │
│  ├── 录制状态: StateFlow<RecordState>                   │
│  └── Binder 供 Activity 绑定                            │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  Data Layer                             │
├─────────────────────────────────────────────────────────┤
│  RecordingRepository                                    │
│  ├── 视频文件存储                        │
│  │   ├── files/videos/ (隐私相册-应用私有)              │
│  │   └── MediaStore (系统相册)                          │
│  ├── RecordingDao (Room数据库)                          │
│  ├── PreferencesManager (DataStore设置)                │
│  └── StorageManager (存储空间管理)                      │
└─────────────────────────────────────────────────────────┘
```

---

## 模块详细设计

### 1. 后台录制服务 (RecordForegroundService)

#### 服务声明

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application>
    <service
        android:name=".service.RecordForegroundService"
        android:foregroundServiceType="camera|microphone"
        android:exported="false" />
</application>
```

#### 核心实现逻辑

```kotlin
// RecordForegroundService.kt
class RecordForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "record_channel"

        // Actions
        const val ACTION_START_RECORDING = "action_start_recording"
        const val ACTION_STOP_RECORDING = "action_stop_recording"
        const val ACTION_PAUSE_RECORDING = "action_pause_recording"
    }

    // 录制状态
    sealed class RecordState {
        object Idle : RecordState()
        object Preparing : RecordState()
        data class Recording(val duration: Long) : RecordState()
        object Paused : RecordState()
        object Stopping : RecordState()
        data class Error(val message: String) : RecordState()
    }

    private val _recordState = MutableStateFlow<RecordState>(RecordState.Idle)
    val recordState: StateFlow<RecordState> = _recordState.asStateFlow()

    // CameraX组件
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var activeRecording: ActiveRecording? = null

    // Binder用于Activity绑定
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RecordForegroundService = this@RecordForegroundService
        fun getRecordState(): StateFlow<RecordState> = recordState
        fun bindPreview(previewView: PreviewView) {
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        }
        fun unbindPreview() {
            preview?.setSurfaceProvider(null)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initCamera()
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(this))
    }

    fun startRecording(settings: RecordingSettings) {
        _recordState.value = RecordState.Preparing

        // 启动前台服务通知
        startForeground(NOTIFICATION_ID, createRecordingNotification())

        // 配置录制参数
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    when (settings.resolution) {
                        Resolution.UHD_4K -> Quality.UHD
                        Resolution.FHD_1080P -> Quality.FHD
                        Resolution.HD_720P -> Quality.HD
                    },
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
                )
            )
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        // 设置输出路径
        val outputPath = if (settings.isPrivateAlbum) {
            getPrivateVideoPath()
        } else {
            getPublicVideoPath()
        }

        val outputOptions = FileOutputOptions.Builder(File(outputPath)).build()

        // 开始录制
        activeRecording = videoCapture!!.output
            .prepareRecording(this, outputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        _recordState.value = RecordState.Recording(0L)
                    }
                    is VideoRecordEvent.Status -> {
                        _recordState.value = RecordState.Recording(
                            recordEvent.recordingStats.recordedDurationNanos / 1_000_000
                        )
                        updateNotification(recordEvent.recordingStats)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (recordEvent.hasError()) {
                            _recordState.value = RecordState.Error(recordEvent.errorDescription)
                        } else {
                            saveRecordingToDatabase(outputPath, recordEvent)
                        }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        _recordState.value = RecordState.Idle
                    }
                }
            }
    }
}
```

#### 通知栏控制

```kotlin
// RecordForegroundService.kt (续)
private fun createRecordingNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("正在录制")
        .setContentText("点击返回应用")
        .setSmallIcon(R.drawable.ic_record)
        .setOngoing(true)
        .addAction(R.drawable.ic_stop, "停止", createStopPendingIntent())
        .addAction(R.drawable.ic_pause, "暂停", createPausePendingIntent())
        .setContentIntent(createOpenAppPendingIntent())
        .build()
}

private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "录制服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台录制通知"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
```

### 2. 相机预览管理

```kotlin
// CameraPreviewManager.kt
class CameraPreviewManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun initialize(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                Log.e(TAG, "相机初始化失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // 后台时解绑预览以释放资源
    fun unbindPreview() {
        preview?.setSurfaceProvider(null)
    }

    // 返回前台时重新绑定
    fun rebindPreview(previewView: PreviewView) {
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }
}
```

### 3. 存储管理

#### 隐私相册路径

```kotlin
// StorageManager.kt
class StorageManager(private val context: Context) {

    // 隐私相册路径（应用私有目录）
    fun getPrivateVideoDir(): File {
        return File(context.filesDir, "videos").apply {
            if (!exists()) mkdirs()
        }
    }

    // 生成隐私相册视频文件名
    fun generatePrivateVideoPath(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return File(getPrivateVideoDir(), "VID_$timestamp.mp4").absolutePath
    }

    // 系统相册路径（MediaStore）
    fun getPublicVideoUri(fileName: String, contentResolver: ContentResolver): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/BackCam")
        }

        return contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IOException("无法创建MediaStore条目")
    }
}
```

#### Room数据库实体

```kotlin
// RecordingEntity.kt
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val filePath: String,
    val fileName: String,
    val duration: Long,
    val fileSize: Long,
    val resolution: String,
    val frameRate: Int,
    val createdAt: Long,
    val isPrivate: Boolean,
    val thumbnailPath: String?
)

// RecordingDao.kt
@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE isPrivate = :isPrivate ORDER BY createdAt DESC")
    fun getRecordingsByPrivacy(isPrivate: Boolean): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)
}
```

### 4. 设置管理

```kotlin
// AppSettings.kt
data class AppSettings(
    val resolution: Resolution = Resolution.UHD_4K,
    val frameRate: FrameRate = FrameRate.FPS_60,
    val isPrivacyAlbumEnabled: Boolean = true
)

enum class Resolution(val displayName: String, val width: Int, val height: Int) {
    UHD_4K("4K (2160p)", 3840, 2160),
    FHD_1080P("1080p", 1920, 1080),
    HD_720P("720p", 1280, 720)
}

enum class FrameRate(val displayName: String, val fps: Int) {
    FPS_60("60 fps", 60),
    FPS_30("30 fps", 30)
}

// SettingsDataStore.kt
class SettingsDataStore(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            resolution = Resolution.valueOf(
                prefs[RESOLUTION_KEY] ?: Resolution.UHD_4K.name
            ),
            frameRate = FrameRate.valueOf(
                prefs[FRAME_RATE_KEY] ?: FrameRate.FPS_60.name
            ),
            isPrivacyAlbumEnabled = prefs[PRIVACY_ALBUM_KEY] ?: true
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[RESOLUTION_KEY] = settings.resolution.name
            prefs[FRAME_RATE_KEY] = settings.frameRate.name
            prefs[PRIVACY_ALBUM_KEY] = settings.isPrivacyAlbumEnabled
        }
    }

    companion object {
        private val RESOLUTION_KEY = stringPreferencesKey("resolution")
        private val FRAME_RATE_KEY = stringPreferencesKey("frame_rate")
        private val PRIVACY_ALBUM_KEY = booleanPreferencesKey("privacy_album_enabled")
    }
}
```

### 5. UI界面设计

#### 主要界面导航

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BackCamTheme {
                BackCamApp()
            }
        }
    }
}

// BackCamApp.kt
@Composable
fun BackCamApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "preview") {
        composable("preview") {
            PreviewScreen(
                onNavigateToGallery = { navController.navigate("gallery") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("gallery") {
            GalleryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("video_player/{videoId}") { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId")
            VideoPlayerScreen(
                videoId = videoId ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

#### 预览界面

```kotlin
// PreviewScreen.kt
@Composable
fun PreviewScreen(
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    viewModel.bindPreview(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, "设置")
            }
            IconButton(onClick = onNavigateToGallery) {
                Icon(Icons.Default.PhotoLibrary, "相册")
            }
        }

        // 底部控制栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 录制时长显示
            if (uiState.isRecording) {
                Text(
                    text = formatDuration(uiState.recordingDuration),
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 录制按钮
            RecordButton(
                isRecording = uiState.isRecording,
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording(context)
                    }
                }
            )
        }

        // 录制指示器
        if (uiState.isRecording) {
            RecordingIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
    }
}

@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        backgroundColor = if (isRecording) Color.Red else Color.White,
        modifier = Modifier.size(72.dp)
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
            contentDescription = if (isRecording) "停止" else "录制",
            tint = if (isRecording) Color.White else Color.Red,
            modifier = Modifier.size(48.dp)
        )
    }
}
```

#### 相册界面

```kotlin
// GalleryScreen.kt
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = { Text(if (settings.isPrivacyAlbumEnabled) "隐私相册" else "视频列表") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            actions = {
                if (!settings.isPrivacyAlbumEnabled) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("打开系统相册")
                    }
                }
            }
        )

        // 视频网格
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无视频", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(videos) { video ->
                    VideoThumbnail(
                        video = video,
                        onClick = { /* 导航到播放页面 */ },
                        onDelete = { viewModel.deleteVideo(video.id) },
                        onExport = if (video.isPrivate) {
                            { viewModel.exportToGallery(video.id) }
                        } else null
                    )
                }
            }
        }
    }
}
```

#### 设置界面

```kotlin
// SettingsScreen.kt
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            }
        )

        // 分辨率设置
        SettingsItem(
            title = "视频分辨率",
            subtitle = settings.resolution.displayName,
            onClick = { /* 显示选择对话框 */ }
        )

        Divider()

        // 帧率设置
        SettingsItem(
            title = "视频帧率",
            subtitle = settings.frameRate.displayName,
            onClick = { /* 显示选择对话框 */ }
        )

        Divider()

        // 隐私相册开关
        SettingsSwitch(
            title = "隐私相册",
            subtitle = if (settings.isPrivacyAlbumEnabled) {
                "视频保存到隐私相册，系统相册不可见"
            } else {
                "视频直接保存到系统相册"
            },
            checked = settings.isPrivacyAlbumEnabled,
            onCheckedChange = { viewModel.updatePrivacyAlbum(it) }
        )

        Divider()

        // 存储空间信息
        StorageInfo()
    }
}
```

---

## 权限管理

### 权限列表

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.any" android:required="true" />
```

### 权限请求流程

```kotlin
// PermissionManager.kt
class PermissionManager(private val activity: ComponentActivity) {

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            // 处理权限被拒绝的情况
            val deniedPermissions = permissions.filter { !it.value }.keys
            showPermissionRationale(deniedPermissions)
        }
    }

    fun checkAndRequestPermissions() {
        val ungrantedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isEmpty()) {
            // 所有权限已授予
            onPermissionsGranted()
        } else {
            // 请求权限
            permissionLauncher.launch(ungrantedPermissions.toTypedArray())
        }
    }

    private fun showPermissionRationale(deniedPermissions: Set<String>) {
        // 显示权限说明对话框
        if (deniedPermissions.contains(Manifest.permission.CAMERA)) {
            // 相机权限被拒绝
        }
        if (deniedPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
            // 麦克风权限被拒绝
        }
    }
}
```

---

## 错误处理与边界情况

### 错误处理策略

| 错误场景 | 检测方式 | 处理策略 |
|---------|---------|---------|
| **存储空间不足** | 录制前检查可用空间，录制中监控 | 提前警告，录制中不足时自动停止并保存 |
| **相机被占用** | CameraX异常回调 | 显示错误提示，建议关闭其他相机应用 |
| **电量过低** | 监听BatteryManager | 电量<5%时警告，建议充电或降低分辨率 |
| **设备过热** | 监听系统热状态 | 提示用户暂停录制 |
| **录制进程被杀** | Service生命周期 | 下次启动时检查并清理临时文件 |
| **权限被撤销** | onResume检查权限 | 引导用户重新授权 |

### 存储空间管理

```kotlin
// StorageManager.kt
class StorageManager(private val context: Context) {

    // 检查可用空间
    fun getAvailableStorage(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    // 估算录制所需空间 (4K 60fps 约需 400MB/分钟)
    fun estimateRequiredSpace(durationMinutes: Int, resolution: Resolution): Long {
        val bitrate = when (resolution) {
            Resolution.UHD_4K -> 50_000_000L // 50 Mbps
            Resolution.FHD_1080P -> 20_000_000L // 20 Mbps
            Resolution.HD_720P -> 10_000_000L // 10 Mbps
        }
        return (bitrate * durationMinutes * 60) / 8 // 转换为字节
    }

    // 检查是否有足够空间
    fun hasEnoughSpace(durationMinutes: Int, resolution: Resolution): Boolean {
        val required = estimateRequiredSpace(durationMinutes, resolution)
        val available = getAvailableStorage()
        // 预留500MB空间
        return available > (required + 500_000_000L)
    }
}
```

### 相机占用处理

```kotlin
// RecordForegroundService.kt
private val cameraStateCallback = object : CameraState.StateErrorCallback {
    override fun onError(error: CameraState.StateError) {
        when (error.code) {
            CameraState.ERROR_CAMERA_IN_USE -> {
                _recordState.value = RecordState.Error("相机被其他应用占用")
                stopRecording()
            }
            CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                _recordState.value = RecordState.Error("已达到最大相机数量限制")
                stopRecording()
            }
            CameraState.ERROR_CAMERA_DISABLED -> {
                _recordState.value = RecordState.Error("相机已被禁用")
                stopRecording()
            }
            else -> {
                _recordState.value = RecordState.Error("相机错误: ${error.code}")
                stopRecording()
            }
        }
    }
}
```

---

## 数据流图

### 录制流程

```
用户点击录制按钮
        │
        ▼
检查权限 ────────► 未授权 ──► 请求权限
        │
        ▼ 已授权
检查存储空间 ────► 不足 ──► 警告用户
        │
        ▼ 充足
启动前台服务
        │
        ▼
显示录制通知
        │
        ▼
初始化CameraX
        │
        ▼
配置MediaRecorder
        │
        ▼
开始录制
        │
        ▼
状态更新: RECORDING
        │
        ▼
定时更新通知栏时长
        │
        ▼
用户停止/空间不足
        │
        ▼
停止录制
        │
        ▼
保存到数据库
        │
        ▼
停止前台服务
        │
        ▼
状态更新: IDLE
```

### 后台切换流程

```
用户按Home/锁屏
        │
        ▼
Activity.onPause()
        │
        ▼
解绑PreviewView
        │
        ▼
服务继续运行
        │
        ▼
通知栏显示录制状态
        │
        ▼
用户返回APP
        │
        ▼
Activity.onResume()
        │
        ▼
重新绑定PreviewView
        │
        ▼
恢复预览画面
```

---

## 性能优化

### 电池优化

1. **预览暂停**：后台时解绑PreviewView，减少GPU负载
2. **帧率自适应**：低电量模式降低帧率
3. **唤醒锁管理**：仅在录制时持有PARTIAL_WAKE_LOCK

```kotlin
// 电源管理
class PowerManager(private val context: Context) {

    private val wakeLock: PowerManager.WakeLock by lazy {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BackCam::RecordWakeLock"
            )
    }

    fun acquireWakeLock() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(3 * 60 * 60 * 1000L) // 3小时超时
        }
    }

    fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
```

### 存储优化

1. **分片录制**：每15分钟自动分割文件，避免单文件过大
2. **缩略图缓存**：使用Glide加载，限制内存缓存大小
3. **LRU策略**：存储空间不足时自动清理最旧的视频（需用户确认）

---

## 测试策略

### 单元测试

- ViewModel状态转换测试
- Repository数据操作测试
- 存储空间计算测试
- 设置持久化测试

### 集成测试

- 权限请求流程测试
- 录制启动/停止流程测试
- 后台切换流程测试
- 存储路径切换测试

### UI测试

- Compose UI渲染测试
- 导航流程测试
- 设置更改测试

### 手动测试清单

| 测试项 | 测试步骤 | 预期结果 |
|-------|---------|---------|
| 基本录制 | 开始录制→等待10秒→停止 | 视频保存成功，可播放 |
| 后台录制 | 开始录制→按Home→等待30秒→返回 | 录制持续，时长正确 |
| 锁屏录制 | 开始录制→锁屏→等待30秒→解锁 | 录制持续，时长正确 |
| 通知控制 | 录制中→通知栏点击停止 | 录制停止 |
| 权限拒绝 | 拒绝相机/麦克风权限 | 显示说明，引导设置 |
| 存储不足 | 存储空间<100MB时录制 | 警告提示，无法开始 |
| 隐私相册切换 | 录制中切换隐私设置 | 提示下次生效 |
| 4K录制 | 4K 60fps录制10分钟 | 文件大小合理，播放流畅 |

---

## 项目结构

```
app/
├── src/main/java/com/backcam/recorder/
│   ├── BackCamApplication.kt
│   ├── MainActivity.kt
│   │
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   └── Type.kt
│   │   ├── screens/
│   │   │   ├── PreviewScreen.kt
│   │   │   ├── GalleryScreen.kt
│   │   │   ├── SettingsScreen.kt
│   │   │   └── VideoPlayerScreen.kt
│   │   ├── components/
│   │   │   ├── RecordButton.kt
│   │   │   ├── RecordingIndicator.kt
│   │   │   ├── VideoThumbnail.kt
│   │   │   └── SettingsItem.kt
│   │   └── navigation/
│   │       └── BackCamApp.kt
│   │
│   ├── service/
│   │   └── RecordForegroundService.kt
│   │
│   ├── viewmodel/
│   │   ├── RecordViewModel.kt
│   │   ├── GalleryViewModel.kt
│   │   └── SettingsViewModel.kt
│   │
│   ├── repository/
│   │   ├── RecordingRepository.kt
│   │   └── SettingsRepository.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── RecordingDao.kt
│   │   │   └── RecordingEntity.kt
│   │   └── datastore/
│   │       └── SettingsDataStore.kt
│   │
│   ├── camera/
│   │   ├── CameraManager.kt
│   │   └── CameraPreviewManager.kt
│   │
│   ├── storage/
│   │   ├── StorageManager.kt
│   │   └── MediaStoreHelper.kt
│   │
│   ├── permission/
│   │   └── PermissionManager.kt
│   │
│   ├── model/
│   │   ├── AppSettings.kt
│   │   ├── Recording.kt
│   │   └── RecordState.kt
│   │
│   └── util/
│       ├── FileUtils.kt
│       ├── TimeUtils.kt
│       └── NotificationHelper.kt
│
├── src/main/res/
│   ├── drawable/
│   │   ├── ic_record.xml
│   │   ├── ic_stop.xml
│   │   ├── ic_pause.xml
│   │   └── ic_settings.xml
│   ├── values/
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── xml/
│       └── file_paths.xml
│
└── build.gradle.kts
```

---

## 开发里程碑

### Phase 1: 基础架构 (第1周)

- 项目初始化与依赖配置
- 基础UI框架搭建 (Compose + Navigation)
- 权限管理实现
- 数据库与设置持久化

### Phase 2: 核心功能 (第2-3周)

- 后台录制服务实现
- CameraX集成与预览
- 录制控制（开始/停止/暂停）
- 通知栏控制

### Phase 3: 存储与相册 (第4周)

- 隐私相册存储实现
- 系统相册导出功能
- 隐私开关实现
- 相册界面开发

### Phase 4: 优化与测试 (第5周)

- 性能优化（电池、存储）
- 错误处理完善
- 单元测试与集成测试
- UI测试与手动测试

### Phase 5: 发布准备 (第6周)

- 代码审查与重构
- 文档完善
- 签名与混淆配置
- 上架准备

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| **设备碎片化** | CameraX行为差异 | 使用QualitySelector降级策略 |
| **后台限制变化** | Android版本更新 | 遵循最新前台服务规范 |
| **存储权限限制** | 分区存储限制 | 使用MediaStore API |
| **电池消耗快** | 用户投诉 | 提供低功耗模式选项 |
| **4K录制不稳定** | 部分设备不支持 | 实现降级到1080p逻辑 |

---

## 附录

### 关键配置文件

#### build.gradle.kts (Module: app)

```kotlin
android {
    namespace = "com.backcam.recorder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.backcam.recorder"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
}
```

#### ProGuard规则

```proguard
# CameraX
-keep class androidx.camera.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# DataStore
-keep class * extends androidx.datastore.** { *; }
```

---

## 总结

本文档详细设计了Android后台相机录像应用的完整架构，涵盖：

1. **技术选型**：CameraX + Foreground Service + MediaRecorder
2. **核心功能**：后台录制、可选预览、隐私相册
3. **数据管理**：Room数据库 + DataStore设置
4. **UI设计**：Jetpack Compose声明式UI
5. **权限处理**：运行时权限 + 前台服务权限
6. **错误处理**：存储、相机、电量等边界情况
7. **性能优化**：电池、存储优化策略
8. **测试策略**：单元测试、集成测试、UI测试

该设计满足用户需求，支持Android 13+，可稳定支持1-3小时的后台录制。