package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.AppLocale
import com.example.i18n.AppStrings
import com.example.i18n.formatPaisaAsNpr
import com.example.model.LedgerEntry
import com.example.model.RewardCompletion
import com.example.model.SponsorCampaign
import com.example.model.WalletSummary
import com.example.model.WithdrawalRequest
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.SponsorDisclosureBar
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.SaffronGold
import com.example.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun RewardsScreen(
    locale: AppLocale,
    strings: AppStrings,
    walletSummary: WalletSummary,
    campaignsState: UiState<List<SponsorCampaign>>,
    completionsState: UiState<List<RewardCompletion>>,
    ledgerState: UiState<List<LedgerEntry>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    onCompleteCampaign: (SponsorCampaign, Long) -> Unit,
    onRequestWithdrawal: (Long, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Campaigns, 1=Ledger & Withdrawals
    var activeTaskCampaign by remember { mutableStateOf<SponsorCampaign?>(null) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val completedIds = remember(completionsState) {
        (completionsState as? UiState.Success)?.data?.map { it.campaignId }?.toSet() ?: emptySet()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Sponsor Campaigns") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Ledger & Payouts") })
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Banner & Mandatory Disclosure
            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.height(128.dp).fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_rewards_banner),
                            contentDescription = "Mero Rewards sponsor artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(HimalayanNavy.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = strings.rewardsTeaserTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.availableBalance}: ${formatPaisaAsNpr(walletSummary.availablePaisa, locale)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SaffronGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                SponsorDisclosureBar(text = strings.rewardsDisclaimer)
            }

            // Derived Wallet Strip
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            WalletStat(strings.availableBalance, formatPaisaAsNpr(walletSummary.availablePaisa, locale))
                            WalletStat(strings.pendingBalance, formatPaisaAsNpr(walletSummary.pendingPaisa, locale))
                            WalletStat("Held", formatPaisaAsNpr(walletSummary.heldPaisa, locale))
                            WalletStat(strings.paidOutBalance, formatPaisaAsNpr(walletSummary.paidOutPaisa, locale))
                        }
                        Button(
                            onClick = { showWithdrawDialog = true },
                            enabled = walletSummary.availablePaisa >= 10000L,
                            modifier = Modifier.fillMaxWidth().testTag("rewards_withdraw_button")
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.withdrawTitle)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                when (campaignsState) {
                    is UiState.Loading -> item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    is UiState.Error -> item { ErrorBannerCard(campaignsState.message) }
                    is UiState.Success -> {
                        if (campaignsState.data.isEmpty()) {
                            item { EmptyStateCard("No live sponsor campaigns", "Campaigns appear here only when funded and live.") }
                        } else {
                            items(campaignsState.data, key = { it.id }) { camp ->
                                val isCompleted = camp.id in completedIds
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().testTag("campaign_card_${camp.id}")
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (locale == AppLocale.NE && camp.titleNe.isNotBlank()) camp.titleNe else camp.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Sponsor: ${camp.sponsorName} • ${camp.videoSeconds}s • ${camp.endsAtText}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(color = EmeraldContainer, shape = RoundedCornerShape(10.dp)) {
                                                Text(
                                                    text = "+${formatPaisaAsNpr(camp.rewardPaisa, locale)}",
                                                    color = EmeraldSuccess,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Remaining funded pool: ${formatPaisaAsNpr(camp.remainingPaisa, locale)} • ${camp.termsSummary}",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        if (isCompleted) {
                                            AssistChip(
                                                onClick = {},
                                                leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess) },
                                                label = { Text(strings.completedBadge) }
                                            )
                                        } else {
                                            Button(
                                                onClick = { activeTaskCampaign = camp },
                                                enabled = camp.remainingPaisa >= camp.rewardPaisa,
                                                modifier = Modifier.fillMaxWidth().testTag("start_campaign_${camp.id}")
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text(strings.startTask)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Ledger & Withdrawals
                item {
                    Text(strings.ledgerHistory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (ledgerState is UiState.Success) {
                    if (ledgerState.data.isEmpty()) {
                        item { EmptyStateCard("No ledger entries yet", "Complete a funded sponsor brief to record your first ledger credit.") }
                    } else {
                        items(ledgerState.data, key = { it.id }) { entry ->
                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.reason, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${entry.accountKind} • ${entry.direction.uppercase()} • ${entry.txnId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = (if (entry.direction == "credit") "+" else "-") + formatPaisaAsNpr(entry.amountPaisa, locale),
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.direction == "credit") EmeraldSuccess else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                if (withdrawalsState is UiState.Success && withdrawalsState.data.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        Text("Withdrawal Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(withdrawalsState.data, key = { it.id }) { w ->
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${w.method.uppercase()} (${w.destinationMasked})", fontWeight = FontWeight.Bold)
                                    Text("Status: ${w.status.uppercase()}", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(formatPaisaAsNpr(w.amountPaisa, locale), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Security-Sensitive Task Player Dialog
    activeTaskCampaign?.let { camp ->
        RewardsTaskPlayerDialog(
            campaign = camp,
            locale = locale,
            onDismiss = { activeTaskCampaign = null },
            onSubmitVerified = { watchedMs ->
                onCompleteCampaign(camp, watchedMs)
                activeTaskCampaign = null
            }
        )
    }

    // Withdrawal Sheet / Dialog
    if (showWithdrawDialog) {
        var method by remember { mutableStateOf("esewa") }
        var destination by remember { mutableStateOf("9841****89") }
        var amountNpr by remember { mutableStateOf((walletSummary.availablePaisa / 100).toString()) }
        val parsedPaisa = (amountNpr.toLongOrNull() ?: 0L) * 100L

        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Request Payout (Two-Step Queue)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Available: ${formatPaisaAsNpr(walletSummary.availablePaisa, locale)}. Funds move into a ledger hold account upon request.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("esewa", "khalti", "bank_transfer").forEach { m ->
                            OutlinedButton(
                                onClick = { method = m },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (m == "bank_transfer") "Bank" else m.uppercase(), maxLines = 1)
                            }
                        }
                    }
                    Text("Method: ${method.uppercase()}", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Wallet ID / Masked Account") })
                    OutlinedTextField(value = amountNpr, onValueChange = { amountNpr = it }, label = { Text("Amount (NPR, min 100)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestWithdrawal(parsedPaisa, method, destination)
                        showWithdrawDialog = false
                    },
                    enabled = parsedPaisa in 10000L..walletSummary.availablePaisa && destination.trim().length >= 3
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WalletStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RewardsTaskPlayerDialog(
    campaign: SponsorCampaign,
    locale: AppLocale,
    onDismiss: () -> Unit,
    onSubmitVerified: (Long) -> Unit
) {
    val totalSeconds = campaign.videoSeconds.coerceAtLeast(5L)
    var elapsedSec by remember(campaign.id) { mutableLongStateOf(0L) }
    var selectedOption by remember(campaign.id) { mutableIntStateOf(-1) }
    var errorNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(campaign.id) {
        while (elapsedSec < totalSeconds) {
            delay(1000L)
            elapsedSec += 1L
        }
    }

    val watchRatio = (elapsedSec.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val questionUnlocked = watchRatio >= 0.90f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(campaign.sponsorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(campaign.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = HimalayanNavy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (questionUnlocked) "Brief Verified (${elapsedSec}s / ${totalSeconds}s)" else "Sponsor Brief Playing • Heartbeat ${elapsedSec}s / ${totalSeconds}s",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        LinearProgressIndicator(
                            progress = { watchRatio },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = SaffronGold,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        Text(
                            text = "Reward upon verified completion: ${formatPaisaAsNpr(campaign.rewardPaisa, locale)}",
                            color = Color(0xFFFDE68A),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (!questionUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            "Comprehension check unlocks at 90% watch time (anti-skip enforced).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        Text("Answer to complete:", fontWeight = FontWeight.Bold)
                    }
                    Text(campaign.questionPrompt, style = MaterialTheme.typography.bodyMedium)
                    campaign.questionOptions.forEachIndexed { idx, opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedOption == idx,
                                onClick = {
                                    selectedOption = idx
                                    errorNote = null
                                }
                            )
                            Text(opt, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (errorNote != null) {
                    Text(errorNote ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOption == campaign.correctOptionIndex.toInt()) {
                        onSubmitVerified(elapsedSec * 1000L)
                    } else {
                        errorNote = "Incorrect answer — please review the brief and select the right option."
                    }
                },
                enabled = questionUnlocked && selectedOption >= 0,
                modifier = Modifier.testTag("rewards_submit_completion_button")
            ) {
                Text("Submit & Credit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
