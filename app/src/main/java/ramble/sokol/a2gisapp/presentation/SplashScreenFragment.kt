package ramble.sokol.a2gisapp.presentation

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import ramble.sokol.a2gisapp.R
import ramble.sokol.a2gisapp.databinding.FragmentSplashScreenBinding

class SplashScreenFragment : Fragment() {

    private var binding: FragmentSplashScreenBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSplashScreenBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fadeInAnimation = AnimationUtils.loadAnimation(requireActivity(), R.anim.splash_screen_animation)
        binding!!.imageIconSplashScreen.startAnimation(fadeInAnimation)
        binding!!.imageTextSplashScreen.startAnimation(fadeInAnimation)
        Handler().postDelayed(Runnable {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            val mainMenuFragment = MainMenuFragment()
            transaction.replace(R.id.layout_fragment, mainMenuFragment)
            transaction.disallowAddToBackStack()
            transaction.commit()
        }, 3000)
    }

}