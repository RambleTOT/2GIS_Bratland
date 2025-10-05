package ru.gishackathon.app01.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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

        binding.featureSloi.setOnClickListener {
            // здесь ебать важно будет
        }

        binding.iconEnd.setOnClickListener {
            Toast.makeText(requireActivity(), "Это уже есть в приложении 2ГИС и в MVP нашего решения не входит", Toast.LENGTH_SHORT).show()
        }

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
