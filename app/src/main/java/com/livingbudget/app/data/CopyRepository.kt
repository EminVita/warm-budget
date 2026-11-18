package com.livingbudget.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/** Presentation copy only: no UI label is used as an event, enum, or database key. */
data class CopyBook(val entries: Map<String,String>) {
    operator fun get(key: String): String = entries[key] ?: "[$key]"
    fun format(key: String, vararg args: Any): String = try {
        String.format(java.util.Locale.SIMPLIFIED_CHINESE,get(key),*args)
    } catch (_: Exception) { get(key) }
}
class CopyRepository(context: Context) {
    private val file=File(context.filesDir,"copy-overrides.json")
    private val defaults=context.assets.open("copy.zh-CN.json").bufferedReader().use { parse(it.readText()) }
    private val mutable=MutableStateFlow(CopyBook(defaults + runCatching { parse(file.readText()) }.getOrDefault(emptyMap())))
    val flow=mutable.asStateFlow()
    val current get()=mutable.value
    fun exportDefaults(): String = JSONObject(defaults).toString(2)
    fun import(raw: String) {
        require(raw.toByteArray().size<=256_000)
        val map=parse(raw)
        require(map.isNotEmpty() && map.keys.all { it in defaults })
        map.forEach { (key,value) ->
            require(value.length<=2000)
            // Keep conversion types and ordering intact so formatting cannot crash after edits.
            val pattern=Regex("%(?:[0-9]+\\$)?[sd]")
            require(pattern.findAll(defaults.getValue(key)).map { it.value }.toList()==pattern.findAll(value).map { it.value }.toList())
        }
        val temporary=File(file.parentFile,"copy-overrides.tmp")
        temporary.writeText(JSONObject(map).toString(2))
        check(temporary.renameTo(file))
        mutable.value=CopyBook(defaults+map)
    }
    fun reset() { if(file.exists()) check(file.delete());mutable.value=CopyBook(defaults) }
    private fun parse(raw: String): Map<String,String> {
        val json=JSONObject(raw)
        return json.keys().asSequence().associateWith { key ->
            val value=json.get(key);require(value is String);value
        }
    }
}
