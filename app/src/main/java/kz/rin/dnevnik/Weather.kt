package kz.rin.dnevnik

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class WKind { SUN, PARTLY, CLOUD, FOG, RAIN, SNOW, STORM }

data class Weather(
    val temp: Double,
    val wind: Double,
    val code: Int,
    val precip: Double,
    val place: String
) {
    val kind: WKind
        get() = when (code) {
            0, 1 -> WKind.SUN
            2 -> WKind.PARTLY
            3 -> WKind.CLOUD
            45, 48 -> WKind.FOG
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WKind.RAIN
            71, 73, 75, 77, 85, 86 -> WKind.SNOW
            95, 96, 99 -> WKind.STORM
            else -> WKind.CLOUD
        }

    val title: String
        get() = when (code) {
            0 -> "Ясно"
            1 -> "Преимущественно ясно"
            2 -> "Переменная облачность"
            3 -> "Пасмурно"
            45, 48 -> "Туман"
            51, 53, 55 -> "Морось"
            56, 57 -> "Ледяная морось"
            61 -> "Небольшой дождь"
            63 -> "Дождь"
            65 -> "Сильный дождь"
            66, 67 -> "Ледяной дождь"
            71 -> "Небольшой снег"
            73 -> "Снег"
            75 -> "Сильный снег"
            77 -> "Снежная крупа"
            80, 81, 82 -> "Ливень"
            85, 86 -> "Снегопад"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Облачно"
        }

    fun tempText(): String {
        val t = temp.roundToInt()
        return (if (t > 0) "+$t" else "$t").replace("-", "−") + "°"
    }

    fun windText(): String = "${"%.1f".format(wind).replace(',', '.')} м/с"

    fun precipText(): String =
        if (precip <= 0.0) "без осадков" else "${"%.1f".format(precip).replace(',', '.')} мм"

    fun toJson(): String = JSONObject()
        .put("t", temp).put("w", wind).put("c", code).put("p", precip).put("pl", place)
        .toString()

    companion object {
        fun fromJson(s: String?): Weather? {
            if (s == null) return null
            return try {
                val o = JSONObject(s)
                Weather(o.getDouble("t"), o.getDouble("w"), o.getInt("c"), o.getDouble("p"), o.optString("pl", ""))
            } catch (e: Exception) {
                null
            }
        }
    }
}

object WeatherApi {
    // Если определить местоположение не удалось — Уральск
    private const val DEF_LAT = 51.2278
    private const val DEF_LON = 51.3865

    private fun hasLocationPermission(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Возвращает null при ошибке сети. Вызывать из фонового потока. */
    fun fetch(ctx: Context): Weather? {
        var lat = DEF_LAT
        var lon = DEF_LON
        var place = "Уральск"
        if (hasLocationPermission(ctx)) {
            try {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var best: android.location.Location? = null
                for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
                    val l = try { lm.getLastKnownLocation(p) } catch (e: SecurityException) { null }
                    if (l != null && (best == null || l.time > best.time)) best = l
                }
                if (best != null) {
                    lat = best.latitude
                    lon = best.longitude
                    place = "Ваше местоположение"
                }
            } catch (e: Exception) {
                // остаёмся на значениях по умолчанию
            }
        }
        return try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,wind_speed_10m,weather_code,precipitation" +
                    "&wind_speed_unit=ms&timezone=auto"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val cur = JSONObject(body).getJSONObject("current")
            Weather(
                cur.getDouble("temperature_2m"),
                cur.getDouble("wind_speed_10m"),
                cur.getInt("weather_code"),
                cur.optDouble("precipitation", 0.0),
                place
            )
        } catch (e: Exception) {
            null
        }
    }
}

// ---------- Анимированные значки погоды ----------

private fun DrawScope.drawCloud(cx: Float, cy: Float, w: Float, color: Color) {
    val h = w * 0.55f
    val r1 = h * 0.5f
    drawCircle(color, r1, Offset(cx - w * 0.22f, cy + h * 0.05f))
    drawCircle(color, h * 0.68f, Offset(cx + w * 0.02f, cy - h * 0.15f))
    drawCircle(color, h * 0.5f, Offset(cx + w * 0.26f, cy + h * 0.05f))
    drawRoundRect(
        color,
        topLeft = Offset(cx - w * 0.36f, cy + h * 0.02f),
        size = Size(w * 0.72f, h * 0.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.25f, h * 0.25f)
    )
}

private fun DrawScope.drawSun(cx: Float, cy: Float, r: Float, spin: Float) {
    val sun = Color(0xFFFFC83D)
    val ray = Color(0xFFFFB300)
    rotate(spin, Offset(cx, cy)) {
        for (i in 0 until 8) {
            val a = (i * 45.0) * PI / 180.0
            val s = Offset(cx + (r * 1.35f * cos(a)).toFloat(), cy + (r * 1.35f * sin(a)).toFloat())
            val e = Offset(cx + (r * 1.75f * cos(a)).toFloat(), cy + (r * 1.75f * sin(a)).toFloat())
            drawLine(ray, s, e, strokeWidth = r * 0.22f, cap = StrokeCap.Round)
        }
    }
    drawCircle(Brush_sun(sun, cx, cy, r), r, Offset(cx, cy))
}

@Suppress("FunctionName")
private fun Brush_sun(c: Color, cx: Float, cy: Float, r: Float) =
    androidx.compose.ui.graphics.Brush.radialGradient(
        listOf(Color(0xFFFFE680), c),
        center = Offset(cx - r * 0.25f, cy - r * 0.25f),
        radius = r * 1.3f
    )

@Composable
fun WeatherIcon(kind: WKind, size: Dp = 80.dp, modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "wx")
    val t by tr.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    val spin by tr.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val bob by tr.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "bob"
    )
    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val cx = w / 2f
            val cy = w / 2f
            val cloud = Color(0xFFEAF1FA)
            val grey = Color(0xFFB7C4D6)
            val dx = bob * w * 0.03f
            when (kind) {
                WKind.SUN -> drawSun(cx, cy, w * 0.22f, spin)
                WKind.PARTLY -> {
                    drawSun(cx - w * 0.14f, cy - w * 0.12f, w * 0.17f, spin)
                    drawCloud(cx + w * 0.04f + dx, cy + w * 0.1f, w * 0.7f, cloud)
                }
                WKind.CLOUD -> {
                    drawCloud(cx - w * 0.1f - dx, cy - w * 0.08f, w * 0.5f, grey)
                    drawCloud(cx + dx, cy + w * 0.06f, w * 0.72f, cloud)
                }
                WKind.FOG -> {
                    drawCloud(cx, cy - w * 0.12f, w * 0.64f, grey)
                    for (i in 0 until 3) {
                        val y = cy + w * (0.18f + i * 0.1f)
                        val off = bob * w * 0.05f * (if (i % 2 == 0) 1 else -1)
                        drawLine(cloud, Offset(w * 0.22f + off, y), Offset(w * 0.78f + off, y),
                            strokeWidth = w * 0.045f, cap = StrokeCap.Round)
                    }
                }
                WKind.RAIN -> {
                    drawCloud(cx + dx, cy - w * 0.12f, w * 0.7f, grey)
                    for (i in 0 until 3) {
                        val p = (t + i / 3f) % 1f
                        val x = w * (0.34f + i * 0.16f)
                        val y = cy + w * 0.08f + p * w * 0.28f
                        drawLine(Color(0xFF4FC3F7).copy(alpha = 1f - p * 0.7f), Offset(x, y), Offset(x - w * 0.03f, y + w * 0.09f),
                            strokeWidth = w * 0.04f, cap = StrokeCap.Round)
                    }
                }
                WKind.SNOW -> {
                    drawCloud(cx + dx, cy - w * 0.12f, w * 0.7f, grey)
                    for (i in 0 until 4) {
                        val p = (t + i / 4f) % 1f
                        val x = w * (0.3f + i * 0.13f) + sin((p * 6.28f + i)) * w * 0.02f
                        val y = cy + w * 0.08f + p * w * 0.3f
                        drawCircle(Color.White.copy(alpha = 1f - p * 0.6f), w * 0.028f, Offset(x, y))
                    }
                }
                WKind.STORM -> {
                    drawCloud(cx + dx, cy - w * 0.12f, w * 0.7f, Color(0xFF8A99AE))
                    val flash = if (t > 0.55f && t < 0.75f) 1f else 0.85f
                    val bolt = Path().apply {
                        moveTo(cx + w * 0.04f, cy + w * 0.02f)
                        lineTo(cx - w * 0.08f, cy + w * 0.2f)
                        lineTo(cx, cy + w * 0.2f)
                        lineTo(cx - w * 0.06f, cy + w * 0.38f)
                        lineTo(cx + w * 0.12f, cy + w * 0.14f)
                        lineTo(cx + w * 0.03f, cy + w * 0.14f)
                        close()
                    }
                    drawPath(bolt, Color(0xFFFFD54F).copy(alpha = flash))
                }
            }
        }
    }
}
