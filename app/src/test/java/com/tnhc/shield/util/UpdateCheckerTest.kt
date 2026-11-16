package com.tnhc.shield.util

import org.junit.Assert.*
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testVersionComparison() {
        val method = UpdateChecker::class.java.getDeclaredMethod(
            "isNewerVersion",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }

        assertTrue(method.invoke(UpdateChecker, "1.5.0", "1.0.0") as Boolean)
        assertTrue(method.invoke(UpdateChecker, "2.0.0", "1.9.9") as Boolean)
        assertTrue(method.invoke(UpdateChecker, "1.0.1", "1.0.0") as Boolean)
        assertFalse(method.invoke(UpdateChecker, "1.0.0", "1.0.0") as Boolean)
        assertFalse(method.invoke(UpdateChecker, "0.9.0", "1.0.0") as Boolean)
    }
}
