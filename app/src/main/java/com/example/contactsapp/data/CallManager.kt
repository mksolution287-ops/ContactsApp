package com.example.contactsapp.data

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

@SuppressLint("StaticFieldLeak")
object CallManager {
    var currentCall: Call? = null
        private set

    @SuppressLint("StaticFieldLeak")
    var inCallService: InCallService? = null

    private val listeners = mutableListOf<() -> Unit>()

    private var _isMuted = false
    private var _isSpeaker = false

    fun setCall(call: Call?) {
        currentCall = call
        notifyListeners()
    }

    fun notifyListeners() = listeners.forEach { it() }

    fun addListener(listener: () -> Unit) = listeners.add(listener)
    fun removeListener(listener: () -> Unit) = listeners.remove(listener)

    fun answer() { currentCall?.answer(0) }
    fun decline() { currentCall?.reject(false, null) }
    fun hangUp() { currentCall?.disconnect() }

//    fun toggleMute() {
//        val service = inCallService ?: return
//        val isMuted = service.callAudioState?.isMuted ?: false
//        service.setMuted(!isMuted)
//    }
fun toggleMute() {
    val service = inCallService ?: return
    _isMuted = !_isMuted          // ← flip local state first
    service.setMuted(_isMuted)    // ← then apply
    notifyListeners()
}

//    fun toggleSpeaker() {
//        val service = inCallService ?: return
//        val current = service.callAudioState?.route ?: return
//        val newRoute = if (current == CallAudioState.ROUTE_SPEAKER)
//            CallAudioState.ROUTE_EARPIECE
//        else
//            CallAudioState.ROUTE_SPEAKER
//        service.setAudioRoute(newRoute)
//    }
fun toggleSpeaker() {
    val service = inCallService ?: return
    _isSpeaker = !_isSpeaker      // ← flip local state first
    val route = if (_isSpeaker)
        CallAudioState.ROUTE_SPEAKER
    else
        CallAudioState.ROUTE_EARPIECE
    service.setAudioRoute(route)  // ← then apply
    notifyListeners()
}

    // ← Called by CallService when audio state changes externally (headset plugged in, etc.)
    fun onAudioStateChanged(state: CallAudioState) {
        _isMuted   = state.isMuted
        _isSpeaker = state.route == CallAudioState.ROUTE_SPEAKER
        notifyListeners()
    }

//    fun isMuted(): Boolean =
//        inCallService?.callAudioState?.isMuted ?: false
//
//    fun isSpeakerOn(): Boolean =
//        inCallService?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER

    fun isMuted(): Boolean = _isMuted
    fun isSpeakerOn(): Boolean = _isSpeaker

    // Reset audio state when call ends
    fun resetAudioState() {
        _isMuted = false
        _isSpeaker = false
    }

    val callState: Int
        get() = currentCall?.state ?: Call.STATE_DISCONNECTED

    val isRinging: Boolean
        get() = callState == Call.STATE_RINGING

    val isActive: Boolean
        get() = callState == Call.STATE_ACTIVE

    val isOnHold: Boolean
        get() = callState == Call.STATE_HOLDING
}