package com.vido.local

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.LruCache
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vido.local.data.PrivateDirectoryEntity
import com.vido.local.data.PlayerMode
import com.vido.local.data.VideoItem
import com.vido.local.player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VidoApp(viewModel) }
    }
}

private val AppBlack = androidx.compose.ui.graphics.Color(0xFF181818)
private val AppGray = androidx.compose.ui.graphics.Color(0xFF777777)
private val AppSkyBlue = androidx.compose.ui.graphics.Color(0xFF38BDF8)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private object ThumbnailMemoryCache : LruCache<String, Bitmap>(24 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
}

@Composable
private fun VidoApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val systemVideos by viewModel.systemVideos.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val playbackQueue by viewModel.playbackQueue.collectAsStateWithLifecycle()
    val privateVideos by viewModel.privateVideos.collectAsStateWithLifecycle()
    val autoPlayNext by viewModel.autoPlayNext.collectAsStateWithLifecycle()
    val playerMode by viewModel.playerMode.collectAsStateWithLifecycle()
    val showPrivateThumbnails by viewModel.showPrivateThumbnails.collectAsStateWithLifecycle()
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) viewModel.loadSystemVideos()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) viewModel.loadSystemVideos()
    }

    fun playVideo(video: VideoItem, queue: List<VideoItem>) {
        if (playerMode == PlayerMode.SYSTEM) {
            openInSystemPlayer(context, video)
        } else {
            viewModel.selectVideo(video, queue)
            navController.navigate("player")
        }
    }

    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = AppBlack,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            surface = androidx.compose.ui.graphics.Color.White,
            onSurface = AppBlack,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1F1F1),
        ),
    ) {
        val entry by navController.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        Scaffold(
            bottomBar = {
                if (route != "private" && route != "player") BottomBar(navController, route)
            },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "library",
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(90)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(90)) },
            ) {
                composable("library") {
                    LibraryScreen(
                        videos = systemVideos,
                        permissionGranted = permissionGranted,
                        requestPermission = { permissionLauncher.launch(permission) },
                        onPlay = { playVideo(it, systemVideos) },
                    )
                }
                composable("folders") { FoldersScreen(systemVideos) { playVideo(it, systemVideos) } }
                composable("search") { SearchScreen(systemVideos) { playVideo(it, systemVideos) } }
                composable("settings") {
                    SettingsScreen(
                        autoPlayNext = autoPlayNext,
                        onAutoPlayNextChange = viewModel::setAutoPlayNext,
                        playerMode = playerMode,
                        onPlayerModeChange = viewModel::setPlayerMode,
                        onOpenPrivate = { navController.navigate("private") },
                    )
                }
                composable("private") {
                    PrivateScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onPlay = { playVideo(it, privateVideos) },
                    )
                }
                composable("player") {
                    selectedVideo?.let {
                        PlayerScreen(
                            video = it,
                            queue = playbackQueue,
                            autoPlayNext = autoPlayNext,
                            showPrivateThumbnails = showPrivateThumbnails,
                            viewModel = viewModel,
                            onSelect = viewModel::selectQueueItem,
                            onBack = { navController.popBackStack() },
                        )
                    }
                        ?: LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, route: String?) {
    val destinations = listOf(
        Triple("library", "视频库", Icons.Default.VideoLibrary),
        Triple("folders", "文件夹", Icons.Default.Folder),
        Triple("search", "搜索", Icons.Default.Search),
        Triple("settings", "设置", Icons.Default.Settings),
    )
    NavigationBar {
        destinations.forEach { (target, label, icon) ->
            NavigationBarItem(
                selected = route == target,
                onClick = { navController.navigate(target) { launchSingleTop = true; popUpTo("library") { saveState = true }; restoreState = true } },
                icon = { Icon(icon, label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(48.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        subtitle?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, color = AppGray, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun LibraryScreen(
    videos: List<VideoItem>,
    permissionGranted: Boolean,
    requestPermission: () -> Unit,
    onPlay: (VideoItem) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("视频库", if (permissionGranted && videos.isNotEmpty()) "${videos.size} 个视频" else null)
        if (!permissionGranted) {
            PermissionState(requestPermission, Modifier.weight(1f))
        } else if (videos.isEmpty()) {
            EmptyState("未发现视频", "手机中的标准视频会显示在这里", Modifier.weight(1f))
        } else {
            VideoGrid(videos, onPlay, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PermissionState(requestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(14.dp))
            Text("需要视频读取权限", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("仅用于读取和播放本机视频", color = AppGray)
            Spacer(Modifier.height(18.dp))
            Button(onClick = requestPermission) { Text("允许访问视频") }
        }
    }
}

@Composable
private fun FoldersScreen(videos: List<VideoItem>, onPlay: (VideoItem) -> Unit) {
    var openedFolder by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        PageHeader(openedFolder ?: "文件夹", action = if (openedFolder != null) ({ IconButton({ openedFolder = null }) { Icon(Icons.Default.ArrowBack, "返回") } }) else null)
        if (openedFolder != null) {
            VideoGrid(videos.filter { it.folder == openedFolder }, onPlay, Modifier.weight(1f))
        } else if (videos.isEmpty()) {
            EmptyState("暂无文件夹", "授权后会按视频所在位置自动分组", Modifier.weight(1f))
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                items(videos.groupBy { it.folder }.toList()) { (folder, files) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { openedFolder = folder }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(folder, fontWeight = FontWeight.SemiBold)
                            Text("${files.size} 个视频", color = AppGray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(videos: List<VideoItem>, onPlay: (VideoItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    val result = remember(videos, query) { videos.filter { it.title.contains(query, ignoreCase = true) } }
    Column(Modifier.fillMaxSize()) {
        PageHeader("搜索")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("搜索视频标题") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (query.isNotBlank() && result.isEmpty()) EmptyState("没有匹配的视频", "换个关键词试试", Modifier.weight(1f))
        else VideoGrid(result, onPlay, Modifier.weight(1f))
    }
}

@Composable
private fun SettingsScreen(
    autoPlayNext: Boolean,
    onAutoPlayNextChange: (Boolean) -> Unit,
    playerMode: PlayerMode,
    onPlayerModeChange: (PlayerMode) -> Unit,
    onOpenPrivate: () -> Unit,
) {
    var taps by remember { mutableIntStateOf(0) }
    var showPrivateConfirmation by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showPlayerModeDialog by remember { mutableStateOf(false) }
    val versionInteractions = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxSize()) {
        PageHeader("设置") {
            IconButton(onClick = { showAbout = true }) { Icon(Icons.Default.Info, "关于") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("自动连播", fontWeight = FontWeight.SemiBold)
            }
            Switch(checked = autoPlayNext, onCheckedChange = onAutoPlayNextChange)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { showPlayerModeDialog = true }.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("播放器", fontWeight = FontWeight.SemiBold)
                Text(if (playerMode == PlayerMode.IN_APP) "应用内播放器" else "系统播放器", color = AppGray, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Vido ${BuildConfig.VERSION_NAME}",
            color = AppGray,
            modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = versionInteractions,
                indication = null,
            ) {
                taps++
                if (taps >= 7) { taps = 0; showPrivateConfirmation = true }
            }.padding(28.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (showPrivateConfirmation) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("确定？") },
            text = { Text("确定进入？") },
            confirmButton = {
                Button(onClick = { showPrivateConfirmation = false; onOpenPrivate() }) { Text("确定") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPrivateConfirmation = false }) { Text("取消") }
            },
        )
    }
    if (showPlayerModeDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerModeDialog = false },
            title = { Text("播放器") },
            text = {
                Column {
                    PlayerMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onPlayerModeChange(mode)
                                showPlayerModeDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = playerMode == mode, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (mode == PlayerMode.IN_APP) "应用内播放器" else "系统播放器")
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于") },
            text = { Text("Vido ${BuildConfig.VERSION_NAME}\n本地视频播放器\n所有数据仅保存在本机，应用不请求网络权限。") },
            confirmButton = { Button(onClick = { showAbout = false }) { Text("确定") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivateScreen(viewModel: MainViewModel, onBack: () -> Unit, onPlay: (VideoItem) -> Unit) {
    val context = LocalContext.current
    val privateVideos by viewModel.privateVideos.collectAsStateWithLifecycle()
    val directories by viewModel.privateDirectories.collectAsStateWithLifecycle()
    val extensions by viewModel.privateExtensions.collectAsStateWithLifecycle()
    val showPrivateThumbnails by viewModel.showPrivateThumbnails.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddExtension by remember { mutableStateOf(false) }
    var showPrivateSettings by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        val label = DocumentFile.fromTreeUri(context, uri)?.name ?: "已授权目录"
        viewModel.addPrivateDirectory(uri.toString(), label)
    }
    LaunchedEffect(Unit) {
        viewModel.scanPrivateDirectories()
    }
    LaunchedEffect(scanResult) {
        scanResult?.let { result ->
            val inaccessible = if (result.inaccessibleDirectories > 0) "，${result.inaccessibleDirectories} 个目录无权访问" else ""
            snackbar.showSnackbar("识别 ${result.discovered} 个视频，忽略 ${result.rejected} 个非视频文件$inaccessible")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            PageHeader("私密目录") {
                IconButton(onClick = { showPrivateSettings = true }) { Icon(Icons.Default.Settings, "设置") }
                IconButton(onClick = { viewModel.scanPrivateDirectories() }, enabled = !isScanning) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { picker.launch(null) }) { Text("添加目录") }
                    OutlinedButton(onClick = { viewModel.scanPrivateDirectories() }, enabled = !isScanning) {
                        Text(if (isScanning) "正在扫描" else "手动刷新")
                    }
                }
            }
            item { Text("已授权目录", fontWeight = FontWeight.SemiBold) }
            if (directories.isEmpty()) item { Text("尚未添加目录", color = AppGray) }
            items(directories, key = { it.treeUri }) { directory -> DirectoryRow(directory, viewModel) }
            item { Text("识别结果", fontWeight = FontWeight.SemiBold) }
            if (privateVideos.isEmpty()) item { Text("暂无识别结果", color = AppGray) }
            items(privateVideos, key = { it.uri }) { video -> PrivateVideoRow(video, showPrivateThumbnails, onPlay) }
        }
    }
    if (showPrivateSettings) {
        PrivateSettingsDialog(
            extensions = extensions,
            showThumbnails = showPrivateThumbnails,
            onShowThumbnailsChange = viewModel::setShowPrivateThumbnails,
            onRemoveExtension = viewModel::removeExtension,
            onAddExtension = { showAddExtension = true },
            onDismiss = { showPrivateSettings = false },
        )
    }
    if (showAddExtension) AddExtensionDialog(
        onDismiss = { showAddExtension = false },
    onConfirm = { value -> viewModel.addExtension(value) { }; showAddExtension = false },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivateSettingsDialog(
    extensions: Set<String>,
    showThumbnails: Boolean,
    onShowThumbnailsChange: (Boolean) -> Unit,
    onRemoveExtension: (String) -> Unit,
    onAddExtension: () -> Unit,
    onDismiss: () -> Unit,
) {
    var editingExtensions by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("显示视频封面", modifier = Modifier.weight(1f))
                    Switch(checked = showThumbnails, onCheckedChange = onShowThumbnailsChange)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("识别后缀", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { editingExtensions = !editingExtensions }) {
                        Text(if (editingExtensions) "完成" else "编辑")
                    }
                }
                if (editingExtensions) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        extensions.sorted().forEach { extension ->
                            InputChip(
                                selected = false,
                                onClick = { onRemoveExtension(extension) },
                                label = { Text(".$extension") },
                                trailingIcon = { Icon(Icons.Default.Close, "删除 .$extension", modifier = Modifier.size(16.dp)) },
                            )
                        }
                        OutlinedButton(onClick = onAddExtension) { Text("添加后缀") }
                    }
                } else if (extensions.isEmpty()) {
                    Text("未配置", color = AppGray, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(extensions.sorted().joinToString(separator = "  ") { ".${it}" }, color = AppGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun DirectoryRow(directory: PrivateDirectoryEntity, viewModel: MainViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Folder, null)
        Spacer(Modifier.width(10.dp))
        Text(directory.displayName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("移除", color = AppGray, modifier = Modifier.clickable { viewModel.removePrivateDirectory(directory.treeUri) }.padding(8.dp))
    }
}

@Composable
private fun PrivateVideoRow(video: VideoItem, showThumbnail: Boolean, onPlay: (VideoItem) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onPlay(video) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showThumbnail) {
            Box(Modifier.size(92.dp, 52.dp).clip(RoundedCornerShape(6.dp))) {
                VideoThumbnail(Uri.parse(video.uri), Modifier.fillMaxSize(), extractFrame = true)
            }
        } else {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text("${video.folder}  ${formatDuration(video.durationMs)}", color = AppGray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AddExtensionDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加识别后缀") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it; invalid = false },
                label = { Text("例如 mp4") },
                singleLine = true,
                isError = invalid,
                supportingText = { if (invalid) Text("后缀只能包含字母或数字") },
            )
        },
        confirmButton = {
            Button(onClick = {
                val normalized = value.trim().removePrefix(".")
                if (normalized.isBlank() || normalized.any { !it.isLetterOrDigit() }) invalid = true
                else onConfirm(normalized)
            }) { Text("添加") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGrid(videos: List<VideoItem>, onPlay: (VideoItem) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(145.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(videos, key = { it.uri }) { video -> VideoCard(video, onPlay) }
    }
}

@Composable
private fun VideoCard(video: VideoItem, onPlay: (VideoItem) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF3F3F3)),
        modifier = Modifier.fillMaxWidth().clickable { onPlay(video) },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            VideoThumbnail(Uri.parse(video.uri), Modifier.fillMaxSize())
            Row(
                Modifier.align(Alignment.BottomEnd).padding(5.dp).background(AppBlack.copy(alpha = .74f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                Text(formatDuration(video.durationMs), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(9.dp, 8.dp, 9.dp, 2.dp), style = MaterialTheme.typography.bodyMedium)
        Text(video.folder, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppGray, modifier = Modifier.padding(9.dp, 0.dp, 9.dp, 8.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun VideoThumbnail(uri: Uri, modifier: Modifier = Modifier, extractFrame: Boolean = false) {
    val context = LocalContext.current
    val cacheKey = remember(uri, extractFrame) { if (extractFrame) "frame:$uri" else uri.toString() }
    val bitmap by produceState<Bitmap?>(ThumbnailMemoryCache.get(cacheKey), cacheKey) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    if (!extractFrame && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(uri, Size(480, 270), null)
                    } else {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            retriever.getFrameAtTime(0)
                        } finally {
                            retriever.release()
                        }
                    }
                }.getOrNull()
            }?.also { ThumbnailMemoryCache.put(cacheKey, it) }
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(androidx.compose.ui.graphics.Color(0xFFDCDCDC)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = AppGray)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = AppGray)
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1_000
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun resumablePlaybackPosition(positionMs: Long, durationMs: Long): Long {
    val position = positionMs.coerceAtLeast(0L)
    return if (durationMs > 0L && position >= durationMs - 1_000L) 0L else position
}

@Composable
private fun PlayerScreen(
    video: VideoItem,
    queue: List<VideoItem>,
    autoPlayNext: Boolean,
    showPrivateThumbnails: Boolean,
    viewModel: MainViewModel,
    onSelect: (VideoItem) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLandscape by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackPositionMs by remember(video.uri) { mutableLongStateOf(0L) }
    var durationMs by remember(video.uri) { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var scrubPositionMs by remember(video.uri) { mutableStateOf<Long?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsTimeoutToken by remember { mutableIntStateOf(0) }
    val currentIndex = queue.indexOfFirst { it.uri == video.uri }
    val nextVideo = queue.getOrNull(currentIndex + 1)
    val playlistState = rememberLazyListState()
    val token = remember(context) { SessionToken(context, android.content.ComponentName(context, PlaybackService::class.java)) }
    val exitPlayer = {
        controller?.let { activeController ->
            runCatching {
                val duration = activeController.duration.takeIf { it > 0L } ?: video.durationMs
                viewModel.savePlaybackPosition(video.uri, resumablePlaybackPosition(activeController.currentPosition, duration))
                activeController.stop()
                activeController.clearMediaItems()
            }
        }
        onBack()
    }
    val exitFullscreen = {
        isLandscape = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    BackHandler(onBack = { if (isLandscape) exitFullscreen() else exitPlayer() })

    DisposableEffect(token) {
        var disposed = false
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val connectedController = runCatching { future.get() }.getOrElse {
                if (!disposed) error = "播放器连接失败，请返回后重试"
                return@addListener
            }
            if (disposed) connectedController.release() else controller = connectedController
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            future.cancel(false)
            controller?.release()
            controller = null
        }
    }
    DisposableEffect(video.uri) {
        onDispose {
            controller?.let { activeController ->
                runCatching {
                    val duration = activeController.duration.takeIf { it > 0L } ?: video.durationMs
                    viewModel.savePlaybackPosition(video.uri, resumablePlaybackPosition(activeController.currentPosition, duration))
                }
            }
        }
    }
    DisposableEffect(controller, nextVideo?.uri, autoPlayNext) {
        val listener = object : Player.Listener {
            override fun onPlayerError(playbackException: PlaybackException) {
                error = "此文件无法在当前设备上播放"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.savePlaybackPosition(video.uri, 0L)
                    if (autoPlayNext) nextVideo?.let(onSelect)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }
    LaunchedEffect(controller, video.uri) {
        val player = controller ?: return@LaunchedEffect
        try {
            error = null
            val item = MediaItem.Builder().setUri(video.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(video.title).build()).build()
            player.setMediaItem(item)
            player.prepare()
            val duration = player.duration.takeIf { it > 0L } ?: video.durationMs
            player.seekTo(resumablePlaybackPosition(viewModel.playbackPosition(video.uri), duration))
            player.play()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            error = "播放器初始化失败，请返回后重试"
        }
    }
    LaunchedEffect(controller, video.uri) {
        while (true) {
            val player = controller ?: break
            playbackPositionMs = player.currentPosition.coerceAtLeast(0L)
            player.duration.takeIf { it > 0 }?.let { durationMs = it }
            delay(500)
        }
    }
    LaunchedEffect(controller, playbackSpeed) {
        runCatching { controller?.setPlaybackSpeed(playbackSpeed) }
    }
    LaunchedEffect(video.uri, queue) {
        if (currentIndex >= 0) playlistState.scrollToItem(currentIndex)
    }
    DisposableEffect(activity) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.isAppearanceLightStatusBars = false
        onDispose { insetsController?.isAppearanceLightStatusBars = true }
    }
    DisposableEffect(activity) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    LaunchedEffect(controlsVisible, controlsTimeoutToken) {
        if (controlsVisible) {
            delay(3_000)
            controlsVisible = false
        }
    }

    val showControls: () -> Unit = {
        controlsVisible = true
        controlsTimeoutToken += 1
    }
    val toggleControls: () -> Unit = {
        if (controlsVisible) controlsVisible = false else showControls()
    }

    if (isLandscape) {
        Column(Modifier.fillMaxSize().background(AppBlack)) {
            Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
            PlayerSurface(
                controller = controller,
                isPlaying = isPlaying,
                playbackPositionMs = playbackPositionMs,
                durationMs = durationMs,
                scrubPositionMs = scrubPositionMs,
                playbackSpeed = playbackSpeed,
                isLandscape = true,
                controlsVisible = controlsVisible,
                error = error,
                onBack = exitFullscreen,
                onToggleControls = toggleControls,
                onInteraction = showControls,
                onTogglePlay = { if (isPlaying) controller?.pause() else controller?.play() },
                onSeekBack = { controller?.seekTo((playbackPositionMs - 10_000).coerceAtLeast(0L)) },
                onSeekForward = { controller?.seekTo((playbackPositionMs + 10_000).coerceAtMost(durationMs)) },
                onScrubPositionChange = { scrubPositionMs = it.toLong() },
                onScrubFinished = { scrubPositionMs?.let { controller?.seekTo(it) }; scrubPositionMs = null },
                onCycleSpeed = { playbackSpeed = nextPlaybackSpeed(playbackSpeed) },
                onToggleFullscreen = exitFullscreen,
                modifier = Modifier.weight(1f),
            )
        }
    } else Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {
        Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(AppBlack))
        PlayerSurface(
            controller = controller,
            isPlaying = isPlaying,
            playbackPositionMs = playbackPositionMs,
            durationMs = durationMs,
            scrubPositionMs = scrubPositionMs,
            playbackSpeed = playbackSpeed,
            isLandscape = false,
            controlsVisible = controlsVisible,
            error = error,
            onBack = exitPlayer,
            onToggleControls = toggleControls,
            onInteraction = showControls,
            onTogglePlay = { if (isPlaying) controller?.pause() else controller?.play() },
            onSeekBack = { controller?.seekTo((playbackPositionMs - 10_000).coerceAtLeast(0L)) },
            onSeekForward = { controller?.seekTo((playbackPositionMs + 10_000).coerceAtMost(durationMs)) },
            onScrubPositionChange = { scrubPositionMs = it.toLong() },
            onScrubFinished = { scrubPositionMs?.let { controller?.seekTo(it) }; scrubPositionMs = null },
            onCycleSpeed = { playbackSpeed = nextPlaybackSpeed(playbackSpeed) },
            onToggleFullscreen = {
                isLandscape = true
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("${video.folder}  ${formatDuration(video.durationMs)}", color = AppGray, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { nextVideo?.let(onSelect) }, enabled = nextVideo != null) {
                Icon(Icons.Default.SkipNext, "播放下一个")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放列表", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${if (currentIndex >= 0) currentIndex + 1 else 1} / ${queue.size}", color = AppGray, style = MaterialTheme.typography.bodySmall)
        }
        if (queue.isEmpty()) {
            EmptyState("播放列表为空", "返回视频库后重新选择视频", Modifier.weight(1f))
        } else {
            LazyColumn(
                state = playlistState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(queue, key = { it.uri }) { queueVideo ->
                    PlaybackQueueRow(
                        video = queueVideo,
                        selected = queueVideo.uri == video.uri,
                        showThumbnail = !queueVideo.isPrivate || showPrivateThumbnails,
                        onClick = { onSelect(queueVideo) },
                    )
                }
            }
        }
    }
}

private fun nextPlaybackSpeed(speed: Float): Float = when (speed) {
    1f -> 1.25f
    1.25f -> 1.5f
    1.5f -> 2f
    else -> 1f
}

@Composable
private fun PlayerSurface(
    controller: Player?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    durationMs: Long,
    scrubPositionMs: Long?,
    playbackSpeed: Float,
    isLandscape: Boolean,
    controlsVisible: Boolean,
    error: String?,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onInteraction: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onScrubPositionChange: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(AppBlack)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    isClickable = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                playerView.player = controller
                playerView.setOnClickListener { onToggleControls() }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (controlsVisible) {
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                IconButton(
                    onClick = { onInteraction(); onBack() },
                    modifier = Modifier.size(36.dp).background(AppBlack.copy(alpha = .6f), RoundedCornerShape(50)),
                ) { Icon(Icons.Default.ArrowBack, "返回", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp)) }
            }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1fx", playbackSpeed),
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                        onInteraction()
                        onCycleSpeed()
                    }.padding(horizontal = 6.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                IconButton(
                    onClick = { onInteraction(); onToggleFullscreen() },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        if (isLandscape) "退出横屏" else "横屏播放",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(AppBlack.copy(alpha = .74f)).padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onInteraction(); onSeekBack() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Replay10, "后退十秒", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = { onInteraction(); onTogglePlay() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) "暂停" else "播放",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(21.dp),
                    )
                }
                IconButton(onClick = { onInteraction(); onSeekForward() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Forward10, "前进十秒", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(19.dp))
                }
                Text(formatDuration(scrubPositionMs ?: playbackPositionMs), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = (scrubPositionMs ?: playbackPositionMs).coerceIn(0L, durationMs).toFloat(),
                    onValueChange = { onInteraction(); onScrubPositionChange(it) },
                    onValueChangeFinished = { onInteraction(); onScrubFinished() },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = AppSkyBlue,
                        activeTrackColor = AppSkyBlue,
                        inactiveTrackColor = androidx.compose.ui.graphics.Color(0xFF8C8C8C),
                    ),
                    modifier = Modifier.weight(1f).height(28.dp).padding(horizontal = 6.dp),
                )
                Text(formatDuration(durationMs), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        error?.let {
            Text(it, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.Center).background(AppBlack.copy(alpha = .8f), RoundedCornerShape(8.dp)).padding(16.dp))
        }
    }
}

@Composable
private fun PlaybackQueueRow(video: VideoItem, selected: Boolean, showThumbnail: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(
            if (selected) androidx.compose.ui.graphics.Color(0xFFF0F0F0) else androidx.compose.ui.graphics.Color.Transparent,
        ).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(112.dp, 63.dp).clip(RoundedCornerShape(5.dp))) {
            if (showThumbnail) {
                VideoThumbnail(Uri.parse(video.uri), Modifier.fillMaxSize(), extractFrame = video.isPrivate)
            } else {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFDCDCDC)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = AppGray)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Spacer(Modifier.height(3.dp))
            Text("${video.folder}  ${formatDuration(video.durationMs)}", color = AppGray, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
        }
        if (selected) Icon(Icons.Default.PlayArrow, "正在播放", modifier = Modifier.padding(start = 8.dp))
    }
}

private fun openInSystemPlayer(context: Context, video: VideoItem) {
    val uri = Uri.parse(video.uri)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newRawUri(video.title, uri)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "未找到可用的视频播放器", Toast.LENGTH_SHORT).show()
    }
}
