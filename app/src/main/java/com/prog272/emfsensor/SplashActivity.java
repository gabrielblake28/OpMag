package com.prog272.emfsensor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_SCREEN_DELAY = 5000; // 5 second duration


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView logoText = findViewById(R.id.textLogo);
        Typeface typeface = ResourcesCompat.getFont(this, R.font.futurabolditalic);
        logoText.setTypeface(typeface);


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