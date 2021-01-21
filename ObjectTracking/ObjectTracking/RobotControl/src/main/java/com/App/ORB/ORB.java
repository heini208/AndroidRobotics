//*******************************************************************
/**
@file   ORB.java
@author Thomas Breuer
@date   21.02.2019
@brief
**/

//*******************************************************************
package com.App.ORB;

//*******************************************************************
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;

//*******************************************************************
/**
 *
 */
public class ORB extends ORB_RemoteHandler implements Runnable
{
    //---------------------------------------------------------------
    public class Mode
    {
        public final static byte POWER  = 0;
        public final static byte BRAKE  = 1;
        public final static byte SPEED  = 2;
        public final static byte MOVETO = 3;
    }

    //---------------------------------------------------------------
    public class Sensor
    {
        public final static byte NONE   = 0;
        public final static byte UART   = 1;
        public final static byte I2C    = 2;
        public final static byte ANALOG = 3;
    }

    //---------------------------------------------------------------
    private ORB_RemoteUSB     orb_USB;
    private ORB_RemoteBT      orb_BT;
    private Thread            mainThread;
    private boolean           runMainThread = false;
    private Handler           handler;

    private int dataReceivedMsgId = 0;

    //---------------------------------------------------------------
    /**
     *
     */
    public ORB()
    {
        orb_USB    = new ORB_RemoteUSB(this );
        orb_BT     = new ORB_RemoteBT(this );
        runMainThread = false;
        mainThread = new Thread(this );
    }

    //---------------------------------------------------------------
    /**
     *
     * @param activity
     * @param handler
     */
    public void init( Activity activity, Handler handler, int dataReceivedMsgId )
    {
        if( runMainThread )
            return;
        this.handler  = handler;

        orb_USB.init((UsbManager) activity.getSystemService(Context.USB_SERVICE));
        orb_USB.open(activity.getIntent());

        orb_BT.init();

        if( mainThread.getState() == Thread.State.NEW )
        {
            runMainThread = true;
            mainThread.start();
            mainThread.setPriority(2);
        }

        this.dataReceivedMsgId = dataReceivedMsgId;
    }

    //-----------------------------------------------------------------
    /**
     *
     */
    @Override
    public void run()
    {
        while ( runMainThread )
        {
            if( orb_USB.isConnected() )
            {
                setORB_Remote(orb_USB);
            }
            else
            {
                setORB_Remote(orb_BT);
            }

            if( update() )
            {
                Message msg = Message.obtain();
                msg.what = dataReceivedMsgId;
                handler.sendMessage(msg);
            }

            try
            {
                Thread.sleep(1);
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    //-----------------------------------------------------------------
    /**
     *
     */
    public boolean openBluetooth(BluetoothDevice device)
    {
        if( device != null )
            return( orb_BT.open(device) );
        return( false );
    }

    //-----------------------------------------------------------------
    /**
     *
     */
    public void close()
    {
        orb_USB.close();
        orb_BT.close();
        runMainThread = false;
    }

    //---------------------------------------------------------------
    // Motor
    //---------------------------------------------------------------
    //---------------------------------------------------------------
    /**
        Konfiguration eines Motors.
        <p>
        An den Motor-Port können unterschiedliche Motoren angeschlossen werden.
        Für die Messung und Regelung der Motoren ist ein Encoder erforderlich.
        Mit der Konfiguration werden auch Regelparameter (PI-Regelung) eingestellt.

        @param id ID des Motor-Ports (0,...,3)
        @param ticsPerRotation 1/10 der Encoder-Tics pro Umdrehung
        @param acc Beschleunigung der Drehzahl im Mode.MOVETO (0,...,255)
        @param Kp Regelparameter der Drehzahlregelung, proportionaler Anteil (0,...,255).
        Ein größerer Wert macht die Regelung schneller, kann aber auch
        zu Schwingungen führen.
        @param Ki Regelparameter der Drehzahlregelung, integraler Anteil (0,...,255).
        Ein größerer Wert verringert die Regelabweichung, kann zum
        Überschwingen bei Lastwechsel führen.
    */
    public void  configMotor( int  id,
                              int  ticsPerRotation,
                              int  acc,
                              int  Kp,
                              int  Ki )
    {
        configToORB.configMotor( id, ticsPerRotation, acc, Kp, Ki );
    }

    //---------------------------------------------------------------
    /**
        Stellt den Motor auf die Betriebsart ein.
         <p>
            @param id ID des Motor-Ports (0,...,3)
            @param mode Betriebsart (MotorMode)
            @param speed Je nach Betriebsart:
                         <UL>
                         <LI>Mode.POWER<br>Motorspannung (1/1000 Betriebsspannung)</LI>
                         <LI>Mode.BRAKE<br>nicht verwendet</LI>
                         <LI>Mode.SPEED<br>Drehzahl (1/1000 Umdrehungen/Sekunde)</LI>
                         <LI>Mode.MOVETO<br>Max. Drehzahl (1/1000 Umdrehungen/Sekunde)</LI>
                         </UL>
            @param pos   Nur in der Betriebsart Mode.MOVETO: <br>Absolute Zielposition (1/1000 Umdrehungen)
    */
    public void setMotor( int id,
                          int mode,
                          int speed,
                          int pos )
    {
        propToORB.setMotor(id, mode, speed, pos);
    }

    //---------------------------------------------------------------
    /**
        Liefert aktuelle Motorspannung.

        @param id ID des Motor-Ports (0,...,3)
        @return Motorspannung (1/1000 Betriebsspannung)
    */
    public short getMotorPwr( int id )
    {
        return( propFromORB.getMotorPwr(id) );
    }

    //---------------------------------------------------------------

    /**
        Liefert Motor-Drehzahl.

        @param id ID des Motor-Ports (0,...,3)
        @return Drehzahl (1/1000 Umdrehungen/Sekunde)
    */
    public short getMotorSpeed( int id )
    {
        return( propFromORB.getMotorSpeed(id) );
    }

    //---------------------------------------------------------------

    /**
        Liefert Motor-Position
        @param id ID des Motor-Ports (0,...,3)
        @return Absolute Position (1/1000 Umdrehungen)
    */
    public int getMotorPos( int id )
    {
        return( propFromORB.getMotorPos(id) );
    }

    //---------------------------------------------------------------
    // ModellServo
    //---------------------------------------------------------------
    //---------------------------------------------------------------
    /**
        Stellt Modellbau-Servo.
        @param id ID des Servo-Ports (0,1)
        @param speed Geschwindigkeit, mit der das Steuersignal vom
                     ursprünglichen Winkel auf den neuen Winkel
                     umgestellt wird (1/100 Umdrehungen/Sekunde, 0:aus,...,255:sofort)
        @param angle Pulsbreite des Steuersignals mit dem der Servo
                     gesteuert wird (0,...,100), Einheit: (angle/100 + 1)*ms.
    */
    public void setModelServo( int id,
                               int speed,
                               int angle )
    {
        propToORB.setModelServo(id,speed,angle);
    }

    //---------------------------------------------------------------
    // Sensor
    //---------------------------------------------------------------
    //---------------------------------------------------------------
    /**
        Konfiguriert Sensor.
        @param id ID des Sensor-Ports (0,...,3)
        @param type Sensortyp (SensorType)
        @param mode Betriebsmode, Verwendung abhängig vom Sensortyp
        @param option Zusätzlich Optionen, sensorabhängig
    */
    public void configSensor( int id,
                              int type,
                              int mode,
                              int option )
    {
        configToORB.configSensor((byte)id,(byte)type,(byte)mode,(byte)option);
    }

    //---------------------------------------------------------------
    /**
        Liefert Sensorwert.
        @param id ID des Sensor-Ports (0,...,3)
        @return Sensorwert
    */
    public int getSensorValue( int id )
    {
        return( propFromORB.getSensorValue((byte)id) );
    }

    //---------------------------------------------------------------
    /**
        Liefert die zusätzlichen, konfigurations<b>un</b>abhängigen Sensorwert.
        @param id ID des Sensor-Ports (0,...,3)
        @param ch channel des Sensor-Ports (0: Analog-Pin 1,
                                            1: Analog-Pin 2)

     @return Zusätzlicher analoger Messwert
    */
    public int getSensorValueAnalog( int id, int ch )

    {
        return( propFromORB.getSensorValueAnalog((byte)id, (byte)ch));
    }

    //---------------------------------------------------------------
    /**
     Liefert die zusätzlichen, konfigurations<b>un</b>abhängigen Sensorwert.
     @param id ID des Sensor-Ports (0,...,3)
     @param ch channel des Sensor-Ports (0: Digital-Pin 5,
                                         1: Digital-Pin 6)
     @return Zusätzlicher digitaler Messwert
     */
    public boolean getSensorValueDigital( int id, int ch )
    {
        return( propFromORB.getSensorValueDigital((byte)id, (byte)ch));
    }

    //---------------------------------------------------------------
    /**
        Liefert Sensorwerte der zusätzlichen digitalen Ports.
        @param id ID des digitalen Sensor-Ports (0,1)
        @return Digitalwert (0,1)
    */
    public boolean getSensorDigital( int id )
    {
        return( propFromORB.getSensorDigital((byte)id));
    }

    //---------------------------------------------------------------
    // Time
    //---------------------------------------------------------------
    //---------------------------------------------------------------
    /**
        Liefert Zeit seit Start der Laufzeitumgebung.
        @return Laufzeit (ms)
    */
    public long getTime()
    {
        return( System.currentTimeMillis() );
    }

    //---------------------------------------------------------------
    /**
        Unterbricht die Programmausführung für die angegebene Zeit.
        @param time Unterbrechungsdauer (ms)
    */
    public void wait( int time ) throws Exception
    {
        Thread.sleep( time );
    }

    //---------------------------------------------------------------
    // Miscellaneous
    //---------------------------------------------------------------
    //---------------------------------------------------------------
    /**
        Liefert den Wert der Versorgungsspannung
        @return Versorgungsspannung (Volt)
    */
    public float getVcc()
    {
        return( propFromORB.getVcc()    );
    }

}
