package com.profconq.app.util

import android.os.Build

fun isEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val hardware = Build.HARDWARE.lowercase()
    val brand = Build.BRAND.lowercase()
    val device = Build.DEVICE.lowercase()

    return fingerprint.startsWith("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        model.contains("sdk_gphone") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        product.contains("simulator") ||
        hardware.contains("ranchu") ||
        hardware.contains("goldfish") ||
        hardware.contains("vbox") ||
        (brand.startsWith("generic") && device.startsWith("generic"))
}
