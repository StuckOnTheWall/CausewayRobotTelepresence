package com.three.robot.telepresence

import android.content.Context
import android.os.RemoteException
import android.widget.Toast
import com.ainirobot.coreservice.client.ApiListener
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.listener.ActionListener
import com.ainirobot.coreservice.client.listener.TextListener
import com.ainirobot.coreservice.client.speech.SkillApi
import com.ainirobot.coreservice.client.speech.SkillCallback
import com.ainirobot.coreservice.client.speech.entity.TTSEntity

import kotlin.system.exitProcess

class   RobotController (

) {
    val robotApi = RobotApi.getInstance();
    val skillApi = SkillApi()

    fun launchModuleApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage("com.ainirobot.moduleapp")
        if (intent != null) {
            context.startActivity(intent)
        }
        exitProcess(0)
    }

    fun stopMoving() {
        robotApi.stopNavigation(0)
    }

    fun say(text: String, context: Context) {
        println("Robot TTS text is $text")
        val skillCallback = object : SkillCallback() {
            @Throws(RemoteException::class)
            override fun onSpeechParResult(s: String) {
                // Handle speech recognition result
            }

            @Throws(RemoteException::class)
            override fun onStart() {
                // Start recognition
            }

            @Throws(RemoteException::class)
            override fun onStop() {
                // End recognition
            }

            @Throws(RemoteException::class)
            override fun onVolumeChange(volume: Int) {
                // Handle volume change
            }

            @Throws(RemoteException::class)
            override fun onQueryEnded(status: Int) {
                // Handle query ended
            }

            @Throws(RemoteException::class)
            override fun onQueryAsrResult(asrResult: String) {
                // Handle ASR result
            }
        }

        skillApi.connectApi(context, object : ApiListener {
            override fun handleApiDisabled() {
                Toast.makeText(context, "API disabled", Toast.LENGTH_SHORT).show()
            }

            override fun handleApiConnected() {
                skillApi.registerCallBack(skillCallback)
                skillApi.playText(TTSEntity(text), object : TextListener() {
                    override fun onStart() {
                        // Handle play start
                    }

                    override fun onStop() {
                        // Handle play stop
                    }

                    override fun onError() {
                        // Handle play error
                    }

                    override fun onComplete() {
                        // Handle play completion
                    }
                })
            }

            override fun handleApiDisconnected() {
                Toast.makeText(context, "API disconnected", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun moveToPosition(position: String, onRobotArrived: () -> Unit) {
        println("Moving to $position position")
        robotApi.startNavigation(0, position, 1.0, (10 * 1000).toLong(), object : ActionListener() {
            override fun onResult(result: Int, message: String) {
                println("result:$result, message:$message")
                if (message == "true") {
                    onRobotArrived()
                } else {
                    println("RobotNavigation: Failed to move to position: $message")
                    onRobotArrived()
                }
            }

            override fun onError(errorCode: Int, errorString: String?, extraData: String?) {
                println("error: errorCode:$errorCode")
                println("errorString: $errorString")
                println("errorData : $extraData")
                onRobotArrived()
            }
        })
    }
}
