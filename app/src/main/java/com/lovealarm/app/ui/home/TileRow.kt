package com.lovealarm.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lovealarm.app.data.Tile
import com.lovealarm.app.ui.theme.tileColor

/**
 * One tile on the board. Tap sends it. [onLongClick] is only passed for custom tiles (delete);
 * presets get null, so a long press on them does nothing.
 *
 * The Surface takes no onClick: the click handling lives on the inner Box so it is never handled twice.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TileRow(
    tile: Tile,
    index: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = tileColor(index),
        contentColor = ink,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .combinedClickable(
                    onClickLabel = "Send",
                    onLongClickLabel = if (onLongClick != null) "Delete" else null,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = tile.text,
                style = MaterialTheme.typography.titleMedium,
                color = ink,
            )
        }
    }
}
