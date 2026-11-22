package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tactile 3D-styled glossy icon badge with top-left specular sheen, rich dual-tone gradient,
 * bottom 3D rim depth, and centered crisp vector icon.
 */
@Composable
fun Icon3DBadge(
    icon: ImageVector,
    contentDescription: String?,
    topColor: Color,
    bottomColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 14.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 3D bottom extrusion shadow base
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = 2.5.dp)
                .clip(shape)
                .background(bottomColor.copy(alpha = 0.55f))
        )

        // Main glossy 3D pill body
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation = 6.dp, shape = shape, ambientColor = bottomColor, spotColor = bottomColor)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(topColor, bottomColor)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Top-left glossy specular highlight
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 3.dp)
                    .size(size * 0.42f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Subtle icon drop shadow
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.22f),
                modifier = Modifier
                    .size(iconSize)
                    .offset(x = 0.5.dp, y = 1.2.dp)
            )
            // Crisp foreground icon
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
