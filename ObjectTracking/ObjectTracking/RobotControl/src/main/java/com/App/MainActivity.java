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
    // mittelwert der roten Pixelkoordinaten
	private double centerX =0;
	private double centerY =0;
	//ON OFF
	private boolean toggle = false;
	// Y-Modus ON OFF
	private boolean ymode = false;
	// Abstand zum Objekt geschaetzt in Pixelanzahl
	private int distance =0;

    //State variable ob das Objekt waehrend der y verfolgung verloren ging
	private boolean ymodeLost = false;

	private int angle =0;

	private final int speed = 255;
	private int servoSpeed = speed;
	//Mindestanzahl an Pixeln ab der ein Objekt erkannt wird
	private final int minPixels = 4000;

	// ! Wichtige Methoden sind onCameraFrame(), ymodeObjectFollowing(), objectFollowing() !




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

        //y-Servo
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
		//System.out.println("SensorValue: "+ orb.getSensorValue(0)+ " ," +orb.getSensorValueDigital(0,0)+ " , "+ orb.getSensorValueDigital(0,1) +" , "+orb.getSensorValueAnalog(0,0));

		//Kamerabild
		mRgba = inputFrame.rgba();

		//hsv Grenzbereiche
		Scalar hsv_min = new Scalar(0,100,25);
		Scalar hsv_max = new Scalar(5,255,255);
		Scalar hsv_min2 = new Scalar(175,100,25);
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
        // Modus ob die Kamera in Y-Richtung beweglich sein soll
		ToggleButton ymodeV = (ToggleButton) findViewById(R.id.toggleButton);
		ymodeV.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					ymode = true;
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

			//x-y wert des Mittelpunktes berechnen
			if( densityArray.length > minPixels ) {
				for (int i = 0; i < densityArray.length; i++) {
					x += densityArray[i].x;
					y += densityArray[i].y;
				}

				//Kamera ist 90grad gedreht daher x=y und y=x
				ymodeLost = false;

				centerY = x / densityArray.length;
				centerX = y / densityArray.length;
			}else{
			    //fall wenn der Ball nicht mehr im Bild ist
				ymodeLost = true;
				// wenn der Ball zu schnell aus dem Bild laeuft
				if ( centerX > 240 && centerX < 480 ) {
					centerX = 100;


				}

			}



            //Kontrollwerte
			System.out.println("Y-Wert:" + centerY  + "densitylength= " + densityArray.length + "CENTER:= " + centerX);

			//Punkt bei centerX und centerY Platzieren ( CenterX ist der durschnitts X Wert aller roten Pixel)
			Imgproc.circle(mThresholded, new Point(centerY,centerX), 3, new Scalar(0, 255, 0), 5);

			if (ymode== true){
			    // Methode zur Objektverfolgung mit Y-Bewegung
				ymodeObjectFollowing();
			}else {
			    //Methode zur Objektverfolgung nur auf y=0 grad;
				objectFollowing();
			}

		}else {
		        // wenn ON schalter auf OFF
				orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
				orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
				if ( ymode == true){
					orb.setModelServo(0,servoSpeed,0);

				}
			}


			//Kamerabild drehen und auf Handy Anpassen damit es auf dem display richtig rum angezeigt wird
			mHSV2 = mThresholded.t();
			Core.flip(mThresholded.t(), mHSV2, 1);
			Imgproc.resize(mHSV2, mHSV2, mThresholded.size());
			mThresholded.release();

			return  mHSV2;

	}
    //Variablen zur berechnung der Tatsaechlichen entfernung
	private int ankathete = 50000;
	private int ankatheteClose = 150000;
    private double alpha = 0;
	// grenzbereiche wann sich der Servo nach oben/unten bewegen soll
	private int marginbot = 300;
	private int margintop =660;
	//gibt an ob die kamera ihren maximalen Winkel überschreiten wuerde
	private boolean over60 = false;

	public void ymodeObjectFollowing(){
		double motorspeed = 500;

		//Objekt wurde verloren suche es auf dem Boden
		if(ymodeLost== true){
			//motorspeed = motorspeed/2;
			orb.setModelServo(0,servoSpeed,0);
		}


		//Y-verfolgung
		if (centerY <= marginbot) {
		    //Objekt ist zu weit unten
			servoSpeed = speed;

			//kleinschrittige aenderung des Winkels, da der Servo sonst zu schnell ist ( auch bei speed = 1;)
			angle -= 3;
			if(angle <0){angle =0;}

			orb.setModelServo(0, servoSpeed, angle);

		} else if (centerY >= margintop) {

		    //Objekt ist zu weit oben
			servoSpeed = speed;

			angle += 3;
			if(angle >=60){
			    //Objekt hat Grenzwinkel ueberschritten
				System.out.println("driving Backwards due to angle!");
				over60 = true;
				orb.setMotor(0, ORB.Mode.SPEED, 700, 0);
				orb.setMotor(1, ORB.Mode.SPEED, -700, 0);
				angle =60;}

			orb.setModelServo(0, servoSpeed, angle);

		} else {
		    //Objekt ist ungefaehr mittig im Bild
			orb.setModelServo(0, 0, 0);

            if(over60 == true){
            	over60 = false;
				orb.setMotor(0, ORB.Mode.SPEED, 0, 0);
				orb.setMotor(1, ORB.Mode.SPEED, 0, 0);
			}
            //temporaere Variablen fuer Abstandsgraenzwerte
            int tooCloseTemp = tooClose;
            int tooFarTemp = tooFar;

			//berechnung des tatsächlichen abstands unabängig vom Winkel
			alpha = angle/100;

			tooFarTemp = (int)(ankathete/Math.cos(alpha));
			tooCloseTemp = (int)(ankatheteClose/Math.cos(alpha));

			//umrechnung, da höhere distanz = weniger Pixeldichte bedeutet
			tooFar = tooFar + (tooFar-tooFarTemp);
			tooClose = tooClose + (tooClose - tooCloseTemp);

			System.out.println(tooFar+ " too far: "+ "angle: " + angle);

		}
		if (over60 == false) {
		    // wenn Y-verfolgung erfolgreich X-verfolgung starten
			objectFollowing();


		}




	}

	// grenzwerte wann der Roboter zu weit oder zu nah vom objekt entfernt ist in Form von Anzahl an roter Pixel
	private int tooClose = 250000;
	private int tooFar = 200000;
	public void objectFollowing(){
	    //speed wenn der roboter vorm objekt ist braucht das meiste fine tuning von daher eine Variable
		int speed = 500;
		if (distance >=tooClose) {
			// Rueckwaerts fahren wenn objekt zu nah drann ist
			System.out.println("driving Backwards!");
			orb.setMotor(0, ORB.Mode.SPEED, 700, 0);
			orb.setMotor(1, ORB.Mode.SPEED, -700, 0);
		}else if (distance > minPixels && distance< tooFar) {
			if (centerX > 580) {
				// rechts fahren wenn objekt im Bild aber zu weit weg und zu weit rechts
				System.out.println("driving right!");
				orb.setMotor(0, ORB.Mode.SPEED, -300, 0);
				orb.setMotor(1, ORB.Mode.SPEED, -300, 0);

			}else if (centerX < 140) {
				// links fahren wenn objekt im Bild aber zu weit weg und zu weit links
				System.out.println("driving left!");
				orb.setMotor(0, ORB.Mode.SPEED, +300, 0);
				orb.setMotor(1, ORB.Mode.SPEED, +300, 0);
			}else{
				// forwaerts fahren wenn objekt zu weit weg aber ungefaer mittig
				System.out.println("driving Forward!");
				orb.setMotor(0, ORB.Mode.SPEED, -700, 0);
				orb.setMotor(1, ORB.Mode.SPEED, 700, 0);
			}
		}else if (centerX > 480) {
			// nach rechts korrigieren wenn objekt einen guten Abstand hat aber zu weit rechts ist
			System.out.println("driving right!");
			orb.setMotor(0, ORB.Mode.SPEED, -speed, 0);
			orb.setMotor(1, ORB.Mode.SPEED, -speed, 0);

		}else if (centerX < 240) {
			// nach links korrigieren wenn objekt einen guten Abstand hat aber zu weit links ist
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