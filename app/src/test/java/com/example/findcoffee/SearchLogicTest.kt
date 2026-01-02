package com.example.findcoffee

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchLogicTest {

    @Test
    fun `test highlight logic with simple match`() {
        val text = "Cappuccino"
        val query = "cap"

        // Verificam daca filtrarea de baza (case insensitive) functioneaza
        val isMatch = text.contains(query, ignoreCase = true)
        assertEquals(true, isMatch)
    }

    @Test
    fun `test coffee name formatting`() {
        val rawName = "caffe_latte"
        val formatted = rawName.replace("_", " ").replaceFirstChar { it.uppercase() }
        assertEquals("Caffe latte", formatted)
    }
}
