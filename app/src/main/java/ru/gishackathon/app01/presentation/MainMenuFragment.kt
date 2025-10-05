package ru.gishackathon.app01.presentation

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import ru.gishackathon.app01.R
import ru.gishackathon.app01.databinding.FragmentMainMenuBinding

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabsNavController: NavController

    private val mapHost: MapHostFragment?
        get() = requireActivity().supportFragmentManager.findFragmentById(R.id.mapHost) as? MapHostFragment

    private fun setMapVisibleFor(itemId: Int) {
        val mapHostView = requireActivity().findViewById<View>(ru.gishackathon.app01.R.id.mapHost)
        mapHostView?.isVisible = (itemId == R.id.tab_search || itemId == R.id.tab_transit)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainMenuBinding.bind(view)

        val host = childFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        tabsNavController = host.navController

        tabsNavController.addOnDestinationChangedListener { _, dest: NavDestination, _ ->
            val itemId = when (dest.id) {
                R.id.searchFragment  -> R.id.tab_search
                R.id.transitFragment -> R.id.tab_transit
                R.id.navigatorFragment -> R.id.tab_navigator
                R.id.friendsFragment   -> R.id.tab_friends
                R.id.profileFragment   -> R.id.tab_profile
                else -> binding.bottomBar.selectedItemId
            }
            setMapVisibleFor(itemId)
        }

        binding.bottomBar.setOnItemSelectedListener { item ->
            setMapVisibleFor(item.itemId)
            when (item.itemId) {
                R.id.tab_search -> {
                    tabsNavController.navigate(R.id.searchFragment); true
                }

                R.id.tab_transit -> {
                    tabsNavController.navigate(R.id.transitFragment); true
                }

                R.id.tab_navigator -> {
                    tabsNavController.navigate(R.id.navigatorFragment); true
                }

                R.id.tab_friends -> {
                    tabsNavController.navigate(R.id.friendsFragment); true
                }

                R.id.tab_profile -> {
                    tabsNavController.navigate(R.id.profileFragment); true
                }

                else -> false
            }
        }

        setMapVisibleFor(binding.bottomBar.selectedItemId)

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
