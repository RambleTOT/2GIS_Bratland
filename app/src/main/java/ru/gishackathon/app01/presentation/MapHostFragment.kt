package ru.gishackathon.app01.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import ru.dgis.sdk.DGis
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.geometry.GeoPointWithElevation
import ru.dgis.sdk.map.*
import ru.dgis.sdk.routing.*
import ru.gishackathon.app01.R
import ru.dgis.sdk.map.Map as DgisMap   // алиас

class MapHostFragment : Fragment() {

    private lateinit var mapView: MapView
    private var map: DgisMap? = null

    private var objectManager: MapObjectManager? = null
    private var myLocationMarker: Marker? = null

    private var trafficSource: TrafficSource? = null
    private var roadEventSource: RoadEventSource? = null
    private var routeEditor: RouteEditor? = null
    private var routeEditorSource: RouteEditorSource? = null

    private var centeredOnUser = false

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startMyLocation() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mapView = MapView(requireContext())
        viewLifecycleOwnerLiveData.observe(this) { owner -> owner?.lifecycle?.addObserver(mapView) }
        return mapView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.getMapAsync { m ->
            map = m
            // дефолтная камера на город, затем переведём на геопозицию
            m.camera.position = CameraPosition(point = GeoPoint(55.751244, 37.618423), zoom = Zoom(
                14.0F
            ))
            objectManager = MapObjectManager(m)
            startMyLocation()
        }
    }
    fun setMarkers(points: List<Pair<Double, Double>>) {
        val mgr = objectManager ?: return
        val icon = imageFromResource(DGis.context(), android.R.drawable.ic_menu_mylocation)
        val items = points.map { (lat, lon) ->
            Marker(MarkerOptions(position = GeoPointWithElevation(GeoPoint(lat, lon)), icon = icon))
        }
        mgr.removeAll()
        myLocationMarker = null
        mgr.addObjects(items)
        // при необходимости вернём "моё местоположение"
        ensureMyLocationRetained()
    }

    fun toggleTraffic(enabled: Boolean) {
        val m = map ?: return
        if (enabled) {
            if (trafficSource == null) trafficSource = TrafficSource(DGis.context())
            m.addSource(trafficSource!!)
        } else trafficSource?.let { m.removeSource(it) }
    }

    fun toggleRoadEvents(enabled: Boolean) {
        val m = map ?: return
        if (enabled) {
            if (roadEventSource == null) roadEventSource = RoadEventSource(DGis.context())
            m.addSource(roadEventSource!!)
        } else roadEventSource?.let { m.removeSource(it) }
    }

    fun buildRoute(start: GeoPoint, finish: GeoPoint) {
        val m = map ?: return
        if (routeEditor == null) {
            routeEditor = RouteEditor(DGis.context())
            routeEditorSource = RouteEditorSource(DGis.context(), routeEditor!!)
            m.addSource(routeEditorSource!!)
        }
        val opts = RouteSearchOptions(car = CarRouteSearchOptions())
        routeEditor?.setRouteParams(
            RouteEditorRouteParams(
                startPoint = RouteSearchPoint(coordinates = start),
                finishPoint = RouteSearchPoint(coordinates = finish),
                routeSearchOptions = opts
            )
        )
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

        val fused = LocationServices.getFusedLocationProviderClient(ctx)
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
    }


    private fun placeOrMoveMyLocation(point: GeoPoint) {
        val mgr = objectManager ?: return
        val icon = imageFromResource(DGis.context(), R.drawable.icon_my_place)
        val newMarker = Marker(MarkerOptions(position = GeoPointWithElevation(point), icon = icon))
        myLocationMarker?.let { mgr.removeObject(it) }
        myLocationMarker = newMarker
        mgr.addObject(newMarker)
    }

    fun centerOnMyLocationOnce() {
        if (!centeredOnUser) {
            startMyLocation()
        }
    }

    fun clearOverlaysAndObjectsKeepMap() {
        map?.let { m ->
            trafficSource?.let { m.removeSource(it) }
            roadEventSource?.let { m.removeSource(it) }
        }
        objectManager?.removeAll()
        myLocationMarker?.let { objectManager?.addObject(it) }
    }

    private fun ensureMyLocationRetained() {

        myLocationMarker?.let {
            objectManager?.addObject(it)
        }
    }
}
