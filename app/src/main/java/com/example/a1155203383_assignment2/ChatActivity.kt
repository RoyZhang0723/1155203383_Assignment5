package com.example.a1155203383_assignment2
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.entity.StringEntity
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClientBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json
import java.net.URLEncoder

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the chatroomId from the Intent
        val chatroomId = intent?.getIntExtra("chatroomId", -1) ?: -1

        val messages = mutableStateListOf<Message>()
        CoroutineScope(Dispatchers.IO).launch {
            val infJSON = GET("http://10.0.2.2:8000/get_messages/?chatroom_id=$chatroomId")
            val jsonObject = JSONObject(infJSON)
            val dataArray = jsonObject.getJSONObject("data")
            val messageArray = dataArray.getJSONArray("message")
            for (i in 0 until messageArray.length()) {
                val chatroom = messageArray.getJSONArray(i)
                val message = chatroom.getString(4)
                val name = chatroom.getString(3)
                val message_time = chatroom.getString(2)
                val user_id = chatroom.getInt(1)
                messages.add(Message(message, message_time, user_id == 1))
            }
        }
        setContent {
            val context = LocalContext.current
            Column { Button(onClick = {
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            }) {
                Text(text = "Click for Main Activity")
            }

                ChatScreen(messages, chatroomId, 1) }
        }
    }
}
data class Message(val content: String, val time: String, val isUser: Boolean)

@Serializable
data class PostRequestModel(
    val chatroom_id: Int,
    val user_id: String,
    val name: String,
    val message: String,
    val message_time: String
)

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(messages: MutableList<Message>, chatroom_id: Int, user_id: Int) {
    var inputText by remember { mutableStateOf(TextFieldValue()) }
//    val messages = remember { mutableStateListOf<Message>(*getHardcodedMessages()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chat Activity") })
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message)
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .background(Color.LightGray),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            sendMessage(inputText, messages, chatroom_id, user_id) { inputText = it }
                        })
                    )
                    IconButton(onClick = {
                        sendMessage(inputText, messages, chatroom_id, user_id) { inputText = it }
                    }) {
                        Image(
                            painter = painterResource(id = android.R.drawable.ic_menu_send),
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun MessageBubble(message: Message) {
    if (message.isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isUser) Color(0xFFD1F7C4) else Color(0xFFF0F0F0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Gray),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
                Column {
                    Text(text = message.content)
                    Text(text = message.time, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

fun getHardcodedMessages(): Array<Message> {
    return arrayOf(
        Message("Hello!", "10:00", false),
        Message("Hi there!", "10:01", true),
        Message("How are you?", "10:02", false),
        Message("I'm good, thanks!", "10:03", true)
    )
}

fun getCurrentTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return simpleDateFormat.format(Date())
}

fun sendMessage(inputText: TextFieldValue, messages: MutableList<Message>, chatroom_id: Int, user_id: Int, clearInput: (TextFieldValue) -> Unit) {
    if (inputText.text.isNotBlank()) {
        val currentTime = getCurrentTime()
        val newMessage = Message(inputText.text, currentTime, true)
        messages.add(newMessage)

        // Create the data for the POST request
        val postData = PostRequestModel(
            chatroom_id = chatroom_id, // Replace with actual chatroom ID
            user_id = "1155203383", // Replace with actual user ID
            name = "User", // Replace with actual user name
            message = inputText.text,
            message_time = getCurrentTime()
        )

        // Launch a coroutine to send the POST request
        CoroutineScope(Dispatchers.IO).launch {
            val response = POST("http://10.0.2.2:8000/send_message/", postData)
            Log.d("POST Response", response)
        }

        clearInput(TextFieldValue())
    }
}


fun POST(url: String, data: PostRequestModel): String {
    var inputStream: InputStream? = null
    var result = ""
//    // 1. create HttpURLConnection
//    val conn = URL(url).openConnection() as HttpURLConnection
//    conn.requestMethod = "POST"
//    conn.doOutput = true
//    conn.doInput = true
//    conn.readTimeout = 15000
//    conn.connectTimeout = 15000
//    conn.setRequestProperty("Content-Type", "application/json")
//    conn.setRequestProperty("Accept", "application/json")
    val httpclient: HttpClient = HttpClientBuilder.create().build()
    val httpPost = HttpPost(url)
    httpPost.setHeader("Content-type", "application/json")
    // 2. build JSON object
    // 2. build JSON object
    val json = JSONObject()
    json.put("chatroom_id", data.chatroom_id)
    json.put("user_id", data.user_id)
    json.put("name", data.name)
    json.put("message", data.message)
    json.put("message_time", data.message_time)
    val entity = StringEntity(json.toString())
    httpPost.entity = entity
    val httpResponse: HttpResponse = httpclient.execute(httpPost)
    inputStream = httpResponse.entity.content
    result = if(inputStream != null) {
        convertInputStreamToString(inputStream).toString()
    } else{
        "ERROR"
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
