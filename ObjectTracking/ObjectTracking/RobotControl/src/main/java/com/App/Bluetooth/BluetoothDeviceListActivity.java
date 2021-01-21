//*******************************************************************
/**
 @file   BluetoothDeviceListActivity.java
 @author Thomas Breuer
 @date   07.10.2019
 @brief
 **/

//*******************************************************************
package com.App.Bluetooth;

//*******************************************************************
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.App.R;

import java.util.ArrayList;
import java.util.Set;

//*******************************************************************
public class BluetoothDeviceListActivity extends Activity
{
    //---------------------------------------------------------------
    BluetoothAdapter     BT_Adapter;
    Set<BluetoothDevice> BT_PairedDevices;
    BluetoothDevice      BT_Device;

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //---------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
        createDeviceList();
    }

    //---------------------------------------------------------------
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    //---------------------------------------------------------------
    class myOnItemClickListener implements AdapterView.OnItemClickListener
    {
        //-----------------------------------------------------------
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
        {
            //setStatus("onItemClick:"+pos);

            BT_Device = (BluetoothDevice)BT_PairedDevices.toArray()[pos];
            //close();
            //open( BT_Device);


            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, BT_Device.getAddress());

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();

        }
    }
    //---------------------------------------------------------------
    public void createDeviceList()
    {
        BT_Adapter       = BluetoothAdapter.getDefaultAdapter();
        BT_PairedDevices = BT_Adapter.getBondedDevices();

        ArrayList<String> list = new ArrayList<String>();

        for(BluetoothDevice BT_Device : BT_PairedDevices)
        {
            list.add(new String( BT_Device.getName()));
        }

        final ArrayAdapter<String> adapter
                = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                list);

        ListView lv = (ListView)findViewById(R.id.paired_devices);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener( new myOnItemClickListener() );
    }

    //-----------------------------------------------------------------
    /**
     *
     * @param requestCode
     */
    public static void startBluetoothDeviceSelect(Activity activity, int requestCode)
    {
        Intent serverIntent = new Intent(activity, BluetoothDeviceListActivity.class);
        activity.startActivityForResult(serverIntent,  requestCode);
    }

    //-----------------------------------------------------------------
    /**
     *
     * @param resultCode
     * @param data
     */
    public static BluetoothDevice onActivityResult(int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {
            BluetoothDevice BT_Device;
            String addr = data.getExtras().getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);

            if( addr.length() > 0 )
            {
                BT_Device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);
                //orb.openBluetooth( BT_Device);
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                return(BT_Device );
            }
        }
        return( null );
    }

}
