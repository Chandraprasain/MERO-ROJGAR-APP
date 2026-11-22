package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.VoiceSearchFilters
import com.example.i18n.AppLocale
import com.example.i18n.AppStrings
import com.example.model.AutoApplyRule
import com.example.model.JobApplication
import com.example.model.JobListing
import com.example.model.ResumeVersion
import com.example.model.SavedJob
import com.example.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.viewmodel.AiCallState
import com.example.viewmodel.UiState

@Composable
fun JobsScreen(
    locale: AppLocale,
    strings: AppStrings,
    profile: UserProfile?,
    jobsState: UiState<List<JobListing>>,
    isRefreshingJobs: Boolean,
    applicationsState: UiState<List<JobApplication>>,
    savedJobsState: UiState<List<SavedJob>>,
    resumesState: UiState<List<ResumeVersion>>,
    autoApplyRulesState: UiState<List<AutoApplyRule>>,
    voiceIntentState: AiCallState<VoiceSearchFilters>,
    initialSelectedJob: JobListing?,
    openAutoApplyOnStart: Boolean,
    onClearInitialJob: () -> Unit,
    onClearAutoApplyFlag: () -> Unit,
    onRefreshJobs: () -> Unit,
    onToggleSavedJob: (JobListing, Boolean) -> Unit,
    onParseVoiceQuery: (String) -> Unit,
    onClearVoiceIntent: () -> Unit,
    onApplyToJob: (JobListing, String, String) -> Unit,
    onWithdrawApplication: (String) -> Unit,
    onSaveAutoApplyRule: (String, String, String, String, Long, Long, Long, String, String) -> Unit,
    onToggleAutoApplyRule: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=JobFeed, 1=Applications, 2=Auto Apply

    LaunchedEffect(openAutoApplyOnStart) {
        if (openAutoApplyOnStart) {
            selectedTab = 2
            onClearAutoApplyFlag()
        }
    }

    val resumes = (resumesState as? UiState.Success)?.data.orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (locale == AppLocale.NE) "जागिर फिड" else "JobFeed") },
                modifier = Modifier.testTag("jobs_tab_explore")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (locale == AppLocale.NE) "मेरा आवेदन" else "Applications") },
                modifier = Modifier.testTag("jobs_tab_applications")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Auto Apply") },
                modifier = Modifier.testTag("jobs_tab_auto_apply")
            )
        }

        when (selectedTab) {
            0 -> {
                JobFeedScreen(
                    locale = locale,
                    strings = strings,
                    profile = profile,
                    jobsState = jobsState,
                    isRefreshing = isRefreshingJobs,
                    applicationsState = applicationsState,
                    savedJobsState = savedJobsState,
                    resumesState = resumesState,
                    voiceIntentState = voiceIntentState,
                    initialSelectedJob = initialSelectedJob,
                    onClearInitialJob = onClearInitialJob,
                    onRefreshJobs = onRefreshJobs,
                    onToggleSavedJob = onToggleSavedJob,
                    onParseVoiceQuery = onParseVoiceQuery,
                    onClearVoiceIntent = onClearVoiceIntent,
                    onApplyToJob = onApplyToJob,
                    onOpenApplicationsTab = { selectedTab = 1 },
                    modifier = Modifier.fillMaxSize()
                )
            }

            1 -> {
                when (applicationsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorBannerCard(applicationsState.message) }
                    is UiState.Success -> {
                        if (applicationsState.data.isEmpty()) {
                            Box(Modifier.padding(16.dp)) {
                                EmptyStateCard(
                                    title = "No applications yet",
                                    subtitle = "Explore verified jobs in JobFeed and apply in one tap with your chosen Resume version."
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(applicationsState.data, key = { it.id }) { app ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(app.jobTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = app.stage.replace("_", " ").uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${app.orgName} • Source: ${app.source} • Match ${app.matchScore}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (app.matchSummary.isNotBlank()) {
                                                Text(app.matchSummary, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (app.stage != "withdrawn") {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                    OutlinedButton(onClick = { onWithdrawApplication(app.id) }) {
                                                        Text("Withdraw")
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
            }

            2 -> {
                AutoApplySection(
                    rulesState = autoApplyRulesState,
                    resumes = resumes,
                    onSaveRule = onSaveAutoApplyRule,
                    onToggleRule = onToggleAutoApplyRule
                )
            }
        }
    }
}

@Composable
private fun AutoApplySection(
    rulesState: UiState<List<AutoApplyRule>>,
    resumes: List<ResumeVersion>,
    onSaveRule: (String, String, String, String, Long, Long, Long, String, String) -> Unit,
    onToggleRule: (String, Boolean) -> Unit
) {
    var ruleName by remember { mutableStateOf("Kathmandu Civil & Tech Rule") }
    var keywords by remember { mutableStateOf("Engineer, Kotlin, Supervisor") }
    var district by remember { mutableStateOf("Kathmandu") }
    var workMode by remember { mutableStateOf("any") }
    var minSalary by remember { mutableStateOf("40000") }
    var minMatch by remember { mutableFloatStateOf(75f) }
    var dailyCap by remember { mutableFloatStateOf(5f) }
    var excludedOrgs by remember { mutableStateOf("") }
    var approvedResumeId by remember(resumes) { mutableStateOf(resumes.firstOrNull()?.id ?: "resume_v1") }
    var showConfirmSummary by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Auto Apply Rule Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "You stay in full control: every rule requires a plain-language confirmation, a daily cap, and an explicitly approved Resume version.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = ruleName, onValueChange = { ruleName = it }, label = { Text("Rule Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = keywords, onValueChange = { keywords = it }, label = { Text("Keywords (comma-separated)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = minSalary, onValueChange = { minSalary = it }, label = { Text("Min NPR") }, modifier = Modifier.weight(1f))
                    }
                    Text("Minimum Match Score: ${minMatch.toInt()}% • Daily Cap: ${dailyCap.toInt()} jobs/day", style = MaterialTheme.typography.labelMedium)
                    Slider(value = minMatch, onValueChange = { minMatch = it }, valueRange = 50f..95f)
                    Slider(value = dailyCap, onValueChange = { dailyCap = it }, valueRange = 1f..15f, steps = 13)
                    OutlinedTextField(value = excludedOrgs, onValueChange = { excludedOrgs = it }, label = { Text("Excluded Companies (Optional)") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = { showConfirmSummary = true },
                        modifier = Modifier.fillMaxWidth().testTag("auto_apply_review_button")
                    ) {
                        Text("Review Plain-Language Rule & Activate")
                    }
                }
            }
        }

        if (rulesState is UiState.Success && rulesState.data.isNotEmpty()) {
            item {
                Text("Saved Auto Apply Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(rulesState.data, key = { it.id }) { rule ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.ruleName, fontWeight = FontWeight.Bold)
                            Text(
                                "${rule.district} • Min ${rule.minMatchThreshold}% match • Cap ${rule.dailyCap}/day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rule.active,
                            onCheckedChange = { onToggleRule(rule.id, it) }
                        )
                    }
                }
            }
        }
    }

    if (showConfirmSummary) {
        val sal = minSalary.toLongOrNull() ?: 30000L
        AlertDialog(
            onDismissRequest = { showConfirmSummary = false },
            title = { Text("Confirm Auto Apply Rule") },
            text = {
                Text(
                    "Plain-language summary:\n\n" +
                        "Apply automatically up to ${dailyCap.toInt()} times per day to jobs in $district matching \"$keywords\" with salary >= NPR $sal and match score >= ${minMatch.toInt()}%, using Resume ID $approvedResumeId." +
                        if (excludedOrgs.isNotBlank()) "\nExcluded: $excludedOrgs" else ""
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSaveRule(ruleName, keywords, district, workMode, sal, minMatch.toLong(), dailyCap.toLong(), excludedOrgs, approvedResumeId)
                    showConfirmSummary = false
                }) {
                    Text("Activate Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSummary = false }) { Text("Cancel") }
            }
        )
    }
}
