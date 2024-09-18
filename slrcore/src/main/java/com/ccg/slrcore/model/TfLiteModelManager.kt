package com.ccg.slrcore.model

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ccg.slrcore.common.Config
import com.ccg.slrcore.common.FilterUnit
import com.ccg.slrcore.common.PassThroughFilterSingle
import com.ccg.slrcore.common.PredictionFilter
import com.ccg.slrcore.common.SLREventHandler
import com.ccg.slrcore.common.argmax
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import jakarta.mail.util.SharedByteArrayInputStream
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.LinkedList
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


abstract class TfLiteModelManager(
        model: File,
    ) {
    protected val interpreter: Interpreter = Interpreter(
        model,
        Interpreter.Options().apply{ this.setNumThreads(Config.NUM_WORKER_THREADS_MEDIAPIPE) }
    )
}

class SLRTfLiteModel <T> (
    model: File,
    val mapping: List<T>
): TfLiteModelManager(model) {
    private val modelInputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 60, 21 * 2, 1), DataType.FLOAT32)
    private val modelOutputTensor = TensorBuffer.createFixedSize(interpreter.getOutputTensor(0).shape(), DataType.FLOAT32)

    public var outputFilters: MutableList<PredictionFilter<T>> = LinkedList<PredictionFilter<T>>().also { it.add(PassThroughFilterSingle()) }

    fun runModel(inputArray: FloatArray) {
        modelInputTensor.loadArray(inputArray)
        interpreter.run(modelInputTensor.buffer,  modelOutputTensor.buffer)
        var values = FilterUnit(mapping,modelOutputTensor.floatArray)
        for (filter in outputFilters)
            values = filter.filter(values)
        callbacks.forEach {
            (_, cb) ->
            if (values.mapping.size == 1)
                cb.handle(values.mapping[0])
            else if (values.mapping.size > 1)
                cb.handle(values.mapping[values.probabilities.argmax()])
        }
    }

    private val callbacks: HashMap<String, SLREventHandler<T>> = HashMap()
    fun addCallback(name: String, callback: SLREventHandler<T>) {
        this.callbacks[name] = callback
    }

    fun removeCallback(name: String) {
        if (name in this.callbacks) this.callbacks.remove(name)
    }
}

/**
 * This function requires the CCG SLR Model Server API to be implemented
 */
fun NetworkModelLoader(context: Context, modelServer: String, target: String, callback: (SLRTfLiteModel<String>) -> Unit) {
    //TODO: get signed certificate and then use remove this complicated code
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }

        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
    }
    )

    // Install the all-trusting trust manager
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustAllCerts, SecureRandom())
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

    // Create all-trusting host name verifier
    val allHostsValid =
        HostnameVerifier { hostname, session -> true }

    // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
    // TODO: save credentials somewhere else
    val basicAuth = "Basic " + String(Base64.getEncoder().encode("invite:CCG_invite_USERS_2024!".encodeToByteArray()))

    var cookieString = "none;"
    val cookie = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        ".model-cookie"
        )
    if (cookie.exists()) cookieString = BufferedReader(InputStreamReader(FileInputStream(cookie))).readLine()

    //TODO: make more competent
    Thread {
        try {
            with(URL("$modelServer?cookie=$cookieString").openConnection() as HttpURLConnection) {
                setRequestProperty("Authorization", basicAuth)
                if (responseCode == 100) {

                } else {
                    if (!File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            target
                        ).exists()
                    ) File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        target
                    ).mkdirs()
                    BufferedReader(InputStreamReader(inputStream)).use {
                        fun writeToFile(
                            multipart: MimeMultipart,
                            filename: String,
                            contentID: String
                        ) {
                            val file = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                filename
                            )
                            if (!file.exists()) file.createNewFile()
                            val output = FileOutputStream(file)
                            if (contentID == "model")
                                output.write((multipart.getBodyPart(contentID).content as SharedByteArrayInputStream).readBytes()) // Write your downloaded file data here
                            else if (contentID == "mapping" || contentID == "cookie")
                                output.write((multipart.getBodyPart(contentID).content as String).toByteArray())
                            output.close()
                            if (!file.exists()) throw RuntimeException("File does not exist")
                        }

                        val multipart =
                            MimeMultipart(ByteArrayDataSource(inputStream, "multipart/form-data"))
                        writeToFile(multipart, "$target/model.tflite", "model")
                        writeToFile(multipart, "$target/mapping.txt", "mapping")
                        writeToFile(multipart, "$target/.model-cookie", "cookie")
                        print("Written model and mapping")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkModelLoader", "Failed to load model from network, defaulting to last one available")
            throw e
        }
        // TODO: document that app will throw Exception if model cannot be loaded
        callback(SLRTfLiteModel(
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "$target/model.tflite"),
            BufferedReader(FileReader(File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "$target/mapping.txt")
            )).readLines()
                .map { it.trim() }
        ))

    }.start()
}
