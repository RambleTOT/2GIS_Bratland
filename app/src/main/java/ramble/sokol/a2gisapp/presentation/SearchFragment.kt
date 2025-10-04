import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ramble.sokol.a2gisapp.R
import ramble.sokol.a2gisapp.databinding.FragmentSearchBinding
import ramble.sokol.a2gisapp.domain.MapViewModel
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.geometry.GeoPointWithElevation
import ru.dgis.sdk.map.CameraPosition
import ru.dgis.sdk.map.MapObjectManager
import ru.dgis.sdk.map.Marker
import ru.dgis.sdk.map.MarkerOptions
import ru.dgis.sdk.map.RoadEventSource
import ru.dgis.sdk.map.RouteEditorSource
import ru.dgis.sdk.map.TrafficSource
import ru.dgis.sdk.map.Zoom
import ru.dgis.sdk.routing.RouteEditor
import ru.dgis.sdk.routing.RouteSearchPoint

class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val vm: MapViewModel by viewModels()

    private var mapObjectManager: MapObjectManager? = null
    private var trafficSource: TrafficSource? = null
    private var roadEventSource: RoadEventSource? = null

    private var routeEditor: RouteEditor? = null
    private var routeEditorSource: RouteEditorSource? = null

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        // Подписываем MapView на жизненный цикл фрагмента
        viewLifecycleOwner.lifecycle.addObserver(binding.mapView)

        // Переключатель видимости панели слоёв
        binding.fabLayers.setOnClickListener {
            binding.layersSheet.visibility =
                if (binding.layersSheet.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Подключаем карту
        binding.mapView.getMapAsync { map ->
            // начальная позиция камеры (можно получить из VM)
            map.camera.position = CameraPosition(
                point = GeoPoint(vm.state.value.centerLat, vm.state.value.centerLon),
                zoom = Zoom(vm.state.value.zoom.toFloat())
            )

            // менеджер динамических объектов
            mapObjectManager = MapObjectManager(map)

            // отрисуем маркеры из VM
            drawMarkers(vm.state.value.markers)

            // логику UI состояния делаем реактивной
            lifecycleScope.launchWhenStarted {
                vm.state.collect { s ->
                    applyLayers(map, s.showTraffic, s.showRoadEvents)
                }
            }

            // свитчи меняют стейт во VM
            binding.switchTraffic.setOnCheckedChangeListener { _, _ -> vm.onToggleTraffic() }
            binding.switchRoadEvents.setOnCheckedChangeListener { _, _ -> vm.onToggleRoadEvents() }

            // построение маршрута A→B
            binding.fabRoute.setOnClickListener { buildRoute(map) }
        }

        ensureLocationPermissions()
    }

    private fun drawMarkers(list: List<Pair<Double, Double>>) {
        val mgr = mapObjectManager ?: return
        val markers = list.map { (lat, lon) ->
            Marker(MarkerOptions(position = GeoPointWithElevation(lat, lon)))
        }
        mgr.clear()
        mgr.addObjects(markers) // пачкой — быстрее
    }

    private fun applyLayers(map: Map, traffic: Boolean, events: Boolean) {
        if (traffic) {
            if (trafficSource == null) trafficSource = TrafficSource(requireContext())
            if (!map.getSources().contains(trafficSource)) map.addSource(trafficSource!!)
        } else {
            trafficSource?.let { map.removeSource(it) }
        }

        if (events) {
            if (roadEventSource == null) roadEventSource = RoadEventSource(requireContext())
            if (!map.getSources().contains(roadEventSource)) map.addSource(roadEventSource!!)
        } else {
            roadEventSource?.let { map.removeSource(it) }
        }
    }

    private fun buildRoute(map: Map) {
        if (routeEditor == null) {
            routeEditor = RouteEditor(requireContext())
            routeEditorSource = RouteEditorSource(requireContext(), routeEditor!!)
            map.addSource(routeEditorSource!!)
        }
        // Пример: произвольные точки A→B
        routeEditor?.setRouteParams(
            RouteParams(
                startPoint = RouteSearchPoint(GeoPoint(55.759909, 37.618806)),
                finishPoint = RouteSearchPoint(GeoPoint(55.752425, 37.613983))
            )
        )
    }

    private fun ensureLocationPermissions() {
        val ctx = requireContext()
        val needFine = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        val needCoarse = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        if (needFine || needCoarse) {
            requestLocation.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
