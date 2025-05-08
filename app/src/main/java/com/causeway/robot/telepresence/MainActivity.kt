package com.causeway.robot.telepresence

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.causeway.robot.telepresence.R

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "365RobotPrefs"
        const val KEY_ROBOT_SN = "robotSN"
        const val KEY_DISTANCE_THRESH = "keyDistanceThresh"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var robotSN by remember { mutableStateOf(loadRobotSN(context)) }
            var initialDistanceThresh by remember { mutableStateOf(loadDistanceThresh(context)) }
            var showDialog by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                // Full-screen background image
                Image(
                    painter = painterResource(id = R.drawable.image_background),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Transparent Scaffold overlays the background
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("", color = Color.White) },
                            actions = {
                                IconButton(onClick = { showDialog = true }) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    // Column to position content: top section and join button at the bottom
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Section: Title and description text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(40.dp))
                            Text(
                                text = "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        // Bottom Section: Join button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 30.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Button(
                                onClick = {
                                    if (robotSN.isEmpty()) {
                                        Toast.makeText(context, "Please set the Robot SN in Settings", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val intent = Intent(applicationContext, VideoActivity::class.java)
                                    intent.putExtra("RobotSN", robotSN)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    ContextCompat.startActivity(applicationContext, intent, Bundle())
                                },
                                modifier = Modifier.width(200.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF37053))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = "Join",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                Text(
                                    text = "Join Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Settings Dialog for entering Robot SN
                if (showDialog) {
                    SettingsDialog(
                        initialSN = robotSN,
                        initialDistanceThresh = initialDistanceThresh,
                        onSave = { newSN, distanceThresh ->
                            robotSN = newSN
                            saveSettings(context, newSN, distanceThresh)
                            showDialog = false
                            // Restart app
                            startActivity(Intent(applicationContext, MainActivity::class.java))
                        },
                        onDismiss = { showDialog = false }
                    )
                }
            }
        }

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
//