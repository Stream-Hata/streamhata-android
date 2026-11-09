package dev.rdpctd.streamhata.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun StreamHataLoading(
    label: String = "Загрузка…",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val scale by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "dot-scale"
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .graphicsLayer {
                            scaleX = if (index == 1) scale else 1f
                            scaleY = if (index == 1) scale else 1f
                        }
                        .background(
                            if (index == 1) StreamHataPink else StreamHataCyan,
                            CircleShape
                        )
                )
            }
        }
        Text(label, color = StreamHataMuted)
    }
}
