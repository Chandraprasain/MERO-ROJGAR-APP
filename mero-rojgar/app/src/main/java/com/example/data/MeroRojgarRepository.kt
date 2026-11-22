package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.MatchEngine
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
import com.example.model.WithdrawalRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MeroRojgarRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logFirestoreErrors(
        uid: String,
        op: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    // --- Real-time Observations ---

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    fun observePublishedJobs(): Flow<List<JobListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("jobs")
                .whereEqualTo("status", "published")
                .snapshots()
                .map { snap ->
                    snap.toObjects(JobListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.urgent }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "jobs")
        )
    }

    suspend fun fetchPublishedJobsOnce(): Result<List<JobListing>> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val snap = try {
                db.collection("jobs")
                    .whereEqualTo("status", "published")
                    .get(Source.SERVER)
                    .await()
            } catch (_: Exception) {
                db.collection("jobs")
                    .whereEqualTo("status", "published")
                    .get(Source.DEFAULT)
                    .await()
            }
            val list = snap.toObjects(JobListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.urgent }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "jobs")
            Result.failure(e)
        }
    }

    fun observeSavedJobs(userId: String): Flow<List<SavedJob>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/saved_jobs"
        emitAll(
            db.collection("users").document(uid).collection("saved_jobs")
                .snapshots()
                .map { snap -> snap.toObjects(SavedJob::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeEmployerJobs(userId: String): Flow<List<JobListing>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("jobs")
                .whereEqualTo("postedByUserId", uid)
                .snapshots()
                .map { snap -> snap.toObjects(JobListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, "jobs")
        )
    }

    fun observeEmployerApplicants(userId: String): Flow<List<EmployerApplicant>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("employer_applicants")
                .whereEqualTo("employerUserId", uid)
                .snapshots()
                .map { snap -> snap.toObjects(EmployerApplicant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, "employer_applicants")
        )
    }

    fun observeMyApplications(userId: String): Flow<List<JobApplication>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/applications"
        emitAll(
            db.collection("users").document(uid).collection("applications")
                .snapshots()
                .map { snap -> snap.toObjects(JobApplication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeResumes(userId: String): Flow<List<ResumeVersion>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/resumes"
        emitAll(
            db.collection("users").document(uid).collection("resumes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ResumeVersion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.version }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeAutoApplyRules(userId: String): Flow<List<AutoApplyRule>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/auto_apply_rules"
        emitAll(
            db.collection("users").document(uid).collection("auto_apply_rules")
                .snapshots()
                .map { snap -> snap.toObjects(AutoApplyRule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeOpenGigs(): Flow<List<GigListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("gigs")
                .whereEqualTo("status", "open")
                .snapshots()
                .map { snap -> snap.toObjects(GigListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, "gigs")
        )
    }

    fun observeLiveCampaigns(): Flow<List<SponsorCampaign>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("campaigns")
                .whereEqualTo("status", "live")
                .snapshots()
                .map { snap -> snap.toObjects(SponsorCampaign::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, "campaigns")
        )
    }

    fun observeLedgerEntries(userId: String): Flow<List<LedgerEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/ledger_entries"
        emitAll(
            db.collection("users").document(uid).collection("ledger_entries")
                .snapshots()
                .map { snap -> snap.toObjects(LedgerEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeRewardCompletions(userId: String): Flow<List<RewardCompletion>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/reward_completions"
        emitAll(
            db.collection("users").document(uid).collection("reward_completions")
                .snapshots()
                .map { snap -> snap.toObjects(RewardCompletion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/withdrawals"
        emitAll(
            db.collection("users").document(uid).collection("withdrawals")
                .snapshots()
                .map { snap -> snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeSkillEvidence(userId: String): Flow<List<SkillEvidence>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/skill_evidence"
        emitAll(
            db.collection("users").document(uid).collection("skill_evidence")
                .snapshots()
                .map { snap -> snap.toObjects(SkillEvidence::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeInterviewSessions(userId: String): Flow<List<InterviewSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/interview_sessions"
        emitAll(
            db.collection("users").document(uid).collection("interview_sessions")
                .snapshots()
                .map { snap -> snap.toObjects(InterviewSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeTalentDirectory(): Flow<List<TalentDirectoryEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("talent_directory")
                .whereIn("visibility", listOf("public", "recruiter_visible"))
                .snapshots()
                .map { snap -> snap.toObjects(TalentDirectoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(uid, OperationType.LIST, "talent_directory")
        )
    }

    // --- Initialization & Seed Data ---

    suspend fun ensureInitializedAndSeeded(
        displayName: String,
        registrationDraft: RegistrationDraft? = null
    ): Result<Boolean> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val cleanDraft = registrationDraft?.sanitized()
            var isNewWithoutDraft = false

            if (!userSnap.exists()) {
                val cleanName = cleanDraft?.displayName?.ifBlank { displayName }
                    ?: displayName.ifBlank { "Aarav Shrestha" }.take(80)
                val initialProfile = if (cleanDraft != null) {
                    val visibility = if (cleanDraft.consentsRecruiterDiscovery) "recruiter_visible" else "private"
                    UserProfile(
                        userId = uid,
                        displayName = cleanName,
                        locale = cleanDraft.locale,
                        activeRole = cleanDraft.activeRole,
                        headline = cleanDraft.headline.ifBlank { "Mero Rojgar Member" },
                        district = cleanDraft.district,
                        availability = "Immediate",
                        visibility = visibility,
                        contactHidden = true,
                        expectedSalaryMin = cleanDraft.expectedSalaryMin,
                        expectedSalaryMax = cleanDraft.expectedSalaryMax,
                        experienceMonths = 12L,
                        educationLevel = "Bachelor",
                        skills = cleanDraft.skills.ifEmpty { listOf("Communication", "Nepali") },
                        verificationLevel = "self_reported",
                        consentsRecruiterDiscovery = cleanDraft.consentsRecruiterDiscovery,
                        consentsRewardsTerms = cleanDraft.consentsRewardsTerms
                    )
                } else {
                    isNewWithoutDraft = true
                    UserProfile(
                        userId = uid,
                        displayName = cleanName,
                        locale = "ne",
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
                }
                userRef.set(initialProfile.toCreateMap()).await()

                if (initialProfile.consentsRecruiterDiscovery) {
                    db.collection("talent_directory").document(uid).set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to initialProfile.displayName.take(80),
                            "headline" to initialProfile.headline.take(120),
                            "district" to initialProfile.district.take(60),
                            "skills" to initialProfile.skills.take(30),
                            "verificationLevel" to "self_reported",
                            "visibility" to initialProfile.visibility,
                            "openStatus" to "open_to_opportunities",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                userRef.collection("resumes").document("resume_v1").set(
                    mapOf(
                        "userId" to uid,
                        "title" to "${initialProfile.displayName.take(32)} CV (v1)",
                        "language" to if (initialProfile.locale == "en") "en" else "ne",
                        "summary" to initialProfile.headline.ifBlank {
                            "Results-driven professional building verified skills across Nepal."
                        }.take(1500),
                        "experienceText" to "Junior Engineer, Bagmati Infra Works (2024–2026) — Managed BOQ verification and safety compliance.",
                        "educationText" to "B.E. Civil Engineering, Tribhuvan University",
                        "skills" to initialProfile.skills.ifEmpty { listOf("Communication", "Nepali") }.take(30),
                        "atsScore" to 78L,
                        "aiAssisted" to false,
                        "defaultCv" to true,
                        "version" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                userRef.collection("skill_evidence").document("skill_ev_1").set(
                    mapOf(
                        "userId" to uid,
                        "skillName" to (initialProfile.skills.firstOrNull() ?: "AutoCAD & Structural Drafting").take(60),
                        "level" to 4L,
                        "verificationLevel" to "assessment_completed",
                        "provider" to "Mero Skill Council",
                        "method" to "Practical Drafting Check",
                        "scorePercent" to 88L,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                userRef.collection("skill_evidence").document("skill_ev_2").set(
                    mapOf(
                        "userId" to uid,
                        "skillName" to "Site Supervision",
                        "level" to 3L,
                        "verificationLevel" to "self_reported",
                        "provider" to "Candidate Declaration",
                        "method" to "Self-Reported (Unverified)",
                        "scorePercent" to 0L,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (cleanDraft != null) {
                completeAccountRegistration(cleanDraft)
            }

            // Seed marketplace jobs if none published yet
            val existingJobs = db.collection("jobs").whereEqualTo("status", "published").limit(1).get().await()
            if (existingJobs.isEmpty) {
                seedNepalJobs(uid)
            }

            val existingGigs = db.collection("gigs").whereEqualTo("status", "open").limit(1).get().await()
            if (existingGigs.isEmpty) {
                seedNepalGigs(uid, cleanDraft?.displayName ?: displayName.ifBlank { "Sita Gurung" })
            }

            val existingCampaigns = db.collection("campaigns").whereEqualTo("status", "live").limit(1).get().await()
            if (existingCampaigns.isEmpty) {
                seedSponsorCampaigns(uid)
            }

            val existingApplicants = db.collection("employer_applicants")
                .whereEqualTo("employerUserId", uid).limit(1).get().await()
            if (existingApplicants.isEmpty) {
                seedEmployerApplicants(uid)
            }

            Result.success(isNewWithoutDraft)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/init")
            Result.failure(e)
        }
    }

    suspend fun completeAccountRegistration(registrationDraft: RegistrationDraft): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val clean = registrationDraft.sanitized()
        val visibility = if (clean.consentsRecruiterDiscovery) "recruiter_visible" else "private"
        return try {
            val batch = db.batch()
            val userRef = db.collection("users").document(uid)
            batch.update(
                userRef,
                mapOf(
                    "displayName" to clean.displayName.ifBlank { "Member" },
                    "locale" to clean.locale,
                    "activeRole" to clean.activeRole,
                    "headline" to clean.headline,
                    "district" to clean.district,
                    "visibility" to visibility,
                    "expectedSalaryMin" to clean.expectedSalaryMin,
                    "expectedSalaryMax" to clean.expectedSalaryMax,
                    "skills" to clean.skills,
                    "consentsRecruiterDiscovery" to clean.consentsRecruiterDiscovery,
                    "consentsRewardsTerms" to clean.consentsRewardsTerms,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val talentRef = db.collection("talent_directory").document(uid)
            if (clean.consentsRecruiterDiscovery) {
                batch.set(
                    talentRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to clean.displayName.ifBlank { "Member" },
                        "headline" to clean.headline,
                        "district" to clean.district,
                        "skills" to clean.skills,
                        "verificationLevel" to "self_reported",
                        "visibility" to visibility,
                        "openStatus" to "open_to_opportunities",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.delete(talentRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    private suspend fun seedNepalJobs(ownerUid: String) {
        val batch = db.batch()
        val jobs = listOf(
            "seed_job_ktm_civil" to mapOf(
                "postedByUserId" to ownerUid,
                "orgName" to "Himalayan Hydropower Ltd.",
                "orgPan" to "601482910",
                "orgVerificationLevel" to 3L,
                "title" to "Senior Civil Site Supervisor",
                "titleNe" to "वरिष्ठ सिभिल साइट सुपरभाइजर",
                "description" to "Lead concrete QA, BOQ tracking, and site safety compliance for our Trishuli corridor project. Transparent monthly NPR salary and SSF benefits.",
                "employmentType" to "full_time",
                "workMode" to "onsite",
                "district" to "Kathmandu",
                "salaryMin" to 55000L,
                "salaryMax" to 85000L,
                "salaryVisible" to true,
                "requiredSkills" to listOf("AutoCAD", "Site Supervision", "Project Planning", "Safety"),
                "minExperienceMonths" to 24L,
                "educationLevel" to "Bachelor",
                "urgent" to true,
                "scamRiskScore" to 0.04,
                "scamRiskReason" to "Admin-reviewed PAN, SSF compliant, transparent NPR range",
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_job_lalitpur_kotlin" to mapOf(
                "postedByUserId" to ownerUid,
                "orgName" to "Kathmandu FinTech Labs",
                "orgPan" to "609218433",
                "orgVerificationLevel" to 2L,
                "title" to "Android Kotlin Engineer (Compose)",
                "titleNe" to "एन्ड्रोइड कोटलिन इन्जिनियर",
                "description" to "Build low-bandwidth offline-first Android apps for Nepali merchants using Jetpack Compose, Coroutines, and Firebase.",
                "employmentType" to "full_time",
                "workMode" to "hybrid",
                "district" to "Lalitpur",
                "salaryMin" to 70000L,
                "salaryMax" to 120000L,
                "salaryVisible" to true,
                "requiredSkills" to listOf("Kotlin", "Jetpack Compose", "Firebase", "REST API"),
                "minExperienceMonths" to 18L,
                "educationLevel" to "Bachelor",
                "urgent" to false,
                "scamRiskScore" to 0.05,
                "scamRiskReason" to "PAN document submitted, transparent salary",
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_job_pokhara_elec" to mapOf(
                "postedByUserId" to ownerUid,
                "orgName" to "Annapurna Solar & Grid Co.",
                "orgPan" to "302918221",
                "orgVerificationLevel" to 2L,
                "title" to "Certified Solar & Electrical Technician",
                "titleNe" to "दक्ष सोलार तथा इलेक्ट्रिकल प्राविधिक",
                "description" to "Install and maintain commercial hybrid solar inverters and 3-phase panels across Kaski & Gandaki.",
                "employmentType" to "full_time",
                "workMode" to "onsite",
                "district" to "Pokhara",
                "salaryMin" to 32000L,
                "salaryMax" to 48000L,
                "salaryVisible" to true,
                "requiredSkills" to listOf("Electrical Wiring", "Solar Inverter", "Safety", "Nepali"),
                "minExperienceMonths" to 12L,
                "educationLevel" to "Diploma",
                "urgent" to true,
                "scamRiskScore" to 0.06,
                "scamRiskReason" to "Verified company registration, clear CTEVT preference",
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_job_biratnagar_ops" to mapOf(
                "postedByUserId" to ownerUid,
                "orgName" to "Koshi Agri-Logistics Pvt. Ltd.",
                "orgPan" to "501293841",
                "orgVerificationLevel" to 3L,
                "title" to "Supply Chain & Dispatch Coordinator",
                "titleNe" to "आपूर्ति श्रृंखला समन्वयक",
                "description" to "Coordinate cold-chain agricultural shipments from Morang farms to Kathmandu retail hubs.",
                "employmentType" to "full_time",
                "workMode" to "onsite",
                "district" to "Biratnagar",
                "salaryMin" to 38000L,
                "salaryMax" to 55000L,
                "salaryVisible" to true,
                "requiredSkills" to listOf("Project Planning", "Excel", "Nepali", "Communication"),
                "minExperienceMonths" to 12L,
                "educationLevel" to "Bachelor",
                "urgent" to false,
                "scamRiskScore" to 0.05,
                "scamRiskReason" to "Verified employer, no fee indicators",
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_job_remote_design" to mapOf(
                "postedByUserId" to ownerUid,
                "orgName" to "Everest Digital Collective",
                "orgPan" to "",
                "orgVerificationLevel" to 1L,
                "title" to "Product UI/UX Designer (Nepali & English)",
                "titleNe" to "प्रोडक्ट UI/UX डिजाइनर",
                "description" to "Design accessible mobile-first interfaces supporting Devanagari typography and low-end Android devices.",
                "employmentType" to "contract",
                "workMode" to "remote",
                "district" to "Kathmandu",
                "salaryMin" to 45000L,
                "salaryMax" to 75000L,
                "salaryVisible" to true,
                "requiredSkills" to listOf("Figma", "Devanagari Typography", "Design Systems", "Communication"),
                "minExperienceMonths" to 12L,
                "educationLevel" to "Bachelor",
                "urgent" to false,
                "scamRiskScore" to 0.14,
                "scamRiskReason" to "Contact verified; PAN pending upload",
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        for ((docId, payload) in jobs) {
            batch.set(db.collection("jobs").document(docId), payload)
        }
        batch.commit().await()
    }

    private suspend fun seedNepalGigs(ownerUid: String, posterName: String) {
        val batch = db.batch()
        val gigs = listOf(
            "seed_gig_solar_ktm" to mapOf(
                "postedByUserId" to ownerUid,
                "posterName" to posterName,
                "title" to "3kW Rooftop Solar & Inverter Checkup",
                "category" to "Electrician / Solar",
                "district" to "Kathmandu",
                "budgetNpr" to 4500L,
                "localTrade" to true,
                "milestoneCount" to 2L,
                "description" to "Inspect battery bank wiring and configure MPPT charge controller in Patan/Kathmandu.",
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_gig_nepali_menu" to mapOf(
                "postedByUserId" to ownerUid,
                "posterName" to "Lakeside Roasters",
                "title" to "Bilingual Nepali/English Cafe Menu & QR Poster",
                "category" to "Graphic Design",
                "district" to "Pokhara",
                "budgetNpr" to 8000L,
                "localTrade" to false,
                "milestoneCount" to 2L,
                "description" to "Print-ready A3 & mobile menu design in Inter and Noto Sans Devanagari.",
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_gig_plumbing_btl" to mapOf(
                "postedByUserId" to ownerUid,
                "posterName" to "Lumbini Residency",
                "title" to "PPR Hot-Water Line Fitting (2 Bathrooms)",
                "category" to "Plumbing / Local Trade",
                "district" to "Butwal",
                "budgetNpr" to 6500L,
                "localTrade" to true,
                "milestoneCount" to 2L,
                "description" to "Licensed plumber needed for weekend PPR pipe fitting and pressure testing.",
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        for ((id, data) in gigs) {
            batch.set(db.collection("gigs").document(id), data)
        }
        batch.commit().await()
    }

    private suspend fun seedSponsorCampaigns(ownerUid: String) {
        val batch = db.batch()
        val campaigns = listOf(
            "seed_camp_ntc_fiber" to mapOf(
                "sponsorUserId" to ownerUid,
                "sponsorName" to "Nepal Telecom Fiber",
                "title" to "Digital Nepal: Safe Home Fiber Setup",
                "titleNe" to "डिजिटल नेपाल: सुरक्षित फाइबर इन्टरनेट",
                "videoSeconds" to 12L,
                "rewardPaisa" to 5500L,
                "fundedPaisa" to 550000L,
                "spentPaisa" to 11000L,
                "questionPrompt" to "Which feature helps protect your home Wi-Fi router?",
                "questionOptions" to listOf("WPA3 / strong unique password", "Sharing admin password publicly", "Disabling all encryption"),
                "correctOptionIndex" to 0L,
                "termsSummary" to "Funded by Nepal Telecom. Watch >= 90% and pass check. 1 completion per account.",
                "endsAtText" to "Ends Dec 31",
                "status" to "live",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_camp_hbl_remit" to mapOf(
                "sponsorUserId" to ownerUid,
                "sponsorName" to "Himalayan Remit Safe",
                "title" to "Formal Remittance & Scam Prevention Guide",
                "titleNe" to "सुरक्षित रेमिट्यान्स र ठगी रोकथाम",
                "videoSeconds" to 10L,
                "rewardPaisa" to 6500L,
                "fundedPaisa" to 650000L,
                "spentPaisa" to 19500L,
                "questionPrompt" to "How should foreign remittance be sent safely?",
                "questionOptions" to listOf("Through licensed banks & remitters only", "Through unverified Hundi agents", "Sharing OTP codes"),
                "correctOptionIndex" to 0L,
                "termsSummary" to "Sponsored financial literacy campaign. Not employment income.",
                "endsAtText" to "Ends Dec 25",
                "status" to "live",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        for ((id, data) in campaigns) {
            batch.set(db.collection("campaigns").document(id), data)
        }
        batch.commit().await()
    }

    private suspend fun seedEmployerApplicants(employerUid: String) {
        val batch = db.batch()
        val applicants = listOf(
            "seed_emp_app_1" to mapOf(
                "employerUserId" to employerUid,
                "applicantUserId" to employerUid,
                "applicantName" to "Prakash Adhikari",
                "applicantHeadline" to "Civil Overseer • 3 yrs Hydropower",
                "jobId" to "seed_job_ktm_civil",
                "jobTitle" to "Senior Civil Site Supervisor",
                "stage" to "shortlisted",
                "matchScore" to 91L,
                "matchReasons" to "Skills 95% • Experience 100% • Kathmandu match",
                "source" to "manual",
                "notes" to "Verified CTEVT Diploma; ready for site interview.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "seed_emp_app_2" to mapOf(
                "employerUserId" to employerUid,
                "applicantUserId" to employerUid,
                "applicantName" to "Anisha Maharjan",
                "applicantHeadline" to "Compose & Firebase Engineer",
                "jobId" to "seed_job_lalitpur_kotlin",
                "jobTitle" to "Android Kotlin Engineer (Compose)",
                "stage" to "interview",
                "matchScore" to 88L,
                "matchReasons" to "Kotlin & Compose match • Lalitpur local",
                "source" to "manual",
                "notes" to "Strong portfolio of offline POS apps.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        for ((id, data) in applicants) {
            batch.set(db.collection("employer_applicants").document(id), data)
        }
        batch.commit().await()
    }

    // --- Mutations ---

    suspend fun updateLocale(localeCode: String): Result<Unit> = updateUserFields(
        mapOf("locale" to localeCode)
    )

    suspend fun updateActiveRole(role: String): Result<Unit> = updateUserFields(
        mapOf("activeRole" to role)
    )

    suspend fun updatePrivacyAndProfile(
        displayName: String,
        headline: String,
        district: String,
        skills: List<String>,
        visibility: String,
        contactHidden: Boolean,
        consentsRecruiterDiscovery: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanName = displayName.trim().ifBlank { "Member" }.take(80)
        val cleanHeadline = headline.trim().take(120)
        val cleanDistrict = district.trim().ifBlank { "Kathmandu" }.take(60)
        val cleanSkills = skills.map { it.trim() }.filter { it.isNotBlank() }.take(30)

        return try {
            val batch = db.batch()
            val userRef = db.collection("users").document(uid)
            batch.update(
                userRef,
                mapOf(
                    "displayName" to cleanName,
                    "headline" to cleanHeadline,
                    "district" to cleanDistrict,
                    "skills" to cleanSkills,
                    "visibility" to visibility,
                    "contactHidden" to contactHidden,
                    "consentsRecruiterDiscovery" to consentsRecruiterDiscovery,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val talentRef = db.collection("talent_directory").document(uid)
            if (visibility == "public" || visibility == "recruiter_visible") {
                batch.set(
                    talentRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "headline" to cleanHeadline,
                        "district" to cleanDistrict,
                        "skills" to cleanSkills,
                        "verificationLevel" to "self_reported",
                        "visibility" to visibility,
                        "openStatus" to "open_to_opportunities",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.delete(talentRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    private suspend fun updateUserFields(fields: Map<String, Any>): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val payload = fields + ("updatedAt" to FieldValue.serverTimestamp())
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun applyToJob(
        job: JobListing,
        profile: UserProfile?,
        resumeId: String,
        source: String,
        questionnaireAnswer: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val breakdown = MatchEngine.computeMatch(profile, job)
        val appId = "app_${job.id}_${uid}".take(120).replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return try {
            val batch = db.batch()
            val userAppRef = db.collection("users").document(uid).collection("applications").document(appId)
            batch.set(
                userAppRef,
                mapOf(
                    "userId" to uid,
                    "jobId" to job.id,
                    "jobTitle" to job.title.take(120),
                    "orgName" to job.orgName.take(120),
                    "resumeId" to resumeId.ifBlank { "resume_v1" }.take(128),
                    "source" to source,
                    "stage" to "new",
                    "matchScore" to breakdown.overallPercent.toLong(),
                    "matchSummary" to breakdown.reasons.joinToString("; ").take(300),
                    "questionnaireSummary" to questionnaireAnswer.trim().take(1000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (job.postedByUserId.isNotBlank()) {
                val empAppRef = db.collection("employer_applicants").document(appId)
                batch.set(
                    empAppRef,
                    mapOf(
                        "employerUserId" to job.postedByUserId,
                        "applicantUserId" to uid,
                        "applicantName" to (profile?.displayName ?: "Candidate").take(80),
                        "applicantHeadline" to (profile?.headline ?: "").take(120),
                        "jobId" to job.id,
                        "jobTitle" to job.title.take(120),
                        "stage" to "new",
                        "matchScore" to breakdown.overallPercent.toLong(),
                        "matchReasons" to breakdown.reasons.joinToString(" • ").take(400),
                        "source" to if (source == "auto_apply") "auto_apply" else "manual",
                        "notes" to questionnaireAnswer.trim().take(1000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/applications/$appId")
            Result.failure(e)
        }
    }

    suspend fun withdrawApplication(applicationId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).collection("applications").document(applicationId)
                .update(
                    mapOf(
                        "stage" to "withdrawn",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/applications/$applicationId")
            Result.failure(e)
        }
    }

    suspend fun toggleSavedJob(job: JobListing, currentlySaved: Boolean): Result<Boolean> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val safeJobId = job.id.take(128).replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val ref = db.collection("users").document(uid).collection("saved_jobs").document(safeJobId)
        return try {
            if (currentlySaved) {
                ref.delete().await()
                Result.success(false)
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "jobId" to safeJobId,
                        "jobTitle" to job.title.take(120),
                        "orgName" to job.orgName.take(120),
                        "savedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/saved_jobs/$safeJobId")
            Result.failure(e)
        }
    }

    suspend fun saveResumeVersion(
        title: String,
        language: String,
        summary: String,
        experienceText: String,
        educationText: String,
        skills: List<String>,
        atsScore: Long,
        aiAssisted: Boolean,
        nextVersion: Long
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "resume_v${nextVersion}_${System.currentTimeMillis()}"
        return try {
            db.collection("users").document(uid).collection("resumes").document(docId).set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifBlank { "Resume v$nextVersion" }.take(100),
                    "language" to if (language == "ne") "ne" else "en",
                    "summary" to summary.trim().take(1500),
                    "experienceText" to experienceText.trim().take(3000),
                    "educationText" to educationText.trim().take(1500),
                    "skills" to skills.map { it.trim() }.filter { it.isNotBlank() }.take(30),
                    "atsScore" to atsScore.coerceIn(0L, 100L),
                    "aiAssisted" to aiAssisted,
                    "defaultCv" to true,
                    "version" to nextVersion.coerceAtLeast(1L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/resumes/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveAutoApplyRule(
        ruleName: String,
        keywords: String,
        district: String,
        workMode: String,
        minSalaryNpr: Long,
        minMatchThreshold: Long,
        dailyCap: Long,
        excludedOrgs: String,
        approvedResumeId: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "rule_${System.currentTimeMillis()}"
        return try {
            db.collection("users").document(uid).collection("auto_apply_rules").document(docId).set(
                mapOf(
                    "userId" to uid,
                    "ruleName" to ruleName.trim().ifBlank { "My Nepal Job Rule" }.take(80),
                    "keywords" to keywords.trim().take(120),
                    "district" to district.trim().take(60),
                    "workMode" to workMode,
                    "minSalaryNpr" to minSalaryNpr.coerceAtLeast(0L),
                    "minMatchThreshold" to minMatchThreshold.coerceIn(0L, 100L),
                    "dailyCap" to dailyCap.coerceIn(1L, 20L),
                    "excludedOrgs" to excludedOrgs.trim().take(300),
                    "approvedResumeId" to approvedResumeId.ifBlank { "resume_v1" }.take(128),
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/auto_apply_rules/$docId")
            Result.failure(e)
        }
    }

    suspend fun toggleAutoApplyRule(ruleId: String, active: Boolean): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("users").document(uid).collection("auto_apply_rules").document(ruleId)
                .update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/auto_apply_rules/$ruleId")
            Result.failure(e)
        }
    }

    suspend fun completeRewardCampaign(
        campaign: SponsorCampaign,
        watchedMs: Long
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val txnId = "txn_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val batch = db.batch()
            val completionRef = db.collection("users").document(uid)
                .collection("reward_completions").document(campaign.id)
            batch.set(
                completionRef,
                mapOf(
                    "userId" to uid,
                    "campaignId" to campaign.id,
                    "campaignTitle" to campaign.title.take(120),
                    "sponsorName" to campaign.sponsorName.take(100),
                    "watchedMs" to watchedMs.coerceAtLeast(1000L),
                    "questionPassed" to true,
                    "rewardPaisa" to campaign.rewardPaisa,
                    "status" to "approved",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            // Append-only double-entry credit into user_available
            val ledgerRef = db.collection("users").document(uid)
                .collection("ledger_entries").document("${txnId}_credit")
            batch.set(
                ledgerRef,
                mapOf(
                    "userId" to uid,
                    "txnId" to txnId,
                    "accountKind" to "user_available",
                    "direction" to "credit",
                    "amountPaisa" to campaign.rewardPaisa,
                    "reason" to "completion_approved: ${campaign.sponsorName}".take(120),
                    "campaignId" to campaign.id,
                    "idempotencyKey" to "${campaign.id}_$uid",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/reward_completions/${campaign.id}")
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(
        amountPaisa: Long,
        method: String,
        destinationMasked: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (amountPaisa < 10000L) {
            return Result.failure(IllegalArgumentException("Minimum withdrawal is NPR 100 (10,000 paisa)."))
        }
        val txnId = "wtxn_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val wId = "w_${System.currentTimeMillis()}"
        return try {
            val batch = db.batch()
            val wRef = db.collection("users").document(uid).collection("withdrawals").document(wId)
            batch.set(
                wRef,
                mapOf(
                    "userId" to uid,
                    "amountPaisa" to amountPaisa,
                    "method" to method,
                    "destinationMasked" to destinationMasked.trim().take(80),
                    "status" to "requested",
                    "providerRef" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val debitAvailRef = db.collection("users").document(uid)
                .collection("ledger_entries").document("${txnId}_debit")
            batch.set(
                debitAvailRef,
                mapOf(
                    "userId" to uid,
                    "txnId" to txnId,
                    "accountKind" to "user_available",
                    "direction" to "debit",
                    "amountPaisa" to amountPaisa,
                    "reason" to "withdrawal_hold ($method)".take(120),
                    "idempotencyKey" to "${wId}_avail_debit",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            val creditHoldRef = db.collection("users").document(uid)
                .collection("ledger_entries").document("${txnId}_hold")
            batch.set(
                creditHoldRef,
                mapOf(
                    "userId" to uid,
                    "txnId" to txnId,
                    "accountKind" to "withdrawal_hold",
                    "direction" to "credit",
                    "amountPaisa" to amountPaisa,
                    "reason" to "withdrawal_requested ($method)".take(120),
                    "idempotencyKey" to "${wId}_hold_credit",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/withdrawals/$wId")
            Result.failure(e)
        }
    }

    suspend fun addSkillEvidence(
        skillName: String,
        level: Long,
        provider: String,
        method: String,
        verificationLevel: String,
        scorePercent: Long
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "skill_${System.currentTimeMillis()}"
        return try {
            db.collection("users").document(uid).collection("skill_evidence").document(docId).set(
                mapOf(
                    "userId" to uid,
                    "skillName" to skillName.trim().take(60),
                    "level" to level.coerceIn(1L, 5L),
                    "verificationLevel" to verificationLevel,
                    "provider" to provider.trim().ifBlank { "Candidate Declaration" }.take(100),
                    "method" to method.trim().ifBlank { "Self-Reported" }.take(100),
                    "scorePercent" to scorePercent.coerceIn(0L, 100L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/skill_evidence/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveInterviewSession(
        roleTitle: String,
        language: String,
        questionCount: Long,
        summaryFeedback: String,
        transcriptPreview: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "int_${System.currentTimeMillis()}"
        return try {
            db.collection("users").document(uid).collection("interview_sessions").document(docId).set(
                mapOf(
                    "userId" to uid,
                    "roleTitle" to roleTitle.trim().take(100),
                    "language" to if (language == "ne") "ne" else "en",
                    "questionCount" to questionCount.coerceAtLeast(1L),
                    "summaryFeedback" to summaryFeedback.take(2000),
                    "transcriptPreview" to transcriptPreview.take(3000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/interview_sessions/$docId")
            Result.failure(e)
        }
    }

    suspend fun postJobListing(
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
        requiredSkills: List<String>,
        urgent: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val (riskScore, riskReason) = MatchEngine.evaluateScamRisk(
            title = title,
            description = description,
            orgPan = orgPan,
            salaryMin = salaryMin,
            salaryMax = salaryMax
        )
        val status = if (riskScore >= 0.45) "pending_review" else "published"
        val docId = "job_${System.currentTimeMillis()}"
        return try {
            db.collection("jobs").document(docId).set(
                mapOf(
                    "postedByUserId" to uid,
                    "orgName" to orgName.trim().take(100),
                    "orgPan" to orgPan.trim().take(30),
                    "orgVerificationLevel" to if (orgPan.isNotBlank()) 2L else 1L,
                    "title" to title.trim().take(120),
                    "titleNe" to titleNe.trim().take(120),
                    "description" to description.trim().take(5000),
                    "employmentType" to employmentType,
                    "workMode" to workMode,
                    "district" to district.trim().take(60),
                    "salaryMin" to salaryMin.coerceAtLeast(0L),
                    "salaryMax" to salaryMax.coerceAtLeast(salaryMin.coerceAtLeast(0L)),
                    "salaryVisible" to true,
                    "requiredSkills" to requiredSkills.take(20),
                    "minExperienceMonths" to 12L,
                    "educationLevel" to "Bachelor",
                    "urgent" to urgent,
                    "scamRiskScore" to riskScore,
                    "scamRiskReason" to riskReason.take(300),
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "jobs/$docId")
            Result.failure(e)
        }
    }

    suspend fun updateApplicantStage(applicantId: String, newStage: String, notes: String): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("employer_applicants").document(applicantId).update(
                mapOf(
                    "stage" to newStage,
                    "notes" to notes.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "employer_applicants/$applicantId")
            Result.failure(e)
        }
    }

    suspend fun postGigListing(
        posterName: String,
        title: String,
        category: String,
        district: String,
        budgetNpr: Long,
        localTrade: Boolean,
        milestoneCount: Long,
        description: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "gig_${System.currentTimeMillis()}"
        return try {
            db.collection("gigs").document(docId).set(
                mapOf(
                    "postedByUserId" to uid,
                    "posterName" to posterName.trim().ifBlank { "Member" }.take(80),
                    "title" to title.trim().take(120),
                    "category" to category.trim().take(60),
                    "district" to district.trim().take(60),
                    "budgetNpr" to budgetNpr.coerceIn(100L, 10_000_000L),
                    "localTrade" to localTrade,
                    "milestoneCount" to milestoneCount.coerceIn(1L, 10L),
                    "description" to description.trim().take(2000),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "gigs/$docId")
            Result.failure(e)
        }
    }

    suspend fun createSponsorCampaign(
        sponsorName: String,
        title: String,
        videoSeconds: Long,
        rewardPaisa: Long,
        fundedPaisa: Long,
        questionPrompt: String,
        options: List<String>
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "camp_${System.currentTimeMillis()}"
        return try {
            db.collection("campaigns").document(docId).set(
                mapOf(
                    "sponsorUserId" to uid,
                    "sponsorName" to sponsorName.trim().take(100),
                    "title" to title.trim().take(120),
                    "titleNe" to title.trim().take(120),
                    "videoSeconds" to videoSeconds.coerceIn(5L, 300L),
                    "rewardPaisa" to rewardPaisa.coerceIn(100L, 100_000L),
                    "fundedPaisa" to fundedPaisa.coerceAtLeast(rewardPaisa),
                    "spentPaisa" to 0L,
                    "questionPrompt" to questionPrompt.trim().take(300),
                    "questionOptions" to options.take(4),
                    "correctOptionIndex" to 0L,
                    "termsSummary" to "Funded by $sponsorName. Optional educational reward.",
                    "endsAtText" to "Ends Next Month",
                    "status" to "live",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "campaigns/$docId")
            Result.failure(e)
        }
    }
}
