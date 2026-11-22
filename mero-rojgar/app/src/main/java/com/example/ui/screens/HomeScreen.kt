package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.MatchEngine
import com.example.i18n.AppLocale
import com.example.i18n.AppStrings
import com.example.i18n.formatNpr
import com.example.i18n.formatPaisaAsNpr
import com.example.model.JobApplication
import com.example.model.JobListing
import com.example.model.UserProfile
import com.example.model.WalletSummary
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.ProfileCompletionRing
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.SaffronGold
import com.example.viewmodel.UiState

@Composable
fun HomeScreen(
    locale: AppLocale,
    strings: AppStrings,
    profile: UserProfile?,
    jobsState: UiState<List<JobListing>>,
    applicationsState: UiState<List<JobApplication>>,
    walletSummary: WalletSummary,
    onVoiceOrTextSearch: (String) -> Unit,
    onOpenJobsTab: () -> Unit,
    onOpenRewardsTab: () -> Unit,
    onOpenResumeStudio: () -> Unit,
    onOpenInterviewCoach: () -> Unit,
    onOpenAutoApply: () -> Unit,
    onOpenSkillPassport: () -> Unit,
    onSelectJob: (JobListing) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                searchText = spoken
                onVoiceOrTextSearch(spoken)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(164.dp).fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_himalayan_hero),
                        contentDescription = "Kathmandu and Himalayas workforce banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        HimalayanNavy.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (locale == AppLocale.NE) {
                                "नमस्ते, ${profile?.displayName ?: "मित्र"}"
                            } else {
                                "Namaste, ${profile?.displayName ?: "Member"}"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }
        }

        // Voice + Natural Language Search
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(strings.searchJobsHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_search_input")
                )
                FilledTonalIconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (locale == AppLocale.NE) "ne-NP" else "en-US")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, strings.voiceSearchTip)
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: Exception) {
                            if (searchText.isNotBlank()) onVoiceOrTextSearch(searchText)
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("home_voice_search_button")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = strings.voiceSearchTip)
                }
            }
            if (searchText.isNotBlank()) {
                TextButton(
                    onClick = { onVoiceOrTextSearch(searchText) },
                    modifier = Modifier.testTag("home_ai_parse_query_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (locale == AppLocale.NE) "AI द्वारा फिल्टर खोज्नुहोस्: \"$searchText\""
                        else "Parse smart filters for \"$searchText\""
                    )
                }
            }
        }

        // Profile Completion & Trust Summary
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileCompletionRing(percent = profile?.completionPercent() ?: 25)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.profileCompletion,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile?.headline?.ifBlank { "Add headline & skills" } ?: "Loading profile...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        VerificationBadge(
                            level = profile?.verificationLevel ?: "self_reported",
                            detailNote = strings.verificationAdvisory
                        )
                    }
                }
            }
        }

        // Career AI & Automation Studio Quick Actions
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickStudioTile(
                        title = if (locale == AppLocale.NE) "जागिर फिड" else "JobFeed",
                        icon = Icons.Filled.DynamicFeed,
                        tint = com.example.ui.theme.NepalCrimson,
                        onClick = onOpenJobsTab,
                        tag = "quick_job_feed"
                    )
                }
                item {
                    QuickStudioTile(
                        title = strings.resumeStudioTitle,
                        icon = Icons.Filled.Description,
                        tint = HimalayanNavy,
                        onClick = onOpenResumeStudio,
                        tag = "quick_resume_studio"
                    )
                }
                item {
                    QuickStudioTile(
                        title = strings.interviewCoachTitle,
                        icon = Icons.Filled.RecordVoiceOver,
                        tint = Color(0xFF8B1E3F),
                        onClick = onOpenInterviewCoach,
                        tag = "quick_interview_coach"
                    )
                }
                item {
                    QuickStudioTile(
                        title = strings.autoApplyTitle,
                        icon = Icons.Filled.AutoAwesome,
                        tint = EmeraldSuccess,
                        onClick = onOpenAutoApply,
                        tag = "quick_auto_apply"
                    )
                }
                item {
                    QuickStudioTile(
                        title = strings.skillPassportTitle,
                        icon = Icons.Filled.Badge,
                        tint = SaffronGold,
                        onClick = onOpenSkillPassport,
                        tag = "quick_skill_passport"
                    )
                }
            }
        }

        // Active Applications Strip
        if (applicationsState is UiState.Success && applicationsState.data.isNotEmpty()) {
            item {
                val activeCount = applicationsState.data.count { it.stage != "withdrawn" }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenJobsTab() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.WorkOutline, contentDescription = null)
                            Text(
                                text = if (locale == AppLocale.NE) "$activeCount सक्रिय आवेदनहरू" else "$activeCount active job applications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View applications")
                    }
                }
            }
        }

        // Recommended Jobs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recommendedForYou,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenJobsTab) {
                    Text(if (locale == AppLocale.NE) "सबै हेर्नुहोस्" else "See all")
                }
            }
        }

        when (jobsState) {
            is UiState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> item {
                ErrorBannerCard(jobsState.message)
            }
            is UiState.Success -> {
                val topJobs = jobsState.data
                    .map { it to MatchEngine.computeMatch(profile, it) }
                    .sortedByDescending { it.second.overallPercent }
                    .take(3)
                if (topJobs.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = if (locale == AppLocale.NE) "कुनै जागिर भेटिएन" else "No jobs posted yet",
                            subtitle = "Pull or switch to Employer mode to publish jobs."
                        )
                    }
                } else {
                    items(topJobs, key = { it.first.id }) { (job, match) ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectJob(job) }
                                .testTag("home_job_card_${job.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (locale == AppLocale.NE && job.titleNe.isNotBlank()) job.titleNe else job.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${job.orgName} • ${job.district}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = EmeraldContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${match.overallPercent}% match",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = match.reasons.firstOrNull().orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VerificationBadge(
                                        level = job.orgVerificationLevel.toString(),
                                        detailNote = "Employer verification tier ${job.orgVerificationLevel}: ${job.scamRiskReason}"
                                    )
                                    if (job.salaryVisible) {
                                        AssistChip(
                                            onClick = { onSelectJob(job) },
                                            label = { Text("${formatNpr(job.salaryMin, locale)}–${formatNpr(job.salaryMax, locale)}") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clearly Separate Mero Rewards Teaser
        item {
            Spacer(Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenRewardsTab() }
                    .testTag("home_rewards_teaser_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.rewardsTeaserTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = strings.rewardsTeaserDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${strings.availableBalance}: ${formatPaisaAsNpr(walletSummary.availablePaisa, locale)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Rewards",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStudioTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(148.dp)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
