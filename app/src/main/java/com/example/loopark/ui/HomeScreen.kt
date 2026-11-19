package com.example.loopark.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.loopark.data.ParkingStorage
import com.example.loopark.location.LocationHelper
import com.example.loopark.location.MapNavigationHelper
import com.example.loopark.model.ParkingSpot
import com.example.loopark.ui.theme.AccentGreen
import com.example.loopark.ui.theme.AccentGreenBorder
import com.example.loopark.ui.theme.AccentGreenContainer
import com.example.loopark.ui.theme.AccentGreenDark
import com.example.loopark.ui.theme.AccentGreenLight
import com.example.loopark.ui.theme.DarkNavyBackground
import com.example.loopark.ui.theme.DarkNavyBorder
import com.example.loopark.ui.theme.DarkNavyBorderLight
import com.example.loopark.ui.theme.DarkNavySurface
import com.example.loopark.ui.theme.DarkNavySurfaceElevated
import com.example.loopark.ui.theme.MetricCardBackground
import com.example.loopark.ui.theme.ParkMateTheme
import com.example.loopark.ui.theme.TextDim
import com.example.loopark.ui.theme.TextLight
import com.example.loopark.ui.theme.TextMuted
import com.example.loopark.ui.theme.TextWhite
import com.example.loopark.ui.theme.WarningAmber
import com.example.loopark.util.PhotoHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val storage = remember { ParkingStorage(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Real GPS-backed state permanently persisted in storage
    var parkingSpot by remember { mutableStateOf(storage.loadParkingSpot()) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var liveDistanceMeters by remember { mutableStateOf<Float?>(null) }

    // Loading states
    var isSavingSpot by remember { mutableStateOf(false) }
    var isFetchingLiveDistance by remember { mutableStateOf(false) }

    // Permission and GPS state
    var hasLocationPermission by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showGpsDisabledDialog by remember { mutableStateOf(false) }

    // Structured Edit Notes dialog states
    var showEditNotesDialog by remember { mutableStateOf(false) }
    var tempFloorText by remember { mutableStateOf(parkingSpot.floorOrLevel) }
    var tempPillarText by remember { mutableStateOf(parkingSpot.pillarNumber) }
    var tempSlotText by remember { mutableStateOf(parkingSpot.slotNumber) }
    var tempNotesText by remember { mutableStateOf(parkingSpot.notes) }
    var tempPhotoPath by remember { mutableStateOf(parkingSpot.photoPath) }

    // Photo Chooser Sheet/Dialog
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }

    // ---------------------------------------------------------
    // PHOTO PICKER & CAMERA LAUNCHERS
    // ---------------------------------------------------------

    // 1. Camera Launcher (Full-resolution via FileProvider)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempFile != null) {
            val savedPath = PhotoHelper.saveFileToInternalStorage(context, cameraTempFile!!)
            if (savedPath != null) {
                tempPhotoPath = savedPath
                val updated = parkingSpot.copy(photoPath = savedPath)
                parkingSpot = updated
                storage.saveParkingSpot(updated)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("📷 Parking photo captured & saved!")
                }
            }
        }
    }

    // 2. Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val (uri, file) = PhotoHelper.createCameraTempUri(context)
                cameraTempFile = file
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to open camera: ${e.localizedMessage}")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take a parking photo.")
            }
        }
    }

    // 3. Gallery Photo Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = PhotoHelper.saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                tempPhotoPath = savedPath
                val updated = parkingSpot.copy(photoPath = savedPath)
                parkingSpot = updated
                storage.saveParkingSpot(updated)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("🖼️ Photo selected from gallery & saved!")
                }
            }
        }
    }

    val triggerCameraCapture: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val (uri, file) = PhotoHelper.createCameraTempUri(context)
                cameraTempFile = file
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to open camera: ${e.localizedMessage}")
                }
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val triggerGalleryPick: () -> Unit = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Re-check permissions and GPS on lifecycle resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = LocationHelper.hasLocationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // -------------------------------------------------------------------------
    // REAL-TIME DISTANCE TRACKING LISTENER
    // Automatically recalculates distance whenever user's GPS location changes
    // -------------------------------------------------------------------------
    DisposableEffect(parkingSpot.isSaved, hasLocationPermission) {
        if (!parkingSpot.isSaved || !hasLocationPermission || !LocationHelper.isGpsEnabled(context)) {
            return@DisposableEffect onDispose { }
        }

        val savedLat = parkingSpot.latitude ?: return@DisposableEffect onDispose { }
        val savedLng = parkingSpot.longitude ?: return@DisposableEffect onDispose { }

        if (liveDistanceMeters == null) {
            isFetchingLiveDistance = true
        }

        val callback = LocationHelper.startLocationUpdates(context) { newLocation ->
            currentLocation = newLocation
            isFetchingLiveDistance = false

            // Requirement 3: Use Android Location.distanceTo()
            val distance = LocationHelper.calculateDistance(
                currentLocation = newLocation,
                savedLatitude = savedLat,
                savedLongitude = savedLng
            )
            liveDistanceMeters = distance
        }

        onDispose {
            LocationHelper.stopLocationUpdates(context, callback)
        }
    }

    // Single-shot GPS capture for "SAVE MY PARKING SPOT"
    val executeGpsCapture: () -> Unit = {
        if (!LocationHelper.isGpsEnabled(context)) {
            showGpsDisabledDialog = true
        } else {
            isSavingSpot = true
            LocationHelper.getCurrentLocation(
                context = context,
                onSuccess = { location ->
                    coroutineScope.launch {
                        val timestamp = SimpleDateFormat(
                            "MMM d, yyyy • h:mm a",
                            Locale.getDefault()
                        ).format(Date())

                        // Reverse-geocode address in background thread
                        val address = LocationHelper.getAddressFromCoordinates(
                            context = context,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )

                        val updatedSpot = parkingSpot.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = address,
                            savedTimestamp = timestamp,
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                            tags = listOfNotNull(
                                "GPS Fix Verified",
                                if (location.hasAccuracy()) "±${location.accuracy.roundToInt()}m" else null,
                                parkingSpot.floorOrLevel.takeIf { it.isNotBlank() },
                                parkingSpot.pillarNumber.takeIf { it.isNotBlank() }?.let { "Pillar $it" }
                            )
                        )

                        // 1. Save permanently in storage
                        parkingSpot = updatedSpot
                        storage.saveParkingSpot(updatedSpot)

                        // 2. Set current location and calculate distance (0m initially at vehicle)
                        currentLocation = location
                        val dist = LocationHelper.calculateDistance(
                            currentLocation = location,
                            savedLatitude = location.latitude,
                            savedLongitude = location.longitude
                        )
                        liveDistanceMeters = dist
                        isSavingSpot = false
                        isFetchingLiveDistance = false

                        val displayLoc = address ?: updatedSpot.formattedCoordinates
                        snackbarHostState.showSnackbar("✅ Spot saved at $displayLoc!")
                    }
                },
                onError = { errorMsg ->
                    isSavingSpot = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("❌ $errorMsg")
                    }
                }
            )
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val granted = fineGranted || coarseGranted
        hasLocationPermission = granted

        if (granted) {
            executeGpsCapture()
        } else {
            isSavingSpot = false
            showPermissionDeniedDialog = true
        }
    }

    val handleSaveSpotClicked: () -> Unit = {
        if (!LocationHelper.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            executeGpsCapture()
        }
    }

    // Manual Refresh handler for Distance
    val handleManualRefreshDistance: () -> Unit = {
        if (!parkingSpot.isSaved) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please save your parking spot first.")
            }
        } else if (!LocationHelper.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!LocationHelper.isGpsEnabled(context)) {
            showGpsDisabledDialog = true
        } else {
            isFetchingLiveDistance = true
            LocationHelper.getCurrentLocation(
                context = context,
                onSuccess = { loc ->
                    currentLocation = loc
                    val savedLat = parkingSpot.latitude ?: return@getCurrentLocation
                    val savedLng = parkingSpot.longitude ?: return@getCurrentLocation
                    val dist = LocationHelper.calculateDistance(loc, savedLat, savedLng)
                    liveDistanceMeters = dist
                    isFetchingLiveDistance = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("📍 Live Distance: ${LocationHelper.formatDistance(dist)}")
                    }
                },
                onError = { err ->
                    isFetchingLiveDistance = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Could not refresh distance: $err")
                    }
                }
            )
        }
    }

    // Navigation intent handler
    val handleNavigateToCar: () -> Unit = {
        if (!parkingSpot.isSaved || parkingSpot.latitude == null || parkingSpot.longitude == null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please save your parking location first.")
            }
        } else {
            MapNavigationHelper.openDirections(
                context = context,
                latitude = parkingSpot.latitude!!,
                longitude = parkingSpot.longitude!!,
                onError = { errorMessage ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("❌ $errorMessage")
                    }
                }
            )
        }
    }

    // Reset / clear saved spot
    val handleClearSpot: () -> Unit = {
        storage.clearParkingSpot()
        parkingSpot = ParkingSpot()
        liveDistanceMeters = null
        currentLocation = null
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Saved parking spot and photo cleared.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkNavyBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkNavyBackground,
                    titleContentColor = TextWhite
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(AccentGreen, AccentGreenDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = "ParkMate Logo",
                                tint = DarkNavyBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ParkMate",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AccentGreenContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreenBorder)
                                ) {
                                    Text(
                                        text = "SPOT SAVER",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = AccentGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Smart Vehicle Locator",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }
                    }
                },
                actions = {
                    if (parkingSpot.isSaved) {
                        IconButton(onClick = handleClearSpot) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Clear spot",
                                tint = TextMuted
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 2. Parking-Themed Welcome Header
            WelcomeHeader(isSpotSaved = parkingSpot.isSaved, spot = parkingSpot)

            // 3. Large "Save My Parking Spot" Button (with Loading Indicator)
            SaveParkingSpotButton(
                isLoading = isSavingSpot,
                onSaveClicked = handleSaveSpotClicked
            )

            // 4. Saved Parking Location Section (Actual GPS coordinates / address)
            SavedLocationSection(
                spot = parkingSpot,
                onShareClicked = {
                    if (parkingSpot.isSaved) {
                        val shareText = "📍 ParkMate: My car is parked at ${parkingSpot.displayTitle}\n" +
                                (if (parkingSpot.floorOrLevel.isNotBlank()) "Floor: ${parkingSpot.floorOrLevel}\n" else "") +
                                (if (parkingSpot.pillarNumber.isNotBlank()) "Pillar: ${parkingSpot.pillarNumber}\n" else "") +
                                (if (parkingSpot.slotNumber.isNotBlank()) "Slot: ${parkingSpot.slotNumber}\n" else "") +
                                (if (parkingSpot.notes.isNotBlank()) "Notes: ${parkingSpot.notes}\n" else "") +
                                "Coordinates: ${parkingSpot.formattedCoordinates}\n" +
                                "Saved on: ${parkingSpot.savedTimestamp}\n" +
                                "Google Maps: https://maps.google.com/?q=${parkingSpot.latitude},${parkingSpot.longitude}"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Parking Spot"))
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please save a parking spot first.")
                        }
                    }
                }
            )

            // 5. Distance to Vehicle Section (REAL-TIME UPDATES VIA Location.distanceTo)
            DistanceToVehicleSection(
                spot = parkingSpot,
                distanceMeters = liveDistanceMeters,
                currentLocation = currentLocation,
                isLoading = isFetchingLiveDistance,
                onRefreshDistance = handleManualRefreshDistance
            )

            // 6. Large "Navigate to My Car" Button
            NavigateToCarButton(
                spot = parkingSpot,
                distanceMeters = liveDistanceMeters,
                onNavigateClicked = handleNavigateToCar
            )

            // 7. Parking Notes and Photo Section
            ParkingNotesAndPhotoSection(
                spot = parkingSpot,
                onEditClicked = {
                    if (parkingSpot.isSaved) {
                        tempFloorText = parkingSpot.floorOrLevel
                        tempPillarText = parkingSpot.pillarNumber
                        tempSlotText = parkingSpot.slotNumber
                        tempNotesText = parkingSpot.notes
                        tempPhotoPath = parkingSpot.photoPath
                        showEditNotesDialog = true
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please save your parking spot first.")
                        }
                    }
                },
                onAddPhotoClicked = {
                    if (parkingSpot.isSaved) {
                        showPhotoSourceDialog = true
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please save your parking spot first.")
                        }
                    }
                }
            )

            // Parking Meter / Session Timer Card
            ParkingTimerCard(spot = parkingSpot)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ---------------------------------------------------------
    // DIALOGS: PERMISSIONS, GPS DISABLED & STRUCTURED NOTES
    // ---------------------------------------------------------

    // Photo Source Chooser Dialog (Camera vs Gallery)
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            containerColor = DarkNavySurfaceElevated,
            title = {
                Text(
                    text = "Add Parking Photo",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "Capture a photo of your parking spot, floor marker, or pillar for quick visual reference:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            triggerCameraCapture()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = DarkNavyBackground
                        )
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            triggerGalleryPick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkNavySurface,
                            contentColor = TextWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // GPS Disabled Dialog
    if (showGpsDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDisabledDialog = false },
            containerColor = DarkNavySurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocationOff,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "GPS Location is Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "ParkMate requires GPS to calculate real-time distance and save your vehicle's parking coordinates. Please turn on Location services in your settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDisabledDialog = false
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = DarkNavyBackground
                    )
                ) {
                    Text("Turn On GPS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDisabledDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Location Permission Denied Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            containerColor = DarkNavySurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Location Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "ParkMate needs access to your device's location to calculate real-time distance and navigate back to your car. Please grant location permission in App Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = DarkNavyBackground
                    )
                ) {
                    Text("Open App Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Structured Parking Notes & Photo Edit Dialog
    if (showEditNotesDialog) {
        AlertDialog(
            onDismissRequest = { showEditNotesDialog = false },
            containerColor = DarkNavySurfaceElevated,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Parking Notes & Photo",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter details to help locate your vehicle easily:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    // 1. Parking Floor
                    OutlinedTextField(
                        value = tempFloorText,
                        onValueChange = { tempFloorText = it },
                        label = { Text("Parking Floor") },
                        placeholder = { Text("e.g. Basement 2 or Level 3", color = TextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = DarkNavyBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = AccentGreen,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = AccentGreen
                        ),
                        singleLine = true
                    )

                    // 2. Pillar Number
                    OutlinedTextField(
                        value = tempPillarText,
                        onValueChange = { tempPillarText = it },
                        label = { Text("Pillar Number") },
                        placeholder = { Text("e.g. B4 or Pillar 12", color = TextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = DarkNavyBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = AccentGreen,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = AccentGreen
                        ),
                        singleLine = true
                    )

                    // 3. Slot Number
                    OutlinedTextField(
                        value = tempSlotText,
                        onValueChange = { tempSlotText = it },
                        label = { Text("Parking Slot Number") },
                        placeholder = { Text("e.g. B-14 or Spot #42", color = TextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = DarkNavyBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = AccentGreen,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = AccentGreen
                        ),
                        singleLine = true
                    )

                    // 4. Additional Notes
                    OutlinedTextField(
                        value = tempNotesText,
                        onValueChange = { tempNotesText = it },
                        label = { Text("Additional Notes") },
                        placeholder = { Text("e.g. Near the lift, ticket in dashboard...", color = TextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = DarkNavyBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = AccentGreen,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = AccentGreen
                        ),
                        minLines = 2,
                        maxLines = 4
                    )

                    // 5. Parking Photo Attachment
                    Text(
                        text = "PARKING PHOTO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = AccentGreen
                    )

                    if (!tempPhotoPath.isNullOrBlank()) {
                        val previewBitmap = remember(tempPhotoPath) {
                            tempPhotoPath?.let { PhotoHelper.loadRotatedBitmap(it, 300) }
                        }
                        if (previewBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, DarkNavyBorderLight, RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Selected Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { tempPhotoPath = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove photo",
                                        tint = TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { triggerCameraCapture() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreenLight)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { triggerGalleryPick() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreenLight)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = parkingSpot.copy(
                            floorOrLevel = tempFloorText.trim(),
                            pillarNumber = tempPillarText.trim(),
                            slotNumber = tempSlotText.trim(),
                            notes = tempNotesText.trim(),
                            photoPath = tempPhotoPath
                        )
                        parkingSpot = updated
                        storage.saveParkingSpot(updated)
                        showEditNotesDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("✅ Parking notes & photo updated!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = DarkNavyBackground
                    )
                ) {
                    Text("Save Details", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNotesDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

// ---------------------------------------------------------
// 2. WELCOME HEADER (PARKING-THEMED)
// ---------------------------------------------------------
@Composable
fun WelcomeHeader(isSpotSaved: Boolean, spot: ParkingSpot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavySurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AccentGreenContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreenBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSpotSaved) AccentGreen else WarningAmber)
                        )
                        Text(
                            text = if (isSpotSaved) "CAR PARKED & GPS LOCKED" else "NO SPOT SAVED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isSpotSaved) AccentGreen else WarningAmber
                        )
                    }
                }

                Text(
                    text = "GPS Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim
                )
            }

            Text(
                text = if (isSpotSaved) "Your car is parked safely 🚗" else "Welcome to ParkMate 🚗",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextWhite
            )

            Text(
                text = if (isSpotSaved) {
                    "Your exact GPS spot was recorded on ${spot.savedTimestamp ?: "today"}. Live distance and notes are ready."
                } else {
                    "Park your car and tap 'Save My Parking Spot' below to capture your phone's real GPS coordinates, floor, and photo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                lineHeight = 20.sp
            )
        }
    }
}

// ---------------------------------------------------------
// 3. LARGE "SAVE MY PARKING SPOT" BUTTON (WITH LOADING INDICATOR)
// ---------------------------------------------------------
@Composable
fun SaveParkingSpotButton(
    isLoading: Boolean,
    onSaveClicked: () -> Unit
) {
    Button(
        onClick = onSaveClicked,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentGreen,
            contentColor = DarkNavyBackground,
            disabledContainerColor = AccentGreen.copy(alpha = 0.6f),
            disabledContentColor = DarkNavyBackground.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = DarkNavyBackground,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ACQUIRING GPS FIX...",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = DarkNavyBackground
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkNavyBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "SAVE MY PARKING SPOT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = DarkNavyBackground
                    )
                    Text(
                        text = "Captures real GPS latitude, longitude & time",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF0F382A)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 4. SAVED PARKING LOCATION SECTION
// ---------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedLocationSection(
    spot: ParkingSpot,
    onShareClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavySurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalParking,
                            contentDescription = "Parking Location",
                            tint = AccentGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "SAVED PARKING LOCATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = AccentGreen
                        )
                        Text(
                            text = spot.savedTimestamp ?: "No spot saved yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                if (spot.isSaved) {
                    IconButton(
                        onClick = onShareClicked,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkNavySurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share spot",
                            tint = TextLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Real Data or Empty State
            if (spot.isSaved) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = spot.displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextWhite
                    )

                    // Real GPS Coordinates Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkNavySurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight)
                        ) {
                            Text(
                                text = "Lat: ${String.format(Locale.US, "%.5f", spot.latitude)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AccentGreenLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkNavySurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight)
                        ) {
                            Text(
                                text = "Lng: ${String.format(Locale.US, "%.5f", spot.longitude)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AccentGreenLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    spot.accuracyMeters?.let { acc ->
                        Text(
                            text = "GPS Precision: ±${acc.roundToInt()} meters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                if (spot.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        spot.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MetricCardBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = TextLight,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkNavySurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No Parking Location Saved",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Text(
                            text = "Tap 'Save My Parking Spot' above to capture your phone's real GPS position and address.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 5. DISTANCE TO VEHICLE SECTION (REAL-TIME VIA Location.distanceTo)
// ---------------------------------------------------------
@Composable
fun DistanceToVehicleSection(
    spot: ParkingSpot,
    distanceMeters: Float?,
    currentLocation: Location?,
    isLoading: Boolean,
    onRefreshDistance: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavySurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = "Distance",
                            tint = AccentGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "DISTANCE TO VEHICLE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = AccentGreen
                        )
                        Text(
                            text = when {
                                !spot.isSaved -> "Waiting for saved spot"
                                isLoading -> "Acquiring live GPS fix..."
                                else -> "Real-time distance tracking"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                if (spot.isSaved) {
                    IconButton(
                        onClick = onRefreshDistance,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkNavySurfaceElevated)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh distance",
                                tint = AccentGreenLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Content States
            when {
                // 1. No spot saved yet
                !spot.isSaved -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkNavySurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "No Parking Spot Saved",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Text(
                                    text = "Save your parking spot above to track your real-time distance and navigate back to your car.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // 2. Spot saved, waiting for GPS location fix (Loading Indicator)
                isLoading && distanceMeters == null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkNavySurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AccentGreen,
                                strokeWidth = 2.5.dp
                            )
                            Column {
                                Text(
                                    text = "Acquiring live GPS location...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Text(
                                    text = "Measuring exact distance to your parked car",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // 3. Real GPS Distance Available
                distanceMeters != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LocationHelper.formatDistance(distanceMeters),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🚶 ~${LocationHelper.estimateWalkMinutes(distanceMeters)} min walk",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = AccentGreenLight
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MetricCardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Explore,
                                    contentDescription = "Compass",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Live Sync",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextWhite
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavySurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Text(
                                text = "Real-time GPS • Distance updates automatically as you walk (${
                                    currentLocation?.accuracy?.let { "±${it.roundToInt()}m" } ?: "Precise"
                                })",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextLight
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 6. LARGE "NAVIGATE TO MY CAR" BUTTON
// ---------------------------------------------------------
@Composable
fun NavigateToCarButton(
    spot: ParkingSpot,
    distanceMeters: Float?,
    onNavigateClicked: () -> Unit
) {
    val isEnabled = spot.isSaved

    Button(
        onClick = onNavigateClicked,
        enabled = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) TextWhite else DarkNavySurfaceElevated,
            contentColor = if (isEnabled) DarkNavyBackground else TextLight
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isEnabled) 6.dp else 2.dp,
            pressedElevation = 2.dp
        ),
        border = if (!isEnabled) androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) AccentGreen else DarkNavyBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = if (isEnabled) DarkNavyBackground else TextDim,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "NAVIGATE TO MY CAR",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isEnabled) DarkNavyBackground else TextWhite
                )
                Text(
                    text = if (isEnabled) {
                        if (distanceMeters != null) {
                            "Walking directions in Google Maps (${LocationHelper.formatDistance(distanceMeters)})"
                        } else {
                            "Open walking directions in Google Maps"
                        }
                    } else {
                        "Tap to open directions (Save parking location first)"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isEnabled) Color(0xFF475569) else TextMuted
                )
            }
        }
    }
}

// ---------------------------------------------------------
// 7. PARKING NOTES & PHOTO SECTION
// ---------------------------------------------------------
@Composable
fun ParkingNotesAndPhotoSection(
    spot: ParkingSpot,
    onEditClicked: () -> Unit,
    onAddPhotoClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavySurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Notes",
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PARKING NOTES & PHOTO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = AccentGreen
                        )
                        Text(
                            text = if (spot.isSaved) "Floor, pillar, slot & visual reference" else "Waiting for saved spot",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEditClicked,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreenLight)
                ) {
                    Text(
                        text = if (spot.hasDetails) "Edit Details" else "Add Details",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Content
            if (!spot.isSaved) {
                // Friendly message if no parking spot is saved yet (Requirement 8)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkNavySurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "No Parking Spot Saved",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "Save your parking spot above to record your floor, pillar, slot number, and parking photo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                // Structured Fields: Floor, Pillar, Slot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Floor Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavySurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "FLOOR",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextDim
                            )
                            Text(
                                text = if (spot.floorOrLevel.isNotBlank()) spot.floorOrLevel else "Not set",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (spot.floorOrLevel.isNotBlank()) AccentGreenLight else TextMuted
                            )
                        }
                    }

                    // Pillar Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavySurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "PILLAR",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextDim
                            )
                            Text(
                                text = if (spot.pillarNumber.isNotBlank()) spot.pillarNumber else "Not set",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (spot.pillarNumber.isNotBlank()) AccentGreenLight else TextMuted
                            )
                        }
                    }

                    // Slot Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavySurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "SLOT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextDim
                            )
                            Text(
                                text = if (spot.slotNumber.isNotBlank()) spot.slotNumber else "Not set",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (spot.slotNumber.isNotBlank()) AccentGreenLight else TextMuted
                            )
                        }
                    }
                }

                // Additional Notes Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkNavySurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ADDITIONAL NOTES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextDim
                        )
                        Text(
                            text = if (spot.notes.isNotBlank()) spot.notes else "No additional notes. Tap 'Edit Details' to add landmarks or reminders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (spot.notes.isNotBlank()) TextWhite else TextMuted,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Parking Photo Display
                if (!spot.photoPath.isNullOrBlank()) {
                    val bitmap = remember(spot.photoPath) {
                        spot.photoPath?.let { PhotoHelper.loadRotatedBitmap(it, 800) }
                    }

                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, DarkNavyBorderLight, RoundedCornerShape(14.dp))
                                .background(MetricCardBackground)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Parking Spot Photo Reference",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlay label
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkNavyBackground.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight),
                                modifier = Modifier
                                    .padding(10.dp)
                                    .align(Alignment.BottomStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Photo Reference",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextWhite
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Prompt button to add photo
                    OutlinedButton(
                        onClick = onAddPhotoClicked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreenBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AccentGreenContainer.copy(alpha = 0.35f),
                            contentColor = AccentGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Parking Photo (Camera / Gallery)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// BONUS: PARKING METER / TIMER CARD
// ---------------------------------------------------------
@Composable
fun ParkingTimerCard(spot: ParkingSpot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavySurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AccentGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Timer",
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "PARKING METER / SESSION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = AccentGreen
                    )
                    Text(
                        text = if (spot.isSaved) "Session active since ${spot.savedTimestamp ?: "now"}" else "No active parking session",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextWhite
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MetricCardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorderLight)
            ) {
                Text(
                    text = if (spot.isSaved) "Active" else "Idle",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (spot.isSaved) AccentGreenLight else TextDim,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09101E)
@Composable
fun HomeScreenPreview() {
    ParkMateTheme {
        HomeScreen()
    }
}
