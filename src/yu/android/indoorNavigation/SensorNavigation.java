package yu.android.indoorNavigation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

public class SensorNavigation extends Activity {

	final int SAMPLE_RATE = 1;	//unit: ms
	Context context = this;
	
	TextView textViewIndoorLatitude, textViewIndoorLongitude, trackingInfo;
	ImageView arrow;
	EditText alpha_input, threshold_input; 
	Dialog settingDialog;
	
	SensorManager sensorManager;
	SensorEventListener listener;
	SensorEventListener orientationListener;
	Sensor aSensor;
	Sensor mSensor;
	Sensor lSensor;
	
	//for reading sensor values
	float[] accelerometerValues = new float[3];
	float[] magneticFieldValues = new float[3];
	float[] linearValues = new float[3];
	float[] orientationValues = new float[3];
	int lastOrientation = 0;
	int currentOrientation = 0;
	float[] rotate = new float[9];
	
	//for low-pass filter
	boolean firstTime = true;
	float lowpass = 0.0f;
	float alpha = 0.1f;
	ArrayList<Float> a = new ArrayList<Float>();
	LinkedList<Float> delayList = new LinkedList<Float>();
	int delayNum = 10;
	float threshold = 1.0f;
	int i = 0;
	
	//for calculating lat and lon
	float latLon[] = new float[2];
	float distance = 0.0f;
	boolean isOrientationChanged = false;
	float stepLength = 1.0f;
	
	
	
	
	final Handler handler=new Handler();
	Runnable timer;
	
	FileOutputStream fos;
	
	Matrix matrix = new Matrix();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensornavigation);
		
		
		textViewIndoorLatitude = (TextView) findViewById(R.id.textViewIndoorLatitude);
		textViewIndoorLongitude = (TextView) findViewById(R.id.textViewIndoorLongitude);

		
		trackingInfo = (TextView) findViewById(R.id.trackingInfo);
		trackingInfo.setMovementMethod(new ScrollingMovementMethod());
		arrow = (ImageView) findViewById(R.id.arrow);
		
		LayoutInflater li = LayoutInflater.from(this);
		View dialogView = li.inflate(R.layout.setting_dialog, null);
		
		//get lat and lon from last activity
		latLon[0] = (float) Main.latitude;
		latLon[1] = (float) Main.longitude;
		
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context); 
		dialogBuilder.setView(dialogView);
		alpha_input = (EditText) dialogView.findViewById(R.id.alpha_input);
		threshold_input = (EditText) dialogView.findViewById(R.id.threshold_input);
		dialogBuilder.setTitle("Setting")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.e("info", "get here");
				
				if(alpha_input.getText().toString() != null){
					alpha = Float.parseFloat(alpha_input.getText().toString());
				}
				if(threshold_input.getText().toString() != null){
					threshold = Float.parseFloat(threshold_input.getText().toString());
				}

			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
				
			}
		});
		settingDialog = dialogBuilder.create();
		
		
		//get sensor manager and sensors
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		lSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

		//listener for linear accelerometer
		listener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				
				if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
					linearValues = event.values;
				}
				if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					accelerometerValues = event.values;
				}
				if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
					magneticFieldValues = event.values;
				}
				SensorManager.getRotationMatrix(rotate, null,
						accelerometerValues, magneticFieldValues);
				SensorManager.getOrientation(rotate, orientationValues);

			}
		};
		
		//listener for accelerometer and magnetic field sensor
		orientationListener = new SensorEventListener(){

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				
			}};
        
		//create a new timer to read the sensor value periodically
		timer=new Runnable() {
		    @Override
		    public void run() {	    	
		    	
		    	//Orientation
		    	int temOrientation = (int) Math.rint(Math.toDegrees(orientationValues[0]));
		    	if((Math.abs(temOrientation - currentOrientation)) > 5){
		    		isOrientationChanged = true;
		    		lastOrientation = currentOrientation;
		    		currentOrientation = temOrientation;
		    		//rotate the arrow
			    	arrow.setScaleType(ScaleType.MATRIX);
					matrix.postRotate(currentOrientation - lastOrientation,
							arrow.getWidth()/2, 
							arrow.getHeight()/2);
					arrow.setImageMatrix(matrix);
		    	}else{
		    		isOrientationChanged = false;
		    	}
		    	
		    	
		    	//use low pass filter to fliter the acceleration
		    	//float alpha = 0.02f/(0.02f*(1/cutoff));
		    	if (linearValues[1]>0.0f) linearValues[1] = 0.0f;
		    	if(firstTime){
		    		lowpass = linearValues[1];
		    		firstTime = false;
		    	}else{
		    		lowpass = lowpass + alpha*(linearValues[1] - lowpass);
		    	}
		    	a.add(lowpass);
		    	delayList.add(lowpass);
		    	if(i<delayNum){
		    		i++;		    		
		    	}else{
		    		float pre = delayList.poll();
		    		if(( pre - lowpass) > threshold){
		    			printNewLine("move toward "+ currentOrientation
		    					+ " with 1 meter");
		    			i = 0;
		    			delayList.clear();
		    			if(!isOrientationChanged){
		    				distance += stepLength;
		    			}else{
		    				float[] temLatLon = getLocation(latLon, lastOrientation, distance);
		    				latLon = temLatLon;
		    				distance = stepLength;
		    			}
		    		}
		    	}
				
		    	textViewIndoorLatitude.setText(Float.toString(latLon[0]));
		    	textViewIndoorLongitude.setText(Float.toString(latLon[1]));
				//redo this task 
				handler.postDelayed(this, SAMPLE_RATE);
				
				
		    	
		    	
		        
		    }
		};

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		sensorManager.registerListener(listener, aSensor,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(listener, mSensor,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(listener, lSensor,
				SensorManager.SENSOR_DELAY_FASTEST);
		handler.postDelayed(timer, SAMPLE_RATE);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		sensorManager.unregisterListener(listener);
		sensorManager.unregisterListener(orientationListener);		
		handler.removeCallbacks(timer); 
		if(isExternalStorageWritable()){
			File file = new File(Environment.getExternalStoragePublicDirectory(
		            Environment.DIRECTORY_DOWNLOADS), "indoorNavigation.txt");
			try {
				BufferedWriter bw= new BufferedWriter(new FileWriter(file));
				for(float data : a){
					bw.write(Float.toString(data)+"\n");
				}
				bw.flush();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem setting = menu.add("Settting");
		setting.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		setting.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		default:
			settingDialog.show();
			break;
		}
		return true;
	}
	
	/* Checks if external storage is available for read and write */
	private boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
//	private String getOrientation(double rotation){
//		if(rotation)
//	}
	
	private void printNewLine(String newLine){
		String pre = trackingInfo.getText().toString();
		trackingInfo.setText(newLine + "\n" + pre);
	}
	
	/**
	 * 
	 * @param preLocation[0]: latitude, preLocation[1]: longitude unit: degree
	 * @param azimuth unit: degree
	 * @param distance
	 * @return newLocation[0]: latitude, newLocation[1]: longitude
	 */
	private float[] getLocation(float[] preLocation, float azimuth, float distance){
		float[] newLocation = new float[2];
		newLocation[0] = (float) (preLocation[0] + (distance / (60 * (Math.cos(Math.toRadians(preLocation[0]))))) * Math.cos(Math.toRadians(360 - azimuth + 90)));
		newLocation[1] = (float) (preLocation[0] + (distance / 60) * Math.sin(Math.toRadians(360 - azimuth + 90)));
		return newLocation;
	}

}
