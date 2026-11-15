package com.tnhc.shield.ui.screens.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tnhc.shield.data.db.entities.CustomRuleEntity
import com.tnhc.shield.data.db.entities.FilterListEntity
import com.tnhc.shield.model.FilterCategory
import com.tnhc.shield.ui.MainViewModel
import com.tnhc.shield.ui.theme.ShieldGreen
import com.tnhc.shield.ui.theme.ShieldRed
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FiltersScreen(viewModel: MainViewModel = hiltViewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Filter Lists") },
                icon = { Icon(Icons.Filled.FilterList, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Custom Rules") },
                icon = { Icon(Icons.Filled.Tune, contentDescription = null) }
            )
        }

        when (selectedTab) {
            0 -> FilterListsTab(viewModel)
            1 -> CustomRulesTab(viewModel)
        }
    }
}

@Composable
fun FilterListsTab(viewModel: MainViewModel) {
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
fun CustomRulesTab(viewModel: MainViewModel) {
    val rules by viewModel.customRules.collectAsStateWithLifecycle()
    var filterType by remember { mutableStateOf<Boolean?>(null) } // null = all, true = whitelist, false = blacklist
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredRules = rules.filter { rule ->
        (filterType == null || rule.isWhitelist == filterType) &&
            (searchQuery.isBlank() || rule.domain.contains(searchQuery.trim(), ignoreCase = true))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Custom Rule")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Custom Rules",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Explicitly allow or block domains regardless of subscribed filter lists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search custom domains...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { filterType = null },
                        label = { Text("All (${rules.size})") }
                    )
                    FilterChip(
                        selected = filterType == true,
                        onClick = { filterType = if (filterType == true) null else true },
                        label = { Text("Whitelist (${rules.count { it.isWhitelist }})") },
                        leadingIcon = {
                            Box(modifier = Modifier.size(8.dp).background(ShieldGreen, CircleShape))
                        }
                    )
                    FilterChip(
                        selected = filterType == false,
                        onClick = { filterType = if (filterType == false) null else false },
                        label = { Text("Blacklist (${rules.count { !it.isWhitelist }})") },
                        leadingIcon = {
                            Box(modifier = Modifier.size(8.dp).background(ShieldRed, CircleShape))
                        }
                    )
                }
            }

            if (filteredRules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "No matching rules" else "No custom rules yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredRules, key = { it.id }) { rule ->
                    CustomRuleCard(
                        rule = rule,
                        onDelete = { viewModel.deleteCustomRule(rule.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { domain, isWhitelist, notes ->
                viewModel.addCustomRule(domain, isWhitelist, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CustomRuleCard(rule: CustomRuleEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        rule.domain,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (rule.isWhitelist) ShieldGreen.copy(alpha = 0.15f) else ShieldRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (rule.isWhitelist) "Allow" else "Block",
                            color = if (rule.isWhitelist) ShieldGreen else ShieldRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!rule.notes.isNullOrBlank()) {
                    Text(
                        rule.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (domain: String, isWhitelist: Boolean, notes: String?) -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var isWhitelist by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = {
                        domain = it
                        hasError = false
                    },
                    label = { Text("Domain (e.g. ads.example.com)") },
                    isError = hasError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Action:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isWhitelist,
                            onClick = { isWhitelist = false }
                        )
                        Text("Block", modifier = Modifier.padding(start = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isWhitelist,
                            onClick = { isWhitelist = true }
                        )
                        Text("Whitelist (Allow)", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = domain.trim().lowercase()
                    if (clean.isBlank() || clean.contains(" ")) {
                        hasError = true
                    } else {
                        onAdd(clean, isWhitelist, notes.ifBlank { null })
                    }
                }
            ) {
                Text("Add Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
