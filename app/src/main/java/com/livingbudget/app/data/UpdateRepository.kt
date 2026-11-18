package com.livingbudget.app.data

import android.content.Context
import com.livingbudget.app.presentation.UpdateInfo
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class UpdateRepository(context: Context) {
    private val manifestUrl=context.assets.open("app-config.json").bufferedReader().use {
        JSONObject(it.readText()).getString("updateManifestUrl")
    }
    val configured get()=manifestUrl.isNotBlank()
    fun check(): UpdateInfo {
        require(URI(manifestUrl).scheme=="https")
        val connection=URL(manifestUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout=15000;connection.readTimeout=15000;connection.instanceFollowRedirects=false
            require(connection.responseCode==200)
            val raw=connection.inputStream.use { it.readBytesLimited(256_000).toString(Charsets.UTF_8) }
            val json=JSONObject(raw)
            val url=json.getString("downloadUrl")
            val uri=URI(url);require(uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null)
            val code=json.getLong("versionCode");require(code>0)
            return UpdateInfo(code,json.getString("versionName"),url,json.optString("notes"))
        } finally { connection.disconnect() }
    }
}
fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val result=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true) { val n=read(buffer);if(n<0)break;require(result.size()+n<=limit);result.write(buffer,0,n) }
    return result.toByteArray()
}
