package rs.raf.banka2.mobile.feature.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * [FE2 Mobile port — AddToWatchlistDialog] Radix-DropdownMenu ekvivalent u
 * Compose AlertDialog-u. Prikazuje listu korisnikovih watchlist-a + opciju
 * "Nova lista" sa inline kreiranjem.
 *
 * Pozivamo iz SecuritiesDetailsScreen ("Dodaj na watchlist" dugme).
 */
@Composable
fun AddToWatchlistDialog(
    listingId: Long,
    listingTicker: String,
    onDismiss: () -> Unit,
    viewModel: AddToWatchlistViewModel = hiltViewModel(),
) {
    LaunchedEffect(listingId) {
        viewModel.reset(listingId, listingTicker)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.successMessage) {
        // Auto-close 1s posle success-a
        if (state.successMessage != null) {
            kotlinx.coroutines.delay(1000)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj $listingTicker na watchlist") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (state.successMessage != null) {
                    Text(
                        text = state.successMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                when {
                    state.loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Ucitavanje listi...")
                        }
                    }
                    state.watchlists.isEmpty() && !state.showInlineCreate -> {
                        Column {
                            Text(
                                "Nemate kreiranih listi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            InlineCreateButton(onClick = viewModel::toggleInlineCreate)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(state.watchlists, key = { it.id }) { wl ->
                                WatchlistOption(
                                    name = wl.name,
                                    itemCount = wl.itemCount,
                                    submitting = state.submittingWatchlistId == wl.id,
                                    onClick = { viewModel.addToExisting(wl.id) },
                                )
                            }
                            if (!state.showInlineCreate) {
                                item {
                                    InlineCreateButton(onClick = viewModel::toggleInlineCreate)
                                }
                            }
                        }
                        if (state.showInlineCreate) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.newListName,
                                onValueChange = viewModel::setNewListName,
                                label = { Text("Naziv nove liste") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = viewModel::toggleInlineCreate) { Text("Otkazi") }
                                TextButton(
                                    onClick = viewModel::submitInlineCreate,
                                    enabled = !state.creatingList && state.newListName.isNotBlank(),
                                ) {
                                    Text(if (state.creatingList) "Kreiranje..." else "Kreiraj i dodaj")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zatvori") }
        },
    )
}

@Composable
private fun WatchlistOption(
    name: String,
    itemCount: Int,
    submitting: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = !submitting, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.BookmarkBorder,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$itemCount stavki",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (submitting) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InlineCreateButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlaylistAdd,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            "Nova lista",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
