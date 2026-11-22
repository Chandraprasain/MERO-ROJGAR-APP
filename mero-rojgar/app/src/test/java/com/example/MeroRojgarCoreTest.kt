package com.example

import com.example.domain.MatchEngine
import com.example.i18n.AppLocale
import com.example.i18n.formatNpr
import com.example.i18n.formatPaisaAsNpr
import com.example.model.JobListing
import com.example.model.LedgerEntry
import com.example.model.UserProfile
import com.example.model.WalletSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeroRojgarCoreTest {

    @Test
    fun ledger_derives_balances_accurately_from_double_entry_events() {
        val entries = listOf(
            LedgerEntry(id = "e1", accountKind = "user_available", direction = "credit", amountPaisa = 5500L),
            LedgerEntry(id = "e2", accountKind = "user_available", direction = "credit", amountPaisa = 6500L),
            LedgerEntry(id = "e3", accountKind = "user_available", direction = "debit", amountPaisa = 10000L),
            LedgerEntry(id = "e4", accountKind = "withdrawal_hold", direction = "credit", amountPaisa = 10000L)
        )
        val summary = WalletSummary.deriveFromLedger(entries)
        assertEquals(2000L, summary.availablePaisa)
        assertEquals(10000L, summary.heldPaisa)
        assertEquals(0L, summary.pendingPaisa)
        assertEquals(0L, summary.paidOutPaisa)
    }

    @Test
    fun explainable_match_computes_weighted_breakdown_and_missing_skills() {
        val profile = UserProfile(
            userId = "u1",
            displayName = "Sita",
            district = "Kathmandu",
            experienceMonths = 24L,
            educationLevel = "Bachelor",
            expectedSalaryMin = 45000L,
            expectedSalaryMax = 80000L,
            skills = listOf("AutoCAD", "Safety")
        )
        val job = JobListing(
            id = "j1",
            title = "Civil Supervisor",
            district = "Kathmandu",
            workMode = "onsite",
            minExperienceMonths = 24L,
            educationLevel = "Bachelor",
            salaryMin = 50000L,
            salaryMax = 75000L,
            requiredSkills = listOf("AutoCAD", "Safety", "Project Planning", "BOQ")
        )
        val breakdown = MatchEngine.computeMatch(profile, job)
        assertEquals(50, breakdown.skillsPercent)
        assertEquals(100, breakdown.experiencePercent)
        assertEquals(100, breakdown.qualificationPercent)
        assertEquals(100, breakdown.locationPercent)
        assertEquals(100, breakdown.salaryPercent)
        assertEquals(80, breakdown.overallPercent)
        assertEquals(listOf("AutoCAD", "Safety"), breakdown.matchedSkills)
        assertEquals(listOf("Project Planning", "BOQ"), breakdown.missingSkills)
    }

    @Test
    fun scam_risk_precheck_flags_upfront_registration_fee() {
        val (risk, reason) = MatchEngine.evaluateScamRisk(
            title = "Urgent Office Helper",
            description = "Pay registration fee of NPR 1500 and contact on WhatsApp only",
            orgPan = "",
            salaryMin = 15000L,
            salaryMax = 700000L
        )
        assertTrue(risk >= 0.45)
        assertTrue(reason.contains("upfront fee", ignoreCase = true))
    }

    @Test
    fun npr_and_paisa_formatting_matches_locale() {
        assertEquals("रु 45,000", formatNpr(45000L, AppLocale.NE))
        assertEquals("NPR 45,000", formatNpr(45000L, AppLocale.EN))
        assertEquals("रु 55", formatPaisaAsNpr(5500L, AppLocale.NE))
        assertEquals("NPR 65.50", formatPaisaAsNpr(6550L, AppLocale.EN))
    }
}
