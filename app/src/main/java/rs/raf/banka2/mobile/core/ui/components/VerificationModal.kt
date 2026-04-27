package rs.raf.banka2.mobile.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import javax.inject.Inject

private const val OTP_TIMER_SECONDS = 300

/**
 * Centralni dijalog za 2FA OTP. Pratimo timer (300s), broj pokusaja
 * (max 3), email fallback, dev-mode prikaz aktivnog koda.
 *
 * Dijalog NE saljemo verifikaciju samostalno — kod se prosledjuje parent-u
 * koji ga ubacuje u svoj API poziv (npr. CreatePayment + otpCode atomicno).
 *
 * Pozivajuca strana je odgovorna da:
 *  - na uspesno verifikaciju zatvori dijalog
 *  - na grešku iz API poziva prosledi tu grešku ovde preko [externalError]
 *
 * Model je preuzet iz `Banka-2-Frontend/src/components/shared/VerificationModal.tsx`
 * sa istim ponasanjem.
 */
@Composable
fun VerificationModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (otpCode: String) -> Unit,
    isVerifying: Boolean,
    externalError: String? = null,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    if (!visible) return

    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(visible) {
        viewModel.initOnOpen()
    }

    LaunchedEffect(visible) {
        while (visible) {
            delay(1000L)
            viewModel.tickOneSecond()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isVerifying) onDismiss()
        },
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Potvrda OTP kodom",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Unesi 6-cifreni kod sa mobilne aplikacije.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    label = { Text("OTP kod") },
                    placeholder = { Text("123456") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    enabled = !isVerifying,
                    isError = externalError != null || state.localError != null,
                    supportingText = (externalError ?: state.localError)?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.devCode != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Aktivan kod: ${state.devCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    TextButton(onClick = { viewModel.fillFromActiveCode() }) {
                        Text("Popuni automatski")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preostalo vreme: ${formatSeconds(state.secondsLeft)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pokusaja: ${state.attemptsRemaining}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { scope.launch { viewModel.requestEmailOtp() } },
                    enabled = !isVerifying && !state.requestingEmail
                ) {
                    Icon(Icons.Outlined.Email, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.requestingEmail) "Saljem..." else "Posalji kod na email")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val code = state.code.trim()
                    if (code.length == 6) onSubmit(code)
                    else viewModel.setLocalError("Kod mora imati 6 cifara.")
                },
                enabled = !isVerifying && state.attemptsRemaining > 0 && state.secondsLeft > 0
            ) {
                Text(if (isVerifying) "Slanje..." else "Potvrdi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) {
                Text("Otkazi")
            }
        }
    )
}

private fun formatSeconds(total: Int): String {
    val mm = total / 60
    val ss = total % 60
    return "%02d:%02d".format(mm, ss)
}

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationState())
    val state: StateFlow<VerificationState> = _state.asStateFlow()

    fun initOnOpen() {
        _state.update {
            VerificationState(
                code = "",
                secondsLeft = OTP_TIMER_SECONDS,
                attemptsRemaining = 3,
                devCode = null,
                localError = null,
                requestingEmail = false
            )
        }
        viewModelScope.launch { fetchActiveCode() }
        viewModelScope.launch { paymentRepository.requestOtpToMobile() }
    }

    fun tickOneSecond() = _state.update {
        if (it.secondsLeft > 0) it.copy(secondsLeft = it.secondsLeft - 1) else it
    }

    fun onCodeChange(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(code = sanitized, localError = null) }
    }

    fun setLocalError(message: String) =
        _state.update { it.copy(localError = message) }

    fun decrementAttempts() = _state.update {
        it.copy(attemptsRemaining = (it.attemptsRemaining - 1).coerceAtLeast(0))
    }

    fun fillFromActiveCode() {
        val code = _state.value.devCode ?: return
        _state.update { it.copy(code = code, localError = null) }
    }

    suspend fun requestEmailOtp() {
        _state.update { it.copy(requestingEmail = true) }
        paymentRepository.requestOtpViaEmail()
        _state.update { it.copy(requestingEmail = false, secondsLeft = OTP_TIMER_SECONDS) }
    }

    private suspend fun fetchActiveCode() {
        when (val result = paymentRepository.getActiveOtp()) {
            is ApiResult.Success -> {
                if (result.data.active && !result.data.code.isNullOrBlank()) {
                    _state.update { it.copy(devCode = result.data.code) }
                }
            }
            else -> Unit
        }
    }
}

data class VerificationState(
    val code: String = "",
    val secondsLeft: Int = OTP_TIMER_SECONDS,
    val attemptsRemaining: Int = 3,
    val devCode: String? = null,
    val localError: String? = null,
    val requestingEmail: Boolean = false
)
