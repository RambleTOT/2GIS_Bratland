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
import ru.dgis.sdk.map.*
import ru.dgis.sdk.routing.*
import ru.gishackathon.app01.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import ru.dgis.sdk.map.Map as DgisMap   // алиас

class MapHostFragment : Fragment() {

    private lateinit var mapView: MapView
    private var map: DgisMap? = null

    private var objectManager: MapObjectManager? = null
    private var myLocationManager: MapObjectManager? = null
    private var myLocationMarker: Marker? = null

    enum class TravelMode { TRANSIT, WALK }
    private var lastMyPoint: GeoPoint? = null
    fun getMyLocation(): GeoPoint? = lastMyPoint

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(requireContext()) }
    private var locationCallback: com.google.android.gms.location.LocationCallback? = null

    private var startMarker: Marker? = null

    private var trafficSource: TrafficSource? = null
    private var roadEventSource: RoadEventSource? = null
    private var routeEditor: RouteEditor? = null
    private var routeEditorSource: RouteEditorSource? = null

    private var centeredOnUser = false

    private var destinationMarker: Marker? = null

    private var pickDetector: GestureDetector? = null
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
        mgr.addObjects(items)
        ensureMyLocationRetained()
    }




    fun setStartMarker(point: GeoPoint) {
        val mgr = objectManager ?: return
        val icon = imageFromResource(DGis.context(), R.drawable.icon_my_location) // твой пин старта
        val marker = Marker(MarkerOptions(position = GeoPointWithElevation(point), icon = icon))
        startMarker?.let { mgr.removeObject(it) }
        startMarker = marker
        mgr.addObject(marker)
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

        // быстрый старт с последнего известного
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

    fun centerOnMyLocationOnce() {
        if (!centeredOnUser) {
            startMyLocation()
        }
    }

    fun setDestinationMarker(point: GeoPoint) {
        val mgr = objectManager ?: return
        val icon = imageFromResource(DGis.context(), R.drawable.ic_target_pin_20)
        val marker = Marker(MarkerOptions(position = GeoPointWithElevation(point), icon = icon))
        destinationMarker?.let { mgr.removeObject(it) }
        destinationMarker = marker
        mgr.addObject(marker)
    }

    fun clearOverlaysAndObjectsKeepMap() {
        map?.let { m ->
            trafficSource?.let { m.removeSource(it) }
            roadEventSource?.let { m.removeSource(it) }
        }
        objectManager?.removeAll()
        myLocationMarker?.let { myLocationManager?.addObject(it) }  // <-- сюда
    }

    private fun ensureMyLocationRetained() {
        myLocationMarker?.let { myLocationManager?.addObject(it) }  // <-- и сюда
    }


    fun enablePickPoint(onPicked: (GeoPoint) -> Unit) {
        if (pickingEnabled) return
        pickingEnabled = true

        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    val m = map ?: return
                    val p = m.camera.projection.screenToMap(ScreenPoint(e.x, e.y))
                    p?.let { onPicked(it) }
                }
            }
        )
        pickDetector = detector
        mapView.setOnTouchListener { _, ev ->
            detector.onTouchEvent(ev)
            false
        }
    }

    fun disablePickPoint() {
        pickingEnabled = false
        pickDetector = null
        mapView.setOnTouchListener(null)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun enablePickPointWithHold(holdMs: Long = 1500L, onPicked: (GeoPoint) -> Unit) {
        if (pickingEnabled) return
        pickingEnabled = true

        val vc = ViewConfiguration.get(requireContext())
        val touchSlop = vc.scaledTouchSlop

        var downX = 0f
        var downY = 0f
        var isDown = false
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
                    downX = ev.x
                    downY = ev.y
                    handler.postDelayed(longPressRunnable, holdMs)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        // палец ушёл слишком далеко — отменяем «долгое»
                        isDown = false
                        handler.removeCallbacks(longPressRunnable)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDown = false
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            false // не блокируем жесты карты
        }
    }

    private fun startLocationUpdates() {
        val ctx = requireContext()
        val fineGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return

        if (locationCallback != null) return // уже запущено

        val req = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000L // интервал (мс)
        )
            .setMinUpdateIntervalMillis(2000L)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val loc = result.lastLocation ?: return
                val p = GeoPoint(loc.latitude, loc.longitude)
                placeOrMoveMyLocation(p)   // двигаем один и тот же маркер
                if (!centeredOnUser && map != null) {
                    map!!.camera.position = CameraPosition(point = p, zoom = Zoom(16.0f))
                    centeredOnUser = true
                }
            }
        }
        fused.requestLocationUpdates(req, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun placeOrMoveMyLocation(point: GeoPoint) {
        val mgr = myLocationManager ?: return
        val icon = imageFromResource(DGis.context(), R.drawable.icon_my_location) // 24x24dp
        val newMarker = Marker(MarkerOptions(position = GeoPointWithElevation(point), icon = icon))

        myLocationMarker?.let { mgr.removeObject(it) }
        myLocationMarker = newMarker
        lastMyPoint = point
        mgr.addObject(newMarker)
    }

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun focusCameraOnEndpoints(a: GeoPoint, b: GeoPoint) {
        val m = map ?: return

        // Простейший «fit»: ставим камеру в середину и подбираем зум по расстоянию.
        val midLat = (a.latitude.value + b.latitude.value) / 2.0
        val midLon = (a.longitude.value + b.longitude.value) / 2.0
        val center = GeoPoint(midLat, midLon)

        // Грубая оценка зума (для города хватает): чем дальше точки, тем меньше зум.
        val dLat = abs(a.latitude.value - b.latitude.value)
        val dLon = abs(a.longitude.value - b.longitude.value)
        val span = max(dLat, dLon)

        val zoom = when {
            span > 0.25   -> 9.5f
            span > 0.12   -> 10.5f
            span > 0.06   -> 12.0f
            span > 0.03   -> 13.0f
            span > 0.015  -> 14.0f
            span > 0.008  -> 15.0f
            else          -> 16.0f
        }

        m.camera.position = CameraPosition(point = center, zoom = Zoom(zoom))
    }

    /** Основной метод построения маршрута. */
    fun buildRoute(start: GeoPoint, finish: GeoPoint, mode: TravelMode) {
        val m = map ?: return

        // Создаём RouteEditor один раз и вешаем источник на карту.
        if (routeEditor == null) {
            routeEditor = RouteEditor(DGis.context())
            routeEditorSource = RouteEditorSource(DGis.context(), routeEditor!!)
            m.addSource(routeEditorSource!!)
        }

        // Выбор опций по режиму
        val opts = when (mode) {
            TravelMode.WALK    -> RouteSearchOptions(
                pedestrian = PedestrianRouteSearchOptions()
            )
            // В твоём UI "TRANSIT" — это «на транспорте». Если хочешь именно ОТ, замени CarRouteSearchOptions на PublicTransportRouteSearchOptions().
            TravelMode.TRANSIT -> RouteSearchOptions(
                car = CarRouteSearchOptions()
            )
        }

        // Выставляем параметры маршрута — SDK сам сформирует линию на карте через RouteEditorSource
        routeEditor?.setRouteParams(
            RouteEditorRouteParams(
                startPoint = RouteSearchPoint(coordinates = start),
                finishPoint = RouteSearchPoint(coordinates = finish),
                routeSearchOptions = opts,
            )
        )

        // Поставим метки старта/финиша (если хочется дублировать визуально)
        setStartMarker(start)
        setDestinationMarker(finish)

        // Подвинем камеру, чтобы пользователь увидел маршрут целиком
        focusCameraOnEndpoints(start, finish)
    }

    /** Перегрузка без режима — по-умолчанию авто/«транспорт». */
    fun buildRoute(start: GeoPoint, finish: GeoPoint) {
        buildRoute(start, finish, TravelMode.TRANSIT)
    }

}
