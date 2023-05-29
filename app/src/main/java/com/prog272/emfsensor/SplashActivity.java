package com.prog272.emfsensor;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_SCREEN_DELAY = 5000; // 5 second duration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //Create new handler and post delayed runnable
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //create intent to start mainactivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent); //start MainActivity
                finish(); //finishes the splash activity
            }
        }, SPLASH_SCREEN_DELAY); //Delay execution for 5 seconds as mentioned above
    }
}