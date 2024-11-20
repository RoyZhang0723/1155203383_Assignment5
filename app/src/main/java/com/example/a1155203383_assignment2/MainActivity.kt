package com.example.a1155203383_assignment2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
//import com.example.a1155203383_assignment2.ui.theme._1155203383_assignment2Theme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClientBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            // Get the registration token
            val token = task.result
            Log.d(TAG, "FCM registration token: $token")
            // Send the token to your server or save it locally
        }

        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.e("TAG", "onCreate: subscribeToTopic", )
                }else{
                    Log.e("TAG", "onCreate: subscribeToTopic failed", )
                }
            }


        requestNotificationPermission()
        enableEdgeToEdge()

        // State to hold chatrooms
        val chatrooms = mutableStateListOf<Pair<Int, String>>()
        CoroutineScope(Dispatchers.IO).launch {
            val infJSON = GET("http://10.0.2.2:8000/get_chatrooms/")
            val jsonObject = JSONObject(infJSON)
            val dataArray = jsonObject.getJSONArray("data")
            for (i in 0 until dataArray.length()) {
                val chatroom = dataArray.getJSONArray(i)
                val id = chatroom.getInt(0)
                val name = chatroom.getString(1)
                chatrooms.add(id to name)
            }
        }
        createNotification(this)
        setContent {
            ChatroomButtons(chatrooms)
        }
    }

    @SuppressLint("MissingPermission")
    private fun createNotification(context: Context) {
        val CHANNEL_ID = "MyNotificaiton"
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("My notification")
            .setContentText("Hello World!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationId = 1
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, builder.build())
    }

    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
            // The code below is related to create notification channel for later use
            val channel = NotificationChannel("MyNotification","MyNotification",
                NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java) as
                    NotificationManager;
            manager.createNotificationChannel(channel)

        }
    }

}


@Composable
fun ChatroomButtons(chatrooms: List<Pair<Int, String>>) {
    val context = LocalContext.current

    LazyColumn {
        items(chatrooms) { chatroom ->
            Button(
                onClick = {
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("chatroomId", chatroom.first)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text(text = "Chatroom: ${chatroom.second}")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    _1155203383_assignment2Theme {
//        Greeting("Android")
//    }
//}

fun GET(url: String?): String {
    var inputStream: InputStream? = null
    var result = ""
    try {
        // 1. create HttpClient
        val httpclient: HttpClient = HttpClientBuilder.create().build()
        // 2. make GET request to the given URL
        val httpResponse: HttpResponse = httpclient.execute(HttpGet(url))
        // 3. receive response as inputStream
        inputStream = httpResponse.getEntity().getContent()
        // 4. convert inputstream to string
        if (inputStream != null) result =
            convertInputStreamToString(inputStream).toString()
        else result = "Did not work!"
    } catch (e: Exception) {
        Log.d("InputStream", e.localizedMessage)
    }
    return result
}

@Throws(IOException::class)
private fun convertInputStreamToString(inputStream: InputStream): String? {
    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
    var line: String? = ""
    var result: String? = ""
    while ((bufferedReader.readLine().also { line = it }) != null)
        result += line
    inputStream.close()
    return result
}


