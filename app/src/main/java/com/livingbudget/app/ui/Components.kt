package com.livingbudget.app.ui

import androidx.compose.foundation.Canvas
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
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(22.dp).then(if(description!=null)Modifier.semantics { contentDescription=description } else Modifier)) {
        val u=size.width/24f
        fun line(x1:Float,y1:Float,x2:Float,y2:Float) { drawLine(color,Offset(x1*u,y1*u),Offset(x2*u,y2*u),1.6f*u) }
        fun path(vararg points: Pair<Float,Float>) { val p=Path();points.forEachIndexed { i,v->if(i==0)p.moveTo(v.first*u,v.second*u) else p.lineTo(v.first*u,v.second*u) };drawPath(p,color,style=Stroke(1.6f*u)) }
        when(glyph) {
            Glyph.HOME -> { path(3f to 10f,12f to 3f,21f to 10f);path(5f to 9f,5f to 21f,10f to 21f,10f to 15f,14f to 15f,14f to 21f,19f to 21f,19f to 9f) }
            Glyph.PLUS -> { line(12f,5f,12f,19f);line(5f,12f,19f,12f) }
            Glyph.ARROW -> path(9f to 5f,16f to 12f,9f to 19f)
            Glyph.BACK -> path(15f to 5f,8f to 12f,15f to 19f)
            Glyph.WALLET -> { path(4f to 6f,20f to 6f,20f to 20f,4f to 20f,4f to 4f,16f to 4f);path(15f to 11f,22f to 11f,22f to 16f,15f to 16f,15f to 11f) }
            Glyph.LEAF -> { val p=Path();p.moveTo(5*u,19*u);p.cubicTo(1*u,8*u,9*u,3*u,20*u,4*u);p.cubicTo(20*u,16*u,13*u,21*u,7*u,17*u);drawPath(p,color,style=Stroke(1.6f*u));line(5f,20f,16f,9f) }
            Glyph.HISTORY -> { drawCircle(color,8*u,Offset(13*u,12*u),style=Stroke(1.6f*u));path(3f to 4f,3f to 11f,10f to 11f);path(13f to 7f,13f to 12f,17f to 15f) }
            Glyph.BELL -> { path(5f to 17f,19f to 17f,17f to 13f,17f to 8f,14f to 4f,10f to 4f,7f to 8f,7f to 13f,5f to 17f);line(10f,21f,14f,21f) }
            Glyph.DOWNLOAD,Glyph.UPLOAD -> {
                val down=glyph==Glyph.DOWNLOAD
                line(12f,3f,12f,16f);path(7f to (if(down)11f else 8f),12f to (if(down)16f else 3f),17f to (if(down)11f else 8f))
                path(4f to 17f,4f to 21f,20f to 21f,20f to 17f)
            }
            Glyph.SUN,Glyph.SETTINGS -> { drawCircle(color,4*u,Offset(12*u,12*u),style=Stroke(1.6f*u));
                for(i in 0..7){val a=i*Math.PI/4;line((12+8*kotlin.math.cos(a)).toFloat(),(12+8*kotlin.math.sin(a)).toFloat(),(12+10*kotlin.math.cos(a)).toFloat(),(12+10*kotlin.math.sin(a)).toFloat())} }
        }
    }
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
