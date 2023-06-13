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
                TextView message = findViewById(R.id.message);
                message.setText("Recording");


            }
        });

        Button outputTwo = findViewById(R.id.stopButton);
        outputTwo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isRecording = false;
                TextView message = findViewById(R.id.messageTwo);
                message.setText("Stopped");


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

}