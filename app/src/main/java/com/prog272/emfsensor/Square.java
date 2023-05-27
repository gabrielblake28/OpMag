package com.prog272.emfsensor;


public class Square {

    public Triangle t1;
    public Triangle t2;

    public Square(float locX, float locY, float scale, float color[]){

        float[] t1Coords;
        float[] t2Coords;

        t1Coords = new float[] {locX, locY, 0,                  // top left
                                locX, locY - scale, 0,          // bottom left
                                locX + scale, locY - scale, 0}; // bottom right

        t2Coords = new float[] {locX, locY, 0,                  // top left
                                locX + scale, locY, 0,          // top right
                                locX + scale, locY - scale, 0}; // bottom right

        t1 = new Triangle(t1Coords, color);
        t2 = new Triangle(t2Coords, color);
    }
}
