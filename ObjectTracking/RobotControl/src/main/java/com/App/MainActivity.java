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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
	private double centerX =350;
	private double radiusR = 200;
	private boolean toggle = false;



	public MainActivity()
	{
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

	public void objectFollowing(){
		int speed = 500;
		if (centerX > 450) {
			// drive left
			System.out.println("driving left!");
			orb.setMotor(0, ORB.Mode.SPEED, -speed, 0);
			orb.setMotor(1, ORB.Mode.SPEED, -speed, 0);

		} else if (centerX < 250) {
			// drive right
			System.out.println("driving right!");
			orb.setMotor(0, ORB.Mode.SPEED, +speed, 0);
			orb.setMotor(1, ORB.Mode.SPEED, +speed, 0);
		} else if (radiusR <= 190) {
			// driving Foward
			System.out.println("driving Forward!");
			orb.setMotor(0, ORB.Mode.SPEED, -speed, 0);
			orb.setMotor(1, ORB.Mode.SPEED, +speed, 0);
		} else if (radiusR >= 260) {
			// driving Backwards
			System.out.println("driving Backwards!");
			orb.setMotor(0, ORB.Mode.SPEED, +speed, 0);
			orb.setMotor(1, ORB.Mode.SPEED, -speed, 0);
		} else {
			orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
			orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
		}

	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
	}


	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();

		Scalar hsv_min = new Scalar(0,150,70);
		Scalar hsv_max = new Scalar(5,255,255);
		Scalar hsv_min2 = new Scalar(175,150,70);
		Scalar hsv_max2 = new Scalar(180,255,255);

		Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV,4);
		Core.inRange(mHSV, hsv_min, hsv_max, mThresholded);
		Core.inRange(mHSV, hsv_min2, hsv_max2, mThresholded2);
		Core.bitwise_or(mThresholded, mThresholded2, mThresholded);



		Mat circles = new Mat();


		Imgproc.GaussianBlur(mThresholded, mThresholded, new Size(7, 7), 0,0);
		Imgproc.HoughCircles(mThresholded, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 10000, 100, 90, 100 , 0);
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

		if ( toggle == true) {

			if (circles.cols() > 0) {
				for (int x = 0; x < Math.min(circles.cols(), 5); x++) {
					double circleVec[] = circles.get(0, x);

					if (circleVec == null) {
						break;
					}
					Point center = new Point((int) circleVec[0], (int) circleVec[1]);
					int radius = (int) circleVec[2];

					Imgproc.circle(mThresholded, center, 3, new Scalar(0, 255, 0), 5);
					Imgproc.circle(mThresholded, center, radius, new Scalar(0, 0, 255), 2);
					//System.out.println("Center" + center);
					if (!(centerX - center.x < 50 && centerX - center.x > -50)) {
						centerX = center.y;
					}
					if (!(radiusR - radius < 50 && radiusR - radius > -50)) {
						radiusR = radius;
					}

					//System.out.println("Center: " + centerX + "\n Radius: " + radiusR);
					/*TextView view;
					view = (TextView) findViewById(R.id.msgcenterX);
					view.setText("Batt:" + String.format("center: ", centerX));
					*/
					objectFollowing();


				}

			} else {
				orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
				orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
			}

			circles.release();
			mHSV2 = mThresholded.t();
			Core.flip(mThresholded.t(), mHSV2, 1);
			Imgproc.resize(mHSV2, mHSV2, mThresholded.size());
			mThresholded.release();

			return  mHSV2;




		}
		else {
			orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
			orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
		}

		return null;
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