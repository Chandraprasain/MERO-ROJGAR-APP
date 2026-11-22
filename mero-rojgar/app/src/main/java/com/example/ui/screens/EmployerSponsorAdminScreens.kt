package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.GeneratedJobDraft
import com.example.domain.MatchEngine
import com.example.i18n.AppLocale
import com.example.i18n.AppStrings
import com.example.i18n.formatNpr
import com.example.i18n.formatPaisaAsNpr
import com.example.model.EmployerApplicant
import com.example.model.JobListing
import com.example.model.SponsorCampaign
import com.example.model.TalentDirectoryEntry
import com.example.model.WithdrawalRequest
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.ScamRiskChip
import com.example.ui.components.SponsorDisclosureBar
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.SaffronGold
import com.example.viewmodel.AiCallState
import com.example.viewmodel.UiState

@Composable
private fun WorkspaceRoleHeader(
    title: String,
    subtitle: String,
    locale: AppLocale,
    onBackToSeeker: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            TextButton(
                onClick = onBackToSeeker,
                modifier = Modifier.testTag("switch_to_seeker_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (locale == AppLocale.NE) "खोजकर्ता मोड" else "Seeker")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployerWorkspaceScreen(
    locale: AppLocale,
    strings: AppStrings,
    employerJobsState: UiState<List<JobListing>>,
    employerApplicantsState: UiState<List<EmployerApplicant>>,
    talentDirectoryState: UiState<List<TalentDirectoryEntry>>,
    jdDraftState: AiCallState<GeneratedJobDraft>,
    onGenerateJobDraft: (String, String, String, String) -> Unit,
    onResetJobDraft: () -> Unit,
    onPostJob: (String, String, String, String, String, String, String, String, Long, Long, List<String>, Boolean) -> Unit,
    onUpdateApplicantStage: (String, String, String) -> Unit,
    onSwitchToSeeker: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onSwitchToSeeker() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Dashboard/Post, 1=ATS, 2=Talent
    var editingApplicant by remember { mutableStateOf<EmployerApplicant?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Post Job") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("ATS Pipeline") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Talent") })
        }

        when (selectedTab) {
            0 -> EmployerPostAndJobsTab(
                locale = locale,
                strings = strings,
                employerJobsState = employerJobsState,
                employerApplicantsState = employerApplicantsState,
                jdDraftState = jdDraftState,
                onGenerateJobDraft = onGenerateJobDraft,
                onResetJobDraft = onResetJobDraft,
                onPostJob = onPostJob,
                onSwitchToSeeker = onSwitchToSeeker
            )
            1 -> EmployerAtsTab(
                locale = locale,
                strings = strings,
                employerApplicantsState = employerApplicantsState,
                onSelectApplicant = { editingApplicant = it },
                onSwitchToSeeker = onSwitchToSeeker
            )
            else -> EmployerTalentTab(
                locale = locale,
                strings = strings,
                talentDirectoryState = talentDirectoryState,
                onSwitchToSeeker = onSwitchToSeeker
            )
        }
    }

    editingApplicant?.let { applicant ->
        ApplicantStageDialog(
            applicant = applicant,
            onDismiss = { editingApplicant = null },
            onSave = { stage, notes ->
                onUpdateApplicantStage(applicant.id, stage, notes)
                editingApplicant = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployerPostAndJobsTab(
    locale: AppLocale,
    strings: AppStrings,
    employerJobsState: UiState<List<JobListing>>,
    employerApplicantsState: UiState<List<EmployerApplicant>>,
    jdDraftState: AiCallState<GeneratedJobDraft>,
    onGenerateJobDraft: (String, String, String, String) -> Unit,
    onResetJobDraft: () -> Unit,
    onPostJob: (String, String, String, String, String, String, String, String, Long, Long, List<String>, Boolean) -> Unit,
    onSwitchToSeeker: () -> Unit
) {
    var orgName by remember { mutableStateOf("Everest Builders Pvt. Ltd.") }
    var orgPan by remember { mutableStateOf("604129880") }
    var title by remember { mutableStateOf("") }
    var titleNe by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Kathmandu") }
    var workMode by remember { mutableStateOf("onsite") }
    var employmentType by remember { mutableStateOf("full_time") }
    var salaryMinText by remember { mutableStateOf("45000") }
    var salaryMaxText by remember { mutableStateOf("75000") }
    var skillsCsv by remember { mutableStateOf("AutoCAD, Site Supervision, Safety") }
    var urgent by remember { mutableStateOf(false) }

    val jobsCount = (employerJobsState as? UiState.Success)?.data?.size ?: 0
    val applicants = (employerApplicantsState as? UiState.Success)?.data.orEmpty()
    val shortlistedCount = applicants.count { it.stage in listOf("shortlisted", "interview", "offer") }

    val (liveRiskScore, liveRiskReason) = remember(title, description, orgPan, salaryMinText, salaryMaxText) {
        MatchEngine.evaluateScamRisk(
            title = title,
            description = description,
            orgPan = orgPan,
            salaryMin = salaryMinText.toLongOrNull() ?: 0L,
            salaryMax = salaryMaxText.toLongOrNull() ?: 0L
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WorkspaceRoleHeader(
                title = strings.employerDashboard,
                subtitle = "Ethical ATS • Transparent NPR salary • Scam Risk Pre-Check",
                locale = locale,
                onBackToSeeker = onSwitchToSeeker
            )
        }

        // KPI Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMiniCard("My Jobs", jobsCount.toString(), Modifier.weight(1f))
                KpiMiniCard("Applicants", applicants.size.toString(), Modifier.weight(1f))
                KpiMiniCard("Shortlisted", shortlistedCount.toString(), Modifier.weight(1f))
            }
        }

        // Post Job & AI JD Studio
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AddBusiness, contentDescription = null, tint = HimalayanNavy)
                        Text(strings.postJobTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = { Text("Organization Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = orgPan,
                            onValueChange = { orgPan = it },
                            label = { Text("PAN / Reg No.") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Job Title (English)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("employer_job_title_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("onsite", "hybrid", "remote").forEach { mode ->
                            FilterChip(
                                selected = workMode == mode,
                                onClick = { workMode = mode },
                                label = { Text(mode.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val roleForAi = title.ifBlank { "Junior Accountant" }
                            onGenerateJobDraft(roleForAi, orgName, district, workMode)
                        },
                        enabled = jdDraftState !is AiCallState.Loading,
                        modifier = Modifier.fillMaxWidth().testTag("employer_ai_jd_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI JD Writer (Bias-Free Draft)")
                    }

                    when (jdDraftState) {
                        is AiCallState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        is AiCallState.Error -> ErrorBannerCard(jdDraftState.message)
                        is AiCallState.Success -> {
                            val draft = jdDraftState.data
                            Surface(
                                color = EmeraldContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("AI JD Proposal Ready", fontWeight = FontWeight.Bold)
                                    Text("${draft.titleEn} / ${draft.titleNe}", style = MaterialTheme.typography.bodySmall)
                                    Text(draft.description, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = onResetJobDraft) { Text("Dismiss") }
                                        Button(
                                            onClick = {
                                                title = draft.titleEn
                                                titleNe = draft.titleNe
                                                description = draft.description
                                                if (draft.suggestedSkills.isNotEmpty()) {
                                                    skillsCsv = draft.suggestedSkills.joinToString(", ")
                                                }
                                                onResetJobDraft()
                                            }
                                        ) {
                                            Text("Apply Draft")
                                        }
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }

                    OutlinedTextField(
                        value = titleNe,
                        onValueChange = { titleNe = it },
                        label = { Text("नेपाली पद शीर्षक (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Job Description (min 10 chars, transparent, no fee)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("employer_job_desc_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = salaryMinText,
                            onValueChange = { salaryMinText = it.filter(Char::isDigit) },
                            label = { Text("Min NPR") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salaryMaxText,
                            onValueChange = { salaryMaxText = it.filter(Char::isDigit) },
                            label = { Text("Max NPR") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = skillsCsv,
                        onValueChange = { skillsCsv = it },
                        label = { Text("Required Skills (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Urgent Hiring Badge", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = urgent, onCheckedChange = { urgent = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scam Pre-Check:", style = MaterialTheme.typography.labelMedium)
                        ScamRiskChip(riskScore = liveRiskScore, reason = liveRiskReason)
                    }

                    Button(
                        onClick = {
                            val minSal = salaryMinText.toLongOrNull() ?: 30000L
                            val maxSal = (salaryMaxText.toLongOrNull() ?: minSal).coerceAtLeast(minSal)
                            val skills = skillsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onPostJob(
                                orgName,
                                orgPan,
                                title,
                                titleNe.ifBlank { title },
                                description,
                                employmentType,
                                workMode,
                                district,
                                minSal,
                                maxSal,
                                skills,
                                urgent
                            )
                            title = ""
                            description = ""
                        },
                        enabled = orgName.isNotBlank() && title.length >= 2 && description.length >= 10,
                        modifier = Modifier.fillMaxWidth().testTag("employer_publish_job_button")
                    ) {
                        Text("Publish Job Listing")
                    }
                }
            }
        }

        item {
            Text("My Posted Jobs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        when (employerJobsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { ErrorBannerCard(employerJobsState.message) }
            is UiState.Success -> {
                if (employerJobsState.data.isEmpty()) {
                    item { EmptyStateCard("No Jobs Posted Yet", "Use the form above to publish a transparent job.") }
                } else {
                    items(employerJobsState.data, key = { it.id }) { job ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(job.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    AssistChip(onClick = {}, label = { Text(job.status.uppercase()) })
                                }
                                Text("${job.district} • ${formatNpr(job.salaryMin, locale)}–${formatNpr(job.salaryMax, locale)}", style = MaterialTheme.typography.bodySmall)
                                ScamRiskChip(riskScore = job.scamRiskScore, reason = job.scamRiskReason)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerAtsTab(
    locale: AppLocale,
    strings: AppStrings,
    employerApplicantsState: UiState<List<EmployerApplicant>>,
    onSelectApplicant: (EmployerApplicant) -> Unit,
    onSwitchToSeeker: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkspaceRoleHeader(
                title = strings.atsPipelineTitle,
                subtitle = "Advisory match reasons • Tap a candidate to advance or add notes",
                locale = locale,
                onBackToSeeker = onSwitchToSeeker
            )
        }
        item {
            SponsorDisclosureBar(text = strings.aiAdvisoryNotice)
        }

        when (employerApplicantsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { ErrorBannerCard(employerApplicantsState.message) }
            is UiState.Success -> {
                if (employerApplicantsState.data.isEmpty()) {
                    item { EmptyStateCard("No Applicants Yet", "Candidates applying to your jobs will appear in this ATS pipeline.") }
                } else {
                    items(employerApplicantsState.data, key = { it.id }) { app ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.applicantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(app.applicantHeadline.ifBlank { app.jobTitle }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(color = EmeraldContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "${app.matchScore}% match",
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text("Job: ${app.jobTitle}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                if (app.matchReasons.isNotBlank()) {
                                    Text(app.matchReasons, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                if (app.notes.isNotBlank()) {
                                    Text("Notes: ${app.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssistChip(onClick = {}, label = { Text("Stage: ${app.stage.uppercase()}") })
                                    Button(
                                        onClick = { onSelectApplicant(app) },
                                        modifier = Modifier.testTag("ats_manage_${app.id}")
                                    ) {
                                        Text("Move Stage")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployerTalentTab(
    locale: AppLocale,
    strings: AppStrings,
    talentDirectoryState: UiState<List<TalentDirectoryEntry>>,
    onSwitchToSeeker: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkspaceRoleHeader(
                title = strings.talentSearchTitle,
                subtitle = "Opt-in candidates only • Contact details remain hidden until approved",
                locale = locale,
                onBackToSeeker = onSwitchToSeeker
            )
        }

        when (talentDirectoryState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { ErrorBannerCard(talentDirectoryState.message) }
            is UiState.Success -> {
                if (talentDirectoryState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Opt-In Profiles Visible Yet",
                            subtitle = "Job seekers can toggle Recruiter Discovery in Profile & Privacy to appear here."
                        )
                    }
                } else {
                    items(talentDirectoryState.data, key = { it.id }) { entry ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.People, contentDescription = null, tint = HimalayanNavy)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.displayName, fontWeight = FontWeight.Bold)
                                        Text("${entry.headline} • ${entry.district}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    VerificationBadge(level = entry.verificationLevel, detailNote = strings.verificationAdvisory)
                                }
                                if (entry.skills.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        entry.skills.take(6).forEach { skill ->
                                            AssistChip(onClick = {}, label = { Text(skill) })
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplicantStageDialog(
    applicant: EmployerApplicant,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedStage by remember { mutableStateOf(applicant.stage) }
    var notes by remember { mutableStateOf(applicant.notes) }
    val stages = listOf("new", "under_review", "shortlisted", "assessment", "interview", "offer", "hired", "rejected")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ATS Stage: ${applicant.applicantName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    stages.forEach { st ->
                        FilterChip(
                            selected = selectedStage == st,
                            onClick = { selectedStage = st },
                            label = { Text(st) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Recruiter Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedStage, notes) }) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SponsorWorkspaceScreen(
    locale: AppLocale,
    strings: AppStrings,
    campaignsState: UiState<List<SponsorCampaign>>,
    onCreateCampaign: (String, String, Long, Long, Long, String, List<String>) -> Unit,
    onSwitchToSeeker: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onSwitchToSeeker() }

    var sponsorName by remember { mutableStateOf("Nepal Digital Skill Fund") }
    var title by remember { mutableStateOf("") }
    var videoSecondsText by remember { mutableStateOf("15") }
    var rewardNprText by remember { mutableStateOf("50") }
    var fundedNprText by remember { mutableStateOf("5000") }
    var questionPrompt by remember { mutableStateOf("What is the main safety takeaway of this campaign?") }
    var optionA by remember { mutableStateOf("Verify PAN & avoid upfront fees") }
    var optionB by remember { mutableStateOf("Share bank OTP with strangers") }

    val campaigns = (campaignsState as? UiState.Success)?.data.orEmpty()
    val totalFundedPaisa = campaigns.sumOf { it.fundedPaisa }
    val totalSpentPaisa = campaigns.sumOf { it.spentPaisa }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WorkspaceRoleHeader(
                title = strings.sponsorConsoleTitle,
                subtitle = "Sponsor-funded educational & product awareness tasks • Fixed budget cap",
                locale = locale,
                onBackToSeeker = onSwitchToSeeker
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMiniCard("Total Funded", formatPaisaAsNpr(totalFundedPaisa, locale), Modifier.weight(1f))
                KpiMiniCard("Spent", formatPaisaAsNpr(totalSpentPaisa, locale), Modifier.weight(1f))
                KpiMiniCard("Live", campaigns.size.toString(), Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = SaffronGold)
                        Text("Create Funded Reward Campaign", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = sponsorName,
                        onValueChange = { sponsorName = it },
                        label = { Text("Sponsor Organization") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Campaign Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sponsor_title_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = videoSecondsText,
                            onValueChange = { videoSecondsText = it.filter(Char::isDigit) },
                            label = { Text("Seconds (5-300)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rewardNprText,
                            onValueChange = { rewardNprText = it.filter(Char::isDigit) },
                            label = { Text("Reward NPR") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fundedNprText,
                            onValueChange = { fundedNprText = it.filter(Char::isDigit) },
                            label = { Text("Pool NPR") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = questionPrompt,
                        onValueChange = { questionPrompt = it },
                        label = { Text("Comprehension Check Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = optionA,
                        onValueChange = { optionA = it },
                        label = { Text("Correct Answer Option") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = optionB,
                        onValueChange = { optionB = it },
                        label = { Text("Distractor Option") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val sec = (videoSecondsText.toLongOrNull() ?: 15L).coerceIn(5L, 300L)
                            val rewardPaisa = ((rewardNprText.toLongOrNull() ?: 25L).coerceIn(5L, 5000L)) * 100L
                            val fundedPaisa = ((fundedNprText.toLongOrNull() ?: 1000L) * 100L).coerceAtLeast(rewardPaisa)
                            onCreateCampaign(
                                sponsorName,
                                title,
                                sec,
                                rewardPaisa,
                                fundedPaisa,
                                questionPrompt,
                                listOf(optionA, optionB)
                            )
                            title = ""
                        },
                        enabled = sponsorName.isNotBlank() && title.length >= 2 && questionPrompt.length >= 5 &&
                            optionA.isNotBlank() && optionB.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("sponsor_launch_button")
                    ) {
                        Text("Fund & Launch Campaign")
                    }
                }
            }
        }

        items(campaigns, key = { it.id }) { camp ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(camp.title, fontWeight = FontWeight.Bold)
                    Text("${camp.sponsorName} • Reward ${formatPaisaAsNpr(camp.rewardPaisa, locale)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Remaining Budget: ${formatPaisaAsNpr(camp.remainingPaisa, locale)} / ${formatPaisaAsNpr(camp.fundedPaisa, locale)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
}

@Composable
fun AdminWorkspaceScreen(
    locale: AppLocale,
    strings: AppStrings,
    employerJobsState: UiState<List<JobListing>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    onSwitchToSeeker: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onSwitchToSeeker() }

    val jobs = (employerJobsState as? UiState.Success)?.data.orEmpty()
    val flaggedJobs = jobs.filter { it.scamRiskScore >= 0.20 || it.status == "pending_review" }
    val withdrawals = (withdrawalsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WorkspaceRoleHeader(
                title = strings.adminConsoleTitle,
                subtitle = "Zero-trust verification • Scam Risk Triage • Two-step Payout Governance",
                locale = locale,
                onBackToSeeker = onSwitchToSeeker
            )
        }

        item {
            SponsorDisclosureBar(
                text = "Governance policy: Withdrawals >= NPR 2,000 require dual admin approval & KYC verification. Self-reported claims are never auto-upgraded to Verified."
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = HimalayanNavy)
                        Text("Safety & Verification Rules Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("• Immutable double-entry ledger enforced in Firestore Security Rules", style = MaterialTheme.typography.bodySmall)
                    Text("• Scam-risk jobs (score >= 0.45) auto-routed to pending_review queue", style = MaterialTheme.typography.bodySmall)
                    Text("• Protected attributes excluded from all candidate match weights", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Text("Scam Risk / Pending Job Queue (${flaggedJobs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (flaggedJobs.isEmpty()) {
            item { EmptyStateCard("Queue Clear", "No pending-review or medium/high risk job listings in your scope.") }
        } else {
            items(flaggedJobs, key = { it.id }) { job ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(job.title, fontWeight = FontWeight.Bold)
                        Text("${job.orgName} • Status: ${job.status}", style = MaterialTheme.typography.bodySmall)
                        ScamRiskChip(riskScore = job.scamRiskScore, reason = job.scamRiskReason)
                    }
                }
            }
        }

        item {
            Text("Withdrawal Review Queue (${withdrawals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (withdrawals.isEmpty()) {
            item { EmptyStateCard("No Withdrawals Pending", "Payout requests created from Mero Rewards appear here for settlement audit.") }
        } else {
            items(withdrawals, key = { it.id }) { req ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${req.method.uppercase()} • ${req.destinationMasked}", fontWeight = FontWeight.Bold)
                            Text("Amount: ${formatPaisaAsNpr(req.amountPaisa, locale)}", style = MaterialTheme.typography.bodySmall)
                        }
                        AssistChip(onClick = {}, label = { Text(req.status.uppercase()) })
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HimalayanNavy)
        }
    }
}
