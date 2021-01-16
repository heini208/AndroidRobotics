//*******************************************************************
/**
 @file   cPropToORB.java
 @author Thomas Breuer
 @date   07.10.2019
 @brief
 **/

//*******************************************************************
package com.App.ORB;

//*******************************************************************
import java.nio.ByteBuffer;

//*******************************************************************
public class cPropToORB
{
	//---------------------------------------------------------------
	static class Motor
	{
		int  mode;
		int  speed;
		int  pos;
	}

	//---------------------------------------------------------------
	class ModellServo
	{
		int  mode;
		int  pos;
	}

	//---------------------------------------------------------------
	public Motor[]        motor = new Motor[4];
	private ModellServo[] servo = new ModellServo[2];

	//---------------------------------------------------------------
	public cPropToORB()
	{
		for( int i = 0; i < 4; i++ )
		{
			motor[i] = new Motor();

			setMotor( i, 0, 0, 0);
		}

        for( int i = 0; i < 2; i++ )
        {
            servo[i] = new ModellServo();

            setModelServo( i, 0, 0);
        }
  }

	//---------------------------------------------------------------
	public void setMotor( int idx, int mode, int speed, int pos )
	{
		synchronized(this)
		{
			if (0 <= idx && idx < 4) {
				motor[idx].mode = mode;
				motor[idx].speed = speed;
				motor[idx].pos = pos;
				//isNew=true;
			}
		}
	}

	//---------------------------------------------------------------
	public void setModelServo( int idx, int mode, int pos )
    {
		synchronized(this)
		{
			if (0 <= idx && idx < 2) {
				servo[idx].mode = mode;
				servo[idx].pos = pos;
				//isNew = true;
			}
		}
    }

	//---------------------------------------------------------------
	public int fill( ByteBuffer buffer )
	{
		int idx = 2;
		synchronized(this)
		{
			buffer.put(idx++, (byte) 1); //id
			buffer.put(idx++, (byte) 0); // reserved


			for (int i = 0; i < 4; i++) {
				buffer.put(idx++, (byte) ((motor[i].mode)));

				buffer.put(idx++, (byte) ((motor[i].speed) & 0xFF)); // LSB
				buffer.put(idx++, (byte) ((motor[i].speed >> 8) & 0xFF));// MSB
				//buffer.put(idx++, (byte) ((motor[i].speed >> 16) & 0xFF));
				//buffer.put(idx++, (byte) ((motor[i].speed >> 24) & 0xFF));

				buffer.put(idx++, (byte) ((motor[i].pos) & 0xFF)); // LSB
				buffer.put(idx++, (byte) ((motor[i].pos >> 8) & 0xFF)); //
				buffer.put(idx++, (byte) ((motor[i].pos >> 16) & 0xFF)); //
				buffer.put(idx++, (byte) ((motor[i].pos >> 24) & 0xFF)); // MSB
			}

			for (int i = 0; i < 2; i++) {
				buffer.put(idx++, (byte) ((servo[i].mode)));
				buffer.put(idx++, (byte) ((servo[i].pos)));
			}
		}
      return ( idx-2 );
	}
}
