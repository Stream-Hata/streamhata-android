package dev.rdpctd.streamhata.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rdpctd.streamhata.core.ApiClient
import dev.rdpctd.streamhata.core.PointsWallet
import dev.rdpctd.streamhata.core.ViewerSummary
import dev.rdpctd.streamhata.core.ui.StreamHataBorder
import dev.rdpctd.streamhata.core.ui.StreamHataMuted
import dev.rdpctd.streamhata.core.ui.StreamHataPink
import dev.rdpctd.streamhata.core.ui.StreamHataSurface

@Composable
fun ViewersScreen(api: ApiClient, onBack: () -> Unit) {
    var viewers by remember { mutableStateOf<List<ViewerSummary>>(emptyList()) }
    LaunchedEffect(Unit) { api.viewers { viewers = it } }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Зрители", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(viewers) { viewer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(viewer.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("@${viewer.login}", color = StreamHataMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("streak ${viewer.watchStreak}", color = StreamHataPink)
                    }
                }
            }
        }
    }
}

@Composable
fun PointsScreen(api: ApiClient, onBack: () -> Unit) {
    var wallets by remember { mutableStateOf<List<PointsWallet>>(emptyList()) }
    LaunchedEffect(Unit) { api.points { wallets = it } }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Баллы", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(wallets) { wallet ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                    border = BorderStroke(1.dp, StreamHataBorder)
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(wallet.displayName.ifBlank { wallet.login })
                        Text("${wallet.balance}", color = StreamHataPink, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
