package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLocale
import com.example.i18n.formatNpr

enum class JobFeedEmptyScenario {
    EMPTY_FEED,
    NO_SEARCH_RESULTS,
    EMPTY_SAVED
}

enum class JobFeedQuickPreset(val labelEn: String, val labelNe: String) {
    KATHMANDU("Kathmandu", "काठमाडौं"),
    REMOTE("Remote", "रिमोट"),
    VERIFIED("Verified 2+", "प्रमाणित २+"),
    URGENT("Urgent Hiring", "अत्यावश्यक")
}

fun resolveJobFeedEmptyScenario(
    totalPublishedJobs: Int,
    savedOnly: Boolean
): JobFeedEmptyScenario = when {
    savedOnly -> JobFeedEmptyScenario.EMPTY_SAVED
    totalPublishedJobs == 0 -> JobFeedEmptyScenario.EMPTY_FEED
    else -> JobFeedEmptyScenario.NO_SEARCH_RESULTS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobFeedEmptyState(
    locale: AppLocale,
    totalPublishedJobs: Int,
    savedCount: Int,
    searchQuery: String,
    districtFilter: String,
    workModeFilter: String,
    minSalaryFilter: Float,
    verifiedOnly: Boolean,
    urgentOnly: Boolean,
    savedOnly: Boolean,
    hasVoiceFilter: Boolean,
    onClearAllFilters: () -> Unit,
    onRefreshJobs: () -> Unit,
    onVoiceSearch: () -> Unit,
    onToggleSavedOnly: (Boolean) -> Unit,
    onApplyQuickPreset: (JobFeedQuickPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val isNepali = locale == AppLocale.NE
    val scenario = resolveJobFeedEmptyScenario(
        totalPublishedJobs = totalPublishedJobs,
        savedOnly = savedOnly
    )

    val activeFilterTags = buildList {
        if (searchQuery.isNotBlank()) add("“${searchQuery.trim()}”")
        if (districtFilter != "All") add(districtFilter)
        if (workModeFilter != "any") add(workModeFilter.replaceFirstChar { it.uppercase() })
        if (minSalaryFilter >= 1f) add("≥ ${formatNpr(minSalaryFilter.toLong(), locale)}")
        if (verifiedOnly) add(if (isNepali) "प्रमाणित (२+)" else "Verified (2+)")
        if (urgentOnly) add(if (isNepali) "अत्यावश्यक" else "Urgent")
        if (hasVoiceFilter) add(if (isNepali) "AI खोज" else "AI Intent")
    }
    val hasActiveFilters = activeFilterTags.isNotEmpty() || savedOnly

    val headline = when (scenario) {
        JobFeedEmptyScenario.EMPTY_FEED -> if (isNepali) {
            "अहिले प्रत्यक्ष जागिर उपलब्ध छैन"
        } else {
            "No live job openings right now"
        }
        JobFeedEmptyScenario.EMPTY_SAVED -> if (isNepali) {
            "तपाईंको सुरक्षित सूची खाली छ"
        } else {
            "No saved jobs yet"
        }
        JobFeedEmptyScenario.NO_SEARCH_RESULTS -> if (isNepali) {
            "मिल्ने जागिर फेला परेन"
        } else {
            "No matching jobs found"
        }
    }

    val subtitle = when (scenario) {
        JobFeedEmptyScenario.EMPTY_FEED -> if (isNepali) {
            "रोजगारदाताले नयाँ अवसर प्रकाशित गर्नासाथ यहाँ तुरुन्तै देखिनेछ। तल तानेर वा रिफ्रेस थिचेर पुनः जाँच गर्नुहोस्।"
        } else {
            "New verified openings appear here the moment employers publish to Firestore. Pull down or refresh to check again."
        }
        JobFeedEmptyScenario.EMPTY_SAVED -> if (isNepali) {
            "कुनै पनि जागिर कार्डमा बुकमार्क आइकन थिच्नुहोस् र तुलना वा पछि आवेदनका लागि सुरक्षित राख्नुहोस्।"
        } else {
            "Tap the bookmark icon on any job card to shortlist promising roles for quick comparison and later application."
        }
        JobFeedEmptyScenario.NO_SEARCH_RESULTS -> if (searchQuery.isNotBlank()) {
            if (isNepali) {
                "“${searchQuery.trim()}” र हालका छनोटहरूसँग मिल्ने कुनै जागिर भेटिएन।"
            } else {
                "We couldn’t find any live roles matching “${searchQuery.trim()}” with your current filters."
            }
        } else {
            if (isNepali) {
                "तपाईंले छान्नुभएको ${activeFilterTags.size.coerceAtLeast(1)} फिल्टरहरूसँग मिल्ने जागिर भेटिएन।"
            } else {
                "No live roles matched your ${activeFilterTags.size.coerceAtLeast(1)} active filter constraints."
            }
        }
    }

    val careerTip = when (scenario) {
        JobFeedEmptyScenario.EMPTY_FEED -> if (isNepali) {
            "प्रतीक्षा गर्दा आफ्नो सीप राहदानी र AI बायोडाटा अद्यावधिक राख्नुहोस् — सही अवसर छिट्टै आउँदैछन्।"
        } else {
            "While fresh listings arrive, keep your Skill Passport and AI Resume sharp so you're ready for one-tap apply."
        }
        JobFeedEmptyScenario.EMPTY_SAVED -> if (isNepali) {
            "२–३ जागिर सुरक्षित गरेर साइड-बाइ-साइड तुलना गर्दा तलब र सीप म्याच बुझ्न सजिलो हुन्छ।"
        } else {
            "Shortlisting 2–3 strong matches unlocks side-by-side comparison before you commit your application."
        }
        JobFeedEmptyScenario.NO_SEARCH_RESULTS -> if (isNepali) {
            "जिल्ला वा न्यूनतम तलब अलि फराकिलो बनाउनुहोस्, वा तलका द्रुत खोज ट्यागबाट सुरु गर्नुहोस्।"
        } else {
            "Try loosening one constraint—like district or minimum salary—or jump straight into a popular filter below."
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("job_feed_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Custom Jetpack Compose Canvas Geometric Vector Illustration
                AnimatedContent(
                    targetState = scenario,
                    transitionSpec = {
                        fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                    },
                    label = "job_feed_empty_illustration_transition"
                ) { activeScenario ->
                    JobFeedEmptyIllustration(
                        scenario = activeScenario,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("job_feed_empty_illustration")
                    )
                }

                // Scenario badge pill
                Surface(
                    color = when (scenario) {
                        JobFeedEmptyScenario.EMPTY_FEED -> MaterialTheme.colorScheme.tertiaryContainer
                        JobFeedEmptyScenario.EMPTY_SAVED -> MaterialTheme.colorScheme.secondaryContainer
                        JobFeedEmptyScenario.NO_SEARCH_RESULTS -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = when (scenario) {
                            JobFeedEmptyScenario.EMPTY_FEED -> if (isNepali) "प्रत्यक्ष फिड स्थिति" else "Live Feed Status"
                            JobFeedEmptyScenario.EMPTY_SAVED -> if (isNepali) "सुरक्षित सूची" else "Saved Shortlist"
                            JobFeedEmptyScenario.NO_SEARCH_RESULTS -> if (isNepali) "खोज परिणाम ०" else "0 Matching Results"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (scenario) {
                            JobFeedEmptyScenario.EMPTY_FEED -> MaterialTheme.colorScheme.onTertiaryContainer
                            JobFeedEmptyScenario.EMPTY_SAVED -> MaterialTheme.colorScheme.onSecondaryContainer
                            JobFeedEmptyScenario.NO_SEARCH_RESULTS -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                // Headline & Diagnostic Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("job_feed_empty_title")
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("job_feed_empty_subtitle")
                    )
                }

                // Active search/filter summary pills when narrowing caused 0 results
                if (activeFilterTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activeFilterTags.forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                                )
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Warm Career-Coach Tip Callout
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = careerTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Primary Recovery CTA
                when (scenario) {
                    JobFeedEmptyScenario.EMPTY_SAVED -> {
                        Button(
                            onClick = { onToggleSavedOnly(false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("job_feed_empty_primary_cta"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isNepali) "सबै जागिर हेर्नुहोस्" else "Browse All Live Jobs")
                        }
                    }
                    JobFeedEmptyScenario.EMPTY_FEED -> {
                        if (hasActiveFilters) {
                            Button(
                                onClick = onClearAllFilters,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("job_feed_empty_primary_cta"),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Filled.FilterAltOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isNepali) "सबै फिल्टर हटाउनुहोस्" else "Clear Filters")
                            }
                        } else {
                            Button(
                                onClick = onRefreshJobs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("job_feed_empty_primary_cta"),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isNepali) "फिड रिफ्रेस गर्नुहोस्" else "Refresh Live Feed")
                            }
                        }
                    }
                    JobFeedEmptyScenario.NO_SEARCH_RESULTS -> {
                        Button(
                            onClick = onClearAllFilters,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("job_feed_empty_primary_cta"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isNepali) "खोज र फिल्टर रिसेट गर्नुहोस्" else "Clear Search & Filters")
                        }
                    }
                }

                // Secondary Recovery Controls: Refresh, Voice/AI Search, Saved / Browse Switcher
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (scenario != JobFeedEmptyScenario.EMPTY_FEED || hasActiveFilters) {
                        OutlinedButton(
                            onClick = onRefreshJobs,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("job_feed_empty_refresh_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isNepali) "रिफ्रेस" else "Refresh")
                        }
                    }

                    FilledTonalButton(
                        onClick = onVoiceSearch,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("job_feed_empty_voice_btn")
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isNepali) "AI आवाज खोज" else "Voice / AI Search")
                    }

                    OutlinedButton(
                        onClick = { onToggleSavedOnly(!savedOnly) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("job_feed_empty_saved_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (savedOnly) Icons.Filled.WorkOutline else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (savedOnly) {
                                if (isNepali) "सबै जागिर" else "All Jobs"
                            } else {
                                if (isNepali) "सुरक्षित ($savedCount)" else "Saved ($savedCount)"
                            }
                        )
                    }
                }

                // Quick Discovery Chips
                if (scenario != JobFeedEmptyScenario.EMPTY_FEED) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isNepali) "लोकप्रिय द्रुत खोजहरू" else "Quick Discovery Presets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            JobFeedQuickPreset.entries.forEach { preset ->
                                val presetIcon = when (preset) {
                                    JobFeedQuickPreset.KATHMANDU -> Icons.Filled.LocationOn
                                    JobFeedQuickPreset.REMOTE -> Icons.Filled.Explore
                                    JobFeedQuickPreset.VERIFIED -> Icons.Filled.Verified
                                    JobFeedQuickPreset.URGENT -> Icons.Filled.Bookmark
                                }
                                AssistChip(
                                    onClick = { onApplyQuickPreset(preset) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = presetIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(if (isNepali) preset.labelNe else preset.labelEn)
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("job_feed_empty_preset_${preset.name.lowercase()}")
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
fun JobFeedEmptyIllustration(
    scenario: JobFeedEmptyScenario,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "empty_illustration_ambient")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_float_offset"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_pulse_scale"
    )

    val accessibilityDesc = when (scenario) {
        JobFeedEmptyScenario.EMPTY_FEED -> "Geometric Himalayan sunrise and compass briefcase illustration indicating no live job listings yet"
        JobFeedEmptyScenario.NO_SEARCH_RESULTS -> "Geometric magnifying glass over job cards illustration indicating no matching search results"
        JobFeedEmptyScenario.EMPTY_SAVED -> "Geometric bookmark crest illustration indicating no saved jobs yet"
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = accessibilityDesc }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.46f

        // Layered geometric backdrop container
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryContainer.copy(alpha = 0.45f),
                    surfaceVariant.copy(alpha = 0.55f)
                )
            ),
            topLeft = Offset(w * 0.08f, h * 0.04f),
            size = Size(w * 0.84f, h * 0.90f),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )

        // Concentric geometric aura ring
        val baseRadius = minOf(w, h) * 0.34f
        drawCircle(
            color = when (scenario) {
                JobFeedEmptyScenario.EMPTY_FEED -> tertiaryContainer.copy(alpha = 0.72f)
                JobFeedEmptyScenario.EMPTY_SAVED -> secondaryContainer.copy(alpha = 0.60f)
                JobFeedEmptyScenario.NO_SEARCH_RESULTS -> primaryContainer.copy(alpha = 0.75f)
            },
            radius = baseRadius * pulseScale,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = outline.copy(alpha = 0.25f),
            radius = baseRadius * 1.24f,
            center = Offset(cx, cy),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        )

        // Architectural dot-grid accents
        val dotColor = primary.copy(alpha = 0.16f)
        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(
                    color = dotColor,
                    radius = 2.dp.toPx(),
                    center = Offset(w * 0.15f + col * 10.dp.toPx(), h * 0.18f + row * 10.dp.toPx())
                )
            }
        }

        // Layered Himalayan Mountain Ridge Silhouette at base
        val baseLineY = h * 0.90f
        val rearRidge = Path().apply {
            moveTo(w * 0.10f, baseLineY)
            lineTo(w * 0.27f, h * 0.62f)
            lineTo(w * 0.43f, h * 0.78f)
            lineTo(w * 0.61f, h * 0.57f)
            lineTo(w * 0.78f, h * 0.75f)
            lineTo(w * 0.90f, baseLineY)
            close()
        }
        drawPath(path = rearRidge, color = primary.copy(alpha = 0.12f))

        val frontRidge = Path().apply {
            moveTo(w * 0.14f, baseLineY)
            lineTo(w * 0.33f, h * 0.69f)
            lineTo(w * 0.50f, h * 0.82f)
            lineTo(w * 0.70f, h * 0.64f)
            lineTo(w * 0.88f, baseLineY)
            close()
        }
        drawPath(path = frontRidge, color = primary.copy(alpha = 0.20f))

        // Crisp snow-peak geometric caps
        val leftCap = Path().apply {
            moveTo(w * 0.33f, h * 0.69f)
            lineTo(w * 0.295f, h * 0.745f)
            lineTo(w * 0.33f, h * 0.73f)
            lineTo(w * 0.365f, h * 0.745f)
            close()
        }
        drawPath(path = leftCap, color = surface.copy(alpha = 0.85f))

        val rightCap = Path().apply {
            moveTo(w * 0.70f, h * 0.64f)
            lineTo(w * 0.66f, h * 0.705f)
            lineTo(w * 0.70f, h * 0.688f)
            lineTo(w * 0.74f, h * 0.705f)
            close()
        }
        drawPath(path = rightCap, color = surface.copy(alpha = 0.85f))

        val floatY = floatOffset.dp.toPx()

        // Scene-specific focal geometric vector emblem
        when (scenario) {
            JobFeedEmptyScenario.EMPTY_FEED -> {
                // Rising geometric sun disc behind briefcase
                drawCircle(
                    color = tertiary.copy(alpha = 0.90f),
                    radius = 22.dp.toPx(),
                    center = Offset(cx + 22.dp.toPx(), cy - 18.dp.toPx() + floatY * 0.4f)
                )

                // Stylized Briefcase / Career Horizon Emblem
                val cardW = 72.dp.toPx()
                val cardH = 50.dp.toPx()
                val left = cx - cardW / 2f
                val top = cy - cardH / 2f + 6.dp.toPx() + floatY

                // Handle
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(cx - 14.dp.toPx(), top - 11.dp.toPx()),
                    size = Size(28.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = 2.6.dp.toPx())
                )
                // Body fill & outline
                drawRoundRect(
                    color = surface,
                    topLeft = Offset(left, top),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(left, top),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 2.6.dp.toPx())
                )
                // Flap divider curve/line
                drawLine(
                    color = primary.copy(alpha = 0.35f),
                    start = Offset(left + 6.dp.toPx(), top + cardH * 0.42f),
                    end = Offset(left + cardW - 6.dp.toPx(), top + cardH * 0.42f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Crimson compass-diamond clasp
                val claspCenter = Offset(cx, top + cardH * 0.42f)
                val diamond = Path().apply {
                    moveTo(claspCenter.x, claspCenter.y - 7.dp.toPx())
                    lineTo(claspCenter.x + 7.dp.toPx(), claspCenter.y)
                    lineTo(claspCenter.x, claspCenter.y + 7.dp.toPx())
                    lineTo(claspCenter.x - 7.dp.toPx(), claspCenter.y)
                    close()
                }
                drawPath(path = diamond, color = secondary)
            }

            JobFeedEmptyScenario.NO_SEARCH_RESULTS -> {
                // Background tilted job card silhouettes
                val bgW = 62.dp.toPx()
                val bgH = 44.dp.toPx()
                drawRoundRect(
                    color = surface.copy(alpha = 0.92f),
                    topLeft = Offset(cx - bgW * 0.72f, cy - bgH * 0.55f),
                    size = Size(bgW, bgH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                drawRoundRect(
                    color = outline.copy(alpha = 0.55f),
                    topLeft = Offset(cx - bgW * 0.72f, cy - bgH * 0.55f),
                    size = Size(bgW, bgH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                // Skeleton text bars on rear card
                drawLine(
                    color = primaryContainer,
                    start = Offset(cx - bgW * 0.54f, cy - 10.dp.toPx()),
                    end = Offset(cx - 2.dp.toPx(), cy - 10.dp.toPx()),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = outline.copy(alpha = 0.35f),
                    start = Offset(cx - bgW * 0.54f, cy + 2.dp.toPx()),
                    end = Offset(cx + 4.dp.toPx(), cy + 2.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Prominent Magnifying Lens
                val lensCenter = Offset(cx + 8.dp.toPx(), cy - 2.dp.toPx() + floatY)
                val lensRadius = 24.dp.toPx()
                drawCircle(
                    color = surface,
                    radius = lensRadius,
                    center = lensCenter
                )
                drawCircle(
                    color = primaryContainer.copy(alpha = 0.45f),
                    radius = lensRadius - 4.dp.toPx(),
                    center = lensCenter
                )
                drawCircle(
                    color = primary,
                    radius = lensRadius,
                    center = lensCenter,
                    style = Stroke(width = 3.dp.toPx())
                )
                // Subtle empty cross inside lens
                val crossSize = 6.dp.toPx()
                drawLine(
                    color = secondary,
                    start = Offset(lensCenter.x - crossSize, lensCenter.y - crossSize),
                    end = Offset(lensCenter.x + crossSize, lensCenter.y + crossSize),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondary,
                    start = Offset(lensCenter.x + crossSize, lensCenter.y - crossSize),
                    end = Offset(lensCenter.x - crossSize, lensCenter.y + crossSize),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Crimson lens handle
                val handleStart = Offset(
                    lensCenter.x + lensRadius * 0.70f,
                    lensCenter.y + lensRadius * 0.70f
                )
                val handleEnd = Offset(
                    handleStart.x + 17.dp.toPx(),
                    handleStart.y + 17.dp.toPx()
                )
                drawLine(
                    color = secondary,
                    start = handleStart,
                    end = handleEnd,
                    strokeWidth = 5.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            JobFeedEmptyScenario.EMPTY_SAVED -> {
                // Backing card frame
                val cardW = 68.dp.toPx()
                val cardH = 68.dp.toPx()
                val cardLeft = cx - cardW / 2f
                val cardTop = cy - cardH / 2f + 2.dp.toPx() + floatY * 0.5f
                drawRoundRect(
                    color = surface,
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                drawRoundRect(
                    color = outline.copy(alpha = 0.45f),
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Bold Geometric Ribbon Bookmark Crest
                val bmW = 30.dp.toPx()
                val bmH = 44.dp.toPx()
                val bmLeft = cx - bmW / 2f
                val bmTop = cy - bmH / 2f + floatY
                val ribbon = Path().apply {
                    moveTo(bmLeft, bmTop)
                    lineTo(bmLeft + bmW, bmTop)
                    lineTo(bmLeft + bmW, bmTop + bmH)
                    lineTo(cx, bmTop + bmH - 11.dp.toPx())
                    lineTo(bmLeft, bmTop + bmH)
                    close()
                }
                drawPath(path = ribbon, color = secondary)
                drawPath(
                    path = ribbon,
                    color = primary,
                    style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
                )
                // Gold star-circle accent at top corner
                drawCircle(
                    color = tertiary,
                    radius = 7.dp.toPx(),
                    center = Offset(bmLeft + bmW + 6.dp.toPx(), bmTop + 4.dp.toPx())
                )
            }
        }

        // Floating ambient geometric sparks
        drawCircle(
            color = tertiary.copy(alpha = 0.85f),
            radius = 4.dp.toPx(),
            center = Offset(w * 0.78f, h * 0.23f - floatY * 0.7f)
        )
        drawCircle(
            color = secondary.copy(alpha = 0.70f),
            radius = 3.dp.toPx(),
            center = Offset(w * 0.22f, h * 0.30f + floatY * 0.6f)
        )
        // Small geometric diamond spark on right
        val sparkCenter = Offset(w * 0.82f, h * 0.48f + floatY * 0.5f)
        val spark = Path().apply {
            moveTo(sparkCenter.x, sparkCenter.y - 4.5.dp.toPx())
            lineTo(sparkCenter.x + 4.5.dp.toPx(), sparkCenter.y)
            lineTo(sparkCenter.x, sparkCenter.y + 4.5.dp.toPx())
            lineTo(sparkCenter.x - 4.5.dp.toPx(), sparkCenter.y)
            close()
        }
        drawPath(path = spark, color = Color(primary.red, primary.green, primary.blue, 0.45f))
    }
}
