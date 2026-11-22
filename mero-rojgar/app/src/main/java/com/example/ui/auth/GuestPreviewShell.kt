package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLocale
import com.example.i18n.stringsFor
import com.example.model.AutoApplyRule
import com.example.model.EmployerApplicant
import com.example.model.GigListing
import com.example.model.InterviewSession
import com.example.model.JobApplication
import com.example.model.JobListing
import com.example.model.LedgerEntry
import com.example.model.ResumeVersion
import com.example.model.SavedJob
import com.example.model.SkillEvidence
import com.example.model.SponsorCampaign
import com.example.model.TalentDirectoryEntry
import com.example.model.UserProfile
import com.example.model.WalletSummary
import com.example.model.WithdrawalRequest
import com.example.ui.components.NepaliEnglishToggleChip
import com.example.ui.screens.AdminWorkspaceScreen
import com.example.ui.screens.EmployerWorkspaceScreen
import com.example.ui.screens.GigsAndPassportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JobsScreen
import com.example.ui.screens.ProfileAndAiScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.SponsorWorkspaceScreen
import com.example.ui.theme.MeroRojgarTheme
import com.example.viewmodel.AiCallState
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

private data class PreviewNavItem(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestPreviewShell(
    initialLocale: AppLocale = AppLocale.EN,
    onExitToAuth: (openSignUp: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var locale by remember { mutableStateOf(initialLocale) }
    val strings = remember(locale) { stringsFor(locale) }
    var activeRole by remember { mutableStateOf("job_seeker") }
    var selectedSeekerTab by remember { mutableIntStateOf(0) }
    var initialSelectedJob by remember { mutableStateOf<JobListing?>(null) }
    var openAutoApplyOnStart by remember { mutableStateOf(false) }
    var openPassportOnStart by remember { mutableStateOf(false) }
    var openResumeStudioOnStart by remember { mutableStateOf(false) }
    var openInterviewCoachOnStart by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    val previewJobs = remember { PreviewSampleData.jobs }
    var previewSavedJobs by remember { mutableStateOf(PreviewSampleData.savedJobs) }
    val previewProfile = remember(locale, activeRole) {
        PreviewSampleData.profile.copy(locale = locale.code, activeRole = activeRole)
    }

    val promptSignIn: (String) -> Unit = { actionLabel ->
        scope.launch {
            val msg = if (locale == AppLocale.NE) {
                "पूर्वावलोकन मोड: $actionLabel सुरक्षित गर्न Google बाट साइन इन वा दर्ता गर्नुहोस्।"
            } else {
                "Preview Mode: Sign in or register with Google to $actionLabel."
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (activeRole == "job_seeker" && selectedSeekerTab != 0) {
        BackHandler { selectedSeekerTab = 0 }
    } else if (activeRole != "job_seeker") {
        BackHandler { activeRole = "job_seeker" }
    }

    val navItems = remember(strings) {
        listOf(
            PreviewNavItem(0, strings.navHome, Icons.Filled.Home, "preview_nav_home"),
            PreviewNavItem(1, strings.navJobs, Icons.Filled.Work, "preview_nav_jobs"),
            PreviewNavItem(2, strings.navGigs, Icons.Filled.Build, "preview_nav_gigs"),
            PreviewNavItem(3, strings.navRewards, Icons.Filled.CardGiftcard, "preview_nav_rewards"),
            PreviewNavItem(4, strings.navProfile, Icons.Filled.Person, "preview_nav_profile")
        )
    }

    MeroRojgarTheme(isNepali = locale == AppLocale.NE) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = strings.appTitle,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            actions = {
                                Box {
                                    val roleLabel = when (activeRole) {
                                        "employer" -> strings.roleEmployer
                                        "sponsor" -> strings.roleSponsor
                                        "platform_admin" -> strings.roleAdmin
                                        else -> strings.roleSeeker
                                    }
                                    AssistChip(
                                        onClick = { showRoleMenu = true },
                                        leadingIcon = {
                                            Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch Role")
                                        },
                                        label = {
                                            Text(roleLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        modifier = Modifier.testTag("preview_role_chip")
                                    )
                                    DropdownMenu(
                                        expanded = showRoleMenu,
                                        onDismissRequest = { showRoleMenu = false }
                                    ) {
                                        listOf(
                                            "job_seeker" to strings.roleSeeker,
                                            "employer" to strings.roleEmployer,
                                            "sponsor" to strings.roleSponsor,
                                            "platform_admin" to strings.roleAdmin
                                        ).forEach { (roleKey, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    showRoleMenu = false
                                                    activeRole = roleKey
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                NepaliEnglishToggleChip(
                                    locale = locale,
                                    onToggle = {
                                        locale = if (locale == AppLocale.NE) AppLocale.EN else AppLocale.NE
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("guest_preview_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (locale == AppLocale.NE) {
                                            "गेस्ट पूर्वावलोकन (नमुना डाटा)"
                                        } else {
                                            "Guest Preview • Sample Nepal data"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    TextButton(
                                        onClick = { onExitToAuth(true) },
                                        modifier = Modifier.testTag("preview_sign_up_cta")
                                    ) {
                                        Text(if (locale == AppLocale.NE) "दर्ता" else "Sign Up")
                                    }
                                    TextButton(
                                        onClick = { onExitToAuth(false) },
                                        modifier = Modifier.testTag("preview_exit_to_auth_btn")
                                    ) {
                                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (locale == AppLocale.NE) "साइन इन" else "Sign In")
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!useRail && activeRole == "job_seeker") {
                        NavigationBar {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    selected = selectedSeekerTab == item.index,
                                    onClick = { selectedSeekerTab = item.index },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && activeRole == "job_seeker") {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            navItems.forEach { item ->
                                NavigationRailItem(
                                    selected = selectedSeekerTab == item.index,
                                    onClick = { selectedSeekerTab = item.index },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxSize()
                        ) {
                            when (activeRole) {
                                "employer" -> EmployerWorkspaceScreen(
                                    locale = locale,
                                    strings = strings,
                                    employerJobsState = UiState.Success(previewJobs),
                                    employerApplicantsState = UiState.Success(PreviewSampleData.employerApplicants),
                                    talentDirectoryState = UiState.Success(PreviewSampleData.talentDirectory),
                                    jdDraftState = AiCallState.Idle,
                                    onGenerateJobDraft = { _, _, _, _ -> promptSignIn("generate AI job drafts") },
                                    onResetJobDraft = {},
                                    onPostJob = { _, _, _, _, _, _, _, _, _, _, _, _ -> promptSignIn("publish jobs") },
                                    onUpdateApplicantStage = { _, _, _ -> promptSignIn("update ATS stages") },
                                    onSwitchToSeeker = { activeRole = "job_seeker" }
                                )

                                "sponsor" -> SponsorWorkspaceScreen(
                                    locale = locale,
                                    strings = strings,
                                    campaignsState = UiState.Success(PreviewSampleData.campaigns),
                                    onCreateCampaign = { _, _, _, _, _, _, _ -> promptSignIn("launch sponsor campaigns") },
                                    onSwitchToSeeker = { activeRole = "job_seeker" }
                                )

                                "platform_admin" -> AdminWorkspaceScreen(
                                    locale = locale,
                                    strings = strings,
                                    employerJobsState = UiState.Success(previewJobs),
                                    withdrawalsState = UiState.Success(PreviewSampleData.withdrawals),
                                    onSwitchToSeeker = { activeRole = "job_seeker" }
                                )

                                else -> when (selectedSeekerTab) {
                                    0 -> HomeScreen(
                                        locale = locale,
                                        strings = strings,
                                        profile = previewProfile,
                                        jobsState = UiState.Success(previewJobs),
                                        applicationsState = UiState.Success(PreviewSampleData.applications),
                                        walletSummary = PreviewSampleData.walletSummary,
                                        onVoiceOrTextSearch = { selectedSeekerTab = 1 },
                                        onOpenJobsTab = { selectedSeekerTab = 1 },
                                        onOpenRewardsTab = { selectedSeekerTab = 3 },
                                        onOpenResumeStudio = {
                                            openResumeStudioOnStart = true
                                            selectedSeekerTab = 4
                                        },
                                        onOpenInterviewCoach = {
                                            openInterviewCoachOnStart = true
                                            selectedSeekerTab = 4
                                        },
                                        onOpenAutoApply = {
                                            openAutoApplyOnStart = true
                                            selectedSeekerTab = 1
                                        },
                                        onOpenSkillPassport = {
                                            openPassportOnStart = true
                                            selectedSeekerTab = 2
                                        },
                                        onSelectJob = { job ->
                                            initialSelectedJob = job
                                            selectedSeekerTab = 1
                                        }
                                    )

                                    1 -> JobsScreen(
                                        locale = locale,
                                        strings = strings,
                                        profile = previewProfile,
                                        jobsState = UiState.Success(previewJobs),
                                        isRefreshingJobs = false,
                                        applicationsState = UiState.Success(PreviewSampleData.applications),
                                        savedJobsState = UiState.Success(previewSavedJobs),
                                        resumesState = UiState.Success(PreviewSampleData.resumes),
                                        autoApplyRulesState = UiState.Success(PreviewSampleData.autoApplyRules),
                                        voiceIntentState = AiCallState.Idle,
                                        initialSelectedJob = initialSelectedJob,
                                        openAutoApplyOnStart = openAutoApplyOnStart,
                                        onClearInitialJob = { initialSelectedJob = null },
                                        onClearAutoApplyFlag = { openAutoApplyOnStart = false },
                                        onRefreshJobs = { promptSignIn("sync live jobs") },
                                        onToggleSavedJob = { job, currentlySaved ->
                                            previewSavedJobs = if (currentlySaved) {
                                                previewSavedJobs.filterNot { it.jobId == job.id }
                                            } else {
                                                previewSavedJobs + SavedJob(
                                                    id = job.id,
                                                    userId = "guest_preview",
                                                    jobId = job.id,
                                                    jobTitle = job.title,
                                                    orgName = job.orgName
                                                )
                                            }
                                        },
                                        onParseVoiceQuery = { promptSignIn("run AI voice parsing") },
                                        onClearVoiceIntent = {},
                                        onApplyToJob = { _, _, _ -> promptSignIn("submit applications") },
                                        onWithdrawApplication = { promptSignIn("manage applications") },
                                        onSaveAutoApplyRule = { _, _, _, _, _, _, _, _, _ -> promptSignIn("save auto-apply rules") },
                                        onToggleAutoApplyRule = { _, _ -> promptSignIn("toggle auto-apply rules") }
                                    )

                                    2 -> GigsAndPassportScreen(
                                        locale = locale,
                                        strings = strings,
                                        posterName = previewProfile.displayName,
                                        gigsState = UiState.Success(PreviewSampleData.gigs),
                                        skillEvidenceState = UiState.Success(PreviewSampleData.skillEvidence),
                                        openPassportOnStart = openPassportOnStart,
                                        onClearPassportFlag = { openPassportOnStart = false },
                                        onPostGig = { _, _, _, _, _, _, _, _ -> promptSignIn("post gigs") },
                                        onAddSkillEvidence = { _, _, _, _, _, _ -> promptSignIn("add skill evidence") }
                                    )

                                    3 -> RewardsScreen(
                                        locale = locale,
                                        strings = strings,
                                        walletSummary = PreviewSampleData.walletSummary,
                                        campaignsState = UiState.Success(PreviewSampleData.campaigns),
                                        completionsState = UiState.Success(emptyList()),
                                        ledgerState = UiState.Success(PreviewSampleData.ledger),
                                        withdrawalsState = UiState.Success(PreviewSampleData.withdrawals),
                                        onCompleteCampaign = { _, _ -> promptSignIn("earn sponsor rewards") },
                                        onRequestWithdrawal = { _, _, _ -> promptSignIn("request payouts") }
                                    )

                                    else -> ProfileAndAiScreen(
                                        locale = locale,
                                        strings = strings,
                                        profile = previewProfile,
                                        resumesState = UiState.Success(PreviewSampleData.resumes),
                                        resumeParseState = AiCallState.Idle,
                                        coachTurns = emptyList(),
                                        coachStreamState = AiCallState.Idle,
                                        interviewSessionsState = UiState.Success(PreviewSampleData.interviewSessions),
                                        openResumeStudioOnStart = openResumeStudioOnStart,
                                        openInterviewCoachOnStart = openInterviewCoachOnStart,
                                        onClearQuickOpenFlags = {
                                            openResumeStudioOnStart = false
                                            openInterviewCoachOnStart = false
                                        },
                                        onSwitchRole = { activeRole = it },
                                        onUpdatePrivacyProfile = { _, _, _, _, _, _, _ ->
                                            promptSignIn("save profile & privacy settings")
                                        },
                                        onAnalyzeResume = { _, _, _ -> promptSignIn("run AI Resume Studio") },
                                        onResetResumeProposal = {},
                                        onSaveResume = { _, _, _, _, _, _, _, _, _ -> promptSignIn("save resume versions") },
                                        onSendCoachMessage = { _, _, _ -> promptSignIn("practice with AI Interview Coach") },
                                        onResetCoach = {},
                                        onSaveCoachSession = { _, _, _, _, _ -> promptSignIn("save interview sessions") }
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

private object PreviewSampleData {
    val profile = UserProfile(
        id = "guest_preview",
        userId = "guest_preview",
        displayName = "Aarav Shrestha (Preview)",
        locale = "en",
        activeRole = "job_seeker",
        headline = "Civil & Infrastructure Engineer | Kotlin Builder",
        district = "Kathmandu",
        availability = "Immediate",
        visibility = "private",
        contactHidden = true,
        expectedSalaryMin = 45000L,
        expectedSalaryMax = 85000L,
        experienceMonths = 24L,
        educationLevel = "Bachelor",
        skills = listOf("AutoCAD", "Site Supervision", "Kotlin", "Project Planning", "Nepali"),
        verificationLevel = "self_reported"
    )

    val jobs = listOf(
        JobListing(
            id = "preview_job_1",
            postedByUserId = "preview_org_1",
            orgName = "Himalayan Hydropower Ltd.",
            orgVerificationLevel = 3L,
            title = "Senior Civil Site Supervisor",
            titleNe = "वरिष्ठ सिभिल साइट सुपरभाइजर",
            description = "Lead concrete QA, BOQ tracking, and site safety compliance for our Trishuli corridor project.",
            employmentType = "full_time",
            workMode = "onsite",
            district = "Kathmandu",
            salaryMin = 55000L,
            salaryMax = 85000L,
            salaryVisible = true,
            requiredSkills = listOf("AutoCAD", "Site Supervision", "Project Planning", "Safety"),
            minExperienceMonths = 24L,
            educationLevel = "Bachelor",
            urgent = true,
            scamRiskScore = 0.04,
            scamRiskReason = "PAN verified Tier-3 employer",
            status = "published"
        ),
        JobListing(
            id = "preview_job_2",
            postedByUserId = "preview_org_2",
            orgName = "Bagmati FinTech Labs",
            orgVerificationLevel = 2L,
            title = "Android Kotlin Engineer (Compose)",
            titleNe = "एन्ड्रोइड कोट्लिन इन्जिनियर",
            description = "Build accessible Nepali/English Material 3 mobile workflows for digital cooperative banking.",
            employmentType = "full_time",
            workMode = "hybrid",
            district = "Lalitpur",
            salaryMin = 65000L,
            salaryMax = 110000L,
            salaryVisible = true,
            requiredSkills = listOf("Kotlin", "Jetpack Compose", "Firebase", "Communication"),
            minExperienceMonths = 18L,
            educationLevel = "Bachelor",
            urgent = true,
            scamRiskScore = 0.06,
            scamRiskReason = "Verified organization",
            status = "published"
        ),
        JobListing(
            id = "preview_job_3",
            postedByUserId = "preview_org_3",
            orgName = "Everest Digital Collective",
            orgVerificationLevel = 1L,
            title = "Product UI/UX Designer (Nepali & English)",
            titleNe = "प्रोडक्ट UI/UX डिजाइनर",
            description = "Design mobile-first interfaces supporting Devanagari typography and low-end Android devices.",
            employmentType = "contract",
            workMode = "remote",
            district = "Pokhara",
            salaryMin = 45000L,
            salaryMax = 75000L,
            salaryVisible = true,
            requiredSkills = listOf("Figma", "Devanagari Typography", "Design Systems"),
            minExperienceMonths = 12L,
            educationLevel = "Bachelor",
            urgent = false,
            scamRiskScore = 0.14,
            scamRiskReason = "Contact verified; PAN pending upload",
            status = "published"
        )
    )

    val savedJobs = listOf(
        SavedJob(
            id = "preview_job_2",
            userId = "guest_preview",
            jobId = "preview_job_2",
            jobTitle = "Android Kotlin Engineer (Compose)",
            orgName = "Bagmati FinTech Labs"
        )
    )

    val applications = listOf(
        JobApplication(
            id = "preview_app_1",
            userId = "guest_preview",
            jobId = "preview_job_1",
            jobTitle = "Senior Civil Site Supervisor",
            orgName = "Himalayan Hydropower Ltd.",
            resumeId = "resume_v1",
            source = "manual",
            stage = "shortlisted",
            matchScore = 91L,
            matchSummary = "Skills 92% • Kathmandu local match"
        )
    )

    val resumes = listOf(
        ResumeVersion(
            id = "resume_v1",
            userId = "guest_preview",
            title = "Nepal Engineering & Tech CV (v1)",
            language = "en",
            summary = "Civil & software builder experienced in Kathmandu Valley infrastructure & Android projects.",
            skills = listOf("AutoCAD", "Site Supervision", "Kotlin", "Project Planning"),
            atsScore = 82L,
            aiAssisted = false,
            defaultCv = true,
            version = 1L
        )
    )

    val autoApplyRules = listOf(
        AutoApplyRule(
            id = "preview_rule_1",
            userId = "guest_preview",
            ruleName = "Kathmandu Verified Civil & Tech",
            keywords = "Civil, Kotlin, Supervisor",
            district = "Kathmandu",
            workMode = "any",
            minSalaryNpr = 45000L,
            minMatchThreshold = 75L,
            dailyCap = 3L,
            approvedResumeId = "resume_v1",
            active = true
        )
    )

    val gigs = listOf(
        GigListing(
            id = "preview_gig_1",
            postedByUserId = "preview_poster_1",
            posterName = "Patan Solar Co-op",
            title = "3kW Rooftop Solar & Inverter Checkup",
            category = "Electrician / Solar",
            district = "Kathmandu",
            budgetNpr = 4500L,
            localTrade = true,
            milestoneCount = 2L,
            description = "Inspect battery bank wiring and configure MPPT charge controller.",
            status = "open"
        ),
        GigListing(
            id = "preview_gig_2",
            postedByUserId = "preview_poster_2",
            posterName = "Lakeside Roasters",
            title = "Bilingual Nepali/English Cafe Menu & QR Poster",
            category = "Graphic Design",
            district = "Pokhara",
            budgetNpr = 8000L,
            localTrade = false,
            milestoneCount = 2L,
            description = "Print-ready A3 & mobile menu design in Inter and Noto Sans Devanagari.",
            status = "open"
        )
    )

    val campaigns = listOf(
        SponsorCampaign(
            id = "preview_camp_1",
            sponsorUserId = "preview_sponsor_1",
            sponsorName = "Nepal Telecom Fiber",
            title = "Digital Nepal: Safe Home Fiber Setup",
            titleNe = "डिजिटल नेपाल: सुरक्षित फाइबर इन्टरनेट",
            videoSeconds = 12L,
            rewardPaisa = 5500L,
            fundedPaisa = 550000L,
            spentPaisa = 11000L,
            questionPrompt = "Which feature helps protect your home Wi-Fi router?",
            questionOptions = listOf(
                "WPA3 / strong unique password",
                "Sharing admin password publicly",
                "Disabling all encryption"
            ),
            correctOptionIndex = 0L,
            termsSummary = "Sponsored digital safety awareness task. Not employment income.",
            endsAtText = "Ends Dec 31",
            status = "live"
        )
    )

    val ledger = listOf(
        LedgerEntry(
            id = "preview_led_1",
            userId = "guest_preview",
            txnId = "TXN-PREVIEW-01",
            accountKind = "user_available",
            direction = "credit",
            amountPaisa = 15500L,
            reason = "Sample Sponsor Brief Completed",
            idempotencyKey = "preview_1"
        )
    )

    val walletSummary = WalletSummary(availablePaisa = 15500L)

    val withdrawals = emptyList<WithdrawalRequest>()

    val skillEvidence = listOf(
        SkillEvidence(
            id = "preview_sk_1",
            userId = "guest_preview",
            skillName = "AutoCAD & Structural Drafting",
            level = 4L,
            verificationLevel = "assessment_completed",
            provider = "Mero Skill Council",
            method = "Practical Drafting Check",
            scorePercent = 88L
        )
    )

    val interviewSessions = emptyList<InterviewSession>()

    val employerApplicants = listOf(
        EmployerApplicant(
            id = "preview_emp_app_1",
            employerUserId = "guest_preview",
            applicantUserId = "preview_cand_1",
            applicantName = "Prakash Adhikari",
            applicantHeadline = "Civil Overseer • 3 yrs Hydropower",
            jobId = "preview_job_1",
            jobTitle = "Senior Civil Site Supervisor",
            stage = "shortlisted",
            matchScore = 91L,
            matchReasons = "Skills 95% • Experience 100% • Kathmandu match",
            source = "manual",
            notes = "Verified CTEVT Diploma; ready for site interview."
        )
    )

    val talentDirectory = listOf(
        TalentDirectoryEntry(
            id = "preview_tal_1",
            userId = "preview_tal_1",
            displayName = "Anisha Maharjan",
            headline = "Android Compose & Firebase Engineer",
            district = "Lalitpur",
            skills = listOf("Kotlin", "Jetpack Compose", "Firebase"),
            verificationLevel = "identity_checked",
            visibility = "recruiter_visible",
            openStatus = "open_to_opportunities"
        )
    )
}
