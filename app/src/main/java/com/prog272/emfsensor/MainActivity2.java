package com.prog272.emfsensor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GestureDetectorCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
public class MainActivity2 extends AppCompatActivity implements SensorEventListener {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Used for the compass
    private ImageView imageView;
    private Sensor sensorAccelerometer;
    private float[] floatGravity = new float[3];
    private float[] floatGeoMagnetic = new float[3];
    private float[] floatOrientation = new float[3];
    private float[] floatRotationMatrix = new float[9];

    private SensorManager sensorManager;
    private Sensor magneticFieldSensor;
    private TextView xValueTextView, yValueTextView, zValueTextView, mValueTextView;
    private GestureDetectorCompat gestureDetectorCompat;

    private boolean isRecording = false;
    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noSwipe();

        // The image view for the compass
        imageView = findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.arrow);


        xValueTextView = findViewById(R.id.xValueTextView);
        yValueTextView = findViewById(R.id.yValueTextView);
        zValueTextView = findViewById(R.id.zValueTextView);
        mValueTextView = findViewById(R.id.mValueTextView);

        gestureDetectorCompat = new GestureDetectorCompat(this, new MyGestureListener());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        if (magneticFieldSensor == null) {
            // No magnetic field sensor available on this device
            finish();
        }


        Button output = findViewById(R.id.recordButton);
        output.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isRecording = true;
                TextView message = findViewById(R.id.message);
                message.setText("Recording");


            }
        });

        Button outputTwo = findViewById(R.id.submitButton);
        outputTwo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isRecording = false;
//                TextView message = findViewById(R.id.messageTwo);
//                message.setText("Stopped");


            }
        });

        SensorEventListener sensorEventListenerAccelrometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                floatGravity = event.values;

                SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravity, floatGeoMagnetic);
                SensorManager.getOrientation(floatRotationMatrix, floatOrientation);

                imageView.setRotation((float) (-floatOrientation[0] * 180 / 3.14159));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        SensorEventListener sensorEventListenerMagneticField = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                floatGeoMagnetic = event.values;

                SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravity, floatGeoMagnetic);
                SensorManager.getOrientation(floatRotationMatrix, floatOrientation);

                imageView.setRotation((float) (-floatOrientation[0] * 180 / 3.14159));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sensorEventListenerAccelrometer, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListenerMagneticField, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onClick() {

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

    ;

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            int color;
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double m = Math.sqrt((x * x) + (y * y) + (z * z));

            xValueTextView.setText("X: " + String.format("%.2f", x) + " μT");
            yValueTextView.setText("Y: " + String.format("%.2f", y) + " μT");
            zValueTextView.setText("Z: " + String.format("%.2f", z) + " μT");
            mValueTextView.setText("M: " + String.format("%.2f", m) + " μT");

            if (m >= 100) {
                color = getResources().getColor(R.color.level11);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 100) {
                color = getResources().getColor(R.color.level10);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 90) {
                color = getResources().getColor(R.color.level9);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 85) {
                color = getResources().getColor(R.color.level8);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                ;
            }
            if (m < 80) {
                color = getResources().getColor(R.color.level7);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 75) {
                color = getResources().getColor(R.color.level6);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 70) {
                color = getResources().getColor(R.color.level5);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 65) {
                color = getResources().getColor(R.color.level4);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 60) {
                color = getResources().getColor(R.color.level3);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 55) {
                color = getResources().getColor(R.color.level2);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }
            if (m < 50) {
                color = getResources().getColor(R.color.level1);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
            }


            if (isRecording && event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {


                isRecording = true;

                // PUT new data
                Map<String, Object> user = new HashMap<>();

                user.put("TimeStamp", FieldValue.serverTimestamp());

                user.put("Microteslas", m);

                user.put("PhoneCoordsX", x);
                user.put("PhoneCoordsY", y);
                user.put("PhoneCoordsZ", z);


                // Add a new document with a generated ID
                db.collection("EdTest01")
                        .add(user)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                            }
                        });


                // Add the new code snippet here
                db.collection("EdTest01")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d(TAG, document.getId() + " => " + document.getData());
                                    }
                                } else {
                                    Log.w(TAG, "Error getting documents.", task.getException());
                                }
                            }
                        });
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

                    if (diffX < 0) {

                        onSwipeRight();
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
        Intent intent = new Intent(MainActivity2.this, MapActivity.class);
        startActivity(intent);

        overridePendingTransition(R.anim.transition2_1, R.anim.transition2_2);
    }

    private void onSwipeRight() {
        Toast.makeText(this, "Loading Data", Toast.LENGTH_SHORT).show();
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.black));

    }

    private void noSwipe() {
        Toast.makeText(this, "Loading Data", Toast.LENGTH_SHORT).show();
        getWindow().getDecorView().setBackgroundResource(R.drawable.gradient_background);

    }
}


