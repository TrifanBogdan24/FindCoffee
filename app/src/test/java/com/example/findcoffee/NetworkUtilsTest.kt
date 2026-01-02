package com.example.findcoffee

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun `test IP cleaning logic`() {
        val input1 = "http://192.168.1.1"
        val input2 = "https://10.0.2.2"
        val input3 = "  192.168.1.5  "

        fun clean(ip: String) = ip.trim().removePrefix("http://").removePrefix("https://")

        assertEquals("192.168.1.1", clean(input1))
        assertEquals("10.0.2.2", clean(input2))
        assertEquals("192.168.1.5", clean(input3))
    }

    @Test
    fun `test API URL construction`() {
        val ip = "192.168.1.1"
        val port = "5000"
        val expected = "http://192.168.1.1:5000/api/coffee_recipes"

        val actual = "http://$ip:$port/api/coffee_recipes"
        assertEquals(expected, actual)
    }
}
