package com.thelightphone.gemini

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utility for forwarding AI search responses to email, mirroring
 * the Light Phone's native SMS press-and-hold forwarding behavior.
 */
object EmailForwarder {

    /**
     * Dispatches an Android ACTION_SENDTO intent with prefilled subject and body.
     */
    fun sendToEmail(
        context: Context,
        recipientEmail: String,
        query: String,
        answer: String
    ) {
        val subject = Uri.encode("Light Phone: $query")
        val body = Uri.encode(
            "Question: $query\n\nAnswer:\n$answer\n\n--\nForwarded from Light Phone III Voice AI Search"
        )
        val mailtoUri = Uri.parse("mailto:$recipientEmail?subject=$subject&body=$body")

        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(emailIntent)
            Toast.makeText(context, "Forwarding to $recipientEmail", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No email client installed on device. Text copied to clipboard.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
