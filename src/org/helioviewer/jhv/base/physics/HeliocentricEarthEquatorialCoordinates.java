package org.helioviewer.jhv.base.physics;

class HeliocentricEarthEquatorialCoordinates
{
    // the origin is at the center of the sun

    // solar rotation axis, positive direction to north pole
    public final double z;

    // points towards intersection of solar equator and solar central meridian
    // as seen from earth
    public final double x;

    // s.t. it forms an orthonormal basis with x and z
    public final double y;

    public HeliocentricEarthEquatorialCoordinates(double newX, double newY, double newZ)
    {
        x = newX;
        y = newY;
        z = newZ;
    }

    public HeliocentricEarthEquatorialCoordinates convertToHeliocentricEarthEquatorialCoordinates()
    {
        return this;
    }
}
