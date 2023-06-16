package com.prog272.emfsensor;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyOpenGLRenderer implements GLSurfaceView.Renderer {

    public Square[] drawList;
    public Square[] localData;

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        localData = loadDummyData();
        drawList = new Square[localData.length];

        for(int i = 0; i < localData.length; i++){
            drawList[i] = localData[i];
        }


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

        // Set the colors for each shape
        int[] colorMatrix = MainActivity.getColorMatrix();

        // Draw shapes

         for(int i = 0; i < drawList.length; i++){

             float[] color;

             switch (colorMatrix[i]) {
                 case 1:
                     color = new float[]{0f, 1.0f, 0f, 1f};
                     break;
                 case 2:
                     color = new float[]{0.1f, 0.9f, 0f, 1f};
                     break;
                 case 3:
                     color = new float[]{0.2f, 0.8f, 0f, 1f};
                     break;
                 case 4:
                     color = new float[]{0.3f, 0.7f, 0f, 1f};
                     break;
                 case 5:
                     color = new float[]{0.4f, 0.6f, 0f, 1f};
                     break;
                 case 6:
                     color = new float[]{0.5f, 0.5f, 0f, 1f};
                     break;
                 case 7:
                     color = new float[]{0.6f, 0.4f, 0f, 1f};
                     break;
                 case 8:
                     color = new float[]{0.7f, 0.3f, 0f, 1f};
                     break;
                 case 9:
                     color = new float[]{0.8f, 0.2f, 0f, 1f};
                     break;
                 case 10:
                     color = new float[]{0.9f, 0.1f, 0f, 1f};
                     break;
                 case 11:
                     color = new float[]{1.0f, 0f, 0f, 1f};
                     break;
                 default:
                     color = new float[]{0.0f, 0f, 0f, 1f};
                     break;
             }

            drawList[i].t1.Color = color;
            drawList[i].t2.Color = color;
            drawList[i].t1.draw(vPMatrix);
            drawList[i].t2.draw(vPMatrix);

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
