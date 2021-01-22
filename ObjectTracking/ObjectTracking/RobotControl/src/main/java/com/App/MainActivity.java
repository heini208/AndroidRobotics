//*******************************************************************
/**
 @file   MainActivity.java
 @author Thomas Breuer
 @date   08.11.2020
 @brief
 **/

//*******************************************************************
package com.App;

//*******************************************************************
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.App.Bluetooth.BluetoothDeviceListActivity;
import com.App.ORB.ORB;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;



//*******************************************************************
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{

	//---------------------------------------------------------------
	private Menu        menuLocal;
	private ORB         orb;
    private Handler     msgHandler;

    private final int ORB_REQUEST_CODE         = 0;
	private final int ORB_DATA_RECEIVED_MSG_ID = 999;

	//---------------------------------------------------------------

	//OpenCV
	JavaCameraView javaCameraView;
	private static String TAG = "MainActivityCV";
	private Mat mRgba;
	private Mat mHSV;
	private Mat mThresholded;
	private Mat mThresholded2;
	private Mat mHSV2;



	//RobotConrol
	private double centerX =0;
	private double centerY =0;
	private boolean toggle = false;
	private boolean ymode = false;
	private int direction = 0;
	private int distance =0;

	private boolean ymodeLost = false;
	private int angle =0;
	private int speed = 255;
	private int servoSpeed = speed;



	public MainActivity()
	{
		// onCameraFrame neue main
		msgHandler = new Handler()
		{
			@Override
			public void handleMessage( Message msg )
			{
				switch( msg.what )
				{
					case ORB_DATA_RECEIVED_MSG_ID:
						setMsg();
						break;
					default:
				}
				super.handleMessage( msg );
			}
		};
    }

	BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status){
				case BaseLoaderCallback.SUCCESS: {

					javaCameraView.enableView();
					break;
				}
				default:{
					super.onManagerConnected(status);
					break;
				}
			}

		}
	};

	//---------------------------------------------------------------
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);


		orb     = new ORB();
		orb.init(this, msgHandler, ORB_DATA_RECEIVED_MSG_ID );
        orb.configMotor(0, 144, 50, 50, 30);
        orb.configMotor(1, 144, 50, 50, 30);

        //Sensor
		//orb.configSensor(0,4,0,0);
		orb.setModelServo(0,servoSpeed,0);
        //openCV
		javaCameraView = (JavaCameraView) findViewById(R.id.camera_view);
		javaCameraView.setCameraIndex(0);


		javaCameraView.setCvCameraViewListener(this);
		javaCameraView.enableView();
	}
	//Open CV
	@Override
	public void onCameraViewStarted(int width, int height) {

		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mHSV = new Mat(height, width, CvType.CV_8UC4);
		mThresholded=new Mat(height,width,CvType.CV_8UC1);
		mThresholded2=new Mat(height,width,CvType.CV_8UC1);



	}




	@Override
	public void onCameraViewStopped() {
		mRgba.release();
	}


	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		//orb.configSensor(0,4,0,0);
		//System.out.println("SensorValue: "+ orb.getSensorValue(0)+ " ," +orb.getSensorValueDigital(0,0)+ " , "+ orb.getSensorValueDigital(0,1) +" , "+orb.getSensorValueAnalog(0,0));




		//Kamerabild
		mRgba = inputFrame.rgba();

		//hsv Grenzbereiche
		Scalar hsv_min = new Scalar(0,100,40);
		Scalar hsv_max = new Scalar(6,255,255);
		Scalar hsv_min2 = new Scalar(175,100,40);
		Scalar hsv_max2 = new Scalar(180,255,255);

		//Kamerabild soll nur Farben im HSV bereich wahrnehmen
		Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV,4);
		Core.inRange(mHSV, hsv_min, hsv_max, mThresholded);
		Core.inRange(mHSV, hsv_min2, hsv_max2, mThresholded2);
		Core.bitwise_or(mThresholded, mThresholded2, mThresholded);




		//AN AUS Schalter
		Switch sw = (Switch) findViewById(R.id.switch1);

		sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				if (isChecked) {
					// The toggle is enabled
					toggle = true;




				} else {
					// The toggle is disabled
					toggle = false;
				}
			}
		});

		ToggleButton ymodeV = (ToggleButton) findViewById(R.id.toggleButton);
		ymodeV.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					ymode = true;
					tooFar = 150000;
				} else {
					ymode = false;
					tooClose = 250000;
					tooFar = 200000;
				}
			}
		});



		//Auswertung des Kamerabilds
		if ( toggle == true) {

			//rot im Bild suchen und durschnitt berechnen
			Mat locations = Mat.zeros(mThresholded.size(),mThresholded.channels());
			Core.findNonZero(mThresholded,locations);
			MatOfPoint density = new MatOfPoint(locations);
			double x = 0;
			double  y = 0;
			Point[] densityArray = density.toArray();

			//entfernung abmessen
			distance = densityArray.length;

			//x-y wert des Punktes berechnen
			//&& densityArray.length < 250000
			if( densityArray.length > 6000 ) {
				for (int i = 0; i < densityArray.length; i++) {
					x += densityArray[i].x;
					y += densityArray[i].y;
				}

				//Kamera ist 90grad gedreht daher x=y und y=x
				ymodeLost = false;

				centerY = x / densityArray.length;
				centerX = y / densityArray.length;
			}else{
				ymodeLost = true;
				if ( centerX > 240 && centerX < 480 ) {
					centerX = 100;
				}

			}




			System.out.println("Ymean= " + y  + "densitylength= " + densityArray.length + "CENTER:= " + centerX);
			System.out.println("Y-Wert:" + centerY);
			//Punkt bei centerX Platzieren ( CenterX ist der durschnitts X Wert aller roten Pixel)
			Imgproc.circle(mThresholded, new Point(centerY,centerX), 3, new Scalar(0, 255, 0), 5);

			if (ymode== true){
				ymodeObjectFollowing();
			}else {
				objectFollowing();
			}

		}else {
				orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
				orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
			}


			//Kamerabild drehen und auf Handy Anpassen damit es auf dem display richtig rum angezeigt wird
			mHSV2 = mThresholded.t();
			Core.flip(mThresholded.t(), mHSV2, 1);
			Imgproc.resize(mHSV2, mHSV2, mThresholded.size());
			mThresholded.release();

			return  mHSV2;

	}

	private int ankathete = 50000;
	private int ankatheteClose = 150000;
	private int marginbot = 300;
	private int margintop =660;
	private double alpha = 0;

	public void ymodeObjectFollowing(){
		double motorspeed = 500;

		if(ymodeLost== true){
			motorspeed = motorspeed/2;
		}



			//Y-verfolgung
		if (centerY <= marginbot) {
			servoSpeed = speed;

			angle -= 3;
			if(angle <0){angle =0;}

			orb.setModelServo(0, servoSpeed, angle);

		} else if (centerY >= margintop) {
			servoSpeed = speed;

			angle += 3;
			if(angle >60){angle =60;}

			orb.setModelServo(0, servoSpeed, angle);

		} else {
			orb.setModelServo(0, 0, 0);
			int tooCloseTemp = tooClose;
			int tooFarTemp = tooFar;

			//berechnung des tatsächlichen abstands unabängig vom Winkel
			alpha = angle/100;
			tooFarTemp = (int)(ankathete/Math.cos(alpha));

			tooCloseTemp = (int)(ankatheteClose/Math.cos(alpha));

			//umrechnung, da höhere distanz = weniger Pixeldichte bedeutet
			tooFar = tooFar + (tooFar-tooFarTemp);
			tooClose = tooClose + (tooClose - tooCloseTemp);

			System.out.println(tooClose+ " too far: " + tooFar + "angle: " + angle);

		}
		objectFollowing();





	}

	private int tooClose = 250000;
	private int tooFar = 200000;
	public void objectFollowing(){
		int speed = 500;
		if (distance >=tooClose) {
			// driving Backwards
			System.out.println("driving Backwards!");
			orb.setMotor(0, ORB.Mode.SPEED, 700, 0);
			orb.setMotor(1, ORB.Mode.SPEED, -700, 0);
		}else if (distance > 6000 && distance< tooFar) {
			if (centerX > 580) {
				// drive left when further away
				System.out.println("driving right!");
				orb.setMotor(0, ORB.Mode.SPEED, -300, 0);
				orb.setMotor(1, ORB.Mode.SPEED, -300, 0);

			}else if (centerX < 140) {
				// drive right when further away
				System.out.println("driving left!");
				orb.setMotor(0, ORB.Mode.SPEED, +300, 0);
				orb.setMotor(1, ORB.Mode.SPEED, +300, 0);
			}else{
				// driving Foward
				System.out.println("driving Forward!");
				orb.setMotor(0, ORB.Mode.SPEED, -700, 0);
				orb.setMotor(1, ORB.Mode.SPEED, 700, 0);
			}
		}else if (centerX > 480) {
			// drive left
			System.out.println("driving right!");
			orb.setMotor(0, ORB.Mode.SPEED, -speed, 0);
			orb.setMotor(1, ORB.Mode.SPEED, -speed, 0);

		}else if (centerX < 240) {
			// drive right
			System.out.println("driving left!");
			orb.setMotor(0, ORB.Mode.SPEED, +speed, 0);
			orb.setMotor(1, ORB.Mode.SPEED, +speed, 0);
		} else {
			orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
			orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
		}

	}

	@Override
	public void onPointerCaptureChanged(boolean hasCapture) {

	}

    //-----------------------------------------------------------------
    @Override
    public void onDestroy()
    {
		if(javaCameraView != null){
			javaCameraView.disableView();
		}
		orb.setModelServo(0,servoSpeed,0);
		orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
		orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
        orb.close();
        super.onDestroy();

    }

    //---------------------------------------------------------------

	@Override
	protected void onPause() {
		super.onPause();
		if(javaCameraView != null){
			javaCameraView.disableView();
		}
		orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
		orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (OpenCVLoader.initDebug()){
			Log.d(TAG,"OpenCV is configured Successfully");
			baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
		}
		else {
			Log.d(TAG,"OpenCV is not working");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,this,baseLoaderCallback);
		}
	}



	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		if (menuLocal == null)
		{
			menuLocal = menu;
			super.onCreateOptionsMenu(menu);
		}
		return true;
	}

	//---------------------------------------------------------------
	@Override
	public
	boolean onOptionsItemSelected( MenuItem item )
	{
		int id = item.getItemId();
		switch (id)
		{
			case R.id.action_connect:
				BluetoothDeviceListActivity.startBluetoothDeviceSelect(this, ORB_REQUEST_CODE );
				break;
            default:
		}
		return super.onOptionsItemSelected( item );
	}

	//-----------------------------------------------------------------
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		super.onActivityResult( requestCode, resultCode, data );
		switch( requestCode )
		{
			case ORB_REQUEST_CODE:
				if(!orb.openBluetooth(BluetoothDeviceListActivity.onActivityResult(resultCode, data)))
				{
					Toast.makeText(getApplicationContext(), "Bluetooth not connected", Toast.LENGTH_LONG).show();
				}
				break;
            default:
		}
	}

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    public void onClick_Start_0( View view )
    {
        orb.setMotor( 0, ORB.Mode.SPEED, -200, 0);
        orb.setMotor( 1, ORB.Mode.SPEED, +200, 0);
    }

    //-----------------------------------------------------------------
    public void onClick_Start_1( View view )
    {
        orb.setMotor(0, ORB.Mode.MOVETO, +500, 0);
        orb.setMotor(1, ORB.Mode.MOVETO, -500, 0);
    }

    //-----------------------------------------------------------------
    public void onClick_Stop( View view )
    {
        orb.setMotor( 0, ORB.Mode.POWER, 0, 0);
        orb.setMotor( 1, ORB.Mode.POWER, 0, 0);
    }

    //-----------------------------------------------------------------
	//-----------------------------------------------------------------
	private void setMsg()
	{
		TextView view;

		view = (TextView) findViewById(R.id.msgVoltage);
		view.setText("Batt:" + String.format("%.1f V", orb.getVcc()));

        view = (TextView) findViewById(R.id.msgORB1);
        view.setText("M0:"     + String.format("%6d,%6d,%6d",orb.getMotorSpeed((byte)0),
                                                             orb.getMotorPos((byte)0),
                       	                                      orb.getMotorPwr((byte)0)) );

        view = (TextView) findViewById(R.id.msgORB2);
        view.setText("M1:"     + String.format("%6d,%6d,%6d",orb.getMotorSpeed((byte)1),
                                                             orb.getMotorPos((byte)1),
                                                             orb.getMotorPwr((byte)1)) );
	}

}