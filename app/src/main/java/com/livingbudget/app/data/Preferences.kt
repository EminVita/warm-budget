package com.livingbudget.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("preferences")
data class Preferences(val reminders: Boolean = false, val hour: Int = 9, val minute: Int = 0, val theme: String = "system")
class PreferenceRepository(private val context: Context) {
    private val enabled=booleanPreferencesKey("reminders")
    private val hour=intPreferencesKey("hour")
    private val minute=intPreferencesKey("minute")
    private val theme=stringPreferencesKey("theme")
    val flow=context.store.data.map { Preferences(it[enabled]?:false,it[hour]?:9,it[minute]?:0,it[theme]?:"system") }
    suspend fun setReminders(value: Boolean) { context.store.edit { it[enabled]=value } }
    suspend fun setTime(h: Int, m: Int) {
        require(h in 0..23 && m in 0..59)
        context.store.edit { it[hour]=h;it[minute]=m }
    }
    suspend fun setTheme(value: String) {
        require(value in setOf("system","light","dark"))
        context.store.edit { it[theme]=value }
    }
}
