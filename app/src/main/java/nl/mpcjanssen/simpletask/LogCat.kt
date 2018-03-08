package nl.mpcjanssen.simpletask

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


object LogCat {
    fun getLog() : List<String> {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(
                    InputStreamReader(process.inputStream))

            val log =  bufferedReader.readLines()
            bufferedReader.close()
            process.destroy()
            log
        } catch (e: IOException) {
            e.stackTrace.map {it.toString()}
        }

    }
}