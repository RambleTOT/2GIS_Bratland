package ru.gishackathon.app01.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import ru.dgis.sdk.DGis
import ru.dgis.sdk.ScreenPoint
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.geometry.GeoPointWithElevation
import ru.dgis.sdk.map.CameraPosition
import ru.dgis.sdk.map.MapObjectManager
import ru.dgis.sdk.map.MapView
import ru.dgis.sdk.map.Marker
import ru.dgis.sdk.map.MarkerOptions
import ru.dgis.sdk.map.RouteEditorSource
import ru.dgis.sdk.map.Zoom
import ru.dgis.sdk.map.imageFromResource
import ru.dgis.sdk.routing.CarRouteSearchOptions
import ru.dgis.sdk.routing.PedestrianRouteSearchOptions
import ru.dgis.sdk.routing.RouteEditor
import ru.dgis.sdk.routing.RouteEditorRouteParams
import ru.dgis.sdk.routing.RouteSearchOptions
import ru.dgis.sdk.routing.RouteSearchPoint
import ru.gishackathon.app01.R
import kotlin.math.hypot
import ru.dgis.sdk.map.Map as DgisMap

class MapHostFragment : Fragment() {

    private lateinit var mapView: MapView
    private var map: DgisMap? = null

    private var objectManager: MapObjectManager? = null
    private var myLocationManager: MapObjectManager? = null
    private var myLocationMarker: Marker? = null
    private var startMarker: Marker? = null
    private var destinationMarker: Marker? = null

    enum class TravelMode { TRANSIT, WALK }

    private var lastMyPoint: GeoPoint? = null
    fun getMyLocation(): GeoPoint? = lastMyPoint
    fun exposeMap(): DgisMap? = map

    private val fused by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }
    private var locationCallback: com.google.android.gms.location.LocationCallback? = null

    private var routeEditor: RouteEditor? = null
    private var routeEditorSource: RouteEditorSource? = null
    private var centeredOnUser = false

    private var pickingEnabled = false

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startMyLocation() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mapView = MapView(requireContext())
        viewLifecycleOwnerLiveData.observe(requireActivity()) { owner -> owner?.lifecycle?.addObserver(mapView) }
        return mapView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.getMapAsync { m ->
            map = m
            m.camera.position = CameraPosition(
                point = GeoPoint(55.751244, 37.618423),
                zoom = Zoom(14.0f)
            )
            objectManager = MapObjectManager(m)
            myLocationManager = MapObjectManager(m)

            routeEditor = RouteEditor(DGis.context())
            routeEditorSource = RouteEditorSource(DGis.context(), routeEditor!!)
            m.addSource(routeEditorSource!!)

            startMyLocation()
        }
    }

    fun setStartMarker(point: GeoPoint) {
        val mgr = objectManager ?: return
        val icon = imageFromResource(DGis.context(), R.drawable.icon_my_location)
        val marker = Marker(MarkerOptions(position = GeoPointWithElevation(point), icon = icon))
        startMarker?.let { mgr.removeObject(it) }
        startMarker = marker
        mgr.addObject(marker)
    }

    fun setDestinationMarker(point: GeoPoint) {
        val mgr = objectManager ?: return
        val icon = imageFromResource(DGis.context(), R.drawable.ic_target_pin_20)
        val marker = Marker(MarkerOptions(position = GeoPointWithElevation(point), icon = icon))
        destinationMarker?.let { mgr.removeObject(it) }
        destinationMarker = marker
        mgr.addObject(marker)
    }

    private fun placeOrMoveMyLocation(point: GeoPoint) {
        val mgr = myLocationManager ?: return
        val icon = imageFromResource(DGis.context(), R.drawable.icon_my_location)
        val newMarker = Marker(MarkerOptions(position = GeoPointWithElevation(point), icon = icon))
        myLocationMarker?.let { mgr.removeObject(it) }
        myLocationMarker = newMarker
        lastMyPoint = point
        mgr.addObject(newMarker)
    }

    private fun startMyLocation() {
        val m = map ?: return
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            requestLocation.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val p = GeoPoint(loc.latitude, loc.longitude)
                placeOrMoveMyLocation(p)
                if (!centeredOnUser) {
                    m.camera.position = CameraPosition(point = p, zoom = Zoom(16.0f))
                    centeredOnUser = true
                }
            }
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val ctx = requireContext()
        val fineGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return
        if (locationCallback != null) return

        val req = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(2000L)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val loc = result.lastLocation ?: return
                val p = GeoPoint(loc.latitude, loc.longitude)
                placeOrMoveMyLocation(p)
                if (!centeredOnUser && map != null) {
                    map!!.camera.position = CameraPosition(point = p, zoom = Zoom(16.0f))
                    centeredOnUser = true
                }
            }
        }
        fused.requestLocationUpdates(req, locationCallback!!, Looper.getMainLooper())
    }

    override fun onStart() { super.onStart(); startLocationUpdates() }
    override fun onStop() {
        super.onStop()
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }

    fun centerOnMyLocationOnce() { if (!centeredOnUser) startMyLocation() }

    fun buildRoute(start: GeoPoint, finish: GeoPoint, mode: TravelMode = TravelMode.WALK) {
        val m = map ?: return
        val opts = when (mode) {
            TravelMode.WALK    -> RouteSearchOptions(pedestrian = PedestrianRouteSearchOptions())
            TravelMode.TRANSIT -> RouteSearchOptions(car = CarRouteSearchOptions())
        }
        routeEditor?.setRouteParams(
            RouteEditorRouteParams(
                startPoint = RouteSearchPoint(coordinates = start),
                finishPoint = RouteSearchPoint(coordinates = finish),
                routeSearchOptions = opts
            )
        )
        setStartMarker(start)
        setDestinationMarker(finish)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun enablePickPointWithHold(holdMs: Long = 1500L, onPicked: (GeoPoint) -> Unit) {
        var pickingEnabled = false
        if (pickingEnabled) return
        pickingEnabled = true

        val vc = ViewConfiguration.get(requireContext())
        val touchSlop = vc.scaledTouchSlop
        var downX = 0f; var downY = 0f; var isDown = false
        val handler = Handler(Looper.getMainLooper())
        val longPressRunnable = Runnable {
            if (!isDown) return@Runnable
            val m = map ?: return@Runnable
            val p = m.camera.projection.screenToMap(ScreenPoint(downX, downY))
            p?.let { onPicked(it) }
        }

        mapView.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isDown = true
                    downX = ev.x; downY = ev.y
                    handler.postDelayed(longPressRunnable, holdMs)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.x - downX; val dy = ev.y - downY
                    if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        isDown = false; handler.removeCallbacks(longPressRunnable)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDown = false; handler.removeCallbacks(longPressRunnable)
                }
            }
            false
        }
    }
}
