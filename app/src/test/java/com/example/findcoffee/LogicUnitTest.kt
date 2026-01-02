package com.example.findcoffee

import com.example.findcoffee.coroutines.verifyInternetConnection
import com.example.findcoffee.coroutines.checkServerReachability
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LogicUnitTest {

    @Test
    fun testBuildHighlightedText() {
        // Testam functia buildHighlightedText (va trebui mutata intr-un utilitar sau testata via Compose)
        // Deoarece returneaza AnnotatedString, verificam lungimea sau existenta stilurilor
    }

    @Test
    fun testServerReachability_InvalidAddress() = runBlocking {
        // Un IP invalid ar trebui sa returneze false
        val result = checkServerReachability("0.0.0.0", "8080")
        assertFalse(result)
    }
}
