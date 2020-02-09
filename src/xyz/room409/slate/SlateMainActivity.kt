package xyz.room409.slate

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

fun Activity.toast(text: String) {
    Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
}

class SlateMainActivity : Activity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        System.loadLibrary("rust")
        Log.d("rust", hello("World - kotlin"))
        toast(hello("World - kotlin toasts!"))
        /*Toast.makeText(applicationContext, hello("world kotlin toasts no ext!"), Toast.LENGTH_SHORT).show()*/
    }
    external fun hello(to: String): String
}
