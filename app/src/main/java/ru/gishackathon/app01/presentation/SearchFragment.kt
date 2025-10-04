package ru.gishackathon.app01.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ramble.sokol.app01.domain.MapViewModel
import ru.dgis.sdk.DGis
import ru.dgis.sdk.DGis.context
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
import ru.dgis.sdk.map.Map
import ru.dgis.sdk.map.imageFromResource
import ru.dgis.sdk.routing.RouteEditorRouteParams
import ru.gishackathon.app01.R
import ru.gishackathon.app01.databinding.FragmentSearchBinding


class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val vm: MapViewModel by viewModels()

    private var mapObjectManager: MapObjectManager? = null
    private var trafficSource: TrafficSource? = null
    private var roadEventSource: RoadEventSource? = null

    private var routeEditor: RouteEditor? = null
    private var routeEditorSource: RouteEditorSource? = null

    private var trafficAdded = false
    private var roadEventsAdded = false

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {  }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        viewLifecycleOwner.lifecycle.addObserver(binding.mapView)

        binding.fabLayers.setOnClickListener {
            binding.layersSheet.visibility =
                if (binding.layersSheet.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        binding.mapView.getMapAsync { map ->
            map.camera.position = CameraPosition(
                point = GeoPoint(vm.state.value.centerLat, vm.state.value.centerLon),
                zoom = Zoom(vm.state.value.zoom.toFloat())
            )

            mapObjectManager = MapObjectManager(map)

            drawMarkers(vm.state.value.markers)

            lifecycleScope.launchWhenStarted {
                vm.state.collect { s ->
                    applyLayers(map, s.showTraffic, s.showRoadEvents)
                }
            }
            binding.switchTraffic.setOnCheckedChangeListener { _, _ -> vm.onToggleTraffic() }
            binding.switchRoadEvents.setOnCheckedChangeListener { _, _ -> vm.onToggleRoadEvents() }

            binding.fabRoute.setOnClickListener { buildRoute(map) }
        }

        ensureLocationPermissions()
    }

    private fun drawMarkers(list: List<Pair<Double, Double>>) {
        val mgr = mapObjectManager ?: return
        val icon = imageFromResource(context(), android.R.drawable.ic_menu_mylocation)
        val markers = list.map { (lat, lon) ->
            Marker(
                MarkerOptions(
                    position = GeoPointWithElevation(GeoPoint(lat, lon)),
                    icon = icon
                )
            )
        }
        mgr.removeAll()
        mgr.addObjects(markers)
    }

    private fun applyLayers(map: Map, traffic: Boolean, events: Boolean) {
        if (traffic) {
            if (trafficSource == null) trafficSource = TrafficSource(DGis.context())
            if (!trafficAdded) {
                map.addSource(trafficSource!!)
                trafficAdded = true
            }
        } else if (trafficAdded) {
            trafficSource?.let { map.removeSource(it) }
            trafficAdded = false
        }

        if (events) {
            if (roadEventSource == null) roadEventSource = RoadEventSource(context())
            if (!roadEventsAdded) {
                map.addSource(roadEventSource!!)
                roadEventsAdded = true
            }
        } else if (roadEventsAdded) {
            roadEventSource?.let { map.removeSource(it) }
            roadEventsAdded = false
        }
    }


    private fun buildRoute(map: Map) {
        if (routeEditor == null) {
            routeEditor = RouteEditor(context())
            routeEditorSource = RouteEditorSource(context(), routeEditor!!)
            map.addSource(routeEditorSource!!)
        }

        val start = RouteSearchPoint(coordinates = GeoPoint(55.759909, 37.618806))
        val finish = RouteSearchPoint(coordinates = GeoPoint(55.752425, 37.613983))

        routeEditor?.setRouteParams(
            RouteEditorRouteParams(
                startPoint = start,
                finishPoint = finish,
                routeSearchOptions = TODO()
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
