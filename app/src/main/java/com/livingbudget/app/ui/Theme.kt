package com.livingbudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.luminance
import com.livingbudget.app.R

private val Light=lightColorScheme(
    primary=Color(0xFF58715A),onPrimary=Color(0xFFFBFFF7),secondary=Color(0xFF647B54),
    background=Color(0xFFF0F3E9),onBackground=Color(0xFF354538),surface=Color(0xFFFCFDF8),
    onSurface=Color(0xFF354538),surfaceVariant=Color(0xFFD5DFC5),onSurfaceVariant=Color(0xFF62715A),
    outline=Color(0xFF7D8D73),outlineVariant=Color(0xFFDDE4D4),secondaryContainer=Color(0xFFE1E9D9),
    onSecondaryContainer=Color(0xFF354538),surfaceContainerHighest=Color(0xFFE1E9D9)
)
private val Dark=darkColorScheme(
    primary=Color(0xFFB2CAA2),onPrimary=Color(0xFF20351F),secondary=Color(0xFFB5C7A7),
    background=Color(0xFF1D251E),onBackground=Color(0xFFE3EBDD),surface=Color(0xFF273029),
    onSurface=Color(0xFFE3EBDD),surfaceVariant=Color(0xFF3A4934),onSurfaceVariant=Color(0xFFBDCCB5),
    outline=Color(0xFF92A489),outlineVariant=Color(0xFF40503A),secondaryContainer=Color(0xFF35452F),
    onSecondaryContainer=Color(0xFFE3EBDD),surfaceContainerHighest=Color(0xFF35452F)
)
@Composable fun BudgetTheme(theme: String,content: @Composable ()->Unit) {
    val dark=theme=="dark" || (theme=="system" && isSystemInDarkTheme())
    MaterialTheme(colorScheme=if(dark)Dark else Light,content=content)
}
@Composable fun SageBackdrop(content: @Composable ()->Unit) {
    val dark=MaterialTheme.colorScheme.background.luminance()<0.5f
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Image(painterResource(R.drawable.sage_light),contentDescription=null,modifier=Modifier.fillMaxSize(),
            contentScale=ContentScale.Crop,alpha=if(dark)0.035f else 0.12f)
        content()
    }
}
