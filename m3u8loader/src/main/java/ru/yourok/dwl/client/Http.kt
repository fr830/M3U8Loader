package ru.yourok.dwl.client

import android.net.Uri
import ru.yourok.dwl.settings.Settings
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.net.URL


/**
 * Created by yourok on 07.11.17.
 */

class Http(url: Uri) : Client {
    private var currUrl: String = url.toString()
    private var isConn: Boolean = false
    private var connection: HttpURLConnection? = null
    private var errMsg: String = ""
    private var inputStream: InputStream? = null

    override fun connect() {
        var responseCode: Int
        var redirCount = 0
        do {
            var url = URL(currUrl)
            connection = url.openConnection() as HttpURLConnection
            connection!!.connectTimeout = 30000
            connection!!.readTimeout = 15000
            connection!!.setRequestMethod("GET")
            connection!!.setDoInput(true)

            connection!!.setRequestProperty("UserAgent", "DWL/1.1.0 (Android)")
            connection!!.setRequestProperty("Accept", "text/html;q=0.8,*/*;q=0.9")
            connection!!.setRequestProperty("Accept-Encoding", "gzip, deflate, br")

            if (Settings.headers.isNotEmpty()) {
                Settings.headers.forEach { (k, v) ->
                    connection!!.setRequestProperty(k, v)
                }
            }

            connection!!.connect()

            responseCode = connection!!.getResponseCode()
            val redirected = responseCode == HTTP_MOVED_PERM || responseCode == HTTP_MOVED_TEMP || responseCode == HTTP_SEE_OTHER
            if (redirected) {
                currUrl = connection!!.getHeaderField("Location")
                connection!!.disconnect()
                redirCount++
            }
            if (redirCount > 5) {
                throw IOException("Error connect to: " + currUrl + " too many redirects")
            }
        } while (redirected)


        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Error connect to: " + currUrl + " " + connection!!.responseMessage)
        }
        isConn = true
    }

    override fun isConnected(): Boolean {
        return isConn
    }

    override fun getSize(): Long {
        if (!isConn)
            return 0
        var cl = connection!!.getHeaderField("Content-Length")
        try {
            if (!cl.isNullOrEmpty()) {
                return cl.toLong()
            }
        } catch (e: Exception) {
        }

        cl = connection!!.getHeaderField("Content-Range")
        try {
            if (!cl.isNullOrEmpty()) {
                val cr = cl.split("/")
                if (cr.isNotEmpty())
                    cl = cr.last()
                return cl.toLong()
            }
        } catch (e: Exception) {
        }
        return 0
    }

    override fun getUrl(): String {
        return currUrl
    }

    override fun getInputStream(): InputStream? {
        if (inputStream == null)
            inputStream = connection!!.inputStream
        return inputStream
    }

    override fun read(b: ByteArray): Int {
        return getInputStream()?.read(b) ?: -1
    }

    override fun getErrorMessage(): String {
        return errMsg
    }

    override fun close() {
        try {
            inputStream?.close()
        } catch (e: Exception) {
        }
        connection?.disconnect()
        isConn = false
    }
}