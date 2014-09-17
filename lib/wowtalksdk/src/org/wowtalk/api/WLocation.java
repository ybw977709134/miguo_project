package org.wowtalk.api;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/25/13
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class WLocation {

    public double latitude;

    public double longitude;

    public WLocation() {}

    public WLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public WLocation(WLocation location) {
        this.latitude = location.latitude;
        this.longitude = location.longitude;
    }
}
