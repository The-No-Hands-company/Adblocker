package com.tnhc.shield.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tnhc.shield.data.db.dao.HourlyCount
import com.tnhc.shield.ui.MainViewModel
import com.tnhc.shield.ui.theme.ShieldRed

@Composable
fun DashboardScreen(viewModel: MainViewModel = hiltViewModel()) {
    val blockedToday by viewModel.blockedToday.collectAsStateWithLifecycle()
    val blockedAllTime by viewModel.blockedAllTime.collectAsStateWithLifecycle()
    val topDomains by viewModel.topBlockedDomains.collectAsStateWithLifecycle(emptyList())
    val hourlyBlocks by viewModel.hourlyBlocks.collectAsStateWithLifecycle()
    val totalRules by viewModel.totalRules.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Activity",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Blocked Today",
                    value = formatCount(blockedToday),
                    icon = Icons.Filled.Block,
                    color = ShieldRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "All Time",
                    value = formatCount(blockedAllTime),
                    icon = Icons.Filled.Shield,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Rules",
                    value = formatCount(totalRules.toLong()),
                    icon = Icons.Filled.FilterList,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ChartCard(hourlyData = hourlyBlocks)
        }

        item {
            Text(
                "Top Blocked Domains",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (topDomains.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No blocks recorded yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(topDomains) { domainCount ->
                DomainCountItem(
                    domain = domainCount.domain,
                    count = domainCount.count,
                    total = blockedAllTime
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(hourlyData: List<HourlyCount>) {
    val barColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.surfaceVariant

    val currentHour = remember { System.currentTimeMillis() / 3600000L }
    val counts = remember(hourlyData, currentHour) {
        (0..23).map { offset ->
            val h = currentHour - 23 + offset
            hourlyData.find { it.hour == h }?.count ?: 0
        }
    }
    val maxCount = remember(counts) { counts.maxOrNull()?.coerceAtLeast(1) ?: 1 }
    val hasData = counts.any { it > 0 }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Last 24 Hours",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (hasData) {
                    Text(
                        "peak ${counts.max()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (!hasData) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data yet — blocks appear here in real time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    val totalBars = 24
                    val gap = 2.dp.toPx()
                    val barW = (size.width - gap * (totalBars - 1)) / totalBars

                    counts.forEachIndexed { i, count ->
                        val barH = if (count > 0) (count.toFloat() / maxCount) * size.height else 4.dp.toPx()
                        val x = i * (barW + gap)
                        val y = size.height - barH
                        drawRoundRect(
                            color = if (count > 0) barColor else dimColor,
                            topLeft = Offset(x, y),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("24h ago", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("now", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DomainCountItem(domain: String, count: Int, total: Long) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                domain,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                formatCount(count.toLong()),
                style = MaterialTheme.typography.labelLarge,
                color = ShieldRed,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 4.dp),
            color = ShieldRed.copy(alpha = 0.5f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(10.dp))
    }
}

private fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK".format(n / 1_000.0)
    else -> n.toString()
}
