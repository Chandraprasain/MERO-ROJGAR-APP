package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.i18n.AppLocale
import com.example.i18n.stringsFor
import com.example.model.RegistrationDraft
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.SaffronGold

enum class AuthTabMode {
    SIGN_IN,
    SIGN_UP
}

@Composable
fun AuthScreen(
    initialMode: AuthTabMode = AuthTabMode.SIGN_IN,
    onSaveRegistrationDraft: (RegistrationDraft?) -> Unit = {},
    onEnterGuestPreview: () -> Unit = {},
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var locale by remember { mutableStateOf(AppLocale.NE) }
    val strings = stringsFor(locale)
    var selectedMode by remember(initialMode) { mutableStateOf(initialMode) }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSilent by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmulatorGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = scope
        )
    }

    val launchGoogleSignIn: (RegistrationDraft?) -> Unit = { draft ->
        errorMessage = null
        isLoading = true
        onSaveRegistrationDraft(draft)
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                onSaveRegistrationDraft(null)
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
                onSaveRegistrationDraft(null)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HimalayanNavy,
                        Color(0xFF143A68),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEnterGuestPreview,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("auth_top_preview_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (locale == AppLocale.NE) "पूर्वावलोकन" else "Preview Inside",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FilledTonalButton(
                    onClick = {
                        locale = if (locale == AppLocale.NE) AppLocale.EN else AppLocale.NE
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("auth_language_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = "Switch language",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (locale == AppLocale.NE) "English" else "नेपाली")
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = CircleShape,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                color = Color.White,
                modifier = Modifier.size(120.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mero_rojgar_emblem),
                    contentDescription = "Mero Rojgar Emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = strings.signInTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = strings.signInSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedMode == AuthTabMode.SIGN_IN,
                            onClick = {
                                selectedMode = AuthTabMode.SIGN_IN
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("auth_tab_sign_in")
                        ) {
                            Text(if (locale == AppLocale.NE) "साइन इन" else "Sign In")
                        }
                        SegmentedButton(
                            selected = selectedMode == AuthTabMode.SIGN_UP,
                            onClick = {
                                selectedMode = AuthTabMode.SIGN_UP
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("auth_tab_sign_up")
                        ) {
                            Text(if (locale == AppLocale.NE) "नयाँ खाता दर्ता" else "Sign Up")
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = selectedMode,
                        label = "auth_mode_content"
                    ) { mode ->
                        when (mode) {
                            AuthTabMode.SIGN_IN -> {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    TrustHighlightRow(
                                        icon = Icons.Filled.VerifiedUser,
                                        tint = EmeraldSuccess,
                                        text = if (locale == AppLocale.NE) {
                                            "निःशुल्क जागिर खोज र प्रमाणित रोजगारदाता ब्याज"
                                        } else {
                                            "Free job search & transparent employer verification badges"
                                        }
                                    )
                                    TrustHighlightRow(
                                        icon = Icons.Filled.Shield,
                                        tint = HimalayanNavy,
                                        text = if (locale == AppLocale.NE) {
                                            "AI केवल सल्लाहकार मात्र — सम्पर्क अनुमति बिना साझा हुँदैन"
                                        } else {
                                            "Advisory Career AI & opt-in privacy-first recruiter discovery"
                                        }
                                    )
                                    TrustHighlightRow(
                                        icon = Icons.Filled.CheckCircle,
                                        tint = SaffronGold,
                                        text = if (locale == AppLocale.NE) {
                                            "वैकल्पिक प्रायोजित अभियान पुरस्कार (अपरिवर्तनीय लेजर)"
                                        } else {
                                            "Optional sponsor-funded rewards tracked in an immutable ledger"
                                        }
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Button(
                                        onClick = { launchGoogleSignIn(null) },
                                        enabled = !isLoading && !isCheckingSilent,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NepalCrimson,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("google_sign_in_button")
                                    ) {
                                        if (isLoading || isCheckingSilent) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                if (isCheckingSilent) {
                                                    if (locale == AppLocale.NE) "खाता जाँच हुँदैछ..." else "Checking account..."
                                                } else {
                                                    if (locale == AppLocale.NE) "साइन इन हुँदैछ..." else "Signing in..."
                                                },
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        } else {
                                            Text(
                                                text = strings.signInButton,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = onEnterGuestPreview,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("guest_preview_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (locale == AppLocale.NE) {
                                                "पासवर्ड बिना भित्र हेर्नुहोस् (पूर्वावलोकन)"
                                            } else {
                                                "Explore Inside (Guest Preview)"
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                                    TextButton(
                                        onClick = { showEmulatorGuide = !showEmulatorGuide },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("auth_emulator_guide_toggle")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (locale == AppLocale.NE) {
                                                "ID/पासवर्ड र इमुलेटर लगइन गाइड"
                                            } else {
                                                "Why no ID & password? Emulator & Auth Guide"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (showEmulatorGuide) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null
                                        )
                                    }

                                    AnimatedVisibility(visible = showEmulatorGuide) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("auth_emulator_guide_body")
                                        ) {
                                            Text(
                                                text = if (locale == AppLocale.NE) {
                                                    "सुरक्षाको लागि यस एपमा स्थिर ID/पासवर्ड राखिएको छैन। भित्रका सबै स्क्रिन तुरुन्त हेर्न 'पूर्वावलोकन' थिच्नुहोस्, वा क्लाउड बचतको लागि Google बाट साइन इन / दर्ता गर्नुहोस्। परियोजना एडमिनले चाहेमा Firebase Console > Authentication बाट Email/Password प्रदायक थप्न सक्नुहुन्छ।"
                                                } else {
                                                    "For security, no hardcoded ID/password is bundled. Tap 'Explore Inside (Guest Preview)' to inspect all Seeker, Employer, Sponsor, and Admin screens right away—or sign in/register with a Google account on the emulator to sync live data to Firestore. Project owners can also enable Email/Password under Firebase Console > Authentication > Sign-in method."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            AuthTabMode.SIGN_UP -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = if (locale == AppLocale.NE) {
                                            "आफ्नो विवरण भर्नुहोस् र Google खातासँग जोडेर नयाँ मेरो रोजगार खाता दर्ता गर्नुहोस्।"
                                        } else {
                                            "Complete your registration profile below and link your Google account to create your Mero Rojgar workspace."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    RegistrationFormContent(
                                        locale = locale,
                                        isSubmitting = isLoading || isCheckingSilent,
                                        submitButtonLabel = if (locale == AppLocale.NE) {
                                            "दर्ता गर्नुहोस् र Google बाट जारी राख्नुहोस्"
                                        } else {
                                            "Register & Continue with Google"
                                        },
                                        onSubmitValidDraft = { draft ->
                                            launchGoogleSignIn(draft)
                                        }
                                    )

                                    TextButton(
                                        onClick = onEnterGuestPreview,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("signup_preview_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (locale == AppLocale.NE) {
                                                "पहिले पूर्वावलोकन हेर्नुहोस् (साइन अप पछि)"
                                            } else {
                                                "Preview app first without signing up"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustHighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
