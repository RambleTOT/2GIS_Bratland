package ru.gishackathon.app01.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.dgis.sdk.DGis
import ru.dgis.sdk.coordinates.GeoPoint



object DgisSearch {

    suspend fun suggest(text: String): List<Pair<String,String?>> =
        withContext(Dispatchers.IO) {

            if (text.isBlank()) emptyList() else listOf(text to null)
        }

    suspend fun geocodeFirst(text: String): GeoPoint? =
        withContext(Dispatchers.IO) {
            DgisGeocoder("c4ef43c4-dc29-4f58-beb1-482f41e0a34e").geocodeFirst(text) as GeoPoint?
        }
}
