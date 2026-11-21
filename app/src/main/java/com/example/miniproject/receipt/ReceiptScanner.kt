package com.example.miniproject.receipt

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

private data class OcrLine(val text: String, val left: Int, val centerY: Double, val height: Int)

/**
 * OCR step: image -> text rows -> ReceiptParser.
 * Uses ML Kit's on-device text recogniser, so the receipt photo never leaves the phone.
 */
object ReceiptScanner {

    suspend fun scan(context: Context, uri: Uri): ReceiptData {
        // fromFilePath also applies the photo's EXIF rotation
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = recognizer.process(image).await()
            val rows = toRows(text)
            // Debug aid: copy this from Logcat (filter "ReceiptScan") to see exactly what OCR read
            Log.i("ReceiptScan", "OCR rows:\n" + rows.joinToString("\n"))
            return ReceiptParser.parse(rows).copy(rawText = rows.joinToString("\n"))
        } finally {
            recognizer.close()
        }
    }

    /**
     * ML Kit returns the item name and its price as separate blocks (left and right column).
     * Join everything that sits on the same horizontal line back into one row,
     * otherwise "Nasi Lemak" and "12.50" would never be paired.
     */
    private fun toRows(text: Text): List<String> {
        val lines = text.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrLine(line.text, box.left, box.exactCenterY().toDouble(), box.height())
            }
        if (lines.isEmpty()) return emptyList()

        val medianHeight = lines.map { it.height }.sorted()[lines.size / 2].coerceAtLeast(1)
        val tolerance = medianHeight * 0.6

        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in lines.sortedBy { it.centerY }) {
            val current = rows.lastOrNull()
            if (current != null && abs(line.centerY - current.map { it.centerY }.average()) < tolerance) {
                current += line
            } else {
                rows += mutableListOf(line)
            }
        }
        return rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
