package com.example.findcoffee

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HttpApiTest {

    private lateinit var mockWebServer: MockWebServer
    private var baseUrl: String = ""

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        // Adresa IP si portul sunt generate dinamic de MockWebServer
        baseUrl = mockWebServer.url("/api").toString()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test api root connectivity`() {
        // Pregateste raspuns simulat pentru /api
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val connection = URL(baseUrl).openConnection() as HttpURLConnection
        assertEquals(200, connection.responseCode)
    }

    @Test
    fun `test fetch coffee recipes`() {
        // Simuleaza JSON-ul returnat de request-ul /api/coffee_recipes
        val mockJson = """
        [
            {
                "name": "Espresso",
                "category": "Classic",
                "ingredients": { "standard": { "Coffee": "7g" } },
                "final_volume": { "standard": "30ml" },
                "steps": []
            }
        ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockJson)
            .addHeader("Content-Type", "application/json"))

        // Executa request-ul catre URL-ul simulat
        val response = URL("$baseUrl/coffee_recipes").readText()
        val jsonArray = JSONArray(response)

        assertEquals(1, jsonArray.length())
        assertEquals("Espresso", jsonArray.getJSONObject(0).getString("name"))
    }

    @Test
    fun `test category not found returns 404`() {
        // Simuleaza eroarea HHTP 404
        val errorJson = "{\"error\": \"Category not found\"}"
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody(errorJson))

        val connection = URL("$baseUrl/category/unknown").openConnection() as HttpURLConnection
        assertEquals(404, connection.responseCode)
    }

    @Test
    fun `test get coffee names`() {
        val mockJson = "[\"Espresso\", \"Cappuccino\", \"Latte\"]"
        mockWebServer.enqueue(MockResponse().setBody(mockJson))

        val response = URL("$baseUrl/coffees").readText()
        val jsonArray = JSONArray(response)

        assertEquals(3, jsonArray.length())
        assertEquals("Espresso", jsonArray.getString(0))
    }

    @Test
    fun `test image endpoint construction`() {
        // Verificare: URL-ul sa fie construit corect
        val coffeeName = "caffe_latte"
        val expectedRoute = "/api/images/coffee_list/$coffeeName"

        // Simulam un raspuns OK pentru imagine
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val url = URL(mockWebServer.url(expectedRoute).toString())
        val connection = url.openConnection() as HttpURLConnection

        assertEquals(200, connection.responseCode)
        assertEquals(expectedRoute, url.path)
    }
}
