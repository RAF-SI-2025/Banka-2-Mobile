package rs.raf.banka2.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.format.AccountFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.MiniBarChart
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeRateDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentListItemDto
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto

private val gradients: Map<String, List<Color>> = mapOf(
    "RSD" to listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
    "EUR" to listOf(Color(0xFF6366F1), Color(0xFF6D28D9)),
    "USD" to listOf(Color(0xFF10B981), Color(0xFF047857)),
    "CHF" to listOf(Color(0xFFEF4444), Color(0xFFBE123C)),
    "GBP" to listOf(Color(0xFFA855F7), Color(0xFF6D28D9)),
    "JPY" to listOf(Color(0xFFF97316), Color(0xFFB45309))
)

@Composable
fun ClientHomeContent(
    state: HomeState,
    onNavigate: (HomeAction) -> Unit,
    contentPadding: PaddingValues
) {
    var balanceVisible by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ClientHero(state.profile, state.totalRsdBalance, state.foreignAccountsCount, balanceVisible, onToggle = { balanceVisible = !balanceVisible }, onNavigate) }

        if (state.balanceTrend.size >= 2) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Text(
                        text = "Istorija stanja (poslednjih 7 transakcija)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    MiniBarChart(values = state.balanceTrend, height = 96.dp)
                }
            }
        }

        item { SectionTitle("Moji racuni", icon = Icons.Filled.AccountBalanceWallet) }
        if (state.accounts.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = if (state.loadingAccounts) "Ucitavanje..." else "Nema otvorenih racuna.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.accounts, key = { it.id }) { acc ->
                        AccountCard(account = acc, balanceVisible = balanceVisible, onClick = { onNavigate(HomeAction.OpenAccount(acc.id)) })
                    }
                }
            }
        }

        item { SectionTitle("Brze akcije", icon = Icons.Filled.AttachMoney) }
        item { ClientQuickActions(onNavigate) }

        if (state.recipients.isNotEmpty()) {
            item { SectionTitle("Brzo placanje", icon = Icons.Filled.People) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.recipients, key = { it.id }) { r ->
                        RecipientChip(r, onClick = { onNavigate(HomeAction.NewPayment) })
                    }
                }
            }
        }

        if (state.recentPayments.isNotEmpty()) {
            item { SectionTitle("Poslednje transakcije", icon = Icons.Filled.Receipt) }
            items(state.recentPayments, key = { it.id }) { p ->
                TransactionRow(p, myAccountNumbers = state.accounts.map { it.accountNumber })
            }
        }

        if (state.exchangeRates.isNotEmpty()) {
            item { SectionTitle("Kursna lista", icon = Icons.Filled.CurrencyExchange) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.exchangeRates, key = { it.currency ?: it.fromCurrency ?: it.hashCode().toString() }) { rate ->
                        ExchangeRateCard(rate)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ClientHero(
    profile: UserProfile?,
    totalRsd: Double,
    foreignCount: Int,
    visible: Boolean,
    onToggle: () -> Unit,
    onNavigate: (HomeAction) -> Unit
) {
    val greeting = remember {
        val h = java.time.LocalTime.now().hour
        when {
            h < 6 -> "Dobra noc"
            h < 12 -> "Dobro jutro"
            h < 18 -> "Dobar dan"
            else -> "Dobro vece"
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF8B5CF6))))
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = greeting.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = profile?.firstName ?: profile?.fullName ?: "Korisnice",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ukupno stanje",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onToggle, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (visible) MoneyFormatter.format(totalRsd, 0) else "••••••",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "RSD",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            if (foreignCount > 0) {
                Text(
                    text = "+ $foreignCount devizn${if (foreignCount == 1) "i" else "a"} racun${if (foreignCount == 1) "" else "a"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroChip("Placanje", Icons.Filled.Payments) { onNavigate(HomeAction.NewPayment) }
                HeroChip("Transfer", Icons.Filled.SwapHoriz) { onNavigate(HomeAction.NewTransfer) }
                HeroChip("Menjacnica", Icons.Filled.CurrencyExchange) { onNavigate(HomeAction.OpenExchange) }
                HeroChip("OTP", Icons.Filled.NotificationsActive) { onNavigate(HomeAction.OpenOtp) }
            }
        }
    }
}

@Composable
private fun HeroChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun AccountCard(account: AccountDto, balanceVisible: Boolean, onClick: () -> Unit) {
    val grad = gradients[account.currency ?: "RSD"] ?: listOf(Color(0xFF64748B), Color(0xFF334155))
    Box(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(grad))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = account.name?.takeIf { it.isNotBlank() } ?: account.accountType ?: "Racun",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(account.currency ?: "—", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = AccountFormatter.formatAccountNumber(account.accountNumber),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Text("STANJE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
            Text(
                text = if (balanceVisible) MoneyFormatter.formatWithCurrency(account.balance, account.currency) else "••••",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Raspolozivo: " + (if (balanceVisible) MoneyFormatter.formatWithCurrency(account.availableBalance, account.currency) else "••••"),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ClientQuickActions(onNavigate: (HomeAction) -> Unit) {
    val items = listOf(
        QuickActionItem("Placanje", "Uplata ili prenos", Icons.Filled.Payments, HomeAction.NewPayment),
        QuickActionItem("Transfer", "Izmedju racuna", Icons.Filled.SwapHoriz, HomeAction.NewTransfer),
        QuickActionItem("Menjacnica", "Konverzija valuta", Icons.Filled.CurrencyExchange, HomeAction.OpenExchange),
        QuickActionItem("Kartice", "Upravljanje", Icons.Filled.CreditCard, HomeAction.OpenCards),
        QuickActionItem("Krediti", "Apliciranje", Icons.Filled.AccountBalance, HomeAction.OpenLoans),
        QuickActionItem("Primaoci", "Sacuvani kontakti", Icons.Filled.People, HomeAction.OpenRecipients),
        QuickActionItem("Berza", "Hartije", Icons.AutoMirrored.Filled.ShowChart, HomeAction.OpenSecurities),
        QuickActionItem("Portfolio", "Moje hartije", Icons.Filled.WorkOutline, HomeAction.OpenPortfolio),
        QuickActionItem("OTC", "Trgovina", Icons.Filled.People, HomeAction.OpenOtc),
        QuickActionItem("Fondovi", "Investicioni", Icons.Filled.AccountBalance, HomeAction.OpenFunds),
        QuickActionItem("Marzni", "Margin racun", Icons.Filled.AccountBalance, HomeAction.OpenMargin),
        QuickActionItem("Istorija", "Sve transakcije", Icons.Filled.Receipt, HomeAction.OpenPaymentHistory)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    QuickActionTileLarge(item, onClick = { onNavigate(item.action) }, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private data class QuickActionItem(val label: String, val sub: String, val icon: ImageVector, val action: HomeAction)

@Composable
private fun QuickActionTileLarge(item: QuickActionItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF7C3AED)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            item.sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecipientChip(r: RecipientDto, onClick: () -> Unit) {
    val initials = r.name.split(' ', '\t').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF7C3AED)))),
            contentAlignment = Alignment.Center
        ) {
            Text(initials.ifBlank { "?" }, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            r.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            AccountFormatter.formatAccountNumber(r.accountNumber),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun TransactionRow(p: PaymentListItemDto, myAccountNumbers: List<String>) {
    val isOutgoing = !p.direction.equals("INCOMING", true)
    val color = if (isOutgoing) Color(0xFFEF4444) else Color(0xFF10B981)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOutgoing) Icons.Filled.Payments else Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = p.recipientName ?: p.description ?: "Transakcija",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = p.status ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = (if (isOutgoing) "-" else "+") + MoneyFormatter.formatWithCurrency(p.amount, p.currency),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExchangeRateCard(rate: ExchangeRateDto) {
    val rsdPerUnit = rate.middleRate?.takeIf { it > 0 }?.let { 1 / it } ?: rate.rate ?: 0.0
    Box(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = rate.currency ?: rate.fromCurrency.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = MoneyFormatter.format(rsdPerUnit, 2) + " RSD",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "1 ${rate.currency ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
