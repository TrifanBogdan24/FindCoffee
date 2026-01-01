package com.example.findcoffee

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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

    // Manager pentru cererea permisiunilor de locatie la runtime
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Daca utilizatorul a acceptat, reincarcam activitatea pentru a activa harta
            recreate()
        } else {
            Toast.makeText(this, "Permisiunea de locatie este necesara pentru harti!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializare SDK Places folosind cheia din BuildConfig (injectata prin Secrets Plugin)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        // Verificam permisiunile imediat la pornire
        checkLocationPermission()

        setContent {
            FindCoffeeTheme {
                MapScreen()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MapScreen() {
        val context = LocalContext.current
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cafenele in apropiere") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
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
                                // Configuram harta doar dupa ce este gata si avem permisiuni
                                setupMapWithLocation(fusedLocationClient)
                            }
                        }
                    },
                    update = { mapView ->
                        mapView.onResume()
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupMapWithLocation(fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient) {
        val map = googleMap ?: return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            // Activeaza punctul albastru (locatia utilizatorului) pe harta
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true

            // Obtine locatia curenta si centreaza camera
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)

                    // Zoom 15f este ideal pentru a vedea cafenelele la nivel de strada
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

                    // Cautam automat cafenele in jurul punctului gasit
                    searchNearbyCoffees(userLatLng)
                } else {
                    Toast.makeText(this, "GPS activ? Nu am putut detecta locatia.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun searchNearbyCoffees(location: LatLng) {
        val placesClient = Places.createClient(this)

        // Definim ce date dorim sa primim de la Google despre fiecare loc
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS
        )

        // Cautare textuala automata
        val request = SearchByTextRequest.builder("coffee shop barista coffee", placeFields)
            .setMaxResultCount(15) // Afisam maxim 15 rezultate pentru claritate
            .setLocationBias(com.google.android.libraries.places.api.model.CircularBounds.newInstance(location, 3000.0)) // Raza 3km
            .build()

        placesClient.searchByText(request)
            .addOnSuccessListener { response ->
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
                Toast.makeText(this, "Eroare Places: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}
