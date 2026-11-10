package com.tenants.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Saves streamed PDF bytes into application cache and launches Android Share/View intent.
 */
open class PdfDownloadHelper {

    open fun saveAndSharePdf(context: Context, pdfBytes: ByteArray, fileName: String) {
        val pdfDir = File(context.cacheDir, "invoices")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val pdfFile = File(pdfDir, fileName)
        FileOutputStream(pdfFile).use { fos ->
            fos.write(pdfBytes)
            fos.flush()
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Tenant Invoice via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
