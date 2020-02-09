package xyz.room409.slate;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class SlateMainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        System.loadLibrary("rust");
        Log.d("rust", hello("World"));
    }
    private static native String hello(final String to);
}
