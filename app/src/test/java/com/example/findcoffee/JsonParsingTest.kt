package com.example.findcoffee

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JsonParsingTest {

    @Test
    fun `test parsing coffee recipe from mock JSON`() {
        val jsonString = """
            {
                "name": "Flat White",
                "category": "Milk Coffee",
                "final_volume": { "standard": "200ml" },
                "ingredients": { "standard": { "milk": "150ml", "espresso": "1 shot" } },
                "steps": { "1": { "title": "Brew", "description": "Make espresso" } }
            }
        """.trimIndent()

        val json = JSONObject(jsonString)

        assertEquals("Flat White", json.getString("name"))
        val finalVolumeObj = json.getJSONObject("final_volume")
        assertEquals("200ml", finalVolumeObj.getString("standard"))

        val steps = json.getJSONObject("steps")
        assertEquals(1, steps.length())
    }
}
