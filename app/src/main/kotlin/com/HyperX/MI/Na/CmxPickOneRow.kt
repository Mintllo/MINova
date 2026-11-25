// MINova: shared "pick one for me" row used by the favorites and playlist pages.
// The 3D cover flow itself is CmxRecommendationDeck (cmx/CmxDailyHero.kt); this row only
// triggers a random play from the list the caller already holds.
package com.HyperX.MI.Na

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CmxPickOneRow(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = "帮我挑一首",
    onPick: () -> Unit,
) {
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center) {
        Button(
            onClick = onPick,
            enabled = enabled,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 30.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .92f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
