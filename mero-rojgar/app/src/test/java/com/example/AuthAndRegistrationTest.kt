package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.i18n.AppLocale
import com.example.model.RegistrationDraft
import com.example.model.RegistrationValidationResult
import com.example.model.validateRegistrationDraft
import com.example.ui.auth.GuestPreviewShell
import com.example.ui.auth.RegistrationFormContent
import com.example.ui.theme.MeroRojgarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthAndRegistrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `validateRegistrationDraft validates bounds and sanitizes inputs`() {
        val blankName = validateRegistrationDraft(
            displayName = " ",
            activeRole = "job_seeker",
            locale = "en",
            district = "Kathmandu",
            headline = "Engineer",
            skills = listOf("Kotlin"),
            salaryMinText = "40000",
            salaryMaxText = "80000",
            consentsRecruiterDiscovery = true,
            consentsRewardsTerms = true
        )
        assertTrue(blankName is RegistrationValidationResult.Invalid)

        val badSalary = validateRegistrationDraft(
            displayName = "Sita Gurung",
            activeRole = "job_seeker",
            locale = "en",
            district = "Pokhara",
            headline = "Designer",
            skills = listOf("Figma"),
            salaryMinText = "90000",
            salaryMaxText = "50000",
            consentsRecruiterDiscovery = false,
            consentsRewardsTerms = true
        )
        assertTrue(badSalary is RegistrationValidationResult.Invalid)

        val valid = validateRegistrationDraft(
            displayName = "  Sita Gurung  ",
            activeRole = "employer",
            locale = "en",
            district = "  Lalitpur ",
            headline = "  Tech Lead ",
            skills = listOf("Kotlin", "Compose", "Kotlin"),
            salaryMinText = "50000",
            salaryMaxText = "90000",
            consentsRecruiterDiscovery = true,
            consentsRewardsTerms = true
        )
        assertTrue(valid is RegistrationValidationResult.Valid)
        val draft = (valid as RegistrationValidationResult.Valid).draft
        assertEquals("Sita Gurung", draft.displayName)
        assertEquals("employer", draft.activeRole)
        assertEquals("Lalitpur", draft.district)
        assertEquals(listOf("Kotlin", "Compose"), draft.skills)
    }

    @Test
    fun `RegistrationFormContent validates and submits RegistrationDraft`() {
        var submitted: RegistrationDraft? = null

        composeTestRule.setContent {
            MeroRojgarTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RegistrationFormContent(
                        locale = AppLocale.EN,
                        isSubmitting = false,
                        submitButtonLabel = "Register & Continue",
                        onSubmitValidDraft = { submitted = it }
                    )
                }
            }
        }

        // Submitting empty name triggers inline validation error
        composeTestRule.onNodeWithTag("signup_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("signup_validation_error").assertIsDisplayed()

        // Fill valid details and submit
        composeTestRule.onNodeWithTag("signup_name_input").performScrollTo().performTextInput("Bikash Thapa")
        composeTestRule.onNodeWithTag("signup_role_employer").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("signup_district_input").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("signup_district_input").performTextInput("Pokhara")
        composeTestRule.onNodeWithTag("signup_headline_input").performScrollTo().performTextInput("Hydropower Contractor")
        composeTestRule.onNodeWithTag("signup_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(submitted)
        assertEquals("Bikash Thapa", submitted?.displayName)
        assertEquals("employer", submitted?.activeRole)
        assertEquals("Pokhara", submitted?.district)
        assertEquals("Hydropower Contractor", submitted?.headline)
    }

    @Test
    fun `GuestPreviewShell renders preview banner, tabs, and exits to auth`() {
        var exitedOpenSignUp: Boolean? = null

        composeTestRule.setContent {
            GuestPreviewShell(
                initialLocale = AppLocale.EN,
                onExitToAuth = { openSignUp -> exitedOpenSignUp = openSignUp }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("guest_preview_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_nav_jobs").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("job_feed_lazy_column").assertIsDisplayed()

        composeTestRule.onNodeWithTag("preview_sign_up_cta").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, exitedOpenSignUp)
    }
}
