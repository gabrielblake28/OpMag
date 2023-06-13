package com.prog272.emfsensor;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyOpenGLRenderer implements GLSurfaceView.Renderer {

    public Square[] drawList;
    public Square[] localData;

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private Timer timer = new Timer();
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            bIsYouAreHereShowing = !bIsYouAreHereShowing;
        }
    };
    private Boolean bIsYouAreHereShowing = true;
    private Square youAreHereSquare;

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        localData = loadDummyData();
        drawList = new Square[localData.length];

        for(int i = 0; i < localData.length; i++){
            drawList[i] = localData[i];
        }

        timer.scheduleAtFixedRate(timerTask, 0L, 1000L);
        youAreHereSquare = new Square(0f, 0f, 0.1f, new float[]{100f, 100f, 100f, 0f});


    }

    private Square[] loadDummyData(){
        final int RANGE = 20;
        final int COUNT = 400;
        final float SCALE = 0.1f;
        final float ORIGIN = 1.0f;
        Square[] sqrs = new Square[COUNT];

        for(int i = 0; i < COUNT; i++){
            sqrs[i] = new Square(
                    -1.0f + 0.1f*(i%RANGE),
                    1.0f - 0.1f*(Math.floorDiv(i, RANGE)),
                    SCALE,
                    new float[]{0.1f,
                                0.1f,
                                0.1f,
                                0.0f
                    }
            );
        }
        return sqrs;
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // Draw shapes

         for(int i = 0; i < drawList.length; i++){
                    drawList[i].t1.draw(vPMatrix);
                    drawList[i].t2.draw(vPMatrix);
                }
         // Draw the you are here location
        if(bIsYouAreHereShowing){
            youAreHereSquare.t1.draw(vPMatrix);
            youAreHereSquare.t2.draw(vPMatrix);
        }


    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}
