package ramble.sokol.a2gisapp.domain

class LoadMarkersUseCase(private val repo: TwoGisMapRepository) {
    operator fun invoke(): List<Pair<Double, Double>> = repo.defaultMarkers()
}
class ToggleTrafficUseCase { operator fun invoke(cur:Boolean) = !cur }
class ToggleRoadEventsUseCase { operator fun invoke(cur:Boolean) = !cur }
