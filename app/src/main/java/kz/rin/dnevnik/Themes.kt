package kz.rin.dnevnik

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

class AppTheme(
    val name: String,
    val dark: Boolean,
    val bg: List<Color>,
    val accent: Color,
    val text: Color
) {
    val card: Color get() = if (dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.72f)
    val cardBorder: Color get() = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.06f)
    val textSoft: Color get() = text.copy(alpha = 0.68f)
    val onAccent: Color get() = if (dark) Color(0xFF10101A) else Color.White
}

private fun c(hex: Long) = Color(0xFF000000 or hex)

val DARK_THEMES = listOf(
    AppTheme("Полночь", true, listOf(c(0x0F0C29), c(0x302B63), c(0x24243E)), c(0x9D8CFF), c(0xF1F0FF)),
    AppTheme("Закат", true, listOf(c(0x1A0A2E), c(0x5B1E55), c(0xB3453D)), c(0xFFA98A), c(0xFFF1EC)),
    AppTheme("Океан", true, listOf(c(0x021B2E), c(0x064663), c(0x0B7A75)), c(0x4DD0E1), c(0xE9FBFF)),
    AppTheme("Лес", true, listOf(c(0x06190F), c(0x0F3D2A), c(0x1B5E3B)), c(0x69F0AE), c(0xEAFFF3)),
    AppTheme("Вино", true, listOf(c(0x200010), c(0x5A0F2E), c(0x8E1B3A)), c(0xFF6E91), c(0xFFEFF3)),
    AppTheme("Космос", true, listOf(c(0x05050F), c(0x1A1040), c(0x3A1C71)), c(0xB388FF), c(0xF3EEFF)),
    AppTheme("Уголь", true, listOf(c(0x0B0B0F), c(0x1C1C26), c(0x2E2E3E)), c(0x90CAF9), c(0xEEF3F8)),
    AppTheme("Янтарь", true, listOf(c(0x1A0F00), c(0x4A2800), c(0x7A4A0A)), c(0xFFC857), c(0xFFF6E3)),
    AppTheme("Изумруд", true, listOf(c(0x001A1A), c(0x004D4D), c(0x0B7A5E)), c(0x5EEAD4), c(0xE8FFFB)),
    AppTheme("Неон", true, listOf(c(0x0A0014), c(0x2B0057), c(0x00577A)), c(0x00E5FF), c(0xEFFBFF))
)

val LIGHT_THEMES = listOf(
    AppTheme("Облако", false, listOf(c(0xF5F7FF), c(0xE3E9FF), c(0xD6E0FF)), c(0x4F5BD5), c(0x1B1F3B)),
    AppTheme("Персик", false, listOf(c(0xFFF4EC), c(0xFFE0CC), c(0xFFC9B0)), c(0xE4572E), c(0x3A1F14)),
    AppTheme("Мята", false, listOf(c(0xF0FFF7), c(0xD5F5E3), c(0xB8EBD0)), c(0x1B9E6B), c(0x12302A)),
    AppTheme("Лаванда", false, listOf(c(0xFAF5FF), c(0xEBDDFF), c(0xD9C4FF)), c(0x7B3FE4), c(0x2A1650)),
    AppTheme("Лимон", false, listOf(c(0xFFFEF0), c(0xFFF6C2), c(0xFFEB99)), c(0xB07A00), c(0x3A3000))
)

val ALL_THEMES: List<AppTheme> = DARK_THEMES + LIGHT_THEMES

class FontOption(val name: String, val family: FontFamily)

/**
 * Шрифты лежат в assets/fonts, список — в assets/fonts/fonts.txt (строки «файл|Название»).
 * Файлы подтягивает сборка (scripts/fetch_fonts.py). Если файла нет — шрифт пропускается.
 */
fun loadFonts(ctx: Context): List<FontOption> {
    val result = ArrayList<FontOption>()
    result.add(FontOption("Системный", FontFamily.Default))
    try {
        val lines = ctx.assets.open("fonts/fonts.txt").bufferedReader().readLines()
        for (line in lines) {
            val parts = line.split("|")
            if (parts.size < 2) continue
            val file = parts[0].trim()
            val title = parts[1].trim()
            try {
                val family = FontFamily(Font(ctx.assets, "fonts/$file"))
                result.add(FontOption(title, family))
            } catch (e: Exception) {
                // шрифт не загрузился — пропускаем
            }
        }
    } catch (e: Exception) {
        // fonts.txt отсутствует — остаётся системный шрифт
    }
    return result
}
