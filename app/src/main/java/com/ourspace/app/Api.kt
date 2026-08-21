package com.ourspace.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class Session(val accessToken: String, val refreshToken: String, val userId: String)
data class Couple(val id: String, val code: String, val boyId: String?, val girlId: String?)
data class Note(val id: Long, val text: String?, val imageUrl: String?, val userId: String, val createdAt: String)

object Api {
    private fun request(method: String, url: String, token: String? = null, body: String? = null, contentType: String = "application/json"): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
        if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", contentType)
        conn.setRequestProperty("Accept", "application/json")
        if (url.contains("/rest/v1/")) conn.setRequestProperty("Prefer", "return=representation")
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) throw Exception(parseError(text, code))
        return text
    }

    private fun parseError(text: String, code: Int): String {
        return try {
            val o = JSONObject(text)
            o.optString("msg").ifBlank {
                o.optString("message").ifBlank { o.optString("error_description").ifBlank { "Request failed ($code)" } }
            }
        } catch (_: Exception) { "Request failed ($code)" }
    }

    fun signUp(email: String, password: String): Session? {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val r = request("POST", "${SupabaseConfig.URL}/auth/v1/signup", body = body)
        val o = JSONObject(r)
        val access = o.optString("access_token")
        if (access.isBlank()) return null
        return Session(access, o.optString("refresh_token"), o.getJSONObject("user").getString("id"))
    }

    fun signIn(email: String, password: String): Session {
        val body = JSONObject().put("email", email).put("password", password).toString()
        val r = request("POST", "${SupabaseConfig.URL}/auth/v1/token?grant_type=password", body = body)
        val o = JSONObject(r)
        return Session(o.getString("access_token"), o.optString("refresh_token"), o.getJSONObject("user").getString("id"))
    }

    fun createProfile(session: Session, name: String, role: String) {
        val body = JSONObject()
            .put("id", session.userId)
            .put("display_name", name.ifBlank { "My Love" })
            .put("role", role)
            .toString()
        request("POST", "${SupabaseConfig.URL}/rest/v1/profiles", session.accessToken, body)
    }

    fun getProfileRole(session: Session): String {
        val q = URLEncoder.encode("id", "UTF-8")
        val r = request("GET", "${SupabaseConfig.URL}/rest/v1/profiles?select=role&$q=eq.${session.userId}&limit=1", session.accessToken)
        val a = JSONArray(r)
        if (a.length() == 0) throw Exception("Profile not found. Please finish setup.")
        return a.getJSONObject(0).getString("role")
    }

    fun createCouple(session: Session, role: String): Pair<String,String> {
        val body = JSONObject().put("p_role", role).toString()
        val r = request("POST", "${SupabaseConfig.URL}/rest/v1/rpc/create_couple", session.accessToken, body)
        val o = JSONObject(r)
        return o.getString("couple_id") to o.getString("code")
    }

    fun joinCouple(session: Session, role: String, code: String): Pair<String,String> {
        val body = JSONObject().put("p_code", code.uppercase()).put("p_role", role).toString()
        val r = request("POST", "${SupabaseConfig.URL}/rest/v1/rpc/join_couple", session.accessToken, body)
        val o = JSONObject(r)
        return o.getString("couple_id") to o.getString("code")
    }

    fun getCouple(session: Session): Couple? {
        val r = request(
            "GET",
            "${SupabaseConfig.URL}/rest/v1/couples?select=id,code,boy_id,girl_id&or=(boy_id.eq.${session.userId},girl_id.eq.${session.userId})&limit=1",
            session.accessToken
        )
        val a = JSONArray(r)
        if (a.length() == 0) return null
        val o = a.getJSONObject(0)
        return Couple(o.getString("id"), o.getString("code"), o.optString("boy_id").ifBlank { null }, o.optString("girl_id").ifBlank { null })
    }

    fun listNotes(session: Session, coupleId: String): List<Note> {
        val r = request(
            "GET",
            "${SupabaseConfig.URL}/rest/v1/notes?select=id,text,image_url,created_at,user_id&couple_id=eq.$coupleId&order=created_at.desc&limit=50",
            session.accessToken
        )
        val a = JSONArray(r)
        return buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Note(o.getLong("id"), o.optString("text").ifBlank { null }, o.optString("image_url").ifBlank { null }, o.getString("user_id"), o.getString("created_at")))
            }
        }
    }

    fun addNote(session: Session, coupleId: String, text: String, imageUrl: String?): Note {
        val body = JSONObject()
            .put("couple_id", coupleId)
            .put("user_id", session.userId)
            .put("text", if (text.isBlank()) JSONObject.NULL else text)
            .put("image_url", imageUrl ?: JSONObject.NULL)
            .toString()
        val r = request(
            "POST",
            "${SupabaseConfig.URL}/rest/v1/notes?select=id,text,image_url,created_at,user_id",
            session.accessToken,
            body
        )
        val o = JSONArray(r).getJSONObject(0)
        return Note(o.getLong("id"), o.optString("text").ifBlank { null }, o.optString("image_url").ifBlank { null }, o.getString("user_id"), o.getString("created_at"))
    }

    fun uploadImage(session: Session, coupleId: String, bytes: ByteArray, mime: String): String {
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val path = "$coupleId/${UUID.randomUUID()}.$ext"
        val conn = URL("${SupabaseConfig.URL}/storage/v1/object/media/$path").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        conn.setRequestProperty("Content-Type", mime)
        conn.setRequestProperty("x-upsert", "false")
        conn.outputStream.use { it.write(bytes) }
        val code = conn.responseCode
        val err = if (code !in 200..299) conn.errorStream?.bufferedReader()?.use { it.readText() } else null
        conn.disconnect()
        if (code !in 200..299) throw Exception(parseError(err ?: "", code))
        return "${SupabaseConfig.URL}/storage/v1/object/public/media/$path"
    }

    fun downloadBitmap(url: String): Bitmap? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 15000
        c.inputStream.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) { null }

    fun readBytes(uri: Uri, resolver: android.content.ContentResolver): Pair<ByteArray,String> {
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val input: InputStream = resolver.openInputStream(uri) ?: throw Exception("Could not open image")
        input.use { original ->
            val bitmap = BitmapFactory.decodeStream(original) ?: throw Exception("Unsupported image")
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            return out.toByteArray() to "image/jpeg"
        }
    }
}
