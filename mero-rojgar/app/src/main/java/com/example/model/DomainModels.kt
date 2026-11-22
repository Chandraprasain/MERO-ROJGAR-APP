package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val locale: String = "ne",
    val activeRole: String = "job_seeker",
    val headline: String = "",
    val district: String = "Kathmandu",
    val availability: String = "Immediate",
    val visibility: String = "private",
    val contactHidden: Boolean = true,
    val expectedSalaryMin: Long = 35000L,
    val expectedSalaryMax: Long = 70000L,
    val experienceMonths: Long = 12L,
    val educationLevel: String = "Bachelor",
    val skills: List<String> = emptyList(),
    val verificationLevel: String = "self_reported",
    val consentsRecruiterDiscovery: Boolean = false,
    val consentsRewardsTerms: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun completionPercent(): Int {
        var score = 20
        if (displayName.isNotBlank()) score += 15
        if (headline.isNotBlank()) score += 15
        if (district.isNotBlank()) score += 10
        if (skills.isNotEmpty()) score += 20
        if (educationLevel.isNotBlank()) score += 10
        if (expectedSalaryMin > 0) score += 10
        return score.coerceIn(0, 100)
    }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.ifBlank { "Member" }.take(80),
        "locale" to locale,
        "activeRole" to activeRole,
        "headline" to headline.take(120),
        "district" to district.take(60),
        "availability" to availability.take(40),
        "visibility" to visibility,
        "contactHidden" to contactHidden,
        "expectedSalaryMin" to expectedSalaryMin,
        "expectedSalaryMax" to expectedSalaryMax,
        "experienceMonths" to experienceMonths,
        "educationLevel" to educationLevel.take(60),
        "skills" to skills.take(30),
        "verificationLevel" to verificationLevel,
        "consentsRecruiterDiscovery" to consentsRecruiterDiscovery,
        "consentsRewardsTerms" to consentsRewardsTerms,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ResumeVersion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val language: String = "en",
    val summary: String = "",
    val experienceText: String = "",
    val educationText: String = "",
    val skills: List<String> = emptyList(),
    val atsScore: Long = 70L,
    val aiAssisted: Boolean = false,
    val defaultCv: Boolean = false,
    val version: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JobListing(
    @DocumentId val id: String = "",
    val postedByUserId: String = "",
    val orgName: String = "",
    val orgPan: String = "",
    val orgVerificationLevel: Long = 1L,
    val title: String = "",
    val titleNe: String = "",
    val description: String = "",
    val employmentType: String = "full_time",
    val workMode: String = "onsite",
    val district: String = "Kathmandu",
    val salaryMin: Long = 0L,
    val salaryMax: Long = 0L,
    val salaryVisible: Boolean = true,
    val requiredSkills: List<String> = emptyList(),
    val minExperienceMonths: Long = 0L,
    val educationLevel: String = "Bachelor",
    val urgent: Boolean = false,
    val scamRiskScore: Double = 0.05,
    val scamRiskReason: String = "Standard listing checks passed",
    val status: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JobApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val orgName: String = "",
    val resumeId: String = "",
    val source: String = "manual",
    val stage: String = "new",
    val matchScore: Long = 0L,
    val matchSummary: String = "",
    val questionnaireSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AutoApplyRule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ruleName: String = "",
    val keywords: String = "",
    val district: String = "",
    val workMode: String = "any",
    val minSalaryNpr: Long = 30000L,
    val minMatchThreshold: Long = 70L,
    val dailyCap: Long = 5L,
    val excludedOrgs: String = "",
    val approvedResumeId: String = "default",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LedgerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txnId: String = "",
    val accountKind: String = "user_available",
    val direction: String = "credit",
    val amountPaisa: Long = 0L,
    val reason: String = "",
    val campaignId: String = "",
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RewardCompletion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val campaignId: String = "",
    val campaignTitle: String = "",
    val sponsorName: String = "",
    val watchedMs: Long = 0L,
    val questionPassed: Boolean = false,
    val rewardPaisa: Long = 0L,
    val status: String = "approved",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amountPaisa: Long = 10000L,
    val method: String = "esewa",
    val destinationMasked: String = "",
    val status: String = "requested",
    val providerRef: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SkillEvidence(
    @DocumentId val id: String = "",
    val userId: String = "",
    val skillName: String = "",
    val level: Long = 3L,
    val verificationLevel: String = "self_reported",
    val provider: String = "",
    val method: String = "",
    val scorePercent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InterviewSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roleTitle: String = "",
    val language: String = "en",
    val questionCount: Long = 1L,
    val summaryFeedback: String = "",
    val transcriptPreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EmployerApplicant(
    @DocumentId val id: String = "",
    val employerUserId: String = "",
    val applicantUserId: String = "",
    val applicantName: String = "",
    val applicantHeadline: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val stage: String = "new",
    val matchScore: Long = 0L,
    val matchReasons: String = "",
    val source: String = "manual",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GigListing(
    @DocumentId val id: String = "",
    val postedByUserId: String = "",
    val posterName: String = "",
    val title: String = "",
    val category: String = "",
    val district: String = "Kathmandu",
    val budgetNpr: Long = 5000L,
    val localTrade: Boolean = false,
    val milestoneCount: Long = 2L,
    val description: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SponsorCampaign(
    @DocumentId val id: String = "",
    val sponsorUserId: String = "",
    val sponsorName: String = "",
    val title: String = "",
    val titleNe: String = "",
    val videoSeconds: Long = 15L,
    val rewardPaisa: Long = 2500L,
    val fundedPaisa: Long = 500000L,
    val spentPaisa: Long = 0L,
    val questionPrompt: String = "",
    val questionOptions: List<String> = emptyList(),
    val correctOptionIndex: Long = 0L,
    val termsSummary: String = "",
    val endsAtText: String = "",
    val status: String = "live",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val remainingPaisa: Long get() = (fundedPaisa - spentPaisa).coerceAtLeast(0L)
}

@Keep
@IgnoreExtraProperties
data class TalentDirectoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val headline: String = "",
    val district: String = "Kathmandu",
    val skills: List<String> = emptyList(),
    val verificationLevel: String = "self_reported",
    val visibility: String = "recruiter_visible",
    val openStatus: String = "open_to_opportunities",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val orgName: String = "",
    @ServerTimestamp val savedAt: Timestamp? = null
)

data class WalletSummary(
    val pendingPaisa: Long = 0L,
    val availablePaisa: Long = 0L,
    val paidOutPaisa: Long = 0L,
    val heldPaisa: Long = 0L
) {
    companion object {
        fun deriveFromLedger(entries: List<LedgerEntry>): WalletSummary {
            var pending = 0L
            var available = 0L
            var paidOut = 0L
            var held = 0L
            for (entry in entries) {
                val delta = if (entry.direction == "credit") entry.amountPaisa else -entry.amountPaisa
                when (entry.accountKind) {
                    "user_pending" -> pending += delta
                    "user_available" -> available += delta
                    "user_paid_out" -> paidOut += delta
                    "withdrawal_hold" -> held += delta
                }
            }
            return WalletSummary(
                pendingPaisa = pending.coerceAtLeast(0L),
                availablePaisa = available.coerceAtLeast(0L),
                paidOutPaisa = paidOut.coerceAtLeast(0L),
                heldPaisa = held.coerceAtLeast(0L)
            )
        }
    }
}

data class MatchBreakdown(
    val overallPercent: Int,
    val skillsPercent: Int,
    val experiencePercent: Int,
    val qualificationPercent: Int,
    val locationPercent: Int,
    val salaryPercent: Int,
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
    val reasons: List<String>,
    val modelVersion: String = "merorojgar-explainable-v1.0"
)

data class RegistrationDraft(
    val displayName: String,
    val activeRole: String = "job_seeker",
    val locale: String = "ne",
    val district: String = "Kathmandu",
    val headline: String = "",
    val skills: List<String> = emptyList(),
    val expectedSalaryMin: Long = 35000L,
    val expectedSalaryMax: Long = 75000L,
    val consentsRecruiterDiscovery: Boolean = false,
    val consentsRewardsTerms: Boolean = true
) {
    fun sanitized(): RegistrationDraft {
        val allowedRoles = setOf("job_seeker", "employer", "sponsor")
        val safeRole = if (activeRole in allowedRoles) activeRole else "job_seeker"
        val safeLocale = if (locale == "en") "en" else "ne"
        val safeMin = expectedSalaryMin.coerceAtLeast(0L)
        val safeMax = expectedSalaryMax.coerceAtLeast(safeMin)
        return copy(
            displayName = displayName.trim().take(80),
            activeRole = safeRole,
            locale = safeLocale,
            district = district.trim().ifBlank { "Kathmandu" }.take(60),
            headline = headline.trim().take(120),
            skills = skills.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(30),
            expectedSalaryMin = safeMin,
            expectedSalaryMax = safeMax
        )
    }
}

sealed interface RegistrationValidationResult {
    data class Valid(val draft: RegistrationDraft) : RegistrationValidationResult
    data class Invalid(val messageEn: String, val messageNe: String) : RegistrationValidationResult
}

fun validateRegistrationDraft(
    displayName: String,
    activeRole: String,
    locale: String,
    district: String,
    headline: String,
    skills: List<String>,
    salaryMinText: String,
    salaryMaxText: String,
    consentsRecruiterDiscovery: Boolean,
    consentsRewardsTerms: Boolean
): RegistrationValidationResult {
    val trimmedName = displayName.trim()
    if (trimmedName.length < 2) {
        return RegistrationValidationResult.Invalid(
            messageEn = "Please enter your full name (at least 2 characters).",
            messageNe = "कृपया आफ्नो पूरा नाम (कम्तीमा २ अक्षर) लेख्नुहोस्।"
        )
    }
    if (trimmedName.length > 80) {
        return RegistrationValidationResult.Invalid(
            messageEn = "Full name must be 80 characters or fewer.",
            messageNe = "पूरा नाम ८० अक्षर वा सोभन्दा कम हुनुपर्छ।"
        )
    }
    val trimmedDistrict = district.trim()
    if (trimmedDistrict.isBlank() || trimmedDistrict.length > 60) {
        return RegistrationValidationResult.Invalid(
            messageEn = "Please enter a valid Nepal district (1–60 characters).",
            messageNe = "कृपया मान्य जिल्ला नाम (१–६० अक्षर) लेख्नुहोस्।"
        )
    }
    if (headline.trim().length > 120) {
        return RegistrationValidationResult.Invalid(
            messageEn = "Professional headline must be 120 characters or fewer.",
            messageNe = "पेशेवर शीर्षक १२० अक्षर वा सोभन्दा कम हुनुपर्छ।"
        )
    }
    val minSalary = salaryMinText.trim().takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L
    val maxSalary = salaryMaxText.trim().takeIf { it.isNotEmpty() }?.toLongOrNull() ?: minSalary
    if (minSalary < 0L || maxSalary < minSalary) {
        return RegistrationValidationResult.Invalid(
            messageEn = "Maximum expected salary must be greater than or equal to minimum salary.",
            messageNe = "अधिकतम अपेक्षित तलब न्यूनतम तलब बराबर वा बढी हुनुपर्छ।"
        )
    }
    if (!consentsRewardsTerms) {
        return RegistrationValidationResult.Invalid(
            messageEn = "Please accept the platform terms & verification notice to register.",
            messageNe = "दर्ता गर्न कृपया प्लेटफर्म नियम र प्रमाणिकरण सूचना स्वीकार गर्नुहोस्।"
        )
    }

    val draft = RegistrationDraft(
        displayName = trimmedName,
        activeRole = activeRole,
        locale = locale,
        district = trimmedDistrict,
        headline = headline.trim(),
        skills = skills,
        expectedSalaryMin = minSalary,
        expectedSalaryMax = maxSalary,
        consentsRecruiterDiscovery = consentsRecruiterDiscovery,
        consentsRewardsTerms = consentsRewardsTerms
    ).sanitized()
    return RegistrationValidationResult.Valid(draft)
}

