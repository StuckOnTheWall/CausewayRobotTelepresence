package com.causeway.robot.telepresence
import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class VideoCallStateViewModel(application: Application) : AndroidViewModel(application) {
    enum class CALL_STATE {
        READY,
        NOT_READY,
        ONGOING
    }

    public var currentState = mutableStateOf(CALL_STATE.NOT_READY)
}