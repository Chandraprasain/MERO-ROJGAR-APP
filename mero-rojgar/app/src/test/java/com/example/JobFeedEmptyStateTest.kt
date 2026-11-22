package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.i18n.AppLocale
import com.example.ui.components.JobFeedEmptyScenario
import com.example.ui.components.JobFeedEmptyState
import com.example.ui.components.JobFeedQuickPreset
import com.example.ui.components.resolveJobFeedEmptyScenario
import com.example.ui.theme.MeroRojgarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JobFeedEmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `resolveJobFeedEmptyScenario distinguishes empty feed, search results, and saved`() {
        assertEquals(
            JobFeedEmptyScenario.EMPTY_FEED,
            resolveJobFeedEmptyScenario(totalPublishedJobs = 0, savedOnly = false)
        )
        assertEquals(
            JobFeedEmptyScenario.NO_SEARCH_RESULTS,
            resolveJobFeedEmptyScenario(totalPublishedJobs = 12, savedOnly = false)
        )
        assertEquals(
            JobFeedEmptyScenario.EMPTY_SAVED,
            resolveJobFeedEmptyScenario(totalPublishedJobs = 12, savedOnly = true)
        )
    }

    @Test
    fun `empty feed scenario renders illustration and triggers refresh`() {
        var refreshed = false
        var voiceTriggered = false

        composeTestRule.setContent {
            MeroRojgarTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    JobFeedEmptyState(
                        locale = AppLocale.EN,
                        totalPublishedJobs = 0,
                        savedCount = 2,
                        searchQuery = "",
                        districtFilter = "All",
                        workModeFilter = "any",
                        minSalaryFilter = 0f,
                        verifiedOnly = false,
                        urgentOnly = false,
                        savedOnly = false,
                        hasVoiceFilter = false,
                        onClearAllFilters = {},
                        onRefreshJobs = { refreshed = true },
                        onVoiceSearch = { voiceTriggered = true },
                        onToggleSavedOnly = {},
                        onApplyQuickPreset = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("job_feed_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_feed_empty_illustration").assertIsDisplayed()
        composeTestRule.onNodeWithTag("job_feed_empty_title")
            .assertTextContains("No live job openings right now")

        composeTestRule.onNodeWithTag("job_feed_empty_primary_cta").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("job_feed_empty_voice_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(refreshed)
        assertTrue(voiceTriggered)
    }

    @Test
    fun `no search results scenario renders query, clear action, and quick presets`() {
        var cleared = false
        var selectedPreset: JobFeedQuickPreset? = null

        composeTestRule.setContent {
            MeroRojgarTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    JobFeedEmptyState(
                        locale = AppLocale.EN,
                        totalPublishedJobs = 8,
                        savedCount = 1,
                        searchQuery = "Flutter",
                        districtFilter = "Pokhara",
                        workModeFilter = "any",
                        minSalaryFilter = 0f,
                        verifiedOnly = false,
                        urgentOnly = false,
                        savedOnly = false,
                        hasVoiceFilter = false,
                        onClearAllFilters = { cleared = true },
                        onRefreshJobs = {},
                        onVoiceSearch = {},
                        onToggleSavedOnly = {},
                        onApplyQuickPreset = { selectedPreset = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("job_feed_empty_title")
            .assertTextContains("No matching jobs found")
        composeTestRule.onNodeWithTag("job_feed_empty_subtitle")
            .assertTextContains("Flutter", substring = true)

        composeTestRule.onNodeWithTag("job_feed_empty_primary_cta").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("job_feed_empty_preset_kathmandu").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(cleared)
        assertEquals(JobFeedQuickPreset.KATHMANDU, selectedPreset)
    }

    @Test
    fun `saved empty scenario renders Nepali copy and browse all toggle`() {
        var toggledTo: Boolean? = null

        composeTestRule.setContent {
            MeroRojgarTheme(isNepali = true) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    JobFeedEmptyState(
                        locale = AppLocale.NE,
                        totalPublishedJobs = 5,
                        savedCount = 0,
                        searchQuery = "",
                        districtFilter = "All",
                        workModeFilter = "any",
                        minSalaryFilter = 0f,
                        verifiedOnly = false,
                        urgentOnly = false,
                        savedOnly = true,
                        hasVoiceFilter = false,
                        onClearAllFilters = {},
                        onRefreshJobs = {},
                        onVoiceSearch = {},
                        onToggleSavedOnly = { toggledTo = it },
                        onApplyQuickPreset = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("job_feed_empty_title")
            .assertTextContains("तपाईंको सुरक्षित सूची खाली छ")
        composeTestRule.onNodeWithTag("job_feed_empty_primary_cta").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(false, toggledTo)
    }
}
