package ramble.sokol.app01.domain

interface TwoGisMapRepository {
    fun defaultMarkers(): List<Pair<Double, Double>>
}

class TwoGisMapRepositoryImpl : TwoGisMapRepository {
    override fun defaultMarkers(): List<Pair<Double, Double>> = listOf(
        55.752425 to 37.613983,
        55.760186 to 37.618711,
        55.747795 to 37.620528
    )
}
