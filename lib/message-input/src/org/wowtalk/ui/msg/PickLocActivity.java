/**
 * Pick a location on map.
 */
package org.wowtalk.ui.msg;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class PickLocActivity extends android.support.v4.app.FragmentActivity 
implements LocationListener, OnCameraChangeListener, OnClickListener {

	private GoogleMap mMap;
	private Marker mMarker;
	private LocationManager locationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.msg_activity_map_pick_loc);
		
		// get lat/lon (async)
		Bundle data = this.getIntent().getExtras();
		if(data != null) {
			double lat = data.getDouble("target_lat");
			double lon = data.getDouble("target_lon");

			if(Math.abs(lat) > Double.MIN_NORMAL && Math.abs(lon) > Double.MIN_NORMAL) {
				setTarget(lat, lon);
			}

			if(data.getBoolean("auto_loc")) {
				locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 
				Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); 
				if(location != null) {
					setTarget(location.getLatitude(), location.getLongitude());
				}
				try {
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 10, this);
					//locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null); // API LEVEL 9+
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if(data.getBoolean("hide_pick_buttons")) {
				this.findViewById(R.id.relativeLayout1).setVisibility(View.GONE);
			}
		}

		//View fragment1 = this.findViewById(R.id.map); // android.support.v4.app.NoSaveStateFrameLayout
		//android.app.Fragment fragment2 = this.getFragmentManager().findFragmentById(R.id.map); // null
		android.support.v4.app.Fragment fragment = this.getSupportFragmentManager().findFragmentById(R.id.map); // SupportMapFragment
		mMap = ((SupportMapFragment)fragment).getMap();
		
		// GooglePlayServices: Google Play Store is missing?
		if(mMap == null) {
			this.findViewById(R.id.relativeLayout1).setVisibility(View.GONE);
			
			AlertDialog dlg = new AlertDialog.Builder(this)
			.setMessage(R.string.msg_common_google_play_services_enable_text)
			.create();
			dlg.setCanceledOnTouchOutside(true);
			dlg.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					PickLocActivity.this.finish();
				}
			});
			dlg.show();
			return;
		}

		if(mMap != null) {
			mMap.setOnCameraChangeListener(this);
		}
		
		this.findViewById(R.id.btn_done).setOnClickListener(this);
		this.findViewById(R.id.btn_cancel).setOnClickListener(this);
	}

	private void setTarget(double lat, double lon) {
		if(mMap == null) return;
		CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(lat, lon));
		CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
		mMap.moveCamera(center);
		mMap.animateCamera(zoom);
	}

	@Override
	public void onLocationChanged(Location location) {
		setTarget(location.getLatitude(), location.getLongitude());
		locationManager.removeUpdates(this);
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}

	public static String getAddr(double lat, double lon){
		URL myFileUrl = null; 
		String jsonStr = null;
		String formatted_address = "......";
		
		try {
			myFileUrl = new URL("http://maps.google.com/maps/api/geocode/json?latlng="+lat+","+lon+"&sensor=true&language="+ Locale.getDefault().getLanguage());
		} catch (MalformedURLException e) {
			return null;
		}
		
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) myFileUrl.openConnection();
			conn.setDoInput(true);   
			conn.connect();   
			InputStream is = conn.getInputStream();
			jsonStr = inputStream2String(is);
			//Log.i("test", "addressJson="+jsonStr);
		} catch (IOException e) {
            e.printStackTrace();
		} catch (Exception e) {
            e.printStackTrace();
        }

        if (jsonStr != null) {
            try {
                JSONObject json=new JSONObject(jsonStr);
                JSONArray jsonArray=json.getJSONArray("results");
                JSONObject addressJson=jsonArray.getJSONObject(0);
                if (addressJson != null && addressJson.has("formatted_address")){
                    formatted_address=addressJson.getString("formatted_address");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

		return formatted_address;
	}

	private static String inputStream2String (InputStream in) throws IOException   {  

		StringBuffer out = new StringBuffer();
		byte[]  b = new byte[4096];
		int n;
		while ((n = in.read(b))!= -1){
			out.append(new String(b,0,n));
		}
		return  out.toString();
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {

		if(mMarker != null)
			mMarker.remove();
		
		mMarker = mMap.addMarker(new MarkerOptions()
		.position(arg0.target));
	}

	@Override
	public void onClick(View arg0) {
		if (arg0.getId() == R.id.btn_done) {
			if(mMap == null) {
				this.onBackPressed();
				return;
			}
			Intent data = new Intent();
			double lat = mMap.getCameraPosition().target.latitude;
			double lon = mMap.getCameraPosition().target.longitude;
			data.putExtra("target_lat", lat);
			data.putExtra("target_lon", lon);
			// HTTP request needed to get address, we do not want to wait for this, so leave
			// this task to the caller, however, we're kind enough to provide the getAddr() static method.
			this.setResult(RESULT_OK, data);
			this.finish();
		} else if (arg0.getId() == R.id.btn_cancel) {
			this.onBackPressed();
		}
	}
}
