package kz.rin.dnevnik

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

enum class Tab(val title: String, val icon: ImageVector) {
    TODAY("Сегодня", Icons.Filled.EditNote),
    DAYS("Дни", Icons.Filled.CalendarMonth),
    LOOK("Оформление", Icons.Filled.Palette)
}

private fun Typography.withFamily(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun DiaryApp(repo: Repo, prefs: Prefs) {
    val ctx = LocalContext.current
    val fonts = remember { loadFonts(ctx) }
    var themeIdx by remember { mutableStateOf(prefs.themeIndex.coerceIn(0, ALL_THEMES.size - 1)) }
    var fontIdx by remember { mutableStateOf(prefs.fontIndex.coerceIn(0, fonts.size - 1)) }
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var openDay by remember { mutableStateOf<String?>(null) }

    val theme = ALL_THEMES[themeIdx]
    val family = fonts[fontIdx].family

    // Цвета плавно перетекают при смене темы
    val c0 by animateColorAsState(theme.bg[0], tween(600), label = "c0")
    val c1 by animateColorAsState(theme.bg[1], tween(600), label = "c1")
    val c2 by animateColorAsState(theme.bg[2], tween(600), label = "c2")

    val drift = rememberInfiniteTransition(label = "drift")
    val shift by drift.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shift"
    )

    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window
        if (w != null) {
            val ctrl = WindowCompat.getInsetsController(w, view)
            ctrl.isAppearanceLightStatusBars = !theme.dark
            ctrl.isAppearanceLightNavigationBars = !theme.dark
        }
    }

    val typography = remember(family) { Typography().withFamily(family) }

    MaterialTheme(typography = typography) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides TextStyle(fontFamily = family, color = theme.text)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(c0, c1, c2),
                            start = Offset(0f, 0f),
                            end = Offset(600f + 900f * shift, 2000f - 600f * shift)
                        )
                    )
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .imePadding()
                ) {
                    Box(Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = Pair(tab, openDay),
                            transitionSpec = {
                                (fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 14 }) togetherWith
                                    fadeOut(tween(160))
                            },
                            label = "screens"
                        ) { (t, day) ->
                            if (day != null) {
                                DayScreen(
                                    date = day, repo = repo, theme = theme,
                                    onBack = { openDay = null },
                                    onDeleted = { openDay = null }
                                )
                            } else when (t) {
                                Tab.TODAY -> TodayScreen(repo, theme)
                                Tab.DAYS -> DaysScreen(repo, theme, onOpen = { openDay = it })
                                Tab.LOOK -> LookScreen(
                                    theme = theme, fonts = fonts,
                                    themeIdx = themeIdx, fontIdx = fontIdx,
                                    onTheme = { themeIdx = it; prefs.themeIndex = it },
                                    onFont = { fontIdx = it; prefs.fontIndex = it }
                                )
                            }
                        }
                    }
                    BottomBar(tab, theme) { tab = it; openDay = null }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(current: Tab, theme: AppTheme, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(theme.card)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Tab.values().forEach { t ->
            val selected = t == current
            val bg by animateColorAsState(
                if (selected) theme.accent else Color.Transparent, tween(300), label = "navbg"
            )
            val fg by animateColorAsState(
                if (selected) theme.onAccent else theme.textSoft, tween(300), label = "navfg"
            )
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(t) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(t.icon, contentDescription = t.title, tint = fg)
                if (selected) {
                    Text(
                        "  " + t.title,
                        color = fg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Плавное появление элемента списка с задержкой по номеру. */
@Composable
fun Appear(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(8) * 45).toLong())
        a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 200f))
    }
    Box(
        modifier
            .alpha(a.value.coerceIn(0f, 1f))
            .graphicsLayer { translationY = (1f - a.value) * 40f }
    ) { content() }
}
