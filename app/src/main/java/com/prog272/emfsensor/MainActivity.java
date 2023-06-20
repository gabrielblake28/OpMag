package com.prog272.emfsensor;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
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



import androidx.core.content.ContextCompat;
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


public class MainActivity extends Activity implements SensorEventListener {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static int[] colorMatrix = new int[400];

    // Used for the compass
    private ImageView imageView;
    private ImageView arrowView;
    private Sensor sensorAccelerometer;
    private float[] floatGravity = new float[3];
    private float[] floatGeoMagnetic = new float[3];
    private float[] floatOrientation = new float[3];
    private float[] floatRotationMatrix = new float[9];

    private SensorManager sensorManager;
    private Sensor magneticFieldSensor;
    private TextView xValueTextView, yValueTextView, zValueTextView, mValueTextView, hint;
    private GestureDetectorCompat gestureDetectorCompat;

    private float currentRotationAngle = 0f;


    private boolean isRecording = false;
    private float m;
    private float x;
    private float y;
    private float z;
    private boolean isArrowUp = false;

    private static final String TAG = "MainActivity";

    private Timer timer = new Timer();
    private Timer viewTimer = new Timer();

    private MediaPlayer mediaPlayer;

    private int magLevel = 0;
    private int prevMagLevel = 0; // mag levels are used to trigger sound and set the color based on magnitude.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noSwipe();

        GLSurfaceView glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new MyOpenGLRenderer());
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // The image view for the compass
        imageView = findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.arrow);

        int startingColor = ContextCompat.getColor(this, R.color.teal_200);


        imageView.setColorFilter(startingColor);




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

        //record button
        Button output = findViewById(R.id.recordButton);
        output.setOnClickListener(new OnClickListener() {
            @Override

            //record button if hit twice
            public void onClick(View view) {
                if (isRecording) {
                    isRecording = false;
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    Toast.makeText(MainActivity.this, "Recording Stopped!", Toast.LENGTH_SHORT).show();
                    ArrayList<String> data = readFromFile(MainActivity.this, "\\myData.txt");
                    clearData(MainActivity.this, "\\myData.txt");
                    System.out.println(data);

                    //record button if hit once
                } else {
                    isRecording = true;
                    if (timer == null) {
                        timer = new Timer(); // Reset the timer reference
                    }
                    Toast.makeText(MainActivity.this, "Recording Started!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //submit button
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

        TimerTask dataViewTask = new TimerTask() {
            @Override
            public void run() {
                colorMatrix[0] = magLevel;
                for(int i = 399; i > 0; i--){
                    colorMatrix[i] = colorMatrix[i-1];
                }
            }
        };
        viewTimer.schedule(dataViewTask, 0l, 100l);

        mediaPlayer = MediaPlayer.create(this, R.raw.beep);

        TimerTask playSoundTask = new TimerTask() {
            @Override
            public void run() {
                if((magLevel > prevMagLevel || magLevel > 9) && magLevel > 2){
                    mediaPlayer.start();
                }
                prevMagLevel = magLevel;
            }
        };
        viewTimer.schedule(playSoundTask, 0l, 100l);

        //Moved Gabriel's code here
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(isRecording){
                    String Content = String.format("%s,%s,%s,%s,%s", FieldValue.serverTimestamp(), m, x, y, z);
                    writeToFile(MainActivity.this, "\\myData.txt", Content + "\n" );
                    System.out.println("recording");
                }

            }
        };

        timer.schedule(task, 0, 5000);


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
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            int color;

            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            double m = Math.sqrt((x * x) + (y * y) + (z * z));



            xValueTextView.setText("X: " + String.format("%.2f", x) + " μT");
            yValueTextView.setText("Y: " + String.format("%.2f", y) + " μT");
            zValueTextView.setText("Z: " + String.format("%.2f", z) + " μT");
            mValueTextView.setText("M: " + String.format("%.2f", m) + " μT");

            // Calculate the target Y position based on the mValueTextView value
            int targetY = (int) (m * -5);

            //if magnetism measurement goes over 200 and arrow goes off screen

            // Inside the if (m > 200) condition
            if (m > 200) {
                // Calculate the target Y position to move the arrow down
                int targetYCapacity = 500; // Adjust this value based on your desired position

                // Animate the arrow drawable to move down
                ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(R.id.arrowImageView), "translationY", targetYCapacity);
                animator.setDuration(1000); // Adjust the duration as needed
                animator.start();


                // Calculate the target rotation angle based on the floatOrientation value
                float targetRotationAngle = (float) (-floatOrientation[0] * 180 / Math.PI);

                // Calculate the difference between the target angle and current angle
                float rotationDiff = targetRotationAngle - currentRotationAngle;

                // Normalize the rotation difference to the range -180 to 180 degrees
                if (rotationDiff > 180) {
                    rotationDiff -= 360;
                } else if (rotationDiff < -180) {
                    rotationDiff += 360;
                }

                // Calculate the final rotation angle by adding the rotation difference to the current angle
                float finalRotationAngle = currentRotationAngle + rotationDiff;

                // Create a ValueAnimator to smoothly animate the rotation
                ValueAnimator animatorCompassFloat = ValueAnimator.ofFloat(currentRotationAngle, finalRotationAngle);
                animatorCompassFloat.setDuration(50); // Adjust the duration as needed

                animatorCompassFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float value = (float) valueAnimator.getAnimatedValue();
                        imageView.setRotation(value);
                    }
                });

                animatorCompassFloat.start();

                // Update the current rotation angle
                currentRotationAngle = finalRotationAngle;
            }

            // Animate the arrow drawable to move up and down
            ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(R.id.arrowImageView), "translationY", targetY);
            animator.setDuration(100);

            // Add a listener to detect when the animation reaches the target y-axis position
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    // Get the current y-axis translation
                    float currentY = findViewById(R.id.arrowImageView).getTranslationY();

                    // Check if the current y-axis position is greater than or equal to 300
                    if (currentY >= 300) {
                        // Reverse the direction by multiplying the targetY by -1
                        int reverseTargetY = (int) (targetY * -1);

                        // Animate the arrow drawable to move down
                        ObjectAnimator reverseAnimator = ObjectAnimator.ofFloat(findViewById(R.id.arrowImageView), "translationY", reverseTargetY);
                        reverseAnimator.setDuration(1000); // Increase the duration to slow down the animation
                        reverseAnimator.start();

                        isArrowUp = false; // Set the flag to indicate that the arrow is down
                    } else {
                        isArrowUp = true; // Set the flag to indicate that the arrow is up
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });

            animator.start();


            if (m >= 100) {
                color = getResources().getColor(R.color.level11);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 11;
            }
            if (m < 100) {
                color = getResources().getColor(R.color.level10);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 10;
            }
            if (m < 90) {
                color = getResources().getColor(R.color.level9);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 9;
            }
            if (m < 85) {
                color = getResources().getColor(R.color.level8);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 8;
            }
            if (m < 80) {
                color = getResources().getColor(R.color.level7);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 7;
            }
            if (m < 75) {
                color = getResources().getColor(R.color.level6);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 6;
            }
            if (m < 70) {
                color = getResources().getColor(R.color.level5);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 5;
            }
            if (m < 65) {
                color = getResources().getColor(R.color.level4);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 4;
            }
            if (m < 60) {
                color = getResources().getColor(R.color.level3);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 3;
            }
            if (m < 55) {
                color = getResources().getColor(R.color.level2);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 2;
            }
            if (m < 50) {
                color = getResources().getColor(R.color.level1);
                mValueTextView.setTextColor(color);
                imageView.setColorFilter(color);
                magLevel = 1;
            }

            if(isRecording){
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        String Content = String.format("%s,%s,%s,%s,%s", FieldValue.serverTimestamp(), m, x, y, z);
                        writeToFile(MainActivity.this, "\\myData.txt", Content + "\n" );
                        System.out.println("recording");
                    }
                };


                timer.schedule(task, 0, 5000);
            }


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
//        Toast.makeText(this, "Loading Data", Toast.LENGTH_SHORT).show();
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
    private int[] backgroundResources = {R.drawable.gradient_background, R.drawable.darkest_gradient, R.drawable.black_background, R.drawable.lightest_gradient, R. drawable.splash_background, R.drawable.cyan_background, R.drawable.black_gradient};

    private int currentTextColorIndex = 0;
    private int[] textColorResources = {R.color.white, R.color.white, R.color.white, R.color.black, R.color.white, R.color.black, R.color.white};

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

        if (currentBackgroundIndex == 3) {
            onSwipeUpGreen();
        }

        if (currentBackgroundIndex != 3) {
            onSwipeUpRegular();
        }

    }

    private void onSwipeUpGreen() {


        // The image view for the arrow
        arrowView = findViewById(R.id.arrowImageView);
        arrowView.setImageResource(R.drawable.greenarrowleftagain);
        int startingColorArrow = ContextCompat.getColor(this, R.color.black);
        arrowView.setColorFilter(startingColorArrow);

        // The image view for the compass
        imageView = findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.arrow);
        int startingColor = ContextCompat.getColor(this, R.color.black);
        imageView.setColorFilter(startingColor);
    }

    private void onSwipeUpRegular() {


        // The image view for the arrow
        arrowView = findViewById(R.id.arrowImageView);
        arrowView.setImageResource(R.drawable.greenarrowleftagain);
        int startingColorArrow = ContextCompat.getColor(this, R.color.level1);
        arrowView.setColorFilter(startingColorArrow);

        // The image view for the compass
        imageView = findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.arrow);
        int startingColor = ContextCompat.getColor(this, R.color.teal_200);
        imageView.setColorFilter(startingColor);
    }






    private void noSwipe() {
//        Toast.makeText(this, "StartUp", Toast.LENGTH_SHORT).show();
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

    public static int[] getColorMatrix() {
        return colorMatrix;
    }

}