package ru.gishackathon.app01.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.dgis.sdk.coordinates.GeoPoint
import ru.gishackathon.app01.R

class TransitFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        return inflater.inflate(R.layout.fragment_transit, container, false)
    }

    private val mapHost: MapHostFragment?
        get() = requireActivity().supportFragmentManager.findFragmentById(R.id.mapHost) as? MapHostFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapHost?.buildRoute(
            start = ru.dgis.sdk.coordinates.GeoPoint(55.759909, 37.618806),
            finish = ru.dgis.sdk.coordinates.GeoPoint(55.752425, 37.613983)
        )
    }


}