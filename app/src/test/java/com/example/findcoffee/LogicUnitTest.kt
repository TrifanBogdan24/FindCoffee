package com.example.findcoffee

import com.example.findcoffee.coroutines.verifyInternetConnection
import com.example.findcoffee.coroutines.checkServerReachability
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LogicUnitTest {
    @Test
    fun testServerReachability_InvalidAddress() = runBlocking {
        // IP invalid -> va returna false
        val result = checkServerReachability("0.0.0.0", "8080")
        assertFalse(result)
    }
}
