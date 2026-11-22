package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.MeroRojgarRepository
import com.example.i18n.AppLocale
import com.example.i18n.stringsFor
import com.example.model.JobListing
import com.example.model.RegistrationDraft
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthTabMode
import com.example.ui.auth.GuestPreviewShell
import com.example.ui.auth.RegistrationFormContent
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
import com.example.util.configureAppCheck
import com.example.viewmodel.AiStudioViewModel
import com.example.viewmodel.MainAppViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must be called immediately before any Firebase AI or Firestore call
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }
}

private fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    Firebase.auth.addAuthStateListener(listener)
    trySend(Firebase.auth.currentUser)
    awaitClose { Firebase.auth.removeAuthStateListener(listener) }
}

private data class SeekerNavItem(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun AppContent() {
    val currentUser by authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var isGuestPreview by remember { mutableStateOf(false) }
    var authInitialMode by remember { mutableStateOf(AuthTabMode.SIGN_IN) }
    var pendingRegistrationDraft by remember { mutableStateOf<RegistrationDraft?>(null) }

    val user = currentUser
    if (user == null) {
        if (isGuestPreview) {
            GuestPreviewShell(
                onExitToAuth = { openSignUp ->
                    authInitialMode = if (openSignUp) AuthTabMode.SIGN_UP else AuthTabMode.SIGN_IN
                    isGuestPreview = false
                }
            )
        } else {
            MeroRojgarTheme(isNepali = true) {
                AuthScreen(
                    initialMode = authInitialMode,
                    onSaveRegistrationDraft = { pendingRegistrationDraft = it },
                    onEnterGuestPreview = { isGuestPreview = true },
                    onAuthSuccess = { isGuestPreview = false }
                )
            }
        }
    } else {
        key(user.uid) {
            AuthenticatedShell(
                user = user,
                pendingRegistrationDraft = pendingRegistrationDraft,
                onClearRegistrationDraft = { pendingRegistrationDraft = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    user: FirebaseUser,
    pendingRegistrationDraft: RegistrationDraft?,
    onClearRegistrationDraft: () -> Unit
) {
    val context = LocalContext.current
    val dbId = remember { context.getString(R.string.firestore_database_id) }

    val mainViewModel: MainAppViewModel = viewModel(
        key = "main_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val repo = MeroRojgarRepository(FirebaseFirestore.getInstance(dbId))
                MainAppViewModel(
                    repository = repo,
                    currentUserId = user.uid,
                    initialDisplayName = user.displayName.orEmpty(),
                    pendingRegistrationDraft = pendingRegistrationDraft,
                    onRegistrationDraftConsumed = onClearRegistrationDraft
                )
            }
        }
    )

    val aiViewModel: AiStudioViewModel = viewModel(
        key = "ai_${user.uid}",
        factory = viewModelFactory {
            initializer { AiStudioViewModel() }
        }
    )

    val locale by mainViewModel.locale.collectAsStateWithLifecycle()
    val strings = remember(locale) { stringsFor(locale) }

    val profileState by mainViewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val activeRole = profile?.activeRole ?: "job_seeker"

    val jobsState by mainViewModel.jobsState.collectAsStateWithLifecycle()
    val isRefreshingJobs by mainViewModel.isRefreshingJobs.collectAsStateWithLifecycle()
    val savedJobsState by mainViewModel.savedJobsState.collectAsStateWithLifecycle()
    val employerJobsState by mainViewModel.employerJobsState.collectAsStateWithLifecycle()
    val employerApplicantsState by mainViewModel.employerApplicantsState.collectAsStateWithLifecycle()
    val applicationsState by mainViewModel.applicationsState.collectAsStateWithLifecycle()
    val resumesState by mainViewModel.resumesState.collectAsStateWithLifecycle()
    val autoApplyRulesState by mainViewModel.autoApplyRulesState.collectAsStateWithLifecycle()
    val gigsState by mainViewModel.gigsState.collectAsStateWithLifecycle()
    val campaignsState by mainViewModel.campaignsState.collectAsStateWithLifecycle()
    val ledgerState by mainViewModel.ledgerState.collectAsStateWithLifecycle()
    val walletSummary by mainViewModel.walletSummaryState.collectAsStateWithLifecycle()
    val completionsState by mainViewModel.completionsState.collectAsStateWithLifecycle()
    val withdrawalsState by mainViewModel.withdrawalsState.collectAsStateWithLifecycle()
    val skillEvidenceState by mainViewModel.skillEvidenceState.collectAsStateWithLifecycle()
    val interviewSessionsState by mainViewModel.interviewSessionsState.collectAsStateWithLifecycle()
    val talentDirectoryState by mainViewModel.talentDirectoryState.collectAsStateWithLifecycle()
    val bannerMessage by mainViewModel.bannerMessage.collectAsStateWithLifecycle()
    val needsOnboardingPrompt by mainViewModel.needsOnboardingPrompt.collectAsStateWithLifecycle()

    val resumeParseState by aiViewModel.resumeParseState.collectAsStateWithLifecycle()
    val voiceIntentState by aiViewModel.voiceIntentState.collectAsStateWithLifecycle()
    val jdDraftState by aiViewModel.jdDraftState.collectAsStateWithLifecycle()
    val coachTurns by aiViewModel.coachTurns.collectAsStateWithLifecycle()
    val coachStreamState by aiViewModel.coachStreamState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearBanner()
        }
    }

    var selectedSeekerTab by remember { mutableIntStateOf(0) }
    var initialSelectedJob by remember { mutableStateOf<JobListing?>(null) }
    var openAutoApplyOnStart by remember { mutableStateOf(false) }
    var openPassportOnStart by remember { mutableStateOf(false) }
    var openResumeStudioOnStart by remember { mutableStateOf(false) }
    var openInterviewCoachOnStart by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    if (activeRole == "job_seeker" && selectedSeekerTab != 0) {
        BackHandler { selectedSeekerTab = 0 }
    }

    val navItems = remember(strings) {
        listOf(
            SeekerNavItem(0, strings.navHome, Icons.Filled.Home, "nav_home"),
            SeekerNavItem(1, strings.navJobs, Icons.Filled.Work, "nav_jobs"),
            SeekerNavItem(2, strings.navGigs, Icons.Filled.Build, "nav_gigs"),
            SeekerNavItem(3, strings.navRewards, Icons.Filled.CardGiftcard, "nav_rewards"),
            SeekerNavItem(4, strings.navProfile, Icons.Filled.Person, "nav_profile")
        )
    }

    MeroRojgarTheme(isNepali = locale == AppLocale.NE) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
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
                                        Icon(
                                            Icons.Filled.SwapHoriz,
                                            contentDescription = "Switch Role",
                                            modifier = Modifier.padding(0.dp)
                                        )
                                    },
                                    label = { Text(roleLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("topbar_role_chip")
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
                                                mainViewModel.switchRole(roleKey)
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            NepaliEnglishToggleChip(
                                locale = locale,
                                onToggle = { mainViewModel.toggleLocale() }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    )
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
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                            when (activeRole) {
                                "employer" -> EmployerWorkspaceScreen(
                                    locale = locale,
                                    strings = strings,
                                    employerJobsState = employerJobsState,
                                    employerApplicantsState = employerApplicantsState,
                                    talentDirectoryState = talentDirectoryState,
                                    jdDraftState = jdDraftState,
                                    onGenerateJobDraft = aiViewModel::generateJobDraft,
                                    onResetJobDraft = aiViewModel::resetJobDraft,
                                    onPostJob = mainViewModel::postJob,
                                    onUpdateApplicantStage = mainViewModel::updateApplicantStage,
                                    onSwitchToSeeker = { mainViewModel.switchRole("job_seeker") }
                                )

                                "sponsor" -> SponsorWorkspaceScreen(
                                    locale = locale,
                                    strings = strings,
                                    campaignsState = campaignsState,
                                    onCreateCampaign = mainViewModel::createCampaign,
                                    onSwitchToSeeker = { mainViewModel.switchRole("job_seeker") }
                                )

                                "platform_admin" -> AdminWorkspaceScreen(
                                    locale = locale,
                                    strings = strings,
                                    employerJobsState = employerJobsState,
                                    withdrawalsState = withdrawalsState,
                                    onSwitchToSeeker = { mainViewModel.switchRole("job_seeker") }
                                )

                                else -> when (selectedSeekerTab) {
                                    0 -> HomeScreen(
                                        locale = locale,
                                        strings = strings,
                                        profile = profile,
                                        jobsState = jobsState,
                                        applicationsState = applicationsState,
                                        walletSummary = walletSummary,
                                        onVoiceOrTextSearch = { query ->
                                            aiViewModel.parseVoiceQuery(query)
                                            selectedSeekerTab = 1
                                        },
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
                                        profile = profile,
                                        jobsState = jobsState,
                                        isRefreshingJobs = isRefreshingJobs,
                                        applicationsState = applicationsState,
                                        savedJobsState = savedJobsState,
                                        resumesState = resumesState,
                                        autoApplyRulesState = autoApplyRulesState,
                                        voiceIntentState = voiceIntentState,
                                        initialSelectedJob = initialSelectedJob,
                                        openAutoApplyOnStart = openAutoApplyOnStart,
                                        onClearInitialJob = { initialSelectedJob = null },
                                        onClearAutoApplyFlag = { openAutoApplyOnStart = false },
                                        onRefreshJobs = mainViewModel::refreshJobs,
                                        onToggleSavedJob = mainViewModel::toggleSavedJob,
                                        onParseVoiceQuery = aiViewModel::parseVoiceQuery,
                                        onClearVoiceIntent = aiViewModel::clearVoiceIntent,
                                        onApplyToJob = { job, resumeId, answer ->
                                            mainViewModel.applyToJob(job, profile, resumeId, "manual", answer)
                                        },
                                        onWithdrawApplication = mainViewModel::withdrawApplication,
                                        onSaveAutoApplyRule = mainViewModel::saveAutoApplyRule,
                                        onToggleAutoApplyRule = mainViewModel::toggleAutoApplyRule
                                    )

                                    2 -> GigsAndPassportScreen(
                                        locale = locale,
                                        strings = strings,
                                        posterName = profile?.displayName ?: "Member",
                                        gigsState = gigsState,
                                        skillEvidenceState = skillEvidenceState,
                                        openPassportOnStart = openPassportOnStart,
                                        onClearPassportFlag = { openPassportOnStart = false },
                                        onPostGig = mainViewModel::postGig,
                                        onAddSkillEvidence = mainViewModel::addSkillEvidence
                                    )

                                    3 -> RewardsScreen(
                                        locale = locale,
                                        strings = strings,
                                        walletSummary = walletSummary,
                                        campaignsState = campaignsState,
                                        completionsState = completionsState,
                                        ledgerState = ledgerState,
                                        withdrawalsState = withdrawalsState,
                                        onCompleteCampaign = mainViewModel::completeRewardCampaign,
                                        onRequestWithdrawal = mainViewModel::requestWithdrawal
                                    )

                                    else -> ProfileAndAiScreen(
                                        locale = locale,
                                        strings = strings,
                                        profile = profile,
                                        resumesState = resumesState,
                                        resumeParseState = resumeParseState,
                                        coachTurns = coachTurns,
                                        coachStreamState = coachStreamState,
                                        interviewSessionsState = interviewSessionsState,
                                        openResumeStudioOnStart = openResumeStudioOnStart,
                                        openInterviewCoachOnStart = openInterviewCoachOnStart,
                                        onClearQuickOpenFlags = {
                                            openResumeStudioOnStart = false
                                            openInterviewCoachOnStart = false
                                        },
                                        onSwitchRole = mainViewModel::switchRole,
                                        onUpdatePrivacyProfile = mainViewModel::updatePrivacyAndProfile,
                                        onAnalyzeResume = aiViewModel::analyzeResume,
                                        onResetResumeProposal = aiViewModel::resetResumeProposal,
                                        onSaveResume = mainViewModel::saveResume,
                                        onSendCoachMessage = aiViewModel::sendInterviewMessage,
                                        onResetCoach = aiViewModel::resetCoachSession,
                                        onSaveCoachSession = mainViewModel::saveInterviewSession
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (needsOnboardingPrompt) {
                ModalBottomSheet(
                    onDismissRequest = mainViewModel::dismissOnboardingPrompt
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (locale == AppLocale.NE) {
                                    "खाता दर्ता पूरा गर्नुहोस्"
                                } else {
                                    "Complete Account Registration"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = mainViewModel::dismissOnboardingPrompt,
                                modifier = Modifier.testTag("onboarding_skip_btn")
                            ) {
                                Text(if (locale == AppLocale.NE) "पहिले छोड्नुहोस्" else "Skip")
                            }
                        }
                        RegistrationFormContent(
                            locale = locale,
                            isSubmitting = false,
                            submitButtonLabel = if (locale == AppLocale.NE) {
                                "दर्ता सुरक्षित गर्नुहोस्"
                            } else {
                                "Save Registration"
                            },
                            initialDisplayName = user.displayName.orEmpty(),
                            onSubmitValidDraft = mainViewModel::submitOnboardingRegistration
                        )
                    }
                }
            }
        }
    }
}
