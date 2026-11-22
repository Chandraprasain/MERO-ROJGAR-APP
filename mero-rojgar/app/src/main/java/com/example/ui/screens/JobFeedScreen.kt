package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VoiceSearchFilters
import com.example.domain.MatchEngine
import com.example.i18n.AppLocale
import com.example.i18n.AppStrings
import com.example.i18n.formatNpr
import com.example.model.JobApplication
import com.example.model.JobListing
import com.example.model.MatchBreakdown
import com.example.model.ResumeVersion
import com.example.model.SavedJob
import com.example.model.UserProfile
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.ExplainabilityPanel
import com.example.ui.components.JobFeedEmptyState
import com.example.ui.components.JobFeedQuickPreset
import com.example.ui.components.ScamRiskChip
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NepalCrimson
import com.example.viewmodel.AiCallState
import com.example.viewmodel.UiState

enum class JobFeedSort(val labelEn: String, val labelNe: String) {
    MATCH_URGENT("Best Match", "उत्तम म्याच"),
    NEWEST("Newest", "नयाँ पहिले"),
    SALARY_HIGH("Highest Salary", "उच्च तलब")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobFeedScreen(
    locale: AppLocale,
    strings: AppStrings,
    profile: UserProfile?,
    jobsState: UiState<List<JobListing>>,
    isRefreshing: Boolean,
    applicationsState: UiState<List<JobApplication>>,
    savedJobsState: UiState<List<SavedJob>>,
    resumesState: UiState<List<ResumeVersion>>,
    voiceIntentState: AiCallState<VoiceSearchFilters>,
    initialSelectedJob: JobListing?,
    onClearInitialJob: () -> Unit,
    onRefreshJobs: () -> Unit,
    onToggleSavedJob: (JobListing, Boolean) -> Unit,
    onParseVoiceQuery: (String) -> Unit,
    onClearVoiceIntent: () -> Unit,
    onApplyToJob: (JobListing, String, String) -> Unit,
    onOpenApplicationsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var districtFilter by remember { mutableStateOf("All") }
    var workModeFilter by remember { mutableStateOf("any") }
    var minSalaryFilter by remember { mutableFloatStateOf(0f) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var urgentOnly by remember { mutableStateOf(false) }
    var savedOnly by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(JobFeedSort.MATCH_URGENT) }
    var isCompact by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var detailJob by remember { mutableStateOf<JobListing?>(initialSelectedJob) }
    var compareList by remember { mutableStateOf<List<JobListing>>(emptyList()) }
    var showCompareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialSelectedJob) {
        if (initialSelectedJob != null) {
            detailJob = initialSelectedJob
            onClearInitialJob()
        }
    }

    LaunchedEffect(voiceIntentState) {
        if (voiceIntentState is AiCallState.Success) {
            val f = voiceIntentState.data
            if (f.titleQuery.isNotBlank()) searchQuery = f.titleQuery
            if (f.district.isNotBlank()) districtFilter = f.district
            if (f.workMode != "any") workModeFilter = f.workMode
            if (f.minSalaryNpr > 0L) minSalaryFilter = f.minSalaryNpr.toFloat()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                searchQuery = spoken
                onParseVoiceQuery(spoken)
            }
        }
    }

    val appliedJobIds = remember(applicationsState) {
        if (applicationsState is UiState.Success) {
            applicationsState.data.filter { it.stage != "withdrawn" }.map { it.jobId }.toSet()
        } else emptySet()
    }

    val savedJobIds = remember(savedJobsState) {
        if (savedJobsState is UiState.Success) {
            savedJobsState.data.map { it.jobId.ifBlank { it.id } }.toSet()
        } else emptySet()
    }

    val clearAllFilters: () -> Unit = {
        searchQuery = ""
        districtFilter = "All"
        workModeFilter = "any"
        minSalaryFilter = 0f
        verifiedOnly = false
        urgentOnly = false
        savedOnly = false
        onClearVoiceIntent()
    }

    val launchVoiceSearch: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (locale == AppLocale.NE) "ne-NP" else "en-US")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            if (searchQuery.isNotBlank()) onParseVoiceQuery(searchQuery)
        }
    }

    val resumes = (resumesState as? UiState.Success)?.data.orEmpty()
    val defaultResumeId = remember(resumes) {
        resumes.firstOrNull { it.defaultCv }?.id ?: resumes.firstOrNull()?.id ?: "resume_v1"
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search, Voice, Density & Filters Header
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = strings.searchJobsHint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("job_feed_search_input")
                )
                FilledTonalIconButton(
                    onClick = launchVoiceSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("job_feed_voice_button")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = strings.voiceSearchTip)
                }
                FilledTonalIconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("job_feed_filter_button")
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filters")
                }
            }

            if (voiceIntentState is AiCallState.Loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Parsing Nepali/English search intent...", style = MaterialTheme.typography.labelMedium)
                }
            } else if (voiceIntentState is AiCallState.Success) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Filters: ${voiceIntentState.data.district.ifBlank { "Any district" }} • Min NPR ${voiceIntentState.data.minSalaryNpr}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            onClearVoiceIntent()
                            districtFilter = "All"
                            workModeFilter = "any"
                            minSalaryFilter = 0f
                        }) {
                            Text("Reset")
                        }
                    }
                }
            }

            // Sort, Density & Quick Filters Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort cycle chip
                item {
                    FilterChip(
                        selected = true,
                        onClick = {
                            val entries = JobFeedSort.entries
                            sortMode = entries[(entries.indexOf(sortMode) + 1) % entries.size]
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.SwapVert, contentDescription = "Change sort", modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(if (locale == AppLocale.NE) sortMode.labelNe else sortMode.labelEn)
                        },
                        modifier = Modifier.testTag("job_feed_sort_chip")
                    )
                }
                // Compact vs Detailed toggle chip
                item {
                    FilterChip(
                        selected = isCompact,
                        onClick = { isCompact = !isCompact },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isCompact) Icons.Filled.ViewList else Icons.Filled.ViewAgenda,
                                contentDescription = "Toggle card density",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (isCompact) {
                                    if (locale == AppLocale.NE) "संक्षिप्त" else "Compact"
                                } else {
                                    if (locale == AppLocale.NE) "विस्तृत" else "Detailed"
                                }
                            )
                        },
                        modifier = Modifier.testTag("job_feed_density_chip")
                    )
                }
                // Saved Only chip
                item {
                    FilterChip(
                        selected = savedOnly,
                        onClick = { savedOnly = !savedOnly },
                        leadingIcon = {
                            Icon(
                                imageVector = if (savedOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Saved jobs filter",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            val count = savedJobIds.size
                            Text(if (locale == AppLocale.NE) "सुरक्षित ($count)" else "Saved ($count)")
                        },
                        modifier = Modifier.testTag("job_feed_saved_chip")
                    )
                }
                val districts = listOf("All", "Kathmandu", "Lalitpur", "Pokhara", "Biratnagar")
                items(districts) { dist ->
                    FilterChip(
                        selected = districtFilter.equals(dist, ignoreCase = true),
                        onClick = { districtFilter = dist },
                        label = { Text(dist) }
                    )
                }
                item {
                    FilterChip(
                        selected = workModeFilter == "remote",
                        onClick = { workModeFilter = if (workModeFilter == "remote") "any" else "remote" },
                        label = { Text("Remote") }
                    )
                }
                item {
                    FilterChip(
                        selected = verifiedOnly,
                        onClick = { verifiedOnly = !verifiedOnly },
                        label = { Text("Verified (2+)") }
                    )
                }
                item {
                    FilterChip(
                        selected = urgentOnly,
                        onClick = { urgentOnly = !urgentOnly },
                        label = { Text("Urgent") }
                    )
                }
            }

            AnimatedVisibility(visible = compareList.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compare Tray (${compareList.size}/2)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { compareList = emptyList() }) { Text("Clear") }
                            Button(
                                onClick = { showCompareDialog = true },
                                enabled = compareList.size == 2
                            ) {
                                Text("Compare")
                            }
                        }
                    }
                }
            }
        }

        // Feed Body with Pull-to-Refresh & Vertical LazyColumn
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefreshJobs,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when (jobsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = if (locale == AppLocale.NE) "जागिर सूची लोड हुँदैछ..." else "Fetching live jobs from Firestore...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ErrorBannerCard(jobsState.message)
                        OutlinedButton(
                            onClick = onRefreshJobs,
                            modifier = Modifier.testTag("job_feed_retry_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Firestore Sync")
                        }
                    }
                }

                is UiState.Success -> {
                    val displayed = remember(
                        jobsState.data,
                        profile,
                        searchQuery,
                        districtFilter,
                        workModeFilter,
                        minSalaryFilter,
                        verifiedOnly,
                        urgentOnly,
                        savedOnly,
                        savedJobIds,
                        sortMode
                    ) {
                        val filtered = jobsState.data.filter { job ->
                            val matchesQuery = searchQuery.isBlank() ||
                                job.title.contains(searchQuery, ignoreCase = true) ||
                                job.titleNe.contains(searchQuery, ignoreCase = true) ||
                                job.orgName.contains(searchQuery, ignoreCase = true) ||
                                job.requiredSkills.any { it.contains(searchQuery, ignoreCase = true) }
                            val matchesDistrict = districtFilter == "All" ||
                                job.district.equals(districtFilter, ignoreCase = true)
                            val matchesWorkMode = workModeFilter == "any" ||
                                job.workMode.equals(workModeFilter, ignoreCase = true)
                            val matchesSalary = job.salaryMax >= minSalaryFilter.toLong()
                            val matchesVerified = !verifiedOnly || job.orgVerificationLevel >= 2L
                            val matchesUrgent = !urgentOnly || job.urgent
                            val matchesSaved = !savedOnly || job.id in savedJobIds
                            matchesQuery && matchesDistrict && matchesWorkMode &&
                                matchesSalary && matchesVerified && matchesUrgent && matchesSaved
                        }.map { job ->
                            job to MatchEngine.computeMatch(profile, job)
                        }

                        when (sortMode) {
                            JobFeedSort.MATCH_URGENT -> filtered.sortedWith(
                                compareByDescending<Pair<JobListing, MatchBreakdown>> { it.first.urgent }
                                    .thenByDescending { it.second.overallPercent }
                            )
                            JobFeedSort.NEWEST -> filtered.sortedByDescending {
                                it.first.createdAt?.toDate()?.time ?: 0L
                            }
                            JobFeedSort.SALARY_HIGH -> filtered.sortedByDescending { it.first.salaryMax }
                        }
                    }

                    if (displayed.isEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("job_feed_empty_lazy_column"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "job_feed_empty_state_item") {
                                JobFeedEmptyState(
                                    locale = locale,
                                    totalPublishedJobs = jobsState.data.size,
                                    savedCount = savedJobIds.size,
                                    searchQuery = searchQuery,
                                    districtFilter = districtFilter,
                                    workModeFilter = workModeFilter,
                                    minSalaryFilter = minSalaryFilter,
                                    verifiedOnly = verifiedOnly,
                                    urgentOnly = urgentOnly,
                                    savedOnly = savedOnly,
                                    hasVoiceFilter = voiceIntentState is AiCallState.Success,
                                    onClearAllFilters = clearAllFilters,
                                    onRefreshJobs = onRefreshJobs,
                                    onVoiceSearch = launchVoiceSearch,
                                    onToggleSavedOnly = { target -> savedOnly = target },
                                    onApplyQuickPreset = { preset ->
                                        clearAllFilters()
                                        when (preset) {
                                            JobFeedQuickPreset.KATHMANDU -> districtFilter = "Kathmandu"
                                            JobFeedQuickPreset.REMOTE -> workModeFilter = "remote"
                                            JobFeedQuickPreset.VERIFIED -> verifiedOnly = true
                                            JobFeedQuickPreset.URGENT -> urgentOnly = true
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("job_feed_lazy_column"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "feed_header_summary") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (locale == AppLocale.NE) {
                                            "${displayed.size} उपलब्ध जागिरहरू"
                                        } else {
                                            "${displayed.size} live listings"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = onRefreshJobs,
                                        modifier = Modifier.testTag("job_feed_manual_refresh_btn")
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (locale == AppLocale.NE) "रिफ्रेस" else "Refresh")
                                    }
                                }
                            }

                            items(displayed, key = { it.first.id }) { (job, match) ->
                                val isApplied = job.id in appliedJobIds
                                val isSaved = job.id in savedJobIds
                                val inCompare = compareList.any { it.id == job.id }

                                JobFeedCard(
                                    job = job,
                                    match = match,
                                    locale = locale,
                                    strings = strings,
                                    isCompact = isCompact,
                                    isSaved = isSaved,
                                    isApplied = isApplied,
                                    inCompare = inCompare,
                                    onCardClick = { detailJob = job },
                                    onToggleSave = { onToggleSavedJob(job, isSaved) },
                                    onToggleCompare = {
                                        compareList = if (inCompare) {
                                            compareList.filterNot { it.id == job.id }
                                        } else if (compareList.size < 2) {
                                            compareList + job
                                        } else {
                                            listOf(compareList.last(), job)
                                        }
                                    },
                                    onOneTapApply = {
                                        onApplyToJob(job, defaultResumeId, "")
                                    },
                                    onViewApplied = onOpenApplicationsTab
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Filter Jobs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Minimum Salary: ${formatNpr(minSalaryFilter.toLong(), locale)}")
                Slider(
                    value = minSalaryFilter,
                    onValueChange = { minSalaryFilter = it },
                    valueRange = 0f..120000f,
                    steps = 11
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Verified Employers Only (Tier 2+)")
                    Switch(checked = verifiedOnly, onCheckedChange = { verifiedOnly = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Urgent Hiring Only")
                    Switch(checked = urgentOnly, onCheckedChange = { urgentOnly = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Bookmarks Only")
                    Switch(checked = savedOnly, onCheckedChange = { savedOnly = it })
                }
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Filters")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Job Detail & Custom Apply Sheet
    detailJob?.let { job ->
        val breakdown = MatchEngine.computeMatch(profile, job)
        val isAlreadyApplied = job.id in appliedJobIds
        val isSaved = job.id in savedJobIds
        var questionnaireAnswer by remember(job.id) { mutableStateOf("") }
        var selectedResumeId by remember(job.id, resumes) {
            mutableStateOf(defaultResumeId)
        }

        ModalBottomSheet(onDismissRequest = { detailJob = null }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = if (locale == AppLocale.NE && job.titleNe.isNotBlank()) {
                            "${job.titleNe} (${job.title})"
                        } else {
                            job.title
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onToggleSavedJob(job, isSaved) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove bookmark" else "Save job",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text("${job.orgName} • ${job.district} • ${job.workMode.uppercase()}")
                if (job.salaryVisible) {
                    Text(
                        text = "${formatNpr(job.salaryMin, locale)} – ${formatNpr(job.salaryMax, locale)} / month",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VerificationBadge(
                        level = job.orgVerificationLevel.toString(),
                        detailNote = "PAN: ${job.orgPan.ifBlank { "Not provided" }} • ${job.scamRiskReason}"
                    )
                    ScamRiskChip(job.scamRiskScore, job.scamRiskReason)
                }

                Text(job.description, style = MaterialTheme.typography.bodyMedium)

                ExplainabilityPanel(
                    breakdown = breakdown,
                    title = strings.matchBreakdownTitle,
                    missingTitle = strings.missingRequirements,
                    advisoryNote = strings.aiAdvisoryNotice
                )

                if (!isAlreadyApplied) {
                    HorizontalDivider()
                    Text("Apply with Selected Resume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (resumes.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(resumes, key = { it.id }) { res ->
                                FilterChip(
                                    selected = selectedResumeId == res.id,
                                    onClick = { selectedResumeId = res.id },
                                    label = { Text("${res.title} (ATS ${res.atsScore}%)") }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = questionnaireAnswer,
                        onValueChange = { questionnaireAnswer = it },
                        label = { Text("Note / Availability for ${job.orgName} (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onApplyToJob(job, selectedResumeId, questionnaireAnswer)
                            detailJob = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("job_detail_apply_button")
                    ) {
                        Text(strings.applyNow)
                    }
                } else {
                    AssistChip(
                        onClick = { detailJob = null },
                        leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess) },
                        label = { Text(strings.appliedBadge) }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Compare Dialog
    if (showCompareDialog && compareList.size == 2) {
        val j1 = compareList[0]
        val j2 = compareList[1]
        val m1 = MatchEngine.computeMatch(profile, j1)
        val m2 = MatchEngine.computeMatch(profile, j2)
        AlertDialog(
            onDismissRequest = { showCompareDialog = false },
            title = { Text("Job Comparison") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(j1.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(j1.orgName, style = MaterialTheme.typography.bodySmall)
                        Text("Match: ${m1.overallPercent}%", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        Text("District: ${j1.district}", style = MaterialTheme.typography.bodySmall)
                        Text("Salary: ${formatNpr(j1.salaryMin, locale)}+", style = MaterialTheme.typography.bodySmall)
                        Text("Verified Tier: ${j1.orgVerificationLevel}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(j2.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(j2.orgName, style = MaterialTheme.typography.bodySmall)
                        Text("Match: ${m2.overallPercent}%", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        Text("District: ${j2.district}", style = MaterialTheme.typography.bodySmall)
                        Text("Salary: ${formatNpr(j2.salaryMin, locale)}+", style = MaterialTheme.typography.bodySmall)
                        Text("Verified Tier: ${j2.orgVerificationLevel}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompareDialog = false }) { Text("Done") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobFeedCard(
    job: JobListing,
    match: MatchBreakdown,
    locale: AppLocale,
    strings: AppStrings,
    isCompact: Boolean,
    isSaved: Boolean,
    isApplied: Boolean,
    inCompare: Boolean,
    onCardClick: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleCompare: () -> Unit,
    onOneTapApply: () -> Unit,
    onViewApplied: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (inCompare) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("job_feed_card_${job.id}")
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (job.urgent && !isCompact) {
                        Surface(
                            color = CrimsonContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = if (locale == AppLocale.NE) "अत्यावश्यक" else "URGENT",
                                color = NepalCrimson,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (locale == AppLocale.NE && job.titleNe.isNotBlank()) job.titleNe else job.title,
                        style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${job.orgName} • ${job.district} • ${job.workMode.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${match.overallPercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleSave,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("job_feed_bookmark_${job.id}")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove saved job" else "Save job",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (job.salaryVisible) {
                Text(
                    text = "${formatNpr(job.salaryMin, locale)} – ${formatNpr(job.salaryMax, locale)} / mo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!isCompact) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerificationBadge(
                        level = job.orgVerificationLevel.toString(),
                        detailNote = "Tier ${job.orgVerificationLevel}: ${job.scamRiskReason}"
                    )
                    ScamRiskChip(job.scamRiskScore, job.scamRiskReason)
                }

                if (job.requiredSkills.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        job.requiredSkills.take(4).forEach { skill ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = skill,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleCompare,
                    modifier = Modifier.testTag("job_feed_compare_${job.id}")
                ) {
                    Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (inCompare) "In Compare" else "Compare")
                }

                if (isApplied) {
                    AssistChip(
                        onClick = onViewApplied,
                        leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess) },
                        label = { Text(strings.appliedBadge) }
                    )
                } else {
                    FilledTonalButton(
                        onClick = onOneTapApply,
                        modifier = Modifier.testTag("job_feed_apply_${job.id}")
                    ) {
                        Text(strings.applyNow)
                    }
                }
            }
        }
    }
}
