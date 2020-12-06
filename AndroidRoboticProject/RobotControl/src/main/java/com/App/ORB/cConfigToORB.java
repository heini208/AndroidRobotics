//*******************************************************************
/**
 @file   cConfigToORB.java
 @author Thomas Breuer
 @date   21.02.2019
 @brief
 **/

//*******************************************************************
package com.App.ORB;

//*******************************************************************
import java.nio.ByteBuffer;

//*******************************************************************
public class cConfigToORB
{
	//---------------------------------------------------------------
	private class Sensor
	{
		byte type     = 0;
		byte mode     = 0;
		byte option   = 0;
		byte reserved = 0;
	}

	//---------------------------------------------------------------
	private class Motor
	{
		byte acceleration     = 0;
		short ticsPerRotation = 0;
		byte Regler_Kp        = 0;
		byte Regler_Ki        = 0;
		byte Regler_Kd        = 0;
		byte[] reserved = new byte[2];
	}

	//---------------------------------------------------------------
	private Sensor[] sensor   = new Sensor[4];
	private Motor[]  motor    = new Motor[4];
	private byte[]   reserved = new byte[12];

	public int isNewCnt = 0;

	//---------------------------------------------------------------
	public cConfigToORB()
	{
		for( int i = 0; i < 4; i++ )
		{
			sensor[i] = new Sensor();
			motor[i]  = new Motor();
			configSensor( i, (byte)0, (byte)0, (byte)0 );
	  	}
  	}

	//---------------------------------------------------------------
	public int fill( ByteBuffer buffer )
	{
		int idx = 2;

		synchronized (this)
		{
		buffer.put(idx++,(byte)0); //id
		buffer.put(idx++, (byte)0); // reserved

			for (int i = 0; i < 4; i++) {
				buffer.put(idx++, sensor[i].type);
				buffer.put(idx++, sensor[i].mode);
				buffer.put(idx++, sensor[i].option);
				buffer.put(idx++, sensor[i].reserved);
			}

			for (int i = 0; i < 4; i++) {
				buffer.put(idx++, (byte)( motor[i].ticsPerRotation    &0xFF) );
				buffer.put(idx++, (byte)((motor[i].ticsPerRotation>>8)&0xFF) );
				buffer.put(idx++, motor[i].acceleration);
				buffer.put(idx++, motor[i].Regler_Kp);
				buffer.put(idx++, motor[i].Regler_Ki);
				buffer.put(idx++, motor[i].Regler_Kd);
				buffer.put(idx++, motor[i].reserved[0]);
				buffer.put(idx++, motor[i].reserved[1]);
			}

			for( int i=0;i<12;i++)
            {
                buffer.put(idx++, reserved[i]);
            }
		}
		//Log.d("ORB"," idx="+idx);
	  return( idx-2 );
	}

	//---------------------------------------------------------------
	public void configMotor( int   idx,
						     int   ticsPerRotation,
						     int   acc,
						     int   Kp,
						     int   Ki )
	{
		synchronized (this) {
			if (0 <= idx && idx < 4) {
				motor[idx].ticsPerRotation = (short)ticsPerRotation;
				motor[idx].acceleration    = (byte)acc;
				motor[idx].Regler_Kp       = (byte)Kp;
				motor[idx].Regler_Ki       = (byte)Ki;

				isNewCnt++;
			}
		}
	}

	//---------------------------------------------------------------
	public void configSensor( int idx, byte type, byte mode, byte option )
	{
		synchronized (this) {
			if (0 <= idx && idx < 4) {
				sensor[idx].type = type;
				sensor[idx].mode = mode;
				sensor[idx].option = option;
			    isNewCnt++;
			}
		}
	}
}
