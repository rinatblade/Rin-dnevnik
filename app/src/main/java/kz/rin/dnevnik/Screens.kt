package kz.rin.dnevnik

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale("ru")

fun fullDate(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", RU)).replaceFirstChar { it.uppercase() }

@Composable
fun GlassCard(theme: AppTheme, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(theme.card)
            .border(1.dp, theme.cardBorder, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun rememberLoader(): ImageLoader {
    val ctx = LocalContext.current
    return remember {
        ImageLoader.Builder(ctx)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
}

// ======================= СЕГОДНЯ =======================

@Composable
fun TodayScreen(repo: Repo, theme: AppTheme) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    var weather by remember { mutableStateOf(Weather.fromJson(prefs.lastWeather)) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshKey++
    }
    LaunchedEffect(Unit) { permLauncher.launch(LOCATION_PERMS) }
    LaunchedEffect(refreshKey) {
        loading = true
        val w = withContext(Dispatchers.IO) { WeatherApi.fetch(ctx) }
        if (w != null) {
            weather = w
            prefs.lastWeather = w.toJson()
            failed = false
        } else {
            failed = true
        }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Appear(0) {
            GlassCard(theme, Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 56.sp, fontWeight = FontWeight.Bold, color = theme.text
                    )
                    Text(
                        now.format(DateTimeFormatter.ofPattern(":ss")),
                        fontSize = 24.sp, color = theme.accent,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )
                }
                Text(fullDate(now.toLocalDate()), fontSize = 18.sp, color = theme.textSoft)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(theme.cardBorder)
                )
                Spacer(Modifier.height(12.dp))
                val w = weather
                if (w == null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { refreshKey++ }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = theme.accent)
                        Text(
                            if (loading) "  Загружаю погоду…" else "  Нет данных о погоде. Нажмите, чтобы обновить",
                            color = theme.textSoft, fontSize = 15.sp
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherIcon(w.kind, 88.dp)
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(w.tempText(), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = theme.text)
                            Text(w.title, fontSize = 16.sp, color = theme.text, maxLines = 2)
                            Text(w.place, fontSize = 12.sp, color = theme.textSoft)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(w.windText(), fontSize = 14.sp, color = theme.text)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.Air, "Ветер", tint = theme.accent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(w.precipText(), fontSize = 14.sp, color = theme.text)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.WaterDrop, "Осадки", tint = theme.accent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Icon(
                                Icons.Filled.Refresh, "Обновить", tint = theme.textSoft,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { refreshKey++ }
                            )
                        }
                    }
                    if (failed) {
                        Text("Не удалось обновить — показаны прошлые данные", fontSize = 12.sp, color = theme.textSoft)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        val dateKey = now.toLocalDate().toString()
        androidx.compose.runtime.key(dateKey) {
            Appear(1) { EntryEditor(dateKey, repo, theme) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ======================= РЕДАКТОР ДНЯ =======================

@Composable
fun EntryEditor(date: String, repo: Repo, theme: AppTheme) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val loader = rememberLoader()

    val initial = remember(date) { repo.load(date) }
    var text by remember(date) { mutableStateOf(initial.text) }
    val media = remember(date) { mutableStateListOf<MediaItem>().apply { addAll(initial.media) } }
    var viewer by remember { mutableStateOf<MediaItem?>(null) }
    var pending by remember { mutableStateOf<MediaItem?>(null) }

    fun persist() {
        repo.save(Entry(date, text, media.toList()))
    }

    LaunchedEffect(text) {
        delay(500)
        persist()
    }
    DisposableEffect(date) {
        onDispose { persist() }
    }

    fun importUri(uri: Uri) {
        scope.launch {
            val item = withContext(Dispatchers.IO) {
                try {
                    val type = ctx.contentResolver.getType(uri) ?: ""
                    val isVideo = type.startsWith("video")
                    val name = "m${System.currentTimeMillis()}_${(0..9999).random()}." + (if (isVideo) "mp4" else "jpg")
                    val out = repo.mediaFile(date, name)
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { o -> input.copyTo(o) }
                    }
                    MediaItem(name, isVideo)
                } catch (e: Exception) {
                    null
                }
            }
            if (item != null) {
                media.add(item)
                persist()
            }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        uris.forEach { importUri(it) }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pending
        if (p != null) {
            if (ok) {
                media.add(p)
                persist()
            } else {
                repo.mediaFile(date, p.file).delete()
            }
        }
        pending = null
    }
    val takeVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val p = pending
        if (p != null) {
            if (ok) {
                media.add(p)
                persist()
            } else {
                repo.mediaFile(date, p.file).delete()
            }
        }
        pending = null
    }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val r = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!r.isNullOrBlank()) {
                text = if (text.isBlank()) r.replaceFirstChar { it.uppercase() } else text.trimEnd() + " " + r
            }
        }
    }

    fun startVoice() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите — я запишу в дневник")
        }
        try {
            voice.launch(i)
        } catch (e: Exception) {
            Toast.makeText(ctx, "На этом устройстве нет службы голосового ввода", Toast.LENGTH_LONG).show()
        }
    }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)

    fun startPhoto() {
        val name = "m${System.currentTimeMillis()}.jpg"
        pending = MediaItem(name, false)
        try {
            takePhoto.launch(uriFor(repo.mediaFile(date, name)))
        } catch (e: Exception) {
            pending = null
            Toast.makeText(ctx, "Камера недоступна", Toast.LENGTH_LONG).show()
        }
    }

    fun startVideo() {
        val name = "m${System.currentTimeMillis()}.mp4"
        pending = MediaItem(name, true)
        try {
            takeVideo.launch(uriFor(repo.mediaFile(date, name)))
        } catch (e: Exception) {
            pending = null
            Toast.makeText(ctx, "Камера недоступна", Toast.LENGTH_LONG).show()
        }
    }

    fun openVideo(item: MediaItem) {
        try {
            val i = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uriFor(repo.mediaFile(date, item.file)), "video/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Нет приложения для просмотра видео", Toast.LENGTH_LONG).show()
        }
    }

    Column {
        GlassCard(theme, Modifier.fillMaxWidth()) {
            Text("Запись дня", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = theme.text)
            Spacer(Modifier.height(6.dp))
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 170.dp),
                placeholder = { Text("Как прошёл день? Напишите или нажмите на микрофон…", color = theme.textSoft) },
                textStyle = LocalTextStyle.current.copy(fontSize = 17.sp, lineHeight = 25.sp, color = theme.text),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = theme.accent,
                    focusedTextColor = theme.text,
                    unfocusedTextColor = theme.text
                )
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(Icons.Filled.Mic, "Голос", theme, Modifier.weight(1.3f), pulse = true) { startVoice() }
                ActionButton(Icons.Filled.PhotoLibrary, "Галерея", theme, Modifier.weight(1f)) {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
                ActionButton(Icons.Filled.PhotoCamera, "Фото", theme, Modifier.weight(1f)) { startPhoto() }
                ActionButton(Icons.Filled.Videocam, "Видео", theme, Modifier.weight(1f)) { startVideo() }
            }
        }

        if (media.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            GlassCard(theme, Modifier.fillMaxWidth()) {
                Text(
                    "Фото и видео дня (${media.size})",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = theme.text
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    media.toList().forEach { item ->
                        Box(
                            Modifier
                                .size(112.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(theme.cardBorder)
                                .clickable {
                                    if (item.isVideo) openVideo(item) else viewer = item
                                }
                        ) {
                            AsyncImage(
                                model = repo.mediaFile(date, item.file),
                                contentDescription = null,
                                imageLoader = loader,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (item.isVideo) {
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, "Видео", tint = Color.White)
                                }
                            }
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable {
                                        media.remove(item)
                                        repo.mediaFile(date, item.file).delete()
                                        persist()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, "Удалить", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    val v = viewer
    if (v != null) {
        Dialog(
            onDismissRequest = { viewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { viewer = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = repo.mediaFile(date, v.file),
                    contentDescription = null,
                    imageLoader = loader,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    theme: AppTheme,
    modifier: Modifier = Modifier,
    pulse: Boolean = false,
    onClick: () -> Unit
) {
    val tr = rememberInfiniteTransition(label = "pulse")
    val s by tr.animateFloat(
        1f, if (pulse) 1.07f else 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "s"
    )
    val bg = if (pulse) theme.accent else theme.accent.copy(alpha = 0.16f)
    val fg = if (pulse) theme.onAccent else theme.accent
    Column(
        modifier
            .scale(s)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = fg)
        Text(label, fontSize = 12.sp, color = fg, maxLines = 1)
    }
}

// ======================= СПИСОК ДНЕЙ =======================

@Composable
fun DaysScreen(repo: Repo, theme: AppTheme, onOpen: (String) -> Unit) {
    val entries = remember { repo.listDates().map { repo.load(it) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("Список дней", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = theme.text)
        Text(
            if (entries.isEmpty()) "Записей пока нет" else "Всего записей: ${entries.size}",
            fontSize = 14.sp, color = theme.textSoft
        )
        Spacer(Modifier.height(12.dp))
        if (entries.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.CalendarMonth, null, tint = theme.accent, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Откройте вкладку «Сегодня» и напишите первую запись — она появится здесь.",
                    color = theme.textSoft, fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(entries) { idx, e ->
                    Appear(idx) { DayRow(e, theme) { onOpen(e.date) } }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun DayRow(e: Entry, theme: AppTheme, onClick: () -> Unit) {
    val d = LocalDate.parse(e.date)
    val photos = e.media.count { !it.isVideo }
    val videos = e.media.count { it.isVideo }
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.card)
            .border(1.dp, theme.cardBorder, shape)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.accent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(d.dayOfMonth.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.onAccent)
            Text(
                d.format(DateTimeFormatter.ofPattern("LLL", RU)).replace(".", ""),
                fontSize = 12.sp, color = theme.onAccent
            )
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(fullDate(d), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = theme.text, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                if (e.text.isBlank()) "Только фото и видео" else e.text.trim(),
                fontSize = 14.sp, color = theme.textSoft, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (e.media.isNotEmpty()) {
                val parts = ArrayList<String>()
                if (photos > 0) parts.add("Фото: $photos")
                if (videos > 0) parts.add("Видео: $videos")
                Text(parts.joinToString(" · "), fontSize = 12.sp, color = theme.accent)
            }
        }
    }
}

// ======================= ПРОСМОТР ДНЯ =======================

@Composable
fun DayScreen(date: String, repo: Repo, theme: AppTheme, onBack: () -> Unit, onDeleted: () -> Unit) {
    BackHandler { onBack() }
    var confirm by remember { mutableStateOf(false) }
    val d = LocalDate.parse(date)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.card)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.ArrowBack, "Назад", tint = theme.text) }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(fullDate(d), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.text)
                Text("Запись можно дополнить или исправить", fontSize = 12.sp, color = theme.textSoft)
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.card)
                    .clickable { confirm = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Delete, "Удалить день", tint = theme.text) }
        }
        Spacer(Modifier.height(14.dp))
        EntryEditor(date, repo, theme)
        Spacer(Modifier.height(24.dp))
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Удалить запись?") },
            text = { Text("Текст, фото и видео за ${fullDate(d)} будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    repo.delete(date)
                    onDeleted()
                }) { Text("Удалить", color = Color(0xFFE53935)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Отмена") } }
        )
    }
}

// ======================= ОФОРМЛЕНИЕ =======================

@Composable
fun LookScreen(
    theme: AppTheme,
    fonts: List<FontOption>,
    themeIdx: Int,
    fontIdx: Int,
    onTheme: (Int) -> Unit,
    onFont: (Int) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            Text("Оформление", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = theme.text)
            Text("Тема и шрифт применяются сразу", fontSize = 14.sp, color = theme.textSoft)
        }
        item { SectionTitle("Тёмные темы (${DARK_THEMES.size})", theme) }
        DARK_THEMES.indices.toList().chunked(2).forEach { row ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { i ->
                        ThemeCard(ALL_THEMES[i], i == themeIdx, Modifier.weight(1f)) { onTheme(i) }
                    }
                }
            }
        }
        item { SectionTitle("Светлые темы (${LIGHT_THEMES.size})", theme) }
        LIGHT_THEMES.indices.toList().chunked(2).forEach { row ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { j ->
                        val i = DARK_THEMES.size + j
                        ThemeCard(ALL_THEMES[i], i == themeIdx, Modifier.weight(1f)) { onTheme(i) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { SectionTitle("Шрифты (${fonts.size})", theme) }
        itemsIndexed(fonts) { idx, f ->
            val selected = idx == fontIdx
            val borderColor by animateColorAsState(
                if (selected) theme.accent else theme.cardBorder, tween(250), label = "fb"
            )
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(theme.card)
                    .border(if (selected) 2.dp else 1.dp, borderColor, shape)
                    .clickable { onFont(idx) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontSize = 12.sp, color = theme.accent)
                    Text(
                        "Съешь ещё этих мягких булок 0123",
                        fontSize = 19.sp, color = theme.text, fontFamily = f.family,
                        maxLines = 2
                    )
                }
                if (selected) Icon(Icons.Filled.Check, "Выбрано", tint = theme.accent)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SectionTitle(title: String, theme: AppTheme) {
    Text(
        title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = theme.text,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun ThemeCard(t: AppTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val h by animateDpAsState(if (selected) 96.dp else 88.dp, tween(250), label = "h")
    Box(
        modifier
            .height(h)
            .clip(shape)
            .background(Brush.linearGradient(t.bg))
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) t.accent else Color.White.copy(alpha = 0.25f),
                shape
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(t.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = t.text,
            modifier = Modifier.align(Alignment.TopStart))
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .size(22.dp)
                .clip(CircleShape)
                .background(t.accent)
        )
        if (selected) {
            Icon(Icons.Filled.Check, "Выбрано", tint = t.accent, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}
