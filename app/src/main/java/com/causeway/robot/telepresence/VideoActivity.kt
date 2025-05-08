package com.causeway.robot.telepresence;
import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.RemoteException
import android.telephony.SmsManager
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ainirobot.coreservice.client.ApiListener
import com.ainirobot.coreservice.client.Definition
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.StatusListener
import com.ainirobot.coreservice.client.listener.ActionListener
import com.ainirobot.coreservice.client.listener.CommandListener
import com.ainirobot.coreservice.client.listener.TextListener
import com.ainirobot.coreservice.client.speech.SkillApi
import com.ainirobot.coreservice.client.speech.SkillCallback
import com.ainirobot.coreservice.client.speech.entity.TTSEntity
import com.causeway.robot.telepresence.MainActivity.Companion.KEY_DISTANCE_THRESH
import com.causeway.robot.telepresence.MainActivity.Companion.KEY_ROBOT_SN
import com.causeway.robot.telepresence.MainActivity.Companion.PREFS_NAME
import com.causeway.robot.telepresence.R
import dev.gustavoavila.websocketclient.WebSocketClient
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.system.exitProcess


private const val PERMISSION_REQ_ID = 22

// Ask for Android device permissions at runtime.
private val REQUESTED_PERMISSIONS = arrayOf<String>(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)
private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

class VideoActivity : ComponentActivity() {
  private lateinit var agoraToken:String;
    private  val mSkillCallback: SkillCallback = object : SkillCallback() {
        @Throws(RemoteException::class)
        override fun onSpeechParResult(s: String) {
            // The result of temporary speech recognition
        }

        @Throws(RemoteException::class)
        override fun onStart() {
            // Start recognition
        }

        @Throws(RemoteException::class)
        override fun onStop() {
            // End of recognition
        }

        @Throws(RemoteException::class)
        override fun onVolumeChange(volume: Int) {
            // The size of the recognized voice changes
        }

        @Throws(RemoteException::class)
        override fun onQueryEnded(status: Int) {
            // Handle query ended
        }

        @Throws(RemoteException::class)
        override fun onQueryAsrResult(asrResult: String) {
            // ASR result
        }
    }
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var lastCommandTime: Long = 0 // Timestamp of the last command
    private val commandDelay: Long = 0
    private fun say(text: String, context: Context) {
        println("Robot TTS text is $text")


        val skillApi = SkillApi()
        skillApi.connectApi(context, object : ApiListener {
            override fun handleApiDisabled() {
                // Handle API disabled
                Toast.makeText(context, "API disabled", Toast.LENGTH_SHORT).show()
            }

            override fun handleApiConnected() {
                // Voice service connection is successful, register voice callback
                skillApi.registerCallBack(mSkillCallback)

                // Now you can safely call playText here
                skillApi.playText(
                    TTSEntity(text), object : TextListener() {
                        override fun onStart() {
                            // Play start
                        }

                        override fun onStop() {
                            // Play stop
                        }

                        override fun onError() {
                            // Play error
                        }

                        override fun onComplete() {
                            // Play is complete
                        }
                    })
            }

            override fun handleApiDisconnected() {
                // Voice service has been disconnected
                Toast.makeText(context, "API disconnected", Toast.LENGTH_SHORT).show()
            }
        })

    }

private fun tiltHead(vAngle:Int) {
    RobotApi.getInstance().moveHead(0, "relative", "relative", 0, vAngle,object: CommandListener() {

    })

}
    private fun stopMovement() {
        RobotApi.getInstance().stopMove(0,object:CommandListener(){

        })
    }



    private fun moveToPosition(position: String, onRobotArrived: () -> Unit) {
        println("Moving to $position position")
        // Define coordinates wor location for pressing the button
        RobotApi.getInstance()
            .startNavigation(0, position, 1.0, (10 * 1000).toLong(), object : ActionListener() {
                override fun onResult(result: Int, message: String) {
                    runOnUiThread {


                        println("result:$result, message:$message")
                        if ("true" == message) {
                            onRobotArrived()
                        } else {
                            println("RobotNavigation: " + "Failed to move to position: $message")
                            onRobotArrived()
                        }
                    }
                }

                override fun onError(errorCode: Int, errorString: String?, extraData: String?) {
                    runOnUiThread {
                        println("error: errorCode:$errorCode")
                        println("errorString: $errorString")
                        println("errorData : $extraData")
                        onRobotArrived()
                    }

                }
            })

    }
    private var webSocketClient: WebSocketClient? = null
    private fun initializeWebSocket() {


        if (robotSN.isEmpty()) {
            Log.e("WebSocket", "⚠️ No Robot SN found in SharedPreferences! Cannot register.")
            return
        }

        val client = OkHttpClient()
        val request = Request.Builder().url("wss://hungrygowhere.com.sg:8686").build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "✅ Connected to server with Robot SN: $robotSN")

                // Register this robot with the retrieved SN
                val registrationMessage = "{\"type\": \"register\", \"robotSN\": \"$robotSN\"}"
                webSocket.send(registrationMessage)
                var positionList: List<JSONObject> = emptyList()

// Step 1: Retrieve the list of positions and store them
                RobotApi.getInstance().getPlaceList(reqId, object : CommandListener() {
                    override fun onResult(result: Int, message: String) {
                        try {
                            val jsonArray = JSONArray(message)
                            val tempList = mutableListOf<JSONObject>()

                            for (i in 0 until jsonArray.length()) {
                                val json = jsonArray.getJSONObject(i)
                                tempList.add(json)

                            }
                            positionList = tempList // Store the position list globally

                        } catch (e: JSONException) {
                            Log.e("getPlaceList", "❌ Error parsing place list: ${e.message}")
                        }
                    }
                })

                RobotApi.getInstance()
                    .registerStatusListener(Definition.STATUS_POSE, object : StatusListener() {
                        override fun onStatusUpdate(type: String?, value: String?) {
                            try {
                                // Ensure the received value is not null
                                if (value.isNullOrEmpty()) {
                                 //   Log.w("onStatusUpdate", "⚠️ Received empty status update.")
                                    return
                                }
                               // Log.d("value is ", "⚠️ $value")

                                // Parse the received value as JSON
                                val jsonObject = JSONObject(value)
                                val robotX = jsonObject.getDouble("px")
                                val robotY = jsonObject.getDouble("py")

                                // Find closest position
                                 fun findClosestPosition(robotX: Double, robotY: Double): JSONObject? {
                                    var closest: JSONObject? = null
                                    var minDistance =distanceThresh.toDouble()


                                    for (pos in positionList) {
                                        try {
                                            val x = pos.getDouble("x")
                                            val y = pos.getDouble("y")
                                            val distance = Math.sqrt(Math.pow(robotX - x, 2.0) + Math.pow(robotY - y, 2.0))


                                            if (distance < minDistance) {
                                                minDistance = distance
                                                closest = pos
                                            }
                                        } catch (e: JSONException) {
                                            Log.e("findClosestPosition", "❌ Error reading position: ${e.message}")
                                        }
                                    }

                                    return closest
                                }

                                val closestPosition = findClosestPosition(robotX, robotY)
                                //Log.d("onStatusUpdate","closest position is $closestPosition")

                                // Add robotSN and command fields
                                jsonObject.put("robotSN", robotSN)  // Replace with actual robotSN
                                jsonObject.put("command", "pose_update") // Define a meaningful command
                                jsonObject.put("pos_name",closestPosition?.getString("name")?:"None")

                                // Convert back to a string and send over WebSocket
                                val jsonString = jsonObject.toString()
                                webSocket.send(jsonString)

                              //  Log.d("onStatusUpdate", "🚀 Sent Updated JSON: $jsonString")

                            } catch (e: JSONException) {
                                Log.e("onStatusUpdate", "❌ Error parsing status update: ${e.message}")
                            }
                        }
                    })

            }

            override fun onMessage(webSocket: WebSocket, text: String) {
             //   Log.d("WebSocket", "📩 Received text: $text")
                try {
                    // Parse the JSON string
                    val jsonObject = JSONObject(text)
                    println("JSONObject is $jsonObject")


                    // Check if "command" field exists
                    if (jsonObject.has("command")) {
                        val command = jsonObject.getString("command")
                       // Log.d("WebSocket", "🚀 Extracted Command: $command")

                        // Pass the command to handleCommand()
                        handleCommand(command)
                    } else {
                      //  Log.w("WebSocket", "⚠️ No 'command' field found in message.")
                    }

                } catch (e: JSONException) {
                    Log.e("WebSocket", "❌ Error parsing WebSocket message: ${e.message}")
                }
            }


            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "❌ WebSocket connection error: ${t.message}")
            }
        })
    }


    private fun controlRobot(xPercent: Float, yPercent: Float) {
        Log.d("MainActivity", "X percent: $xPercent, Y percent: $yPercent")

        // Check the time elapsed since the last command
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCommandTime < commandDelay) {
            // If the time elapsed is less than the allowed delay, return early
            Log.d("RobotControl", "Rate limit hit, command delayed.")
            return
        }

        // Define movement parameters
        val linearSpeed = -yPercent * MAX_LINEAR_SPEED // Forward/backward speed
        val angularSpeed = -xPercent * MAX_ANGULAR_SPEED // Left/right rotation speed
        Log.d("MainActivity", "linear Speed: $linearSpeed, angular speed: $angularSpeed")

        // Send movement command to the robot
        reqId +=1
        RobotApi.getInstance().motionArcWithObstacles(
            reqId,
            linearSpeed,
            angularSpeed,
            object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    Log.d("RobotControl", "Movement command result: $result, message: $message reqId:$reqId")
                    if (message == "true") {
                        Log.d("RobotControl", "Movement command successful")
                    } else {
                        Log.e("RobotControl", "Movement command failed: $message")
                    }
                }

                override fun onError(errorCode: Int, errorString: String?, extraData: String?) {
                    super.onError(errorCode, errorString, extraData)
                    Log.d("RobotControl", "Movement command error: $errorCode, $errorString, $extraData")
                }

                override fun onStatusUpdate(status: Int, data: String?, extraData: String?) {
                    super.onStatusUpdate(status, data, extraData)
                    Log.d("RobotControl","Status :$status data:$data extraData:$extraData")
                }
            }
        )


        // Update the last command timestamp
        lastCommandTime = currentTime
    }
    private fun handleCommand(command: String) {
       // Log.d("WebSocket","Command is $command")
        if(command=="pose_update") {
            return
        }
        if(command.contains("navigate")) {
            var location = command.split(" ")[1]
            println("Moving to $location")
            moveToPosition(location) {


            }
return
        }

        try {
            var terms = command.split(" ")
            var speed = terms[2].toFloat()
            var direction = terms[1]
            when (direction) {
                "forward" -> controlRobot(0f, -speed) // Forward movement
                "backward" -> controlRobot(0f, speed) // Backward movement
                "left" -> controlRobot(-speed, 0f) // Left turn
                "right" -> controlRobot(speed, 0f) // Right turn
                "up"-> tiltHead(-(speed*10).toInt())
                "down"->tiltHead((speed*10).toInt())
                "stop"->stopMovement()
                else -> Log.d("WebSocket", "Unknown direction:${direction}")
            }



        }
        catch (e:Exception) {
            Log.e("WebSocket","Exception :$e for command $command")
        }

    }




    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Activity destroyed")
        mEngine.leaveChannel()
    }
    lateinit var mEngine:RtcEngine
    lateinit var robotSN:String
    lateinit var distanceThresh:String



    var appInBackground = false
    override fun onPause() {
        appInBackground=true
        super.onPause()
        exitProcess(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        robotSN = sharedPreferences.getString(KEY_ROBOT_SN, "") ?: ""
        distanceThresh = sharedPreferences.getString(KEY_DISTANCE_THRESH, "0") ?: "0"
        initializeWebSocket()
        println("Registering status listener...")




        val videoCallStateViewModel = VideoCallStateViewModel(application)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val channelName = robotSN
        val userRole = "Broadcaster"

        setContent {
            androidx.compose.material3.Scaffold {
                println(it)
                UIRequirePermissions(
                    permissions = permissions,
                    onPermissionGranted = {

                        CallScreen(
                            channelName,
                            userRole,
                            videoCallStateViewModel
                        )
                    },
                    onPermissionDenied = {
                        AlertScreen(it)
                    }
                )
            }
        }


    }
    @RequiresApi(Build.VERSION_CODES.CUPCAKE)
    @Composable
    private fun CallScreen(
        channelName: String,
        userRole: String,

        videoCallStateViewModel: VideoCallStateViewModel
    ) {
        val context = LocalContext.current
        val localSurfaceView = remember { RtcEngine.CreateTextureView(context) }
        var remoteUserMap by remember { mutableStateOf(mapOf<Int, TextureView?>()) }
        var showDialog by remember { mutableStateOf(false) }
//        var timeLeft by remember { mutableStateOf(COUNTDOWN_DURATION) }
//        var dialogTimeLeft by remember { mutableStateOf(TIMER_DURATION) }

        mEngine = remember {
            initEngine(context, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    Log.d(ContentValues.TAG, "channel:$channel,uid:$uid,elapsed:$elapsed")
                    val context = applicationContext
                    val channel = createNotificationChannel(context)
                    val notification = buildNotification(context)
                    sendNotification(context, notification)
                   // mEngine.adjustPlaybackSignalVolume(0);
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d(ContentValues.TAG, "onUserJoined:$uid")

                    val desiredUserList = remoteUserMap.toMutableMap()
                    desiredUserList[uid] = null
                    remoteUserMap = desiredUserList.toMap()
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    mEngine.leaveChannel()
                    val intent = packageManager.getLaunchIntentForPackage("com.ainirobot.moduleapp")
                    if(!appInBackground) {
                        startActivity(intent)
                    }
                    exitProcess(0)



                }
            }, channelName, userRole, videoCallStateViewModel)
        }
        fun startDialogTimer() {
            val dialogTimer = object : CountDownTimer(TIMER_DURATION.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    //dialogTimeLeft = millisUntilFinished.toInt()
                }

                override fun onFinish() {
                    if (showDialog) {
                        mEngine.leaveChannel()
                        val intent = context.packageManager.getLaunchIntentForPackage("com.ainirobot.moduleapp")
                        if(!appInBackground) {
                            context.startActivity(intent)
                        }

                        exitProcess(0)
                    }
                }
            }
            dialogTimer.start()
        }

        fun startMainTimer() {
            val timer = object : CountDownTimer(COUNTDOWN_DURATION.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    //timeLeft = millisUntilFinished.toInt()
                }

                override fun onFinish() {
                    showDialog = true
                    startDialogTimer()
                }
            }
            timer.start()
        }


        LaunchedEffect(Unit) {
            startMainTimer()
        }

        if (showDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { /* Do nothing */ },
                title = { androidx.compose.material3.Text("Continue Call?") },
                text = { androidx.compose.material3.Text("Do you want to continue the call?") },
                confirmButton = {
                    androidx.compose.material3.Button(onClick = {

                        showDialog = false

//                        timeLeft = COUNTDOWN_DURATION // Reset timer
//                        dialogTimeLeft = TIMER_DURATION // Reset dialog timer

                        //            startMainTimer() // Restart the main timer
                        //
                        mEngine.leaveChannel()
                        mEngine.enableVideo()
                        val intent = Intent(applicationContext, VideoActivity::class.java)
                        startActivity(intent)
//                        val intent = Intent(applicationContext, VideoActivity::class.java)
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // restart the activtiy
//
//                        startActivity(intent)

                    }) {
                        androidx.compose.material3.Text("Continue")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.Button(onClick = {
                        mEngine.leaveChannel()
                        val intent =
                            context.packageManager.getLaunchIntentForPackage("com.ainirobot.moduleapp")
                        if (!appInBackground) {
                            context.startActivity(intent)
                        }

                        //finishAffinity()
                    }) {
                        androidx.compose.material3.Text("Leave")
                    }
                }
            )
        }

        if (videoCallStateViewModel.currentState.value == VideoCallStateViewModel.CALL_STATE.READY) {
            mEngine.setupLocalVideo(VideoCanvas(localSurfaceView, Constants.RENDER_MODE_FIT, 0))
            //mEngine.adjustPlaybackSignalVolume(0)
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f)) {
                        localSurfaceView?.let { local ->
                            AndroidView(factory = { local },
                                Modifier
                                    .weight(1f)
                                    .padding(8.dp))
                        }
                        RemoteView(remoteListInfo = remoteUserMap, mEngine = mEngine,
                            Modifier
                                .weight(1f)
                                .padding(8.dp))
                    }
                    UserControls(mEngine = mEngine,
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp))
                }
            }
        }
    }



    @Composable
    private fun RemoteView(remoteListInfo: Map<Int, TextureView?>, mEngine: RtcEngine, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        Column(
            modifier = modifier
                .fillMaxHeight()
                .verticalScroll(state = rememberScrollState())
        ) {
            remoteListInfo.forEach { entry ->
                val remoteTextureView =
                    RtcEngine.CreateTextureView(context).takeIf { entry.value == null } ?: entry.value
                if(remoteTextureView == null) return
                AndroidView(
                    factory = { remoteTextureView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(8.dp)
                        .background(Color.Black)
                )
                mEngine.setupRemoteVideo(
                    VideoCanvas(
                        remoteTextureView,
                        Constants.RENDER_MODE_HIDDEN,
                        entry.key
                    )
                )
            }
        }
    }


    fun initEngine(current: Context, eventHandler: IRtcEngineEventHandler, channelName: String, userRole: String,videoCallStateViewModel: VideoCallStateViewModel): RtcEngine =
        RtcEngine.create(current, APP_ID, eventHandler).apply {
            enableVideo()
            setChannelProfile(1)
            if (userRole == "Broadcaster") {
                setClientRole(1)
            } else {
                setClientRole(0)
            }
            joinChannel(null, channelName, "", 0)
            videoCallStateViewModel.currentState.value = VideoCallStateViewModel.CALL_STATE.READY
        }
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "channel_id" // Replace with your channel ID
            val channelName = "Channel Name" // Replace with your channel name
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            return notificationManager.createNotificationChannel(channel)
        }
    }
    private fun buildNotification(context: Context): Notification {
        val channelId = "channel_id" // Replace with your channel ID
        val intent = Intent(context, MainActivity::class.java) // Replace MainActivity with your desired activity
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your notification icon
            .setContentTitle("Channel Joined")
            .setContentText("User has entered the call.")
            .setContentIntent(pendingIntent)
            .build()
    }
    private fun sendNotification(context: Context, notification: Notification) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, notification)
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        println( "SMS sent to $phoneNumber")
    }


    @Composable
    private fun UserControls(mEngine: RtcEngine, modifier: Modifier = Modifier) {
        var muted by remember { mutableStateOf(false) }
        var videoDisabled by remember { mutableStateOf(false) }
        var volume by remember { mutableStateOf(50f) }
        val activity = (LocalContext.current as? Activity)
        mEngine.adjustPlaybackSignalVolume(volume.toInt())
        Column(
            modifier = modifier
                .padding(8.dp)
                .fillMaxHeight(0.3f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    mEngine.adjustPlaybackSignalVolume(volume.toInt())
                },
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f)
                    .padding(horizontal = 8.dp)
            )
            androidx.compose.material3.Text(text = "Volume: ${volume.toInt()}")

            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        muted = !muted
                        mEngine.muteLocalAudioStream(muted)
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .height(80.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (muted) Color.Blue else Color.White)
                ) {
                    if (muted) {
                        androidx.compose.material3.Icon(
                            painterResource(id = R.drawable.mic),
                            contentDescription = "Tap to unmute mic",
                            tint = Color.White
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            painterResource(id = R.drawable.unmute),
                            contentDescription = "Tap to mute mic",
                            tint = Color.Blue
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        mEngine.leaveChannel()
                        val intent =
                            packageManager.getLaunchIntentForPackage("com.ainirobot.moduleapp")
                        if (!appInBackground) {
                            startActivity(intent)
                        }
                        exitProcess(0)
                        //        finishAffinity()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(60.dp)
                        .height(80.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Red)
                ) {
                    androidx.compose.material3.Icon(
                        painterResource(R.drawable.cancel),
                        contentDescription = "Tap to disconnect Call",
                        tint = Color.White
                    )
                }
            }
        }
    }

    @Composable
    private fun AlertScreen(requester: () -> Unit) {
        val context = LocalContext.current

        Log.d(ContentValues.TAG, "AlertScreen: ")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Red),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Button(onClick = {
                requestPermissions(
                    context as Activity,
                    permissions,
                    22
                )
                requester()
            }) {
                androidx.compose.material3.Icon(Icons.Rounded.Warning, "Permission Required")
                androidx.compose.material3.Text(text = "Permission Required")
            }
        }
    }

    /**
     * Helper Function for Permission Check
     */
    @Composable
    private fun UIRequirePermissions(
        permissions: Array<String>,
        onPermissionGranted: @Composable () -> Unit,
        onPermissionDenied: @Composable (requester: () -> Unit) -> Unit
    ) {
        Log.d(ContentValues.TAG, "UIRequirePermissions: ")
        val context = LocalContext.current

        var grantState by remember {
            mutableStateOf(permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            })
        }

        if (grantState) {
            onPermissionGranted()
        }
        else {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = {
                    grantState = !it.containsValue(false)
                }
            )
            onPermissionDenied {
                Log.d(ContentValues.TAG, "lahuncher.launch")
                launcher.launch(permissions)
            }
        }
    }
}

