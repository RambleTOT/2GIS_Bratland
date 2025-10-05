package ru.gishackathon.app01.presentation

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.dgis.sdk.map.imageFromBitmap
import ru.dgis.sdk.map.imageFromResource
import ru.gishackathon.app01.R
import ru.gishackathon.app01.databinding.FragmentSearchBinding

class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private var layersBehavior: BottomSheetBehavior<out View>? = null
    private lateinit var accessibilityBehavior: BottomSheetBehavior<LinearLayout>

    private var noiseOn = false
    private var eventsOn = false
    private var anyAccessibilityOn = false

    private val colorOff by lazy { android.content.res.ColorStateList.valueOf(0xFF818181.toInt()) }
    private val colorOn  by lazy { android.content.res.ColorStateList.valueOf(0xFF719EC5.toInt()) }

    private val mapHost: MapHostFragment?
        get() = requireActivity().supportFragmentManager.findFragmentById(R.id.mapHost) as? MapHostFragment


    private var eventsRenderer: EventsRenderer? = null
    private var noiseRenderer: NoisePointsRenderer? = null

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

        val modal = binding.accessibilityModal
        accessibilityBehavior = BottomSheetBehavior.from(modal).apply {
            isFitToContents = true
            skipCollapsed   = true
            isHideable      = true
            state           = BottomSheetBehavior.STATE_HIDDEN
        }
        accessibilityBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) binding.dimView.visibility = View.GONE
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (binding.dimView.visibility != View.VISIBLE) binding.dimView.visibility = View.VISIBLE
                binding.dimView.alpha = 0.6f * slideOffset.coerceIn(0f, 1f)
            }
        })

        binding.iconOutFeature.setOnClickListener { openAccessibilityModal() }

        binding.rowNoise.setOnClickListener {
            noiseOn = !noiseOn
            binding.icNoise.imageTintList = if (noiseOn) colorOn else colorOff
            toggleNoiseLayer(noiseOn)
        }

        binding.rowEvents.setOnClickListener {
            eventsOn = !eventsOn
            binding.icEvents.imageTintList = if (eventsOn) colorOn else colorOff
            toggleEventsLayer(eventsOn)
        }

        binding.btnCloseAccessibility.setOnClickListener { hideAccessibility() }
        binding.dimView.setOnClickListener { hideAccessibility() }

        binding.featureSloi.setOnClickListener {
            binding.dimView.visibility = View.VISIBLE
            binding.dimView.alpha = 0f
            binding.layersModal.visibility = View.VISIBLE
            layersBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.btnCloseLayers.setOnClickListener { hideLayersModal() }
        binding.dimView.setOnClickListener { hideLayersModal() }

        binding.iconEnd.setOnClickListener {
            Toast.makeText(requireActivity(), "Это уже есть в 2ГИС и в MVP не входит", Toast.LENGTH_SHORT).show()
        }

        binding.swAvoidNoise.setOnCheckedChangeListener { _, _ -> updateOutFeatureIcon() }
        binding.swAvoidEvents.setOnCheckedChangeListener { _, _ -> updateOutFeatureIcon() }
        binding.swAvoidCrowded.setOnCheckedChangeListener { _, _ -> updateOutFeatureIcon() }
    }

    override fun onResume() {
        super.onResume()
        mapHost?.centerOnMyLocationOnce()
    }

    private fun toggleNoiseLayer(enable: Boolean) {
        val dgMap = mapHost?.exposeMap()
        if (dgMap == null) {
            Toast.makeText(context, "Карта ещё не готова", Toast.LENGTH_SHORT).show()
            return
        }
        if (noiseRenderer == null) {
            noiseRenderer = NoisePointsRenderer(map = dgMap) // без иконки
        }
        viewLifecycleOwner.lifecycleScope.launch {
            noiseRenderer?.setEnabled(enable)
        }
    }

    private fun toggleEventsLayer(enable: Boolean) {
        val dgMap = mapHost?.exposeMap()
        if (dgMap == null) {
            Toast.makeText(requireContext(), "Карта ещё не готова", Toast.LENGTH_SHORT).show()
            return
        }
        if (eventsRenderer == null && dgMap != null) {
            eventsRenderer = EventsRenderer(
                map = dgMap,
                onPolygonTapped = ::onEventAreaTapped // или { area -> onEventAreaTapped(area) }
                // http = myOkHttp,                 // если нужен свой клиент — раскомментируйте
                // endpointUrl = MY_URL             // если нужен другой URL — раскомментируйте
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            eventsRenderer?.setEnabled(enable)
        }

    }

    private fun onEventAreaTapped(area: EventArea) {
        // тут ваша логика показа информации/навигации
        // пример-заглушка:
        //showEventAreaDialog(area) // или любой ваш метод
    }

//    private fun showNoiseInfoDialog(info: Info) {
//        val sources = if (info.noiseSources!!.isEmpty()) "—"
//        else info.noiseSources!!.joinToString("\n• ", prefix = "• ")
//        val message = buildString {
//            appendLine("Улица: ${info.address!!.ifBlank { "—" }}")
//            appendLine("Частота жалоб: ${info.complaintFrequency!!.ifBlank { "—" }}")
//            appendLine()
//            appendLine("Источники шума:")
//            append(sources)
//        }
//        MaterialAlertDialogBuilder(requireContext())
//            .setTitle("Шумовая точка")
//            .setMessage(message)
//            .setPositiveButton(android.R.string.ok, null)
//            .show()
//    }

    // --- UI helpers ---

    private fun hideLayersModal() {
        layersBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun hideAccessibility() {
        accessibilityBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        binding.dimView.animate().alpha(0f).setDuration(120).withEndAction {
            binding.dimView.isVisible = false
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

    private fun openAccessibilityModal() {

        binding.accessibilityModal.visibility = View.VISIBLE

        binding.dimView.apply {
            isVisible = true
            alpha = 0f
            animate().alpha(0.6f).setDuration(120).start()
            setOnClickListener { closeAccessibilityModal() }
        }


        accessibilityBehavior.apply {
            isHideable = true
            skipCollapsed = true
            isDraggable = true

            isFitToContents = false
            expandedOffset = 0
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }


    private fun closeAccessibilityModal() {
        binding.dimView.isVisible = false
        BottomSheetBehavior.from(binding.accessibilityModal).state =
            BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
