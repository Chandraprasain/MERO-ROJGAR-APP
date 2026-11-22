package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLocale
import com.example.i18n.AppStrings
import com.example.i18n.formatNpr
import com.example.model.GigListing
import com.example.model.SkillEvidence
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.SponsorDisclosureBar
import com.example.ui.components.VerificationBadge
import com.example.viewmodel.UiState

@Composable
fun GigsAndPassportScreen(
    locale: AppLocale,
    strings: AppStrings,
    posterName: String,
    gigsState: UiState<List<GigListing>>,
    skillEvidenceState: UiState<List<SkillEvidence>>,
    openPassportOnStart: Boolean,
    onClearPassportFlag: () -> Unit,
    onPostGig: (String, String, String, String, Long, Boolean, Long, String) -> Unit,
    onAddSkillEvidence: (String, Long, String, String, String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Gigs & Local Trades, 1 = Skill Passport
    var tradeFilter by remember { mutableStateOf("all") } // all, local, digital
    var showPostGigDialog by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openPassportOnStart) {
        if (openPassportOnStart) {
            selectedTab = 1
            onClearPassportFlag()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showPostGigDialog = true else showAddSkillDialog = true
                },
                modifier = Modifier.testTag("gigs_or_skill_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = if (selectedTab == 0) "Post Gig" else "Add Skill")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.gigsTitle) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.skillPassportTitle) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SponsorDisclosureBar(text = strings.gigPaymentDisclosure)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = tradeFilter == "all",
                                onClick = { tradeFilter = "all" },
                                label = { Text("All") }
                            )
                            FilterChip(
                                selected = tradeFilter == "local",
                                onClick = { tradeFilter = "local" },
                                leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Local Trades") }
                            )
                            FilterChip(
                                selected = tradeFilter == "digital",
                                onClick = { tradeFilter = "digital" },
                                leadingIcon = { Icon(Icons.Filled.Computer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Digital") }
                            )
                        }
                    }

                    when (gigsState) {
                        is UiState.Loading -> item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                        is UiState.Error -> item { ErrorBannerCard(gigsState.message) }
                        is UiState.Success -> {
                            val list = gigsState.data.filter {
                                when (tradeFilter) {
                                    "local" -> it.localTrade
                                    "digital" -> !it.localTrade
                                    else -> true
                                }
                            }
                            if (list.isEmpty()) {
                                item { EmptyStateCard("No gigs found", "Tap + to post a freelance or local skilled-worker gig.") }
                            } else {
                                items(list, key = { it.id }) { gig ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(gig.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = formatNpr(gig.budgetNpr, locale),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${gig.category} • ${gig.district} • ${gig.milestoneCount} Milestones",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(gig.description, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = "Posted by ${gig.posterName} • Licensed Partner Ref Mode",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Skill Passport
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SponsorDisclosureBar(text = strings.verificationAdvisory)
                    }
                    when (skillEvidenceState) {
                        is UiState.Loading -> item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                        is UiState.Error -> item { ErrorBannerCard(skillEvidenceState.message) }
                        is UiState.Success -> {
                            if (skillEvidenceState.data.isEmpty()) {
                                item { EmptyStateCard("Skill Passport Empty", "Add a skill claim or assessment evidence.") }
                            } else {
                                items(skillEvidenceState.data, key = { it.id }) { ev ->
                                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    Text(ev.skillName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                }
                                                Text("Level ${ev.level}/5", style = MaterialTheme.typography.labelLarge)
                                            }
                                            VerificationBadge(
                                                level = ev.verificationLevel,
                                                detailNote = "Provider: ${ev.provider}\nMethod: ${ev.method}\nScore: ${ev.scorePercent}%"
                                            )
                                            Text(
                                                text = "Provider: ${ev.provider} • Method: ${ev.method}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPostGigDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Electrician / Solar") }
        var district by remember { mutableStateOf("Kathmandu") }
        var budget by remember { mutableStateOf("5000") }
        var localTrade by remember { mutableStateOf(true) }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPostGigDialog = false },
            title = { Text("Post Gig / Local Trade Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Gig Title") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") })
                    OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Budget (NPR)") })
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Local On-Site Skilled Trade")
                        Switch(checked = localTrade, onCheckedChange = { localTrade = it })
                    }
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Scope & Milestones") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPostGig(
                            posterName,
                            title,
                            category,
                            district,
                            budget.toLongOrNull() ?: 5000L,
                            localTrade,
                            2L,
                            desc.ifBlank { "Milestone delivery via licensed payment partner." }
                        )
                        showPostGigDialog = false
                    },
                    enabled = title.trim().length >= 2
                ) {
                    Text("Publish Gig")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostGigDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddSkillDialog) {
        var skillName by remember { mutableStateOf("") }
        var level by remember { mutableStateOf("4") }
        AlertDialog(
            onDismissRequest = { showAddSkillDialog = false },
            title = { Text("Add Skill Claim (Self-Reported)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Note: Newly added candidate claims are labeled Self-Reported until verified by an employer, document, or assessment.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(value = skillName, onValueChange = { skillName = it }, label = { Text("Skill Name") })
                    OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("Proficiency (1-5)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSkillEvidence(
                            skillName,
                            (level.toLongOrNull() ?: 3L).coerceIn(1L, 5L),
                            "Candidate Declaration",
                            "Self-Reported (Pending Verification)",
                            "self_reported",
                            0L
                        )
                        showAddSkillDialog = false
                    },
                    enabled = skillName.isNotBlank()
                ) {
                    Text("Add to Passport")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSkillDialog = false }) { Text("Cancel") }
            }
        )
    }
}
