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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.App.Bluetooth.BluetoothDeviceListActivity;
import com.App.ORB.ORB;

//*******************************************************************
public class MainActivity extends AppCompatActivity
{
	//---------------------------------------------------------------
	private Menu        menuLocal;
	private ORB         orb;
    private Handler     msgHandler;

    private final int ORB_REQUEST_CODE         = 0;
	private final int ORB_DATA_RECEIVED_MSG_ID = 999;
	private SeekBar seekBar1;
	private SeekBar seekBar2;

	private int speed;



	//---------------------------------------------------------------
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

        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
		seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
		//seekBar2.setProgress(6);

		seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

				if (i>5){
					speed = (i-seekBar2.getMax()/2)*200;

				}

				else if (i<=seekBar2.getMax()/2){
					speed = -((seekBar2.getMax()/2-i)*200);
				}
				orb.setMotor( 0, ORB.Mode.SPEED, -speed, 0);
				orb.setMotor( 1, ORB.Mode.SPEED, +speed, 0);
				if (speed == 0){
					orb.setMotor( 0, ORB.Mode.POWER, 0, 0);
					orb.setMotor( 1, ORB.Mode.POWER, 0, 0);
				}
				setMsg();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			seekBar.setProgress(seekBar2.getMax()/2);
			}
		});

		seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

				if (i>5){
					speed = (i-5)*100;

				}
				else if (i<=seekBar2.getMax()/2){
					speed = -((5-i)*100);
				}
				orb.setMotor( 0, ORB.Mode.SPEED, -speed, 0);
				orb.setMotor( 1, ORB.Mode.SPEED, -speed, 0);
				if (speed == 0){
					orb.setMotor( 0, ORB.Mode.POWER, 0, 0);
					orb.setMotor( 1, ORB.Mode.POWER, 0, 0);
				}
				setMsg();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				seekBar.setProgress(5);

			}
		});



	}

    //-----------------------------------------------------------------
    @Override
    public void onDestroy()
    {
        orb.close();
        super.onDestroy();
    }

    //---------------------------------------------------------------
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
    boolean servotest = false;
    public void onClick_Servo( View view )
    {
        if(servotest == false){
            orb.setModelServo(0, 100, 0);
            servotest = true;
        }else {
            orb.setModelServo(0, 100, 100);
            servotest = false;
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
		view.setText("Batt:" + String.format("%.1f V", orb.getVcc()) + "/n Speed: " + speed);

        view = (TextView) findViewById(R.id.msgORB1);
        view.setText("M0:"     + String.format("%6d,%6d,%6d",orb.getMotorSpeed((byte)0),
                                                             orb.getMotorPos((byte)0),
                                                             orb.getMotorPwr((byte)0))  );

        view = (TextView) findViewById(R.id.msgORB2);
        view.setText("M1:"     + String.format("%6d,%6d,%6d",orb.getMotorSpeed((byte)1),
                                                             orb.getMotorPos((byte)1),
                                                             orb.getMotorPwr((byte)1)) );
	}
}