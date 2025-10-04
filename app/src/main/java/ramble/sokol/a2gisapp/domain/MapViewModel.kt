package ramble.sokol.a2gisapp.domain

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel : ViewModel() {

    private val repo = TwoGisMapRepositoryImpl()

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        val markers = LoadMarkersUseCase(repo).invoke()
        _state.update { it.copy(markers = markers) }
    }

    fun onToggleTraffic() {
        _state.update { it.copy(showTraffic = ToggleTrafficUseCase().invoke(it.showTraffic)) }
    }

    fun onToggleRoadEvents() {
        _state.update { it.copy(showRoadEvents = ToggleRoadEventsUseCase().invoke(it.showRoadEvents)) }
    }
}
