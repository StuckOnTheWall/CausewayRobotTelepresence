package com.three.robot.telepresence

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val TIMEOUT_MS = 20_000L   // 20 seconds

    companion object {
        const val PREFS_NAME = "365RobotPrefs"
        const val KEY_ROBOT_SN = "robotSN"
        const val KEY_DISTANCE_THRESH = "keyDistanceThresh"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TimeoutManager.initialize(this, TIMEOUT_MS)

        setContent {
            val context = LocalContext.current
            var robotSN by remember { mutableStateOf(loadRobotSN(context)) }
            var initialDistanceThresh by remember { mutableStateOf(loadDistanceThresh(context)) }
            var showDialog by remember { mutableStateOf(false) }

            // Detect orientation
            val configuration = LocalConfiguration.current
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

            Box(modifier = Modifier.fillMaxSize()) {
                // Background image: in portrait, fill height and center-crop horizontally
                Image(
                    painter = painterResource(
                        id = if (isPortrait)
                            R.drawable.image_background_portrait
                        else
                            R.drawable.image_background
                    ),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (isPortrait) ContentScale.FillHeight else ContentScale.Crop
                )

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("", color = Color.White) },
                            actions = {
                                // Settings button (unchanged)
                                IconButton(onClick = {
                                    TimeoutManager.cancel()
                                    showDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White
                                    )
                                }
                                // Exit button (new)
                                IconButton(onClick = {
                                    // 1) Get the Xiaobao launch intent
                                    val homeIntent = packageManager.getLaunchIntentForPackage("com.ainirobot.moduleapp")
                                    if (homeIntent != null) {
                                        // 2) Clear your stack and start Xiaobao
                                        homeIntent.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                        startActivity(homeIntent)
                                    }
                                    // 3) Finish this activity so nothing stays running
                                    this@MainActivity.finish()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.ExitToApp,
                                        contentDescription = "Exit",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp),
                        // portrait: center everything so the button floats up
                        // landscape: keep your original SpaceBetween
                        verticalArrangement = if (isPortrait)
                            Arrangement.Center
                        else
                            Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // (You said no title/subtitle needed, so we leave those empty
                        // or you can remove this inner Column entirely if you never need it:)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height( if (isPortrait) 0.dp else 40.dp ))
                            Text(text = "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "",
                                fontSize = 18.sp,
                                color = Color.White)
                        }

                        // Join button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // in portrait, nudge the button up off the very bottom
                                .padding(bottom = if (isPortrait) 20.dp else 30.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Button(
                                onClick = {
                                    if (robotSN.isEmpty()) {
                                        Toast.makeText(context,
                                            "Please set the Robot SN in Settings",
                                            Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    TimeoutManager.cancel()

                                    val intent = Intent(applicationContext, VideoActivity::class.java)
                                    intent.putExtra("RobotSN", robotSN)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    ContextCompat.startActivity(applicationContext, intent, Bundle())
                                },
                                modifier = Modifier
                                    .width(200.dp)
                                    // lift the button up another 10.dp
                                    .offset(y = if (isPortrait) 140.dp else 0.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF37053))
                            ) {
                                Icon(Icons.Filled.ArrowForward,
                                    contentDescription = "Join",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White)
                                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                Text(text = "Join Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White)
                            }
                        }
                    }
                }

                if (showDialog) {
                    SettingsDialog(
                        initialSN = robotSN,
                        initialDistanceThresh = initialDistanceThresh,
                        onSave = { newSN, distanceThresh ->
                            robotSN = newSN
                            saveSettings(context, newSN, distanceThresh)
                            showDialog = false
                            TimeoutManager.initialize(this@MainActivity, TIMEOUT_MS)
                           // startActivity(Intent(applicationContext, MainActivity::class.java))
                        },
                        onDismiss = { showDialog = false
                            TimeoutManager.initialize(this@MainActivity, TIMEOUT_MS)
                        }

                    )
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        TimeoutManager.resetTimer(TIMEOUT_MS)
    }

    // ← And this at the bottom of the class, to clean up
    override fun onDestroy() {
        super.onDestroy()
        TimeoutManager.cancel()

        // Launch OrionStar Xiaobao home
        val homeIntent = packageManager.getLaunchIntentForPackage("com.ainirobot.moduleapp")
        if (homeIntent != null) {
            homeIntent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(homeIntent)
        }

        // Finally kill our process so nothing lingers
        this@MainActivity.finishAffinity()
        kotlin.system.exitProcess(0)
    }


    // ✅ Function to Load Robot SN from SharedPreferences
    private fun loadRobotSN(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ROBOT_SN, "") ?: ""
    }
private fun loadDistanceThresh(context: Context) :String {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences.getString(KEY_DISTANCE_THRESH, "") ?: ""
}
    // ✅ Function to Save Robot SN to SharedPreferences
    private fun saveSettings(context: Context, robotSN: String,distanceThresh:String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_ROBOT_SN, robotSN).apply()
        sharedPreferences.edit().putString(KEY_DISTANCE_THRESH, distanceThresh).apply()
    }
}

// ✅ Dialog for Entering Robot SN
@Composable
fun SettingsDialog(
    initialSN: String,
    initialDistanceThresh: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var robotSN by remember { mutableStateOf(TextFieldValue(initialSN)) }
    var distanceThreshold by remember { mutableStateOf(TextFieldValue(initialDistanceThresh)) }
    var pin by remember { mutableStateOf(TextFieldValue("")) }
    var isRobotSNUnlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val REQUIRED_PIN = "1234"  // Set your required PIN here

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Robot SN section: Locked until correct PIN is entered.
                Text("Robot Serial Number (SN):")
                if (isRobotSNUnlocked) {
                    TextField(
                        value = robotSN,
                        onValueChange = { robotSN = it },
                        singleLine = true
                    )
                } else {
                    // Show a masked version and a PIN entry to unlock.
                    Text("••••••", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter PIN to view/change Robot SN:")
                    TextField(
                        value = pin,
                        onValueChange = { pin = it },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (pin.text == REQUIRED_PIN) {
                            isRobotSNUnlocked = true
                        } else {
                            Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Unlock")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Distance threshold field (remains unprotected)
                Text("Distance threshold for point to be close to robot:")
                TextField(
                    value = distanceThreshold,
                    onValueChange = { distanceThreshold = it },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!isRobotSNUnlocked) {
                    Toast.makeText(context, "Please unlock the Robot SN first", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                onSave(robotSN.text, distanceThreshold.text)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}
