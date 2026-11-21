package com.example.miniproject.receipt

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.miniproject.ui.theme.MoneeAmber
import com.example.miniproject.ui.theme.MoneeBorder
import com.example.miniproject.ui.theme.MoneeCardBorder
import com.example.miniproject.ui.theme.MoneeGradientButton
import com.example.miniproject.ui.theme.MoneeInk
import com.example.miniproject.ui.theme.MoneeMuted
import com.example.miniproject.ui.theme.moneeCardColors
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * "Scan" / "Upload" card in the Monee style. "Scan" opens ML Kit's document scanner, which finds
 * the receipt edges, flattens the perspective and cleans the image before OCR. Runs OCR on the
 * result and hands the parsed receipt to [onResult] so the caller can pre-fill its form.
 * Nothing is saved here: the user reviews and edits the form first.
 */
@Composable
fun ReceiptScanButton(
    modifier: Modifier = Modifier,
    onResult: (ReceiptData) -> Unit,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }

    fun scan(uri: Uri) {
        scope.launch {
            scanning = true
            try {
                val data = ReceiptScanner.scan(context, uri)
                if (data.hasAnything) {
                    onResult(data)
                } else {
                    onError("No text could be read. Retake the photo with good light and the whole receipt in view.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ReceiptScan", "scan failed", e)
                onError("Could not read the receipt: ${e.message ?: "unknown error"}")
            } finally {
                scanning = false
            }
        }
    }

    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)   // the Upload button already covers the gallery
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.pages?.firstOrNull()?.imageUri
            if (uri != null) scan(uri) else onError("No image came back from the scanner.")
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scan(uri)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = moneeCardColors(),
        border = MoneeCardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Scan receipt",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                color = MoneeInk
            )
            Text(
                "Scan or upload a receipt to auto-fill the details. You can edit everything before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MoneeMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (scanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MoneeAmber
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reading receipt...", color = MoneeInk)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    // Primary action: Monee yellow gradient
                    MoneeGradientButton(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity == null) {
                                onError("Could not open the scanner.")
                            } else {
                                scanner.getStartScanIntent(activity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(IntentSenderRequest.Builder(it).build())
                                    }
                                    .addOnFailureListener {
                                        Log.e("ReceiptScan", "scanner unavailable", it)
                                        onError("Scanner unavailable: ${it.message ?: "unknown error"}")
                                    }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan", fontWeight = FontWeight.SemiBold)
                    }

                    // Secondary action: white with the Monee border
                    OutlinedButton(
                        onClick = {
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MoneeBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MoneeInk)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
