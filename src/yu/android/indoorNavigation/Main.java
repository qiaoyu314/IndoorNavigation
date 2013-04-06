package yu.android.indoorNavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {
	
	TextView tv1, tv2;
	Button bt1;
	LocationManager locationManager;
	Criteria criteria;
	LocationListener locationListener;
	Dialog gpsSetting;
	
	public static double latitude;
	public static double longitude;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tv1 = (TextView)findViewById(R.id.tv1);
        tv2 = (TextView)findViewById(R.id.tv2);
        bt1 = (Button)findViewById(R.id.bt1);
          
        
        
        //set click listener for the button
        bt1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				beginSensorNav();
				
			}
		});
        
        //create an alert dialog
        gpsSetting = new AlertDialog.Builder(this)
        .setTitle("GPS is disabled")
        .setMessage("Goto GPS setting?")
        .setPositiveButton("OK", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				//goto GPS setting
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivityForResult(intent, 0);
			}} )
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// exit
				finish();
			}
		})
        .create();
    }
    
    
    
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		openGPSSettings(); 
	}




	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		locationManager.removeUpdates(locationListener);
	}



	/**
     * Initialize GPS
     */
    private void openGPSSettings() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        //set criterial
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(true);//允许产生开销
        criteria.setPowerRequirement(Criteria.POWER_LOW);//消耗大的话，获取的频率高
        criteria.setSpeedRequired(true);//手机位置移动
        criteria.setAltitudeRequired(false);//海拔
        //get provider
        String provider = locationManager.getBestProvider(criteria, false);
        
        
        //if there is no provider, exit
        if(provider == null){
        	Toast.makeText(this, "No GPS Provider available.", Toast.LENGTH_SHORT);
        	return;
        }
        //check if GPS is allowed
        if (locationManager.isProviderEnabled(provider)) {
            Toast.makeText(this, "GPS is working well.", Toast.LENGTH_SHORT)
                    .show();
           
		} else {
			gpsSetting.show();
		}
        
        tv1.setText("Searching GPS...");
        tv2.setText("GPS status:");
        locationListener = new LocationListener(){

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				updateToNewLocation(location);
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				tv1.setText("Provider is enabled");
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				switch (status){
				case LocationProvider.OUT_OF_SERVICE:
					tv2.setText("GPS is out of service");
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					tv2.setText("GPS is temporarily unavailable");
					break;
				}
				
			}
        	
        };
//        updateToNewLocation(locationManager.getLastKnownLocation(provider));
        locationManager.requestLocationUpdates(provider, 1 * 1000, 1, locationListener);
    }
    
    
    
    /**
     * Update the latitude and longitude using GPS
     * @param location
     */
    private void updateToNewLocation(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude= location.getLongitude();
            tv1.setText("Latitude：" +  latitude+ "\nLongitude" + longitude);
        } else {
            tv1.setText("Unable to get GEO info!");
        }

    }
    
    /**
     * Goto to sensor navigation page
     */
    private void beginSensorNav(){
    	startActivity(new Intent("yu.android.indoorNavigation.SENSORNAVIGATION"));
    	finish();
    }
    
    
    
}