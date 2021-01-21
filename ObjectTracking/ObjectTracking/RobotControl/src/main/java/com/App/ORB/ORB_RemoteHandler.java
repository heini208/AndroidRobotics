//*******************************************************************
/*!
\file   ORB_RemoteHandler.java
\author Thomas Breuer
\date   21.02.2019
\brief
*/

//*******************************************************************
package com.App.ORB;

//*******************************************************************
import android.util.Log;

import java.nio.ByteBuffer;

//*******************************************************************
public class ORB_RemoteHandler
{
    //---------------------------------------------------------------
    protected cConfigToORB    configToORB;
    public cPropToORB         propToORB;
    public cPropFromORB       propFromORB;

    ORB_Remote orbRemote;

    //---------------------------------------------------------------
    protected  ORB_RemoteHandler()
    {
        configToORB     = new cConfigToORB();
        propToORB       = new cPropToORB();
        propFromORB     = new cPropFromORB();

        orbRemote = null;
    }

    //---------------------------------------------------------------
    public void init(  )
    {
    }

    public void setORB_Remote( ORB_Remote orbRemote )
    {
        this.orbRemote = orbRemote;
    }

    //---------------------------------------------------------------
    public void process( ByteBuffer data )
    {
        propFromORB.get( data );
    }

    //---------------------------------------------------------------
    public int fill(  ByteBuffer data )
    {
        int size = 0;
        if( configToORB.isNewCnt>0 ) {
            size = configToORB.fill(data);
            configToORB.isNewCnt--;
        }
        else  {
            //Log.d("ORB","propToORB");
            size = propToORB.fill(data);
            //propToORB.isNew = false;
        }
        return( size );
    }

    //---------------------------------------------------------------
    public boolean update()
    {
        if( orbRemote != null )
        {
            return( orbRemote.update() );
        }
        return( false );
    }
}
