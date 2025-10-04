package ru.gishackathon.app01.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import ru.gishackathon.app01.R
import ru.gishackathon.app01.databinding.FragmentMainMenuBinding

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabsNavController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainMenuBinding.bind(view)

        val host = childFragmentManager.findFragmentById(R.id.tabsNavHost) as NavHostFragment
        tabsNavController = host.navController

        binding.bottomBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_search      -> { tabsNavController.navigate(R.id.searchFragment); true }
                R.id.tab_transit   -> { tabsNavController.navigate(R.id.transitFragment); true }
                R.id.tab_navigator-> { tabsNavController.navigate(R.id.navigatorFragment); true }
                R.id.tab_friends   -> { tabsNavController.navigate(R.id.friendsFragment); true }
                R.id.tab_profile -> { tabsNavController.navigate(R.id.profileFragment); true }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
