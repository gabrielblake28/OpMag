package com.prog272.emfsensor;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GestureDetectorCompat;
import java.util.Timer;
import java.util.TimerTask;
import android.Manifest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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

public class MainActivity extends Activity implements SensorEventListener, LocationListener {

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

    private boolean isRecording = false;
    private static final String TAG = "MainActivity";
    private Timer timer;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private LocationManager locationManager;

    private double originX;
    private double originY;
    private TextView coordinatesTextView;
    private long lastUpdate;
    private float lastX, lastY, lastZ;
    private static final int SHAKE_THRESHOLD = 10;
    private float locX = 0f;
    private float locY = 0f;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GLSurfaceView glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new MyOpenGLRenderer());
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // The image view for the compass
        imageView = findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.arrow);

        //Get the location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        coordinatesTextView = findViewById(R.id.coordinatesTextView);

        xValueTextView = findViewById(R.id.xValueTextView);
        yValueTextView = findViewById(R.id.yValueTextView);
        zValueTextView = findViewById(R.id.zValueTextView);
        mValueTextView = findViewById(R.id.mValueTextView);


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

        Button outputTwo = findViewById(R.id.stopButton);
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }

        SensorEventListener sensorEventListenerAccelrometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastUpdate) > 100) {
                        long timeDiff = (currentTime - lastUpdate);
                        lastUpdate = currentTime;

                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        float acceleration = Math.abs(x + y - lastX - lastY) / timeDiff * 10000;

                        if (acceleration > SHAKE_THRESHOLD) {
                            float diffX = x - lastX;
                            float diffY = y - lastY;
                            locX += (diffX) / timeDiff * 1000;
                            locY += (diffY) / timeDiff * 1000;

                            coordinatesTextView.setText("x: " + locX + "\ny: " + locY);
                        }

                        lastX = x;
                        lastY = y;
                        lastZ = z;
                    }
                }

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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1L, 1f, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        if(originX == 0){
            originX = longitude;
            originY = latitude;
        }


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
                imageView.setColorFilter(color);;
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


// <<<<<<< JoesBranch
//             if (isRecording && event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {


                isRecording = true;
// =======
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

}