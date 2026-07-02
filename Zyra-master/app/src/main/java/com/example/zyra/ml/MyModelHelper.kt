package com.example.zyra.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MyModelHelper(context: Context) {

    companion object {
        // TODO: Change this to match your model’s output size (e.g. 2, 4, etc.)
        private const val OUTPUT_SIZE = 2
    }

    private var interpreter: Interpreter

    init {
        val modelBuffer = loadModelFile(context, "fraud_detection.tflite")
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            val fileChannel = input.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    fun runInference(inputData: Array<FloatArray>): Array<FloatArray> {
        val outputData = Array(1) { FloatArray(OUTPUT_SIZE) }
        interpreter.run(inputData, outputData)
        return outputData
    }

    fun close() {
        interpreter.close()
    }
}
