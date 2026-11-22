package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.signOut
import com.example.data.ParsedResumeProposal
import com.example.i18n.AppLocale
import com.example.i18n.AppStrings
import com.example.model.InterviewSession
import com.example.model.ResumeVersion
import com.example.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.ProfileCompletionRing
import com.example.ui.components.SponsorDisclosureBar
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.viewmodel.AiCallState
import com.example.viewmodel.CoachTurn
import com.example.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileAndAiScreen(
    locale: AppLocale,
    strings: AppStrings,
    profile: UserProfile?,
    resumesState: UiState<List<ResumeVersion>>,
    resumeParseState: AiCallState<ParsedResumeProposal>,
    coachTurns: List<CoachTurn>,
    coachStreamState: AiCallState<String>,
    interviewSessionsState: UiState<List<InterviewSession>>,
    openResumeStudioOnStart: Boolean,
    openInterviewCoachOnStart: Boolean,
    onClearQuickOpenFlags: () -> Unit,
    onSwitchRole: (String) -> Unit,
    onUpdatePrivacyProfile: (String, String, String, List<String>, String, Boolean, Boolean) -> Unit,
    onAnalyzeResume: (String, Bitmap?, String) -> Unit,
    onResetResumeProposal: () -> Unit,
    onSaveResume: (String, String, String, String, String, List<String>, Long, Boolean, Long) -> Unit,
    onSendCoachMessage: (String, String, String) -> Unit,
    onResetCoach: () -> Unit,
    onSaveCoachSession: (String, String, Long, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Profile/Privacy, 1=Resume Studio, 2=Interview Coach

    LaunchedEffect(openResumeStudioOnStart, openInterviewCoachOnStart) {
        if (openResumeStudioOnStart) {
            selectedTab = 1
            onClearQuickOpenFlags()
        } else if (openInterviewCoachOnStart) {
            selectedTab = 2
            onClearQuickOpenFlags()
        }
    }

    if (selectedTab != 0) {
        BackHandler { selectedTab = 0 }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(strings.navProfile) },
                modifier = Modifier.testTag("profile_tab_overview")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (locale == AppLocale.NE) "AI बायोडाटा" else "Resume AI") },
                modifier = Modifier.testTag("profile_tab_resume")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(if (locale == AppLocale.NE) "अन्तर्वार्ता कोच" else "Coach AI") },
                modifier = Modifier.testTag("profile_tab_coach")
            )
        }

        when (selectedTab) {
            0 -> ProfilePrivacyTab(
                locale = locale,
                strings = strings,
                profile = profile,
                onSwitchRole = onSwitchRole,
                onUpdatePrivacyProfile = onUpdatePrivacyProfile
            )
            1 -> ResumeStudioTab(
                locale = locale,
                strings = strings,
                resumesState = resumesState,
                resumeParseState = resumeParseState,
                onAnalyzeResume = onAnalyzeResume,
                onResetProposal = onResetResumeProposal,
                onSaveResume = onSaveResume
            )
            else -> InterviewCoachTab(
                locale = locale,
                strings = strings,
                coachTurns = coachTurns,
                coachStreamState = coachStreamState,
                interviewSessionsState = interviewSessionsState,
                onSendMessage = onSendCoachMessage,
                onResetCoach = onResetCoach,
                onSaveSession = onSaveCoachSession
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfilePrivacyTab(
    locale: AppLocale,
    strings: AppStrings,
    profile: UserProfile?,
    onSwitchRole: (String) -> Unit,
    onUpdatePrivacyProfile: (String, String, String, List<String>, String, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
    var headline by remember(profile?.headline) { mutableStateOf(profile?.headline ?: "") }
    var district by remember(profile?.district) { mutableStateOf(profile?.district ?: "Kathmandu") }
    var skillsCsv by remember(profile?.skills) { mutableStateOf(profile?.skills?.joinToString(", ") ?: "") }
    var visibility by remember(profile?.visibility) { mutableStateOf(profile?.visibility ?: "private") }
    var contactHidden by remember(profile?.contactHidden) { mutableStateOf(profile?.contactHidden ?: true) }
    var consentsDiscovery by remember(profile?.consentsRecruiterDiscovery) {
        mutableStateOf(profile?.consentsRecruiterDiscovery ?: false)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                            text = profile?.displayName?.ifBlank { "Member" } ?: "Member",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile?.district ?: "Kathmandu"} • ${profile?.educationLevel ?: "Bachelor"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VerificationBadge(
                            level = profile?.verificationLevel ?: "self_reported",
                            detailNote = strings.verificationAdvisory
                        )
                    }
                }
            }
        }

        // Workspace Role Switcher
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (locale == AppLocale.NE) "कार्यक्षेत्र भूमिका परिवर्तन गर्नुहोस्" else "Switch Workspace Role",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "job_seeker" to strings.roleSeeker,
                            "employer" to strings.roleEmployer,
                            "sponsor" to strings.roleSponsor,
                            "platform_admin" to strings.roleAdmin
                        ).forEach { (roleKey, roleLabel) ->
                            FilterChip(
                                selected = (profile?.activeRole ?: "job_seeker") == roleKey,
                                onClick = { onSwitchRole(roleKey) },
                                label = { Text(roleLabel) },
                                modifier = Modifier.testTag("role_chip_$roleKey")
                            )
                        }
                    }
                }
            }
        }

        // Privacy & Consent Center
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = HimalayanNavy)
                        Text(strings.privacyCenterTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(if (locale == AppLocale.NE) "पूरा नाम" else "Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )

                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text(if (locale == AppLocale.NE) "व्यावसायिक शीर्षक" else "Headline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_headline_input")
                    )

                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text(if (locale == AppLocale.NE) "जिल्ला (जस्तै Kathmandu, Pokhara)" else "District") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_district_input")
                    )

                    OutlinedTextField(
                        value = skillsCsv,
                        onValueChange = { skillsCsv = it },
                        label = { Text(if (locale == AppLocale.NE) "सीपहरू (अल्पविरामले छुट्याउनुहोस्)" else "Skills (comma separated)") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_skills_input")
                    )

                    Text(strings.visibilityLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "private" to "Private",
                            "recruiter_visible" to "Opt-in Recruiters",
                            "public" to "Public"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = visibility == key,
                                onClick = { visibility = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("visibility_chip_$key")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.contactHiddenLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = "Phone & email never shown until you approve a contact request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = contactHidden,
                            onCheckedChange = { contactHidden = it },
                            modifier = Modifier.testTag("contact_hidden_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (locale == AppLocale.NE) "भर्तीकर्ता खोज सहमति" else "Opt-in Recruiter Discovery",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Allow verified employers to view your anonymized skills card.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = consentsDiscovery,
                            onCheckedChange = { consentsDiscovery = it },
                            modifier = Modifier.testTag("recruiter_consent_switch")
                        )
                    }

                    Button(
                        onClick = {
                            val parsedSkills = skillsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onUpdatePrivacyProfile(
                                displayName,
                                headline,
                                district,
                                parsedSkills,
                                visibility,
                                contactHidden,
                                consentsDiscovery
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_privacy_profile_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (locale == AppLocale.NE) "प्रोफाइल र गोपनीयता सुरक्षित गर्नुहोस्" else "Save Profile & Privacy")
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = { signOut(credentialManager, scope) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NepalCrimson),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.signOutLabel)
                Spacer(Modifier.width(8.dp))
                Text(strings.signOutLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumeStudioTab(
    locale: AppLocale,
    strings: AppStrings,
    resumesState: UiState<List<ResumeVersion>>,
    resumeParseState: AiCallState<ParsedResumeProposal>,
    onAnalyzeResume: (String, Bitmap?, String) -> Unit,
    onResetProposal: () -> Unit,
    onSaveResume: (String, String, String, String, String, List<String>, Long, Boolean, Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rawCvText by remember {
        mutableStateOf(
            "Civil & Infrastructure Engineer with 2 years experience at Bagmati Infra Works. " +
                "Skills: AutoCAD, Site Supervision, Kotlin, Project Planning. " +
                "Education: B.E. Civil Engineering, Tribhuvan University."
        )
    }
    var targetLang by remember { mutableStateOf(if (locale == AppLocale.NE) "ne" else "en") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Zero-permission Android Photo Picker (compliant with Google Play Photo & Video policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                selectedBitmap = decodeScaledBitmap(context, uri)
            }
        }
    }

    val currentResumes = (resumesState as? UiState.Success)?.data.orEmpty()
    val nextVersion = (currentResumes.maxOfOrNull { it.version } ?: 0L) + 1L

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SponsorDisclosureBar(text = strings.aiAdvisoryNotice)
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = HimalayanNavy)
                        Text(strings.resumeStudioTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Paste your CV text or pick a photo/scan of your resume. AI extracts structure & suggests edits without inventing experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = targetLang == "en",
                            onClick = { targetLang = "en" },
                            label = { Text("English CV") }
                        )
                        FilterChip(
                            selected = targetLang == "ne",
                            onClick = { targetLang = "ne" },
                            label = { Text("नेपाली बायोडाटा") }
                        )
                    }

                    OutlinedTextField(
                        value = rawCvText,
                        onValueChange = { rawCvText = it },
                        label = { Text("CV / Experience Text") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("resume_raw_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("resume_pick_photo_button")
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (selectedBitmap == null) "Attach CV Photo" else "Change Photo")
                        }
                        if (selectedBitmap != null) {
                            IconButton(onClick = { selectedBitmap = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove attached CV photo")
                            }
                        }
                    }

                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected CV image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Button(
                        onClick = { onAnalyzeResume(rawCvText, selectedBitmap, targetLang) },
                        enabled = (rawCvText.isNotBlank() || selectedBitmap != null) && resumeParseState !is AiCallState.Loading,
                        modifier = Modifier.fillMaxWidth().testTag("resume_analyze_ai_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze & Propose Advisory Diffs")
                    }
                }
            }
        }

        // AI Proposal Diff Card
        when (resumeParseState) {
            is AiCallState.Loading -> item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp))
                        Text("Parsing CV & preparing advisory diffs (never inventing data)...")
                    }
                }
            }
            is AiCallState.Error -> item {
                ErrorBannerCard(resumeParseState.message)
            }
            is AiCallState.Success -> item {
                val prop = resumeParseState.data
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("resume_proposal_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Advisory Proposal (v$nextVersion)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(color = EmeraldContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "ATS ${prop.atsScore}/100",
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(prop.headline, fontWeight = FontWeight.SemiBold)
                        Text(prop.summary, style = MaterialTheme.typography.bodySmall)

                        if (prop.skills.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                prop.skills.take(10).forEach { skill ->
                                    AssistChip(onClick = {}, label = { Text(skill) })
                                }
                            }
                        }

                        HorizontalDivider()
                        Text("Suggested Edits (User Confirmation Required):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        prop.improvementDiffs.forEach { diff ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Text(diff, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onResetProposal,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discard")
                            }
                            Button(
                                onClick = {
                                    onSaveResume(
                                        prop.headline.ifBlank { "Resume v$nextVersion" },
                                        targetLang,
                                        prop.summary,
                                        prop.experienceSummary,
                                        prop.educationSummary,
                                        prop.skills,
                                        prop.atsScore.toLong(),
                                        true,
                                        nextVersion
                                    )
                                    onResetProposal()
                                },
                                modifier = Modifier.weight(1f).testTag("resume_approve_save_button")
                            ) {
                                Text("Approve & Save")
                            }
                        }
                    }
                }
            }
            else -> Unit
        }

        // Saved Resume Versions
        item {
            Text(
                text = if (locale == AppLocale.NE) "सुरक्षित बायोडाटा संस्करणहरू" else "Saved Immutable Resume Versions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        when (resumesState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { ErrorBannerCard(resumesState.message) }
            is UiState.Success -> {
                if (resumesState.data.isEmpty()) {
                    item { EmptyStateCard("No Resume Versions Yet", "Use AI Resume Studio above to create your first CV.") }
                } else {
                    items(resumesState.data, key = { it.id }) { cv ->
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
                                    Text("v${cv.version} • ${cv.title}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("ATS ${cv.atsScore}%", color = EmeraldSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                                Text(
                                    text = cv.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )
                                Text(
                                    text = "${cv.language.uppercase()} • ${if (cv.aiAssisted) "AI-Assisted (User Approved)" else "User Authored"}",
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
}

@Composable
private fun InterviewCoachTab(
    locale: AppLocale,
    strings: AppStrings,
    coachTurns: List<CoachTurn>,
    coachStreamState: AiCallState<String>,
    interviewSessionsState: UiState<List<InterviewSession>>,
    onSendMessage: (String, String, String) -> Unit,
    onResetCoach: () -> Unit,
    onSaveSession: (String, String, Long, String, String) -> Unit
) {
    var roleTitle by remember { mutableStateOf("Civil Site Supervisor") }
    var coachLanguage by remember { mutableStateOf(if (locale == AppLocale.NE) "ne" else "en") }
    var messageInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SponsorDisclosureBar(text = strings.aiAdvisoryNotice)
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = NepalCrimson)
                        Text(strings.interviewCoachTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = roleTitle,
                        onValueChange = { roleTitle = it },
                        label = { Text("Target Role") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("coach_role_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = coachLanguage == "en",
                            onClick = { coachLanguage = "en" },
                            label = { Text("English Coach") }
                        )
                        FilterChip(
                            selected = coachLanguage == "ne",
                            onClick = { coachLanguage = "ne" },
                            label = { Text("नेपाली कोच") }
                        )
                    }
                }
            }
        }

        if (coachTurns.isEmpty() && coachStreamState is AiCallState.Idle) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Start a practice round", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Tap a starter prompt or type your introduction to get live streaming feedback.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                val starter = if (coachLanguage == "ne") {
                                    "नमस्ते, म $roleTitle पदको लागि अन्तर्वार्ता अभ्यास गर्न चाहन्छु। कृपया पहिलो प्रश्न सोध्नुहोस्।"
                                } else {
                                    "Hello, I am practicing for $roleTitle. Please ask me the first interview question."
                                }
                                onSendMessage(roleTitle, coachLanguage, starter)
                            },
                            modifier = Modifier.testTag("coach_start_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (coachLanguage == "ne") "अभ्यास सुरु गर्नुहोस्" else "Start Mock Interview")
                        }
                    }
                }
            }
        }

        items(coachTurns) { turn ->
            val isUser = turn.speaker == "user"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isUser) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isUser) "You" else "Interview Coach (Advisory)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        when (coachStreamState) {
            is AiCallState.Loading -> item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Coach is preparing response...", style = MaterialTheme.typography.bodySmall)
                }
            }
            is AiCallState.Streaming -> item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Interview Coach (Streaming...)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(coachStreamState.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is AiCallState.Error -> item {
                ErrorBannerCard(coachStreamState.message)
            }
            else -> Unit
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text(if (coachLanguage == "ne") "तपाईंको उत्तर लेख्नुहोस्..." else "Type your answer...") },
                    modifier = Modifier.weight(1f).testTag("coach_message_input")
                )
                Button(
                    onClick = {
                        val trimmed = messageInput.trim()
                        if (trimmed.isNotBlank()) {
                            onSendMessage(roleTitle, coachLanguage, trimmed)
                            messageInput = ""
                        }
                    },
                    enabled = messageInput.isNotBlank() && coachStreamState !is AiCallState.Loading && coachStreamState !is AiCallState.Streaming,
                    modifier = Modifier.height(54.dp).testTag("coach_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send interview response")
                }
            }
        }

        if (coachTurns.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onResetCoach,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Session")
                    }
                    Button(
                        onClick = {
                            val lastModel = coachTurns.lastOrNull { it.speaker == "model" }?.text ?: "Practice completed"
                            val preview = coachTurns.joinToString("\n") { "${it.speaker}: ${it.text}" }
                            val userCount = coachTurns.count { it.speaker == "user" }.toLong().coerceAtLeast(1L)
                            onSaveSession(roleTitle, coachLanguage, userCount, lastModel, preview)
                        },
                        modifier = Modifier.weight(1f).testTag("coach_save_session_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Summary")
                    }
                }
            }
        }

        if (interviewSessionsState is UiState.Success && interviewSessionsState.data.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Saved Practice Sessions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(interviewSessionsState.data, key = { it.id }) { session ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${session.roleTitle} (${session.language.uppercase()}) • ${session.questionCount} turns", fontWeight = FontWeight.SemiBold)
                        Text(session.summaryFeedback, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                }
            }
        }
    }
}

private suspend fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxDim = 1024
        var sample = 1
        while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}
