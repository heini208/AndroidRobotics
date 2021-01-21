//*******************************************************************
/**
 @file   ORB_RemoteBT.java
 @author Thomas Breuer
 @date   07.10.2019
 @brief
 **/

//*******************************************************************
package com.App.ORB;

//*******************************************************************
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

//*******************************************************************
public class ORB_RemoteBT extends ORB_Remote
{
    //---------------------------------------------------------------
    //---------------------------------------------------------------
    private BluetoothSocket BT_Socket = null;
    private OutputStream    BT_OutStream;
    private InputStream     BT_InStream;
    private boolean         isConnected = false;

    //---------------------------------------------------------------
    private ByteBuffer bufferIN;
    private ByteBuffer bufferOUT;

    //---------------------------------------------------------------
    private boolean ready     = false;
    private int     pos       = 0;
    private byte    temp      = 0;
    private int     updateCnt = 0;

    //---------------------------------------------------------------
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int  UPDATE_CNT_MAX = 1;

    //---------------------------------------------------------------
    public ORB_RemoteBT(ORB_RemoteHandler handler)
    {
        super(handler);
        bufferIN  = ByteBuffer.allocate(256);
        bufferOUT = ByteBuffer.allocate(256);
        isConnected = false;
    }

    //---------------------------------------------------------------
    public void init()
    {
    }

    //---------------------------------------------------------------
    public boolean open( BluetoothDevice BT_Device )
    {
        close();

        int cnt = 2;

        while( cnt-- > 0 )
        {
            try {
                BT_Socket = BT_Device.createRfcommSocketToServiceRecord(MY_UUID);

                BT_Socket.connect();

                BT_OutStream = BT_Socket.getOutputStream();
                BT_InStream = BT_Socket.getInputStream();

                isConnected = true;
                return( true );


            } catch (IOException e) {
                  close();
                  isConnected = false;
            }
        }
        return( false );
    }

    //---------------------------------------------------------------
    public void close()
    {
        isConnected = false;
        try
        {
            /*
            if( BT_InStream != null ) {
                BT_InStream.close();
            }
            if( BT_OutStream != null ) {
                BT_OutStream.close();
            }
            */
            Thread.sleep(500);
            if( BT_Socket != null  )
            {
               // BT_Socket.getInputStream().close();
               // BT_Socket.getOutputStream().close();
                BT_Socket.close();
            }
        }
        catch( Exception e )
        {
        }
    }

    //---------------------------------------------------------------
    public boolean isConnected()
    {
        return( isConnected );
    }

    //---------------------------------------------------------------

    //---------------------------------------------------------------
    private void updateOut()
    {
        if(!isConnected)
        {
            return;
        }

        short crc;
        int size;

        size = handler.fill(bufferOUT);



        crc = CRC(bufferOUT,2,size);
        bufferOUT.put(0, (byte) (crc & 0xFF));
        bufferOUT.put(1, (byte) ((crc >> 8) & 0xFF));

        byte data[] = new byte[1024];
        short len=0;
        short idx=0;

        data[len++] = (byte)(0x20 | ((bufferOUT.get(idx  )>>4) & 0x0F));
        data[len++] = (byte)(0x30 | ((bufferOUT.get(idx++)) & 0x0F));

        while( idx < size+1)
        {
            data[len++] = (byte)(0x40 | ((bufferOUT.get(idx  )>>4) & 0x0F));
            data[len++] = (byte)(0x50 | ((bufferOUT.get(idx++)   ) & 0x0F));
        }
        data[len++] = (byte)(0x80 | ((bufferOUT.get(idx  )>>4) & 0x0F));
        data[len++] = (byte)(0x90 | ((bufferOUT.get(idx++)   ) & 0x0F));



        try
        {
            BT_OutStream.write(data,0,len);
            BT_OutStream.flush();
        }
        catch( IOException e )
        {
        }
    }

    //---------------------------------------------------------------
    private boolean  updateIn()
    {
        if(!isConnected)
        {
            return(false);
        }

        int r;
        try
        {
            while( !ready && BT_InStream.available() > 0 && (r = BT_InStream.read()) >= 0 )
            {
                if( (r & 0xF0) == 0x20 || pos > 255 )
                {
                    pos = 0;
                    ready = false;
                }

                if( (r & 0xF0) == 0x90 )
                {
                    ready = true;
                }

                if( (r & 0x10) == 0x00 )
                {
                    temp = (byte)((r << 4) & 0xf0);
                }
                else
                {
                    temp |= (r & 0x0f);
                    bufferIN.put( pos, temp );
                    pos++;
                }
            }
        }
        catch (IOException e)
        {
            Log.e("ORB_BT", "error read ");
        }

        if( ready )
        {
            handler.process( bufferIN );
            ready = false;
            return (true);
        }
        return( false );
    }

    //---------------------------------------------------------------
    public boolean update()
    {
        boolean ret = false;

        if( updateIn() )
        {
            ret = true;
            updateCnt = UPDATE_CNT_MAX;
        }

        if( updateIn() )
        {
            ret = true;
            updateCnt = UPDATE_CNT_MAX;
        }

        if( updateCnt++ >= UPDATE_CNT_MAX )
        {
            updateOut();
            updateCnt = 0;
        }
        return( ret );
    }

}
