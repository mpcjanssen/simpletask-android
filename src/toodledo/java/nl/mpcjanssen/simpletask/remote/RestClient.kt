package nl.mpcjanssen.simpletask.remote

import android.util.Base64
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.HashMap

import javax.net.ssl.HttpsURLConnection

object RestClient {
    fun performPostCall(requestURL: String,
                        postDataParams: List<Pair<String, String>>?,
                        headers:  List<Pair<String, String>>?): String {

        val url: URL
        var response = ""
        try {
            url = URL(requestURL)

            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "POST"
            headers?.forEach {
                conn.setRequestProperty(it.first, it.second)
            }
            conn.doInput = true
            conn.doOutput = true


            val os = conn.outputStream
            val writer = BufferedWriter(
                    OutputStreamWriter(os, "UTF-8"))
            writer.write(getPostDataString(postDataParams))

            writer.flush()
            writer.close()
            os.close()
            val responseCode = conn.responseCode

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                var line: String
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                br.forEachLine {
                    response += it
                }
            } else {
                response = ""

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return response
    }

    fun performGetCall(requestURL: String,
                       getDataParams: List<Pair<String, String>>?,
                       headers:  List<Pair<String, String>>? = null): String {

        var response = ""
        try {
            val url = URL(getUrl(requestURL, getDataParams))

            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            headers?.forEach {
                conn.setRequestProperty(it.first, it.second)
            }
            conn.doInput = true
            conn.doOutput = false


            val responseCode = conn.responseCode

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                var line: String
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                br.forEachLine {
                    response += it
                }
            } else {
                response = ""

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return response
    }

    fun getUrl( requestURL: String, getDataParams: List<Pair<String, String>>?): String {
        val getRequest = getDataParams?.map { pair ->
                    "${URLEncoder.encode(pair.first, "UTF-8")}=" +
                    "${URLEncoder.encode(pair.second, "UTF-8")}"
            }?.joinToString("&", prefix="?") ?: ""
        return requestURL + getRequest
    }

    private fun getPostDataString(params: List<Pair<String, String>>?): String? {
        if (params==null) return ""
        val result = StringBuilder()
        var first = true
        for (entry in params) {
            if (first)
                first = false
            else
                result.append("&")

            result.append(URLEncoder.encode(entry.first, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(entry.second, "UTF-8"))
        }

        return result.toString()
    }
    fun basicAuthorizationString(username: String, passwd: String): String {
        return "Basic " + Base64.encodeToString(
                (username + ":" + passwd).toByteArray(),
                Base64.NO_WRAP);

    }

}
