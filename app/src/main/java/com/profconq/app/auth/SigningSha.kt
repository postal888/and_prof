package com.profconq.app.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object SigningSha {
    fun sha1Colon(context: Context): String = digest(context, "SHA-1")

    private fun digest(context: Context, algorithm: String): String {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            val signing = info.signingInfo ?: return ""
            if (signing.hasMultipleSigners()) {
                signing.apkContentsSigners
            } else {
                signing.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
            ).signatures
        }
        val first = signatures?.firstOrNull()?.toByteArray() ?: return ""
        val bytes = MessageDigest.getInstance(algorithm).digest(first)
        return bytes.joinToString(":") { "%02X".format(it) }
    }
}
