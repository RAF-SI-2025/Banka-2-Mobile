package rs.raf.banka2.mobile.feature.payments.details

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.AnimatedBackground
import rs.raf.banka2.mobile.core.ui.components.BankaScaffold
import rs.raf.banka2.mobile.core.ui.components.ErrorBanner
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.core.ui.components.PrimaryButton
import java.io.File

@Composable
fun PaymentDetailsScreen(
    onBack: () -> Unit,
    viewModel: PaymentDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentDetailsEvent.Toast -> snackbar.showSnackbar(event.message)
                is PaymentDetailsEvent.ReceiptDownloaded -> {
                    runCatching {
                        val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
                        val file = File(cacheDir, "potvrda-${state.payment?.id ?: "x"}.pdf")
                        file.writeBytes(event.pdfBytes)
                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(context, "Ne mogu da otvorim PDF — sacuvan u cache.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    BankaScaffold(
        title = "Detalji placanja",
        onBack = onBack,
        snackbarHostState = snackbar,
        backgroundDecoration = {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedBackground()
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error)
            state.payment?.let { payment ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(payment.recipientName ?: "Placanje", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        MoneyFormatter.formatWithCurrency(payment.amount, payment.currency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    LineItem("Status", payment.status ?: "—")
                    LineItem("Datum", DateFormatter.formatDateTime(payment.createdAt))
                    LineItem("Sa racuna", payment.fromAccount ?: "—")
                    LineItem("Na racun", payment.toAccount ?: "—")
                    LineItem("Sifra", payment.paymentCode ?: "—")
                    payment.referenceNumber?.let { LineItem("Poziv na broj", it) }
                    payment.fee?.let { LineItem("Provizija", MoneyFormatter.format(it, 2)) }
                    payment.description?.let { LineItem("Svrha", it) }
                }
                PrimaryButton(
                    text = if (state.downloading) "Generisem PDF..." else "Preuzmi potvrdu (PDF)",
                    onClick = viewModel::downloadReceipt,
                    loading = state.downloading,
                    leadingIcon = Icons.Filled.PictureAsPdf,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LineItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
