package de.philipphock.android.bluetoothremote;

import java.util.Set;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import de.philipphock.android.lib.bluetooth.BluetoothDeviceArrayAdapter;

public class ConnectToDeviceActivity extends ListActivity {
	private BluetoothDeviceArrayAdapter adapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(android.R.layout.);

		adapter = new BluetoothDeviceArrayAdapter(
				this);

		Set<BluetoothDevice> bondedSet = BluetoothAdapter.getDefaultAdapter()
				.getBondedDevices();
		adapter.addAll(bondedSet);

		setListAdapter(adapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
	  BluetoothDevice dret = adapter.getItem(position);
	   Bundle conData = new Bundle();
	   conData.putParcelable("device", dret);
	   Intent intent = new Intent();
	   intent.putExtras(conData);
	   setResult(RESULT_OK, intent);
	   storeMac(dret);
	   finish();
			
		
	}
	
	private void storeMac(BluetoothDevice d){
		SharedPreferences prefs = this.getSharedPreferences(
			      "de.philipphock.android.bluetoothremote", Context.MODE_PRIVATE);
			String devicekey = "device";
			prefs.edit().putString(devicekey, d.getAddress()).commit();
	}
	

}
