package xyz.room409.slate

import android.app.Activity
import android.os.Bundle
import android.util.Log

class SlateMainActivity : Activity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        System.loadLibrary("rust")
        Log.d("rust", hello("World - kotlin"))
    }
    external fun hello(to: String): String
}
