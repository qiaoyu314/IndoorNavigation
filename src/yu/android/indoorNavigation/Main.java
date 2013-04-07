package yu.android.indoorNavigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {

	Context context = this;
	TextView textViewLatitude, textViewLongitude;
	ImageButton buttonNext;
	LocationManager locationManager;
	Criteria criteria;
	LocationListener locationListener;
	GpsStatus.Listener gpsStatusListener;
	Dialog gpsSetting;

	public static double latitude;
	public static double longitude;

	long lastLocationTime;
	boolean isGpsFixed = false;
	Location lastLocation;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Layout
		setContentView(R.layout.main);
		textViewLatitude = (TextView) findViewById(R.id.textViewLatitude);
		textViewLongitude = (TextView) findViewById(R.id.textViewLongitude);
		buttonNext = (ImageButton) findViewById(R.id.buttonNext);

		// Set click listener for the button
		buttonNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				beginSensorNav();

			}
		});

		// create an alert dialog
		gpsSetting = new AlertDialog.Builder(this)
				.setTitle("GPS is disabled")
				.setMessage("Goto GPS setting?")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// goto GPS setting
						Intent intent = new Intent(
								Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivityForResult(intent, 0);
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// exit
								finish();
							}
						}).create();

		gpsStatusListener = new GpsStatus.Listener() {

			@Override
			public void onGpsStatusChanged(int event) {
				// TODO Auto-generated method stub
				switch (event) {
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					if (lastLocation != null)
						isGpsFixed = (SystemClock.elapsedRealtime() - lastLocationTime) < 3000;

					if (isGpsFixed) { // A fix has been acquired.
						// Do something.
					} else { // The fix has been lost.
						// Do something.
						Toast.makeText(context, "GPS signal is lost", Toast.LENGTH_LONG);
					}

					break;
				case GpsStatus.GPS_EVENT_FIRST_FIX:
					// Do something.
					isGpsFixed = true;

					break;
				}
			}
		};
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
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// set criterial
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setCostAllowed(true);// 允许产生开销
		criteria.setPowerRequirement(Criteria.POWER_LOW);// 消耗大的话，获取的频率高
		criteria.setSpeedRequired(true);// 手机位置移动
		criteria.setAltitudeRequired(false);// 海拔
		// get provider
		String provider = locationManager.getBestProvider(criteria, false);

		// if there is no provider, exit
		if (provider == null) {
			Toast.makeText(this, "No GPS Provider available.",
					Toast.LENGTH_SHORT);
			return;
		}
		// check if GPS is allowed
		if (locationManager.isProviderEnabled(provider)) {
			Toast.makeText(this, "GPS is working well.", Toast.LENGTH_SHORT)
					.show();

		} else {
			gpsSetting.show();
		}

		textViewLatitude.setText("...");
		textViewLongitude.setText("...");
		locationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				lastLocation = location;
				lastLocationTime = SystemClock.elapsedRealtime();
				updateToNewLocation(location);
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				textViewLatitude.setText("Provider is enabled");
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				switch (status) {
				case LocationProvider.OUT_OF_SERVICE:
					textViewLongitude.setText("GPS is out of service");
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					textViewLongitude.setText("GPS is temporarily unavailable");
					break;
				}

			}

		};
		// updateToNewLocation(locationManager.getLastKnownLocation(provider));
		locationManager.requestLocationUpdates(provider, 1 * 1000, 1,
				locationListener);
		locationManager.addGpsStatusListener(gpsStatusListener);
	}

	/**
	 * Update the latitude and longitude using GPS
	 * 
	 * @param location
	 */
	private void updateToNewLocation(Location location) {
		if (location != null) {
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			textViewLatitude.setText(Double.toString(latitude));
			textViewLongitude.setText(Double.toString(longitude));
		} else {
			textViewLatitude.setText("Unable to get GEO info!");
		}

	}

	/**
	 * Goto to sensor navigation page
	 */
	private void beginSensorNav() {
		startActivity(new Intent("yu.android.indoorNavigation.SENSORNAVIGATION"));
		finish();
	}

}