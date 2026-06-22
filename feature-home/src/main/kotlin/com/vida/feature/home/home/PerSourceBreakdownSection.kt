package com.vida.feature.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.feature.home.PerSource

/**
 * Per-source breakdown section (R4).
 *
 * Renders wallet + each card + each stash as individual rows with
 * left-aligned label and right-aligned formatted balance.
 * Sections are omitted when their corresponding list is empty (S5).
 */
@Composable
fun PerSourceBreakdownSection(
    perSource: List<PerSource>,
    modifier: Modifier = Modifier,
) {
    if (perSource.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        perSource.forEachIndexed { index, source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = source.formatted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (index < perSource.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
