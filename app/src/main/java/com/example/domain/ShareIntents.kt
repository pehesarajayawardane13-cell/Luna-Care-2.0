package com.example.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareIntents {

    fun sendViaWhatsApp(
        context: Context,
        rawPhone: String,
        message: String,
        onError: (String) -> Unit
    ) {
        val digits = rawPhone.replace(Regex("\\D"), "")
        if (digits.isBlank()) {
            onError("කරුණාකර සැකසුම් තුළ WhatsApp අංකයක් ඇතුළත් කරන්න!")
            return
        }
        val normalized = if (digits.startsWith("0") && digits.length == 10) {
            "94" + digits.substring(1)
        } else {
            digits
        }
        val uri = Uri.parse("https://wa.me/$normalized?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            try {
                context.startActivity(Intent.createChooser(fallback, "Share Report"))
            } catch (_: Exception) {
                onError("WhatsApp හෝ Share යෙදුමක් හමු නොවීය")
            }
        }
    }

    fun sendViaEmail(
        context: Context,
        emailAddress: String,
        subject: String,
        body: String,
        onError: (String) -> Unit
    ) {
        val cleanEmail = emailAddress.trim()
        if (cleanEmail.isBlank()) {
            onError("කරුණාකර සැකසුම් තුළ Email ලිපිනයක් ඇතුළත් කරන්න!")
            return
        }
        val uri = Uri.parse(
            "mailto:${Uri.encode(cleanEmail)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            try {
                context.startActivity(Intent.createChooser(fallback, "Send Email"))
            } catch (_: Exception) {
                onError("Email යෙදුමක් හමු නොවීය")
            }
        }
    }
}
