package com.example.findcoffee.data_base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

suspend fun fetchCoffeeRecipes(ip: String, port: String): List<JSONObject> = withContext(Dispatchers.IO) {
    val url = URL("http://$ip:$port/api/coffee_recipes")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 5000

    return@withContext try {
        val respCode = connection.responseCode
        if (respCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonArr = org.json.JSONArray(response)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until jsonArr.length()) {
            list.add(jsonArr.getJSONObject(i))
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    } finally {
        connection.disconnect()
    }
}
