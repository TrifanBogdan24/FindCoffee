package com.example.findcoffee

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.findcoffee.ui.theme.FindCoffeeTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.gms.maps.MapView

class CoffeeMapsActivity : ComponentActivity() {

    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializare SDK Places
        if (!Places.isInitialized()) {
            // Folosim getString pentru a prelua cheia din strings.xml
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        setContent {
            FindCoffeeTheme {
                MapScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("MissingPermission")
    @Composable
    fun MapScreen() {
        val context = LocalContext.current
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Cafenele în apropiere") })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            onCreate(null)
                            getMapAsync { map ->
                                googleMap = map
                                // Verificam permisiunile inainte de a activa locatia pe harta
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    map.isMyLocationEnabled = true
                                    getCurrentLocationAndSearch(fusedLocationClient)
                                }
                            }
                        }
                    },
                    update = { mapView -> mapView.onResume() }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndSearch(fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                searchNearbyCoffees(userLatLng)
            }
        }
    }

    private fun searchNearbyCoffees(location: LatLng) {
        val placesClient = Places.createClient(this)

        // Fields pe care le cerem de la Google
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

        // Noua metoda de cautare pentru SDK v4.0+
        val request = SearchByTextRequest.builder("coffee shop", placeFields)
            .setMaxResultCount(10)
            .setLocationBias(com.google.android.libraries.places.api.model.CircularBounds.newInstance(location, 5000.0))
            .build()

        placesClient.searchByText(request)
            .addOnSuccessListener { response ->
                // In noile versiuni folosim response.places
                for (place in response.places) {
                    val latLng = place.latLng
                    if (latLng != null) {
                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(place.name)
                                .snippet(place.address)
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Eroare: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}