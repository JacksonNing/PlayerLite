package com.wxy.playerlite.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal object AccountVisualStyle {
    val accentColor = Color(0xFFE53935)
    val accentDeepColor = Color(0xFFC62828)
    val accentSoftColor = Color(0xFFFDECEA)
    val accentTextColor = Color(0xFFA52723)
    val contentHorizontalPadding = 24.dp
    val sectionSpacing = 16.dp
    val contentMaxWidth = 420.dp
    val cardCorner = 12.dp
    val primaryButtonHeight = 52.dp
}

@Composable
internal fun AccountPageBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

@Composable
internal fun AccountCardSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AccountVisualStyle.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                AccountVisualStyle.sectionSpacing
            ),
            content = content
        )
    }
}

@Composable
internal fun AccountPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(AccountVisualStyle.primaryButtonHeight),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccountVisualStyle.accentColor,
            contentColor = Color.White,
            disabledContainerColor = AccountVisualStyle.accentColor.copy(alpha = 0.48f),
            disabledContentColor = Color.White.copy(alpha = 0.82f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
