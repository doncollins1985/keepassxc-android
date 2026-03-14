package org.keepassxc.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JniBridgeTest {

    @Test
    fun testNativeCoreLoaded() {
        // If the native library fails to load, this will throw an UnsatisfiedLinkError
        val str = NativeCore.stringFromJNI()
        assertNotNull(str)
    }
}
