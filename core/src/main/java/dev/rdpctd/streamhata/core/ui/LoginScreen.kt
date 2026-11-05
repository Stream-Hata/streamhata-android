package dev.rdpctd.streamhata.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StreamHataLogin(
    title: String,
    subtitle: String,
    features: List<String>,
    onLogin: () -> Unit,
) {
    Surface(color = Color.Transparent, contentColor = StreamHataText) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .background(
                        Brush.linearGradient(listOf(StreamHataPink, StreamHataCyan)),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("S", color = Color(0xFF0F0817), style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = StreamHataMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StreamHataSurface),
                border = BorderStroke(1.dp, StreamHataBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(StreamHataCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(22.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти через Twitch", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                "Откроется браузер. Пароль не вводится в приложении.",
                style = MaterialTheme.typography.bodySmall,
                color = StreamHataMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
