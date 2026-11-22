package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLocale
import com.example.model.RegistrationDraft
import com.example.model.RegistrationValidationResult
import com.example.model.validateRegistrationDraft
import com.example.ui.theme.NepalCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationFormContent(
    locale: AppLocale,
    isSubmitting: Boolean,
    submitButtonLabel: String,
    initialDisplayName: String = "",
    onSubmitValidDraft: (RegistrationDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    val isNepali = locale == AppLocale.NE

    var fullName by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
    var activeRole by remember { mutableStateOf("job_seeker") }
    var preferredLocale by remember { mutableStateOf(locale.code) }
    var district by remember { mutableStateOf("Kathmandu") }
    var headline by remember { mutableStateOf("") }
    var skillsText by remember { mutableStateOf("Communication, Nepali") }
    var salaryMinText by remember { mutableStateOf("35000") }
    var salaryMaxText by remember { mutableStateOf("75000") }
    var consentsRecruiterDiscovery by remember { mutableStateOf(false) }
    var consentsRewardsTerms by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val quickDistricts = listOf("Kathmandu", "Lalitpur", "Pokhara", "Biratnagar", "Butwal")
    val quickSkills = listOf("AutoCAD", "Kotlin", "Site Supervision", "Accounting", "Electrician", "Figma")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                validationError = null
            },
            label = { Text(if (isNepali) "पूरा नाम *" else "Full Name *") },
            placeholder = { Text(if (isNepali) "उदा. सीता गुरुङ" else "e.g. Sita Gurung") },
            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_name_input")
        )

        // Role selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isNepali) "खाताको भूमिका छान्नुहोस् *" else "Account Role *",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("job_seeker", "Job Seeker", "रोजगार खोज्ने"),
                    Triple("employer", "Employer", "रोजगारदाता"),
                    Triple("sponsor", "Sponsor", "प्रायोजक")
                ).forEach { (key, en, ne) ->
                    FilterChip(
                        selected = activeRole == key,
                        onClick = { activeRole = key },
                        leadingIcon = if (activeRole == key) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text(if (isNepali) ne else en) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("signup_role_$key")
                    )
                }
            }
        }

        // Preferred Language
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isNepali) "प्रयोग भाषा" else "Preferred Language",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = preferredLocale == "ne",
                    onClick = { preferredLocale = "ne" },
                    label = { Text("नेपाली") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("signup_locale_ne")
                )
                FilterChip(
                    selected = preferredLocale == "en",
                    onClick = { preferredLocale = "en" },
                    label = { Text("English") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("signup_locale_en")
                )
            }
        }

        // District + Quick chips
        OutlinedTextField(
            value = district,
            onValueChange = {
                district = it
                validationError = null
            },
            label = { Text(if (isNepali) "जिल्ला *" else "District *") },
            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_district_input")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickDistricts.forEach { d ->
                FilterChip(
                    selected = district.equals(d, ignoreCase = true),
                    onClick = { district = d },
                    label = { Text(d) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }

        OutlinedTextField(
            value = headline,
            onValueChange = {
                headline = it
                validationError = null
            },
            label = {
                Text(
                    when (activeRole) {
                        "employer" -> if (isNepali) "संस्था र उद्योग परिचय" else "Organization & Industry Focus"
                        "sponsor" -> if (isNepali) "ब्रान्ड वा अभियान विषय" else "Brand or Campaign Focus"
                        else -> if (isNepali) "पेशेवर शीर्षक (उदा. सिभिल इन्जिनियर)" else "Professional Headline (e.g. Civil Engineer)"
                    }
                )
            },
            leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_headline_input")
        )

        OutlinedTextField(
            value = skillsText,
            onValueChange = { skillsText = it },
            label = { Text(if (isNepali) "मुख्य सीपहरू (अल्पविरामले छुट्याउनुहोस्)" else "Key Skills (comma-separated)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_skills_input")
        )

        val currentSkillTokens = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickSkills.forEach { skill ->
                val active = currentSkillTokens.any { it.equals(skill, ignoreCase = true) }
                FilterChip(
                    selected = active,
                    onClick = {
                        val updated = if (active) {
                            currentSkillTokens.filterNot { it.equals(skill, ignoreCase = true) }
                        } else {
                            currentSkillTokens + skill
                        }
                        skillsText = updated.joinToString(", ")
                    },
                    label = { Text(skill) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }

        if (activeRole == "job_seeker") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = salaryMinText,
                    onValueChange = {
                        salaryMinText = it.filter { ch -> ch.isDigit() }
                        validationError = null
                    },
                    label = { Text(if (isNepali) "न्यूनतम तलब (रु)" else "Min Salary (NPR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("signup_salary_min_input")
                )
                OutlinedTextField(
                    value = salaryMaxText,
                    onValueChange = {
                        salaryMaxText = it.filter { ch -> ch.isDigit() }
                        validationError = null
                    },
                    label = { Text(if (isNepali) "अधिकतम तलब (रु)" else "Max Salary (NPR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("signup_salary_max_input")
                )
            }
        }

        // Consents
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = consentsRewardsTerms,
                onCheckedChange = {
                    consentsRewardsTerms = it
                    validationError = null
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("signup_terms_checkbox")
            )
            Text(
                text = if (isNepali) {
                    "म मेरो रोजगारको पारदर्शिता र प्रमाणिकरण नियम स्वीकार गर्छु।"
                } else {
                    "I accept Mero Rojgar verification rules and transparent rewards terms."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = consentsRecruiterDiscovery,
                onCheckedChange = { consentsRecruiterDiscovery = it },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("signup_discovery_checkbox")
            )
            Text(
                text = if (isNepali) {
                    "प्रमाणित रोजगारदाताले मेरो सीप खोज्न सक्ने (सम्पर्क लुकेको रहनेछ)"
                } else {
                    "Allow verified recruiters to discover my skills (contact stays hidden)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (validationError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_validation_error")
            ) {
                Text(
                    text = validationError ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = {
                val parsedSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                when (
                    val result = validateRegistrationDraft(
                        displayName = fullName,
                        activeRole = activeRole,
                        locale = preferredLocale,
                        district = district,
                        headline = headline,
                        skills = parsedSkills,
                        salaryMinText = salaryMinText,
                        salaryMaxText = salaryMaxText,
                        consentsRecruiterDiscovery = consentsRecruiterDiscovery,
                        consentsRewardsTerms = consentsRewardsTerms
                    )
                ) {
                    is RegistrationValidationResult.Invalid -> {
                        validationError = if (isNepali) result.messageNe else result.messageEn
                    }
                    is RegistrationValidationResult.Valid -> {
                        validationError = null
                        onSubmitValidDraft(result.draft)
                    }
                }
            },
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = NepalCrimson,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("signup_submit_button")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(if (isNepali) "दर्ता हुँदैछ..." else "Creating Account...")
            } else {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = submitButtonLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
