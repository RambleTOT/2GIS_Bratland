package ru.gishackathon.app01.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

    private var layersBehavior: BottomSheetBehavior<out View>? = null
    private var noiseOn = false
    private var eventsOn = false
    private val colorOff by lazy { ColorStateList.valueOf(0xFF818181.toInt()) }
    private val colorOn  by lazy { ColorStateList.valueOf(0xFF719EC5.toInt()) }

    private lateinit var accessibilityBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<LinearLayout>
    private var anyAccessibilityOn = false

    private val mapHost: MapHostFragment?
        get() = requireActivity().supportFragmentManager.findFragmentById(R.id.mapHost) as? MapHostFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        layersBehavior = BottomSheetBehavior.from(binding.layersModal).apply {
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        binding.dimView.visibility = View.GONE
                        binding.layersModal.visibility = View.GONE
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    binding.dimView.alpha = (0.6f * (slideOffset.coerceIn(0f,1f)))
                }
            })
        }

        binding.iconOutFeature.setOnClickListener { openAccessibilityModal() }

        val modal = binding.accessibilityModal
        accessibilityBehavior = BottomSheetBehavior.from(modal).apply {
            isFitToContents = true
            skipCollapsed   = true
            isHideable      = true
            state           = BottomSheetBehavior.STATE_HIDDEN
        }

        accessibilityBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> binding.dimView.visibility = View.GONE
                    BottomSheetBehavior.STATE_DRAGGING,
                    BottomSheetBehavior.STATE_SETTLING -> {
                        if (accessibilityBehavior.skipCollapsed &&
                            accessibilityBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                            accessibilityBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (binding.dimView.visibility != View.VISIBLE) binding.dimView.visibility = View.VISIBLE
                binding.dimView.alpha = 0.6f * slideOffset.coerceIn(0f, 1f)
            }
        })


        binding.iconOutFeature.setOnClickListener {
            binding.dimView.visibility = View.VISIBLE
            binding.dimView.alpha = 0f
            binding.dimView.animate().alpha(0.6f).setDuration(150).start()
            binding.accessibilityModal.visibility = View.VISIBLE
            accessibilityBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.btnCloseAccessibility.setOnClickListener { hideAccessibility() }
        binding.dimView.setOnClickListener { hideAccessibility() }

        binding.featureSloi.setOnClickListener {
            binding.dimView.visibility = View.VISIBLE
            binding.dimView.alpha = 0f
            binding.layersModal.visibility = View.VISIBLE
            layersBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.rowNoise.setOnClickListener {
            noiseOn = !noiseOn
            binding.icNoise.imageTintList = if (noiseOn) colorOn else colorOff

        }
        binding.rowEvents.setOnClickListener {
            eventsOn = !eventsOn
            binding.icEvents.imageTintList = if (eventsOn) colorOn else colorOff

        }

        binding.btnCloseLayers.setOnClickListener { hideLayersModal() }
        binding.dimView.setOnClickListener { hideLayersModal() }

        binding.iconEnd.setOnClickListener {
            Toast.makeText(requireActivity(), "Это уже есть в приложении 2ГИС и в MVP нашего решения не входит", Toast.LENGTH_SHORT).show()
        }

        binding.swAvoidNoise.setOnCheckedChangeListener { _, _ -> updateOutFeatureIcon() }
        binding.swAvoidEvents.setOnCheckedChangeListener { _, _ -> updateOutFeatureIcon() }
        binding.swAvoidCrowded.setOnCheckedChangeListener { _, _ -> updateOutFeatureIcon() }

    }
    private fun hideLayersModal() {
        layersBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }


    override fun onResume() {
        super.onResume()
        mapHost?.centerOnMyLocationOnce()
    }

    private fun hideAccessibility() {
        accessibilityBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
        binding.dimView.animate().alpha(0f).setDuration(120).withEndAction {
            binding.dimView.visibility = View.GONE
            binding.accessibilityModal.visibility = View.GONE
        }.start()
    }

    private fun updateOutFeatureIcon() {
        anyAccessibilityOn =
            binding.swAvoidNoise.isChecked || binding.swAvoidEvents.isChecked || binding.swAvoidCrowded.isChecked

        binding.iconOutFeature.setImageResource(
            if (anyAccessibilityOn) R.drawable.icon_green_our_feature
            else R.drawable.icon_sheet_last
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun openAccessibilityModal() {
        val behavior = BottomSheetBehavior.from(binding.accessibilityModal)
        binding.dimView.isVisible = true
        binding.dimView.setOnClickListener { closeAccessibilityModal() }
        binding.btnCloseAccessibility.setOnClickListener { closeAccessibilityModal() }

        behavior.isDraggable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun closeAccessibilityModal() {
        binding.dimView.isVisible = false
        BottomSheetBehavior.from(binding.accessibilityModal).state =
            BottomSheetBehavior.STATE_HIDDEN
    }


}
