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
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.geometry.GeoPointWithElevation
import ru.dgis.sdk.map.CameraPosition
import ru.dgis.sdk.map.Map
import ru.dgis.sdk.map.MapObjectManager
import ru.dgis.sdk.map.Marker
import ru.dgis.sdk.map.MarkerOptions
import ru.dgis.sdk.map.Zoom
import ru.dgis.sdk.map.RoadEventSource
import ru.dgis.sdk.map.RouteEditorSource
import ru.dgis.sdk.map.TrafficSource
import ru.dgis.sdk.map.imageFromResource
import ru.dgis.sdk.routing.RouteEditor
import ru.dgis.sdk.routing.RouteEditorRouteParams
import ru.dgis.sdk.routing.RouteSearchPoint
import ru.dgis.sdk.routing.RouteSearchOptions
import ru.dgis.sdk.routing.CarRouteSearchOptions
import ru.gishackathon.app01.presentation.ProfileAppFragment
import ru.gishackathon.app01.R
import ru.gishackathon.app01.databinding.FragmentSearchBinding

class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val mapHost: MapHostFragment?
        get() = requireActivity().supportFragmentManager.findFragmentById(R.id.mapHost) as? MapHostFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        mapHost?.setMarkers(listOf(
            55.760186 to 37.618711,
            55.752425 to 37.613983,
            55.747795 to 37.620528
        ))
        mapHost?.toggleTraffic(true)
    }

    override fun onResume() {
        super.onResume()
        mapHost?.centerOnMyLocationOnce()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
