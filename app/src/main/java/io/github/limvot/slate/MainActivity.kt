package io.github.limvot.slate

import android.os.Bundle
import android.widget.Toast
import android.graphics.fonts.*

import androidx.compose.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.ui.core.*;
import androidx.ui.text.*;
import androidx.ui.text.font.FontFamily;
import androidx.ui.text.style.*;
import androidx.ui.graphics.*;
import androidx.ui.foundation.*;
import androidx.ui.foundation.shape.corner.*;
import androidx.ui.layout.*;
import androidx.ui.unit.*;
import androidx.ui.res.*;
import androidx.ui.material.*;

import org.json.JSONObject;

import com.android.volley.*;
import com.android.volley.toolbox.*;

@Model
data class Status( var text: String)
@Composable
fun StatusV(status: Status, onClickRoom: (Status) -> Unit) {
    Clickable(onClick = { onClickRoom(status) }) {
        Text("Status: ${status.text}")
    }
}

@Model
class MyTextBox(value: String) {
    var text_field: TextFieldValue
    init { text_field = TextFieldValue(value) }
}
@Composable
fun MyTextBoxV(value: MyTextBox) {
    Surface(
            color = Color.LightGray,
            modifier = LayoutWidth.Fill,
            shape = RoundedCornerShape(4.dp)
    ) {
        TextField(
                value = value.text_field,
                onValueChange = {
                    /*var start = it.selection.start*/
                    /*var end = it.selection.start*/
                    /*var idx = it.text.indexOf('\u0000')*/
                    /*while (idx != -1) {*/
                        /*if (idx < start) {*/
                            /*start -= 1;*/
                        /*}*/
                        /*if (idx < end) {*/
                            /*end -= 1;*/
                        /*}*/
                        /*idx = it.text.indexOf('\u0000', startIndex=idx+1)*/
                    /*}*/
                    /*println("changiing ${it.text} for " + it.text.replace("\u0000", ""))*/
                    /*value.text_field = TextFieldValue(it.text.replace("\u0000", ""), TextRange(start, end))*/
                    value.text_field = it
                }
        )
    }
}


@Model
class LoginState(var server: MyTextBox, var username: MyTextBox, var password: MyTextBox) {
    fun login_request(success: (JSONObject) -> Unit, failure: (VolleyError) -> Unit): JsonObjectRequest {
        val server = server.text_field.text.replace("\u0000","")
        val ob = JSONObject(mapOf(
                    "type" to "m.login.password",
                    "user" to username.text_field.text.replace("\u0000",""),
                    "password" to password.text_field.text.replace("\u0000","")
                ));
                println("ob: $ob")
        val request = JsonObjectRequest(
                Request.Method.POST,
                "https://${server}/_matrix/client/r0/login",
                ob,
                Response.Listener { success(it) },
                Response.ErrorListener { failure(it) }
        )
        request.setRetryPolicy(DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS*10,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ))
        return request
    }
}
@Composable
fun LoginStateV(login_state: LoginState, login: (LoginState) -> Unit) {
    MaterialTheme {
        /*Column( modifier = Modifier.padding(16.dp) ){*/
        Column( ) {
            Text("Don't worry about any weird spaces you didn't type - they're nulls due to a bug in either Jetpack Compose or the Samsung Keyboard, I strip them before they're sent")
            MyTextBoxV(value = login_state.server)
            MyTextBoxV(value = login_state.username)
            MyTextBoxV(value = login_state.password)
            Button(onClick = { login(login_state) }) {
                Text("Login")
            }
        }
    }
}

@Model
data class RoomList( var rooms: MutableList<Room>)
@Composable
fun RoomListV(roomList: RoomList, onClickRoom: (String) -> Unit) {
    /*Column( modifier = Modifier.padding(16.dp)) {*/
    Column( ) {
        Text("Rooms:", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.preferredHeight(16.dp))
        AdapterList( data = roomList.rooms ) {
            Clickable(onClick = { onClickRoom(it.id) }) {
                Text(it.name)
            }
        }
    }
}

@Model
data class Room( val id: String, var name: String, var timeline: MutableList<Event>)
@Composable
fun ChatV(room: Room, entry_box: MyTextBox, send_message: (Room, String) -> Unit) {
    /*Column( modifier = Modifier.padding(16.dp)) {*/
    Column( ) {
        Text("${room.name}: ", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.preferredHeight(16.dp))
        MyTextBoxV(entry_box)
        Button(onClick = { send_message(room, entry_box.text_field.text.replace("\u0000","")) }) {
            Text("Send")
        }
        Spacer(modifier = Modifier.preferredHeight(16.dp))
        AdapterList( data = room.timeline ) {
            EventV(it)
        }
    }
}

@Model
data class Event( var sender: String, var content_body: String, val origin_server_ts: Long)
@Composable
fun EventV(event: Event) {
    Text("${event.sender}: ${event.content_body}", style = MaterialTheme.typography.body1)
}

enum class AppStateEnum {
    LOGIN, ROOMS, CHAT
}

@Model
class AppState(val queue: RequestQueue) {
    var app_state_enum: AppStateEnum

    // Common to all
    val status: Status

    // Login
    val login_state: LoginState

    // Common to rooms and chat
    var login_response: JSONObject? = null
    var current_sync: JSONObject? = null
    // Rooms
    var room_list: RoomList
    // Chat
    var current_room: String? = null
    var entry_box: MyTextBox
    init {
        status = Status("")
        app_state_enum = AppStateEnum.LOGIN
        login_state = LoginState( MyTextBox("matrix.org"),
                                  MyTextBox("username"),
                                  MyTextBox("<password>"))
        room_list = RoomList(mutableListOf())
        entry_box = MyTextBox("chat...")
    }
    fun send_message(room: Room, message: String) {
        val room_id = room.id
        val home_server = login_response?.getString("home_server")
        val access_token = login_response?.getString("access_token")
        val url = "https://$home_server/_matrix/client/r0/rooms/$room_id/send/m.room.message?access_token=$access_token";
        val ob = JSONObject(mapOf("msgtype" to "m.text", "body" to message));
        val origin_server_ts = room.timeline.lastOrNull()?.origin_server_ts ?: 0
        val sending_event = Event("You", "sending: $message...", origin_server_ts)
        room.timeline.add(sending_event)
        val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                ob,
                Response.Listener {
                    sending_event.content_body = "✓ $message"
                },
                Response.ErrorListener {
                    sending_event.content_body = "Failed to send $message..."
                }
        )
        request.setRetryPolicy(DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS*10,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ))
        queue.add(request)
    }
    fun sync_request(success: () -> Unit, failure: (VolleyError) -> Unit) {
        val home_server = login_response?.getString("home_server")
        val access_token = login_response?.getString("access_token")
        val timeout_ms = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS*10
        val url = if (current_sync != null) {
            val since = current_sync?.getString("next_batch")
            "https://$home_server/_matrix/client/r0/sync?since=$since&timeout=$timeout_ms&access_token=$access_token"
        } else {
            val limit = 5
            "https://$home_server/_matrix/client/r0/sync?filter={\"room\":{\"timeline\":{\"limit\":$limit}}}&access_token=$access_token"
        }
        val request = JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                Response.Listener {
                    update_with_sync(it)
                    success()
                    sync_request({ }, { failure(it) })
                },
                Response.ErrorListener { failure(it) }
        )
        request.setRetryPolicy(DefaultRetryPolicy(
                timeout_ms,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ))
        queue.add(request)
    }
    fun update_with_sync(sync_object: JSONObject) {
        val rooms = sync_object.optJSONObject("rooms")?.optJSONObject("join")
        if (rooms == null)
            return;
        for (room_key in rooms.keys()) {
            var room = room_list.rooms.find { it.id == room_key } ?: run { val r = Room(room_key, room_key, mutableListOf()); room_list.rooms.add(r); r }

            val state_events = rooms?.optJSONObject(room_key)?.optJSONObject("state")?.optJSONArray("events")
            for (i in 0 until (state_events?.length() ?: 0)) {
                val state_event = state_events?.optJSONObject(i)
                if (state_event?.getString("type") == "m.room.name") {
                    room.name = state_event.getJSONObject("content").getString("name")
                    break;
                }
            }

            val timeline_events = rooms?.optJSONObject(room_key)?.optJSONObject("timeline")?.optJSONArray("events")
            for (i in 0 until (timeline_events?.length() ?: 0)) {
                timeline_events?.optJSONObject(i)?.let {
                    val sender = it.getString("sender");
                    val content_body = it.optJSONObject("content")?.optString("body") ?: "<no body>";
                    val origin_server_ts = it.getLong("origin_server_ts");
                    room.timeline.add(Event(sender, content_body, origin_server_ts))
                }
            }
        }
        room_list.rooms.sortBy { -(it.timeline.lastOrNull()?.origin_server_ts ?: 0) }
        current_sync = sync_object
    }
}
@Composable
fun AppStateV(app_state: AppState) {
    val typography = MaterialTheme.typography
    MaterialTheme {
        Column(
                modifier = Modifier.padding(16.dp)
        ){
            /*Image(image,*/
            /*modifier = Modifier.preferredHeightIn(maxHeight = 180.dp)*/
            /*.fillMaxWidth()*/
            /*.clip(shape = RoundedCornerShape(4.dp)),*/
            /*contentScale = ContentScale.Crop*/
            /*)*/
            Text("Slate:", style = typography.h2)
            StatusV(app_state.status) { status ->
                /*Toast.makeText(context, "You just clicked on ${status.text}", Toast.LENGTH_LONG).show()*/
                status.text += "holla"
            }
            when(app_state.app_state_enum) {
                AppStateEnum.LOGIN -> {
                    LoginStateV(app_state.login_state, { login_state ->
                        app_state.status.text = "Trying to log in"
                        app_state.queue.add(login_state.login_request(
                                { response ->
                                    app_state.status.text = "Logged in, syncing..."
                                    app_state.login_response = response
                                    app_state.app_state_enum = AppStateEnum.ROOMS
                                    app_state.sync_request({
                                        app_state.status.text = "Synced!"
                                    }, { e ->
                                        app_state.status.text = "Sync didn't work: $e, ${e.networkResponse?.headers}, ${e.networkResponse?.data?.let { String(it)}}"
                                    })
                                },
                                { e ->
                                    app_state.status.text = "Login didn't work: $e, ${e.networkResponse?.headers}, ${e.networkResponse?.data?.let { String(it)}}"
                                }
                        ))
                    })
                }
                AppStateEnum.ROOMS -> {
                    RoomListV(app_state.room_list) { room ->
                        app_state.status.text = "room id: $room"
                        app_state.current_room = room
                        app_state.app_state_enum = AppStateEnum.CHAT
                    }
                }
                AppStateEnum.CHAT -> {
                    ChatV(app_state.room_list.rooms.first{ it.id == app_state.current_room },
                          app_state.entry_box,
                          { room, message -> app_state.send_message(room, message) })
                }
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    var app_state: AppState? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            app_state = AppState(Volley.newRequestQueue(this))
            AppStateV(app_state!!)
            /*val typography = TextStyle(fontFamily = FontFamily.Monospace)*/
            /*val typography = Typography(defaultFontFamily = FontFamily.Monospace)*/
        }
    }
    override fun onBackPressed() {
        if (app_state?.app_state_enum == AppStateEnum.CHAT) {
            app_state?.app_state_enum = AppStateEnum.ROOMS
        } else {
            super.onBackPressed()
        }
    }
}
