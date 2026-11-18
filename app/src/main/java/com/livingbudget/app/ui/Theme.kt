package com.livingbudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light=lightColorScheme(
    primary=Color(0xFF94583D),onPrimary=Color(0xFFFFFAF4),secondary=Color(0xFF647763),
    background=Color(0xFFFAF6F0),onBackground=Color(0xFF3F342C),surface=Color(0xFFFFFCF7),
    onSurface=Color(0xFF3F342C),surfaceVariant=Color(0xFFF2DFC9),onSurfaceVariant=Color(0xFF786553),
    outline=Color(0xFF9A8977),outlineVariant=Color(0xFFEAE1D6),secondaryContainer=Color(0xFFE9EDE2)
)
private val Dark=darkColorScheme(
    primary=Color(0xFFE8B393),onPrimary=Color(0xFF41291D),secondary=Color(0xFFB5C7A7),
    background=Color(0xFF211E1A),onBackground=Color(0xFFECE0D3),surface=Color(0xFF2C2721),
    onSurface=Color(0xFFECE0D3),surfaceVariant=Color(0xFF4D3B2C),onSurfaceVariant=Color(0xFFD2BDA6),
    outline=Color(0xFFAA9783),outlineVariant=Color(0xFF4D4439),secondaryContainer=Color(0xFF394333)
)
@Composable fun BudgetTheme(theme: String,content: @Composable ()->Unit) {
    val dark=theme=="dark" || (theme=="system" && isSystemInDarkTheme())
    MaterialTheme(colorScheme=if(dark)Dark else Light,content=content)
}
