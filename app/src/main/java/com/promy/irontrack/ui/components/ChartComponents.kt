package com.promy.irontrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promy.irontrack.data.model.StreakInfo
import com.promy.irontrack.data.model.WeightLogEntity
import com.promy.irontrack.ui.theme.AccentGreen
import com.promy.irontrack.ui.theme.AccentMuted
import com.promy.irontrack.ui.theme.AccentOnVolt
import com.promy.irontrack.ui.theme.BgDark
import com.promy.irontrack.ui.theme.BorderSubtle
import com.promy.irontrack.ui.theme.SurfaceDark
import com.promy.irontrack.ui.theme.SurfaceLightDark
import com.promy.irontrack.ui.theme.TextMuted
import com.promy.irontrack.ui.theme.TextPrimary
import com.promy.irontrack.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Minimal Streak Card: flat card with 1dp border and 8dp corners, no shadows
 */
@Composable
fun StreakCard(
    streakInfo: StreakInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("streak_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = AccentMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${streakInfo.currentStreakDays} day streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceLightDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "${streakInfo.thisWeekWorkoutsCount} of ${streakInfo.weeklyGoal} this week",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (streakInfo.thisWeekWorkoutsCount >= streakInfo.weeklyGoal) AccentGreen else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7 Days of the week row with day letters below circles
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            val cal = Calendar.getInstance()
            val currentDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEachIndexed { index, day ->
                    val isDone = index < streakInfo.thisWeekWorkoutsCount
                    val isToday = index == currentDayOfWeek

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    when {
                                        isDone -> AccentMuted
                                        isToday -> SurfaceLightDark
                                        else -> SurfaceLightDark.copy(alpha = 0.5f)
                                    },
                                    CircleShape
                                )
                                .then(
                                    if (isToday && !isDone) {
                                        Modifier.border(1.dp, AccentMuted, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = AccentOnVolt,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AccentMuted, CircleShape)
                                )
                            }
                        }

                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimal Volume Bar Chart: flat card with 1dp border and 8dp corners
 */
@Composable
fun VolumeChart(
    volumeData: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (volumeData.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(SurfaceDark, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Complete workouts to see volume", color = TextMuted, fontSize = 12.sp)
        }
        return
    }

    val maxVolume = max(volumeData.maxOfOrNull { it.second } ?: 1.0, 1.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent volume",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Peak: ${"%,.0f".format(maxVolume)} kg",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = volumeData.size
                    val spacing = size.width / (count * 2)
                    val barWidth = spacing * 1.1f

                    volumeData.forEachIndexed { index, pair ->
                        val normalizedHeight = ((pair.second / maxVolume) * (size.height * 0.9f)).toFloat()
                        val x = spacing + (index * (spacing * 2))
                        val y = size.height - normalizedHeight

                        // Background slot
                        drawRoundRect(
                            color = SurfaceLightDark,
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )

                        // Value bar (muted accent)
                        drawRoundRect(
                            color = AccentMuted,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, normalizedHeight.coerceAtLeast(3f)),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                volumeData.forEach {
                    Text(text = it.first, fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}

/**
 * Minimal Weight Trend Line Chart: flat card with 1dp border and 8dp corners
 */
@Composable
fun WeightTrendChart(
    logs: List<WeightLogEntity>,
    targetWeight: Double,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(SurfaceDark, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Log your weight to view trend", color = TextMuted, fontSize = 12.sp)
        }
        return
    }

    val weights = logs.map { it.weight }
    val minVal = (weights.minOrNull() ?: targetWeight).coerceAtMost(targetWeight) - 1.5
    val maxVal = (weights.maxOrNull() ?: targetWeight).coerceAtLeast(targetWeight) + 1.5
    val range = (maxVal - minVal).coerceAtLeast(1.0)
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weight trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Target: $targetWeight $unit",
                    fontSize = 11.sp,
                    color = AccentMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Dashed Target line
                    val targetY = height * (1f - ((targetWeight - minVal) / range).toFloat()).coerceIn(0f, height)
                    drawLine(
                        color = TextMuted.copy(alpha = 0.5f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    if (logs.size >= 2) {
                        val path = Path()
                        val stepX = width / (logs.size - 1)
                        val points = logs.mapIndexed { index, item ->
                            val x = index * stepX
                            val y = (height * (1f - ((item.weight - minVal) / range).toFloat())).coerceIn(0f, height)
                            Offset(x, y)
                        }

                        path.moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            path.lineTo(points[i].x, points[i].y)
                        }

                        drawPath(
                            path = path,
                            color = AccentMuted,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        points.forEach { pt ->
                            drawCircle(color = BgDark, radius = 4.dp.toPx(), center = pt)
                            drawCircle(color = AccentMuted, radius = 2.5.dp.toPx(), center = pt)
                        }
                    } else if (logs.size == 1) {
                        val pt = Offset(width / 2, height * (1f - ((logs.first().weight - minVal) / range).toFloat()))
                        drawCircle(color = AccentMuted, radius = 4.dp.toPx(), center = pt)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = dateFormat.format(Date(logs.first().date)), fontSize = 10.sp, color = TextMuted)
                if (logs.size > 1) {
                    Text(text = dateFormat.format(Date(logs.last().date)), fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}
