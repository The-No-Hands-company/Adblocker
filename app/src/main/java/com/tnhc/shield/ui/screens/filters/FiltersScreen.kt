package com.tnhc.shield.ui.screens.filters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tnhc.shield.data.db.entities.FilterListEntity
import com.tnhc.shield.model.FilterCategory
import com.tnhc.shield.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FiltersScreen(viewModel: MainViewModel = hiltViewModel()) {
    val lists by viewModel.allFilterLists.collectAsStateWithLifecycle(emptyList())
    var selectedCategory by remember { mutableStateOf<FilterCategory?>(null) }

    val filtered = if (selectedCategory == null) lists
    else lists.filter { it.category == selectedCategory?.name }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filter Lists",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.refreshFilterLists() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Update all lists")
                }
            }
        }

        item {
            val enabledCount = lists.count { it.enabled }
            val totalRules = lists.sumOf { it.ruleCount }
            AssistChip(
                onClick = {},
                label = { Text("$enabledCount active · ${formatCount(totalRules)} rules total") },
                leadingIcon = { Icon(Icons.Filled.FilterList, null, modifier = Modifier.size(16.dp)) }
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                }
                items(FilterCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.displayName) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { list ->
            FilterListCard(
                list = list,
                onToggle = { enabled -> viewModel.setFilterEnabled(list.id, enabled) }
            )
        }
    }
}

@Composable
fun FilterListCard(list: FilterListEntity, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(list.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (list.isBuiltIn) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "Built-in",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Text(
                    list.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (list.ruleCount > 0) {
                    Text(
                        "${formatCount(list.ruleCount)} rules" +
                            if (list.lastUpdated > 0) " · ${formatDate(list.lastUpdated)}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = list.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.0fK".format(n / 1_000.0)
    else -> n.toString()
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
