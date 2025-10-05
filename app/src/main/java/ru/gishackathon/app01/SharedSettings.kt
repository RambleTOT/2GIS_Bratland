package ru.gishackathon.app01

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SharedSettings {
    private val _avoidNoise     = MutableStateFlow(false)
    private val _avoidEvents    = MutableStateFlow(false)
    private val _avoidCrowded   = MutableStateFlow(false)

    val avoidNoise   : StateFlow<Boolean> get() = _avoidNoise
    val avoidEvents  : StateFlow<Boolean> get() = _avoidEvents
    val avoidCrowded : StateFlow<Boolean> get() = _avoidCrowded

    fun setAvoidNoise(v: Boolean)   { _avoidNoise.value = v }
    fun setAvoidEvents(v: Boolean)  { _avoidEvents.value = v }
    fun setAvoidCrowded(v: Boolean) { _avoidCrowded.value = v }
}
