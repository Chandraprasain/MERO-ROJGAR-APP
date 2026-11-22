package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MeroRojgarRepository
import com.example.i18n.AppLocale
import com.example.model.AutoApplyRule
import com.example.model.EmployerApplicant
import com.example.model.GigListing
import com.example.model.InterviewSession
import com.example.model.JobApplication
import com.example.model.JobListing
import com.example.model.LedgerEntry
import com.example.model.RegistrationDraft
import com.example.model.RewardCompletion
import com.example.model.ResumeVersion
import com.example.model.SavedJob
import com.example.model.SkillEvidence
import com.example.model.SponsorCampaign
import com.example.model.TalentDirectoryEntry
import com.example.model.UserProfile
import com.example.model.WalletSummary
import com.example.model.WithdrawalRequest
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainAppViewModel(
    private val repository: MeroRojgarRepository,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val pendingRegistrationDraft: RegistrationDraft? = null,
    private val onRegistrationDraftConsumed: () -> Unit = {}
) : ViewModel() {

    private companion object {
        const val TAG = "MainAppViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId).toUiState("profile")

    private val _manualJobsOverride = MutableStateFlow<List<JobListing>?>(null)

    val jobsState: StateFlow<UiState<List<JobListing>>> =
        combine(
            repository.observePublishedJobs(),
            _manualJobsOverride
        ) { liveList, manualList ->
            // Prefer live stream once non-empty or updated; manualList gives instant pull-to-refresh sync
            manualList?.takeIf { liveList.isEmpty() } ?: liveList
        }.toUiState("jobs")

    val savedJobsState: StateFlow<UiState<List<SavedJob>>> =
        repository.observeSavedJobs(currentUserId).toUiState("saved jobs")

    private val _isRefreshingJobs = MutableStateFlow(false)
    val isRefreshingJobs: StateFlow<Boolean> = _isRefreshingJobs.asStateFlow()

    val employerJobsState: StateFlow<UiState<List<JobListing>>> =
        repository.observeEmployerJobs(currentUserId).toUiState("employer jobs")

    val employerApplicantsState: StateFlow<UiState<List<EmployerApplicant>>> =
        repository.observeEmployerApplicants(currentUserId).toUiState("employer applicants")

    val applicationsState: StateFlow<UiState<List<JobApplication>>> =
        repository.observeMyApplications(currentUserId).toUiState("applications")

    val resumesState: StateFlow<UiState<List<ResumeVersion>>> =
        repository.observeResumes(currentUserId).toUiState("resumes")

    val autoApplyRulesState: StateFlow<UiState<List<AutoApplyRule>>> =
        repository.observeAutoApplyRules(currentUserId).toUiState("auto apply rules")

    val gigsState: StateFlow<UiState<List<GigListing>>> =
        repository.observeOpenGigs().toUiState("gigs")

    val campaignsState: StateFlow<UiState<List<SponsorCampaign>>> =
        repository.observeLiveCampaigns().toUiState("campaigns")

    val ledgerState: StateFlow<UiState<List<LedgerEntry>>> =
        repository.observeLedgerEntries(currentUserId).toUiState("ledger")

    val walletSummaryState: StateFlow<WalletSummary> =
        repository.observeLedgerEntries(currentUserId)
            .map { WalletSummary.deriveFromLedger(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Wallet summary error", firestoreError ?: error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WalletSummary())

    val completionsState: StateFlow<UiState<List<RewardCompletion>>> =
        repository.observeRewardCompletions(currentUserId).toUiState("completions")

    val withdrawalsState: StateFlow<UiState<List<WithdrawalRequest>>> =
        repository.observeWithdrawals(currentUserId).toUiState("withdrawals")

    val skillEvidenceState: StateFlow<UiState<List<SkillEvidence>>> =
        repository.observeSkillEvidence(currentUserId).toUiState("skill evidence")

    val interviewSessionsState: StateFlow<UiState<List<InterviewSession>>> =
        repository.observeInterviewSessions(currentUserId).toUiState("interview sessions")

    val talentDirectoryState: StateFlow<UiState<List<TalentDirectoryEntry>>> =
        repository.observeTalentDirectory().toUiState("talent directory")

    private val _locale = MutableStateFlow(AppLocale.NE)
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _needsOnboardingPrompt = MutableStateFlow(false)
    val needsOnboardingPrompt: StateFlow<Boolean> = _needsOnboardingPrompt.asStateFlow()

    init {
        viewModelScope.launch {
            val initResult = repository.ensureInitializedAndSeeded(
                displayName = initialDisplayName,
                registrationDraft = pendingRegistrationDraft
            )
            if (pendingRegistrationDraft != null) {
                onRegistrationDraftConsumed()
                if (initResult.isSuccess) {
                    _bannerMessage.value = "Account registered for ${pendingRegistrationDraft.displayName}"
                }
            }
            initResult.onSuccess { isNewWithoutDraft ->
                if (isNewWithoutDraft) {
                    _needsOnboardingPrompt.value = true
                }
            }
        }
        viewModelScope.launch {
            profileState.collect { state ->
                if (state is UiState.Success && state.data != null) {
                    _locale.value = if (state.data.locale == "en") AppLocale.EN else AppLocale.NE
                }
            }
        }
    }

    fun dismissOnboardingPrompt() {
        _needsOnboardingPrompt.value = false
    }

    fun submitOnboardingRegistration(draft: RegistrationDraft) {
        viewModelScope.launch {
            val res = repository.completeAccountRegistration(draft)
            if (res.isSuccess) {
                _needsOnboardingPrompt.value = false
                _locale.value = if (draft.locale == "en") AppLocale.EN else AppLocale.NE
                _bannerMessage.value = "Account registration saved"
            } else {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not save registration"
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun toggleLocale() {
        val next = if (_locale.value == AppLocale.NE) AppLocale.EN else AppLocale.NE
        _locale.value = next
        viewModelScope.launch {
            repository.updateLocale(next.code)
        }
    }

    fun switchRole(role: String) {
        viewModelScope.launch {
            val res = repository.updateActiveRole(role)
            if (res.isFailure) {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to switch role"
            }
        }
    }

    fun applyToJob(job: JobListing, profile: UserProfile?, resumeId: String, source: String = "manual", answer: String = "") {
        viewModelScope.launch {
            val res = repository.applyToJob(job, profile, resumeId, source, answer)
            _bannerMessage.value = if (res.isSuccess) {
                "Application submitted for ${job.title}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not submit application"
            }
        }
    }

    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            val res = repository.withdrawApplication(applicationId)
            _bannerMessage.value = if (res.isSuccess) "Application withdrawn" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun refreshJobs() {
        if (_isRefreshingJobs.value) return
        viewModelScope.launch {
            _isRefreshingJobs.value = true
            try {
                val res = repository.fetchPublishedJobsOnce()
                delay(250L)
                res.onSuccess { list ->
                    _manualJobsOverride.value = list
                }.onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Failed to refresh jobs"
                }
            } finally {
                _isRefreshingJobs.value = false
            }
        }
    }

    fun toggleSavedJob(job: JobListing, currentlySaved: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleSavedJob(job, currentlySaved)
            _bannerMessage.value = res.fold(
                onSuccess = { savedNow ->
                    if (savedNow) "Saved \"${job.title}\" to bookmarks" else "Removed from saved jobs"
                },
                onFailure = { it.localizedMessage ?: "Could not update bookmark" }
            )
        }
    }

    fun saveResume(
        title: String,
        language: String,
        summary: String,
        experienceText: String,
        educationText: String,
        skills: List<String>,
        atsScore: Long,
        aiAssisted: Boolean,
        nextVersion: Long
    ) {
        viewModelScope.launch {
            val res = repository.saveResumeVersion(
                title, language, summary, experienceText, educationText, skills, atsScore, aiAssisted, nextVersion
            )
            _bannerMessage.value = if (res.isSuccess) "Saved Resume v$nextVersion" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveAutoApplyRule(
        ruleName: String,
        keywords: String,
        district: String,
        workMode: String,
        minSalaryNpr: Long,
        minMatchThreshold: Long,
        dailyCap: Long,
        excludedOrgs: String,
        approvedResumeId: String
    ) {
        viewModelScope.launch {
            val res = repository.saveAutoApplyRule(
                ruleName, keywords, district, workMode, minSalaryNpr, minMatchThreshold, dailyCap, excludedOrgs, approvedResumeId
            )
            _bannerMessage.value = if (res.isSuccess) "Auto Apply rule saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun toggleAutoApplyRule(ruleId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleAutoApplyRule(ruleId, active)
        }
    }

    fun completeRewardCampaign(campaign: SponsorCampaign, watchedMs: Long) {
        viewModelScope.launch {
            val res = repository.completeRewardCampaign(campaign, watchedMs)
            _bannerMessage.value = if (res.isSuccess) {
                "Reward credited to ledger: +NPR ${campaign.rewardPaisa / 100}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Completion failed or already credited"
            }
        }
    }

    fun requestWithdrawal(amountPaisa: Long, method: String, destinationMasked: String) {
        viewModelScope.launch {
            val res = repository.requestWithdrawal(amountPaisa, method, destinationMasked)
            _bannerMessage.value = if (res.isSuccess) {
                "Withdrawal queued for review"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Withdrawal request failed"
            }
        }
    }

    fun addSkillEvidence(
        skillName: String,
        level: Long,
        provider: String,
        method: String,
        verificationLevel: String,
        scorePercent: Long
    ) {
        viewModelScope.launch {
            val res = repository.addSkillEvidence(skillName, level, provider, method, verificationLevel, scorePercent)
            _bannerMessage.value = if (res.isSuccess) "Skill Passport updated" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveInterviewSession(roleTitle: String, language: String, count: Long, feedback: String, preview: String) {
        viewModelScope.launch {
            repository.saveInterviewSession(roleTitle, language, count, feedback, preview)
        }
    }

    fun updatePrivacyAndProfile(
        displayName: String,
        headline: String,
        district: String,
        skills: List<String>,
        visibility: String,
        contactHidden: Boolean,
        consentsRecruiterDiscovery: Boolean
    ) {
        viewModelScope.launch {
            val res = repository.updatePrivacyAndProfile(
                displayName, headline, district, skills, visibility, contactHidden, consentsRecruiterDiscovery
            )
            _bannerMessage.value = if (res.isSuccess) "Profile & privacy settings saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun postJob(
        orgName: String,
        orgPan: String,
        title: String,
        titleNe: String,
        description: String,
        employmentType: String,
        workMode: String,
        district: String,
        salaryMin: Long,
        salaryMax: Long,
        skills: List<String>,
        urgent: Boolean
    ) {
        viewModelScope.launch {
            val res = repository.postJobListing(
                orgName, orgPan, title, titleNe, description, employmentType, workMode, district, salaryMin, salaryMax, skills, urgent
            )
            _bannerMessage.value = if (res.isSuccess) "Job submitted to marketplace" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun updateApplicantStage(applicantId: String, stage: String, notes: String) {
        viewModelScope.launch {
            val res = repository.updateApplicantStage(applicantId, stage, notes)
            _bannerMessage.value = if (res.isSuccess) "Moved candidate to $stage" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun postGig(
        posterName: String,
        title: String,
        category: String,
        district: String,
        budgetNpr: Long,
        localTrade: Boolean,
        milestones: Long,
        description: String
    ) {
        viewModelScope.launch {
            val res = repository.postGigListing(
                posterName, title, category, district, budgetNpr, localTrade, milestones, description
            )
            _bannerMessage.value = if (res.isSuccess) "Gig posted" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun createCampaign(
        sponsorName: String,
        title: String,
        videoSeconds: Long,
        rewardPaisa: Long,
        fundedPaisa: Long,
        questionPrompt: String,
        options: List<String>
    ) {
        viewModelScope.launch {
            val res = repository.createSponsorCampaign(
                sponsorName, title, videoSeconds, rewardPaisa, fundedPaisa, questionPrompt, options
            )
            _bannerMessage.value = if (res.isSuccess) "Sponsor campaign launched" else res.exceptionOrNull()?.localizedMessage
        }
    }
}
