package org.keepassxc.android

import com.yubico.yubikit.android.transport.usb.UsbConfiguration

fun test() {
    val methods = UsbConfiguration::class.java.methods
    for (m in methods) {
        println("METHOD: " + m.name)
    }
}
