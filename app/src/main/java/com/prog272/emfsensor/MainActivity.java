package com.prog272.emfsensor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

import androidx.core.view.GestureDetectorCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;


import android.animation.ObjectAnimator;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.Timer;
import java.util.TimerTask;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements SensorEventListener {

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
    private TextView xValueTextView, yValueTextView, zValueTextView, mValueTextView, hint;
    private GestureDetectorCompat gestureDetectorCompat;

    private boolean isRecording = false;
    private static final String TAG = "MainActivity";

    private Timer timer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noSwipe();

        // The image view for the compass
        imageView = findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.arrow);


        //Animations
        ImageView arrowImageView = findViewById(R.id.arrowImageView);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.arrow_animation);
        arrowImageView.startAnimation(animation);

        xValueTextView = findViewById(R.id.xValueTextView);
        yValueTextView = findViewById(R.id.yValueTextView);
        zValueTextView = findViewById(R.id.zValueTextView);
        mValueTextView = findViewById(R.id.mValueTextView);

        hint = findViewById(R.id.helpfulTip);

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
                if (timer == null) {
                    timer = new Timer(); // Reset the timer reference
                }
                Toast.makeText(MainActivity.this, "Recording Started!", Toast.LENGTH_SHORT).show();
            }
        });

        Button outputTwo = findViewById(R.id.submitButton);
        outputTwo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isRecording = false;
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                Toast.makeText(MainActivity.this, "Recording Stopped!", Toast.LENGTH_SHORT).show();
                ArrayList<String> data = readFromFile(MainActivity.this, "\\myData.txt");
                clearData(MainActivity.this, "\\myData.txt");
                System.out.println(data);
            }
        });



        SensorEventListener sensorEventListenerAccelrometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                floatGravity = event.values;

                SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravity, floatGeoMagnetic);
                SensorManager.getOrientation(floatRotationMatrix, floatOrientation);

                imageView.setRotation((float) (-floatOrientation[0]*180/3.14159));
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

                imageView.setRotation((float) (-floatOrientation[0]*180/3.14159));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sensorEventListenerAccelrometer, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListenerMagneticField, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onClick(){

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
    };

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRecording && event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {


            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double m = Math.sqrt((x * x) + (y * y) + (z * z));

            xValueTextView.setText("X: " + String.format("%.2f", x) + " μT");
            yValueTextView.setText("Y: " + String.format("%.2f", y) + " μT");
            zValueTextView.setText("Z: " + String.format("%.2f", z) + " μT");
            mValueTextView.setText("M: " + String.format("%.2f", m) + " μT");

            // Calculate the target Y position based on the mValueTextView value
            int targetY = (int) (m * 10);

            // Animate the arrow drawable to move up and down
            ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(R.id.arrowImageView), "translationY", targetY);
            animator.setDuration(100);
            animator.start();

            if (m >= 100) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level11));
            }
            if (m < 100) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level10));
            }
            if (m < 90) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level9));
            }
            if (m < 85) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level8));
            }
            if (m < 80) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level7));
            }
            if (m < 75) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level6));
            }
            if (m < 70) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level5));
            }
            if (m < 65) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level4));
            }
            if (m < 60) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level3));
            }
            if (m < 55) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level2));
            }
            if (m < 50) {
                mValueTextView.setTextColor(getResources().getColor(R.color.level1));
            }


            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    String Content = String.format("%s,%s,%s,%s,%s", FieldValue.serverTimestamp(), m, x, y, z);
                    writeToFile(MainActivity.this, "\\myData.txt", Content + "\n" );
                    System.out.println("recording");
                }
            };

            timer.schedule(task, 0, 5000);

//                isRecording = true;


            // PUT new data
//                Map<String, Object> user = new HashMap<>();
//
//                user.put("TimeStamp", FieldValue.serverTimestamp());
//
//                user.put("Microteslas", m);
//
//                user.put("PhoneCoordsX", x);
//                user.put("PhoneCoordsY", y);
//                user.put("PhoneCoordsZ", z);


            // Add a new document with a generated ID
//                db.collection("EdTest01")
//                        .add(user)
//                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                            @Override
//                            public void onSuccess(DocumentReference documentReference) {
//                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "Error adding document", e);
//                            }
//                        });


            // Add the new code snippet here
//                db.collection("EdTest01")
//                        .get()
//                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                if (task.isSuccessful()) {
//                                    for (QueryDocumentSnapshot document : task.getResult()) {
//                                        Log.d(TAG, document.getId() + " => " + document.getData());
//                                    }
//                                } else {
//                                    Log.w(TAG, "Error getting documents.", task.getException());
//                                }
//                            }
//                        });

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
                } else if (Math.abs(diffY) > Math.abs(diffX) &&
                        Math.abs(diffY) > SWIPE_THRESHOLD &&
                        Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffY > 0) {
                        onSwipeUp();
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

    public static void writeToFile(Context context, String fileName, String content) {
        try {

            String packageName = context.getPackageName();

            FileOutputStream fOut = new FileOutputStream(new File(context.getApplicationInfo().dataDir, fileName), true);


            fOut.write(content.getBytes());

            fOut.flush();
            fOut.close();


        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> readFromFile(Context context, String filename) {

        ArrayList<String> data = new ArrayList<>();

        try {
            File file = new File(context.getApplicationInfo().dataDir, filename);
            FileReader fileReader = new FileReader(file);

            // Create a BufferedReader to read text from the FileReader
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            // Read each line from the file
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                data.add(line);
            }
            System.out.println(data);
//            System.out.println(data.get(1));
            // Close the BufferedReader
            bufferedReader.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return data;
    }

    public static void clearData(Context context, String filename) {
        try {
            File file = new File(context.getApplicationInfo().dataDir, filename);
            FileWriter fileWriter = new FileWriter(file, false);

            // Write an empty string to the file
            fileWriter.write("");

            // Close the FileWriter
            fileWriter.close();

            System.out.println("File content deleted successfully.");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

//    public void startRecording(View MainActivity) {
//        Toast.makeText(this, "Recording Started!", Toast.LENGTH_SHORT).show();
//        writeToFile(MainActivity.this, "\\myData.txt", String.format("xCord,yCord,zCord,timestamp\n"));
//        readFromFile(MainActivity.this, "\\myData.txt", timer);
//    }

    private int currentBackgroundIndex = 0;
    private int[] backgroundResources = {R.drawable.gradient_background, R.drawable.darkest_gradient, R.drawable.black_background, R.drawable.lightest_gradient, R.drawable.black_gradient};

    private int currentTextColorIndex = 0;
    private int[] textColorResources = {R.color.white, R.color.white, R.color.white, R.color.black, R.color.white};

    private void onSwipeUp() {
        currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundResources.length;
        int backgroundResource = backgroundResources[currentBackgroundIndex];
        getWindow().getDecorView().setBackgroundResource(backgroundResource);

        currentTextColorIndex = (currentTextColorIndex + 1) % textColorResources.length;
        int textColorResource = textColorResources[currentTextColorIndex];

        mValueTextView.setTextColor(getResources().getColor(textColorResource));
        yValueTextView.setTextColor(getResources().getColor(textColorResource));
        zValueTextView.setTextColor(getResources().getColor(textColorResource));
        xValueTextView.setTextColor(getResources().getColor(textColorResource));
        hint.setTextColor(getResources().getColor(textColorResource));

        overridePendingTransition(R.anim.transition2_1, R.anim.transition2_2);
    }



    private void noSwipe() {
        Toast.makeText(this, "StartUp", Toast.LENGTH_SHORT).show();
        getWindow().getDecorView().setBackgroundResource(R.drawable.gradient_background);

        // Set default text color to white
        TextView mDefault = findViewById(R.id.mValueTextView);
        TextView xDefault = findViewById(R.id.xValueTextView);
        TextView zDefault = findViewById(R.id.zValueTextView);
        TextView yDefault = findViewById(R.id.yValueTextView);
        TextView hintDefault = findViewById(R.id.helpfulTip);
        mDefault.setTextColor(Color.WHITE);
        xDefault.setTextColor(Color.WHITE);
        yDefault.setTextColor(Color.WHITE);
        zDefault.setTextColor(Color.WHITE);
        hintDefault.setTextColor(Color.WHITE);
    }

}