package ramble.sokol.a2gisapp.domain

data class MapUiState(
    val centerLat: Double = 55.751244,
    val centerLon: Double = 37.618423,
    val zoom: Double = 14.0,
    val markers: List<Pair<Double, Double>> = emptyList(),
    val showTraffic: Boolean = false,
    val showRoadEvents: Boolean = false
)
