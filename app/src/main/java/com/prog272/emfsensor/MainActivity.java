package com.prog272.emfsensor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GestureDetectorCompat;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor magneticFieldSensor;
    private TextView xValueTextView, yValueTextView, zValueTextView, mValueTextView;
    private GestureDetectorCompat gestureDetectorCompat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xValueTextView = findViewById(R.id.xValueTextView);
        yValueTextView = findViewById(R.id.yValueTextView);
        zValueTextView = findViewById(R.id.zValueTextView);
        mValueTextView = findViewById(R.id.mValueTextView);

        gestureDetectorCompat = new GestureDetectorCompat(this, new MyGestureListener());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (magneticFieldSensor == null) {
            // No magnetic field sensor available on this device
            finish();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double m = Math.sqrt((x*x) + (y*y) + (z*z));

            xValueTextView.setText("X: " + String.format("%.2f", x) + " μT");
            yValueTextView.setText("Y: " + String.format("%.2f", y) + " μT");
            zValueTextView.setText("Z: " + String.format("%.2f", z) + " μT");
            mValueTextView.setText("M: " + String.format("%.2f", m) + " μT");

            if(m >= 100){
                mValueTextView.setTextColor(getResources().getColor(R.color.level11));
            }
            if(m < 100){
                mValueTextView.setTextColor(getResources().getColor(R.color.level10));
            }
            if(m < 90){
                mValueTextView.setTextColor(getResources().getColor(R.color.level9));
            }
            if(m < 85){
                mValueTextView.setTextColor(getResources().getColor(R.color.level8));
            }
            if(m < 80){
                mValueTextView.setTextColor(getResources().getColor(R.color.level7));
            }
            if(m < 75){
                mValueTextView.setTextColor(getResources().getColor(R.color.level6));
            }
            if(m < 70){
                mValueTextView.setTextColor(getResources().getColor(R.color.level5));
            }
            if(m < 65){
                mValueTextView.setTextColor(getResources().getColor(R.color.level4));
            }
            if(m < 60){
                mValueTextView.setTextColor(getResources().getColor(R.color.level3));
            }
            if(m < 55){
                mValueTextView.setTextColor(getResources().getColor(R.color.level2));
            }
            if(m < 50){
                mValueTextView.setTextColor(getResources().getColor(R.color.level1));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 50;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffX = e1.getX() - e2.getX();
                float diffY = e1.getY() - e2.getY();

                if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeLeft();
                    }
                    result = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    private void onSwipeLeft() {
        Toast.makeText(this, "Loading Data", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);

        overridePendingTransition(R.anim.transition2_1, R.anim.transition2_2);
    }
}