package com.livingbudget.app.ui

import androidx.compose.ui.res.painterResource
import com.livingbudget.app.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class Glyph { HOME, SETTINGS, PLUS, LEAF, WALLET, HISTORY, BELL, SUN, DOWNLOAD, UPLOAD, ARROW, BACK }
@Composable fun LineIcon(glyph: Glyph,description: String?=null) {
    val resource=when(glyph) {
        Glyph.HOME -> R.drawable.ic_tabler_home
        Glyph.SETTINGS -> R.drawable.ic_tabler_settings
        Glyph.PLUS -> R.drawable.ic_tabler_plus
        Glyph.LEAF -> R.drawable.ic_tabler_leaf
        Glyph.WALLET -> R.drawable.ic_tabler_wallet
        Glyph.HISTORY -> R.drawable.ic_tabler_history
        Glyph.BELL -> R.drawable.ic_tabler_bell
        Glyph.SUN -> R.drawable.ic_tabler_sun
        Glyph.DOWNLOAD -> R.drawable.ic_tabler_download
        Glyph.UPLOAD -> R.drawable.ic_tabler_upload
        Glyph.ARROW -> R.drawable.ic_tabler_chevron_right
        Glyph.BACK -> R.drawable.ic_tabler_chevron_left
    }
    Icon(painterResource(resource),contentDescription=description,modifier=Modifier.size(22.dp),tint=LocalContentColor.current)
}
@Composable fun WarmCard(modifier: Modifier=Modifier,tinted: Boolean=false,content: @Composable ColumnScope.()->Unit) {
    Surface(modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),
        color=if(tinted)MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable fun SettingRow(label: String,glyph: Glyph,value: String?=null,hint: String?=null,onClick: ()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).heightIn(min=56.dp).padding(vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        LineIcon(glyph)
        Column(Modifier.weight(1f)) { Text(label,style=MaterialTheme.typography.bodyMedium)
            if(hint!=null)Text(hint,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        if(value!=null)Text(value,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        LineIcon(Glyph.ARROW)
    }
}
