package org.wowtalk.ui.msg;

import org.wowtalk.Log;
import org.wowtalk.api.PrefUtil;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.SupportMapFragment;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-6-25
 * Time: 下午3:37
 * To change this template use File | Settings | File Templates.
 */
public class PickLocActivityWithAMap extends FragmentActivity implements AMapLocationListener, AMap.OnCameraChangeListener, View.OnClickListener {
    private AMap aMap;
    private LocationManagerProxy mAMapLocationManager;

    private boolean cameraMoveWithAnim=true;
    private int  cameraMoveAnimInterval=1000;

//    private TextView tvLocationIndicator;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.msg_activity_map_pick_loc_with_amap);

//        tvLocationIndicator=(TextView)findViewById(R.id.textView1);

        setUpMap();
        setupView();
    }

    private void setupView() {
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_done).setOnClickListener(this);

        Bundle data = this.getIntent().getExtras();
        if(null != data) {
            double lat = data.getDouble("target_lat");
            double lon = data.getDouble("target_lon");

            if(Math.abs(lat) > Double.MIN_NORMAL && Math.abs(lon) > Double.MIN_NORMAL) {
                changeCamera(lat, lon, null);
            } else {
                //move to last known position
                LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(null == location) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if(null != location) {
                    Log.d("last location known");
                    changeCamera(location.getLatitude(), location.getLongitude(), null);
                } else {
                    double latitude=getLastSavedLatitude();
                    double longitude=getLastSavedLongitude();
                    if(latitude != PrefUtil.DEF_UNUSED_LOCATION || longitude != PrefUtil.DEF_UNUSED_LOCATION) {
                        boolean savedFlag=cameraMoveWithAnim;
                        cameraMoveWithAnim=false;
                        changeCamera(latitude, longitude, null);
                        cameraMoveWithAnim=savedFlag;
                    }
                }
            }

            if(data.getBoolean("auto_loc")) {
                mAMapLocationManager.requestLocationUpdates(
                        LocationProviderProxy.AMapNetwork, 5000, 10, this);
            }

            if(data.getBoolean("hide_pick_buttons")) {
                this.findViewById(R.id.relativeLayout1).setVisibility(View.GONE);
            }
        }
    }

    private void setLocationIndicator(double latitude,double longitude) {
//        tvLocationIndicator.setText(latitude+":"+longitude);
    }

    private void setUpMap() {
        aMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        //require update
        mAMapLocationManager = LocationManagerProxy.getInstance(this);
        aMap.setOnCameraChangeListener(this);

        setLocationIndicator(aMap.getCameraPosition().target.latitude,aMap.getCameraPosition().target.longitude);
    }

    @Override
    public void onClick(View arg0) {
        if (arg0.getId() == R.id.btn_done) {
            if(null != aMap) {
                Intent data = new Intent();
                double lat = aMap.getCameraPosition().target.latitude;
                double lon = aMap.getCameraPosition().target.longitude;
                data.putExtra("target_lat", lat);
                data.putExtra("target_lon", lon);
                // HTTP request needed to get address, we do not want to wait for this, so leave
                // this task to the caller, however, we're kind enough to provide the getAddr() static method.
                this.setResult(RESULT_OK, data);
            }

            this.finish();
        } else if (arg0.getId() == R.id.btn_cancel) {
            finish();
        }
    }

    private void changeCamera(double latitude,double longitude, AMap.CancelableCallback callback) {
        CameraUpdate cameraUpdate= CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)).zoom(18).bearing(0).build());
        if (cameraMoveWithAnim) {
            aMap.animateCamera(cameraUpdate, cameraMoveAnimInterval, callback);
        } else {
            aMap.moveCamera(cameraUpdate);
        }
        addMarkAtLocation(latitude,longitude,false);
    }

    private long lastContinuesMarkSetTime;
    private final static long CONTINUES_MARK_SET_TIME_INTERVAL=200;
    private void addMarkAtLocation(double latitude,double longitude,boolean continues) {
        boolean setNewMark=true;
        if(continues) {
            if(System.currentTimeMillis()-lastContinuesMarkSetTime>=CONTINUES_MARK_SET_TIME_INTERVAL) {
                lastContinuesMarkSetTime=System.currentTimeMillis();
            } else {
                setNewMark=false;
            }
        }
        if(setNewMark) {
            aMap.clear();
            aMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title("Current Position")
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
        setLocationIndicator(latitude,longitude);
    }

    private double getLastSavedLatitude() {
        String latitude = PrefUtil.getInstance(this).getLastSavedLatitude();
        return Double.valueOf(latitude);
    }

    private double getLastSavedLongitude() {
        String longitude = PrefUtil.getInstance(this).getLastSavedLongitude();
        return Double.valueOf(longitude);
    }

    private void saveCurrentLocation(double latitude,double longitude) {
        PrefUtil prefUtil = PrefUtil.getInstance(this);
        prefUtil.setLastSavedLatitude(String.valueOf(latitude));
        prefUtil.setLastSavedLongitude(String.valueOf(longitude));
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        Log.d("location changed (" + aMapLocation.getLatitude() + "," + aMapLocation.getLongitude() + ")");
        mAMapLocationManager.removeUpdates(this);
        saveCurrentLocation(aMapLocation.getLatitude(), aMapLocation.getLongitude());
        changeCamera(aMapLocation.getLatitude(), aMapLocation.getLongitude(), null);
    }

    @Override
    public void onLocationChanged(Location location) {
        //Deprecated method
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onProviderEnabled(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onProviderDisabled(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
//        LogUtil.debug("camera change to ("+cameraPosition.target.latitude+","+cameraPosition.target.longitude+")");
        addMarkAtLocation(cameraPosition.target.latitude,cameraPosition.target.longitude,true);
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
//        LogUtil.debug("camera change finish ("+cameraPosition.target.latitude+","+cameraPosition.target.longitude+")");
        addMarkAtLocation(cameraPosition.target.latitude,cameraPosition.target.longitude,false);
    }
}
