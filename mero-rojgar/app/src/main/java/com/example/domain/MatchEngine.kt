package com.example.domain

import com.example.model.JobListing
import com.example.model.MatchBreakdown
import com.example.model.UserProfile
import kotlin.math.roundToInt

/**
 * Explainable Match Engine & Scam Risk Evaluator.
 *
 * Weights (versioned):
 *   0.40 * skills + 0.20 * experience + 0.15 * qualification + 0.15 * location + 0.10 * salary
 *
 * Explicitly EXCLUDED protected attributes:
 *   gender, age, religion, ethnicity, caste, marital status, disability, photo.
 */
object MatchEngine {

    val EXCLUDED_PROTECTED_ATTRIBUTES = setOf(
        "gender", "age", "religion", "ethnicity", "caste",
        "marital_status", "disability", "photo"
    )

    fun computeMatch(profile: UserProfile?, job: JobListing): MatchBreakdown {
        if (profile == null) {
            return MatchBreakdown(
                overallPercent = 65,
                skillsPercent = 60,
                experiencePercent = 70,
                qualificationPercent = 75,
                locationPercent = 65,
                salaryPercent = 60,
                matchedSkills = emptyList(),
                missingSkills = job.requiredSkills,
                reasons = listOf("Complete your profile skills & district for tailored matching.")
            )
        }

        val candidateSkillsLower = profile.skills.map { it.trim().lowercase() }.toSet()
        val requiredSkills = job.requiredSkills.filter { it.isNotBlank() }

        val matched = requiredSkills.filter { req ->
            val r = req.trim().lowercase()
            candidateSkillsLower.any { it == r || it.contains(r) || r.contains(it) }
        }
        val missing = requiredSkills.filterNot { it in matched }

        val skillsScore = if (requiredSkills.isEmpty()) 0.85
        else (matched.size.toDouble() / requiredSkills.size.toDouble()).coerceIn(0.0, 1.0)

        val expScore = if (job.minExperienceMonths <= 0L) 1.0
        else (profile.experienceMonths.toDouble() / job.minExperienceMonths.toDouble()).coerceIn(0.25, 1.0)

        val qualScore = qualificationScore(profile.educationLevel, job.educationLevel)

        val locScore = when {
            job.workMode == "remote" -> 1.0
            profile.district.equals(job.district, ignoreCase = true) -> 1.0
            job.workMode == "hybrid" -> 0.70
            else -> 0.35
        }

        val salaryScore = when {
            job.salaryMax <= 0L || profile.expectedSalaryMin <= 0L -> 0.75
            job.salaryMax >= profile.expectedSalaryMin && job.salaryMin <= profile.expectedSalaryMax -> 1.0
            job.salaryMax >= (profile.expectedSalaryMin * 0.8) -> 0.65
            else -> 0.30
        }

        val overall = (
            0.40 * skillsScore +
                0.20 * expScore +
                0.15 * qualScore +
                0.15 * locScore +
                0.10 * salaryScore
            ).coerceIn(0.0, 1.0)

        val reasons = buildList {
            if (matched.isNotEmpty()) add("Matched skills: ${matched.joinToString(", ")}")
            if (locScore >= 0.99) {
                add(if (job.workMode == "remote") "Remote compatible" else "District match: ${job.district}")
            }
            if (expScore >= 0.99) add("Meets ${job.minExperienceMonths}+ months experience")
            if (salaryScore >= 0.99 && job.salaryVisible) add("Salary overlaps your NPR expectation")
        }

        return MatchBreakdown(
            overallPercent = (overall * 100).roundToInt(),
            skillsPercent = (skillsScore * 100).roundToInt(),
            experiencePercent = (expScore * 100).roundToInt(),
            qualificationPercent = (qualScore * 100).roundToInt(),
            locationPercent = (locScore * 100).roundToInt(),
            salaryPercent = (salaryScore * 100).roundToInt(),
            matchedSkills = matched,
            missingSkills = missing,
            reasons = reasons.ifEmpty { listOf("Partial profile match based on general criteria") }
        )
    }

    private fun qualificationScore(candidateEdu: String, requiredEdu: String): Double {
        fun rank(edu: String): Int {
            val e = edu.lowercase()
            return when {
                "phd" in e || "doctor" in e -> 5
                "master" in e || "msc" in e || "mba" in e -> 4
                "bachelor" in e || "be" in e || "bsc" in e || "bba" in e -> 3
                "diploma" in e || "ctevt" in e || "+2" in e || "slc" in e -> 2
                else -> 1
            }
        }
        val candRank = rank(candidateEdu)
        val reqRank = rank(requiredEdu)
        return if (candRank >= reqRank) 1.0 else (candRank.toDouble() / reqRank.toDouble()).coerceIn(0.4, 0.9)
    }

    /**
     * Deterministic Scam Risk Pre-Check combining rule signals.
     * High scores route to human moderation review rather than silent deletion.
     */
    fun evaluateScamRisk(
        title: String,
        description: String,
        orgPan: String,
        salaryMin: Long,
        salaryMax: Long
    ): Pair<Double, String> {
        val text = "$title $description".lowercase()
        val reasons = mutableListOf<String>()
        var risk = 0.05

        val feeKeywords = listOf("registration fee", "deposit money", "send money", "processing charge", "रकम जम्मा", "दर्ता शुल्क")
        if (feeKeywords.any { it in text }) {
            risk += 0.55
            reasons += "Mentions upfront fee/deposit"
        }
        val offPlatform = listOf("whatsapp only", "telegram only", "dm on whatsapp", "ह्वाट्सएप मात्र")
        if (offPlatform.any { it in text }) {
            risk += 0.25
            reasons += "Off-platform contact only"
        }
        if (orgPan.isBlank()) {
            risk += 0.12
            reasons += "No PAN/registration provided"
        }
        if (salaryMax > 600_000L && salaryMin < 20_000L) {
            risk += 0.18
            reasons += "Suspiciously wide salary range"
        }

        val clamped = risk.coerceIn(0.0, 0.99)
        val summary = if (reasons.isEmpty()) {
            "Low risk: transparent salary & no upfront-fee signals"
        } else {
            reasons.joinToString(" • ")
        }
        return clamped to summary
    }
}
