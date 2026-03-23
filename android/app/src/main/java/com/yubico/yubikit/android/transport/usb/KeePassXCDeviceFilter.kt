package com.yubico.yubikit.android.transport.usb

class KeePassXCDeviceFilter : DeviceFilter() {
    override fun checkVendorProductIds(vendorId: Int, productId: Int): Boolean {
        return vendorId == 0x1050 || vendorId == 0x1d50
    }
}
