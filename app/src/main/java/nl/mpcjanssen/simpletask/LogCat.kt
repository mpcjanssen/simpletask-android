package nl.mpcjanssen.simpletask

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


object LogCat {
    fun getLog() : List<String> {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(
                    InputStreamReader(process.inputStream))

            val log =  bufferedReader.readLines()
            bufferedReader.close()
            process.destroy()
            return log
        } catch (e: IOException) {
            return e.stackTrace.map {it.toString()}
        }

    }
}