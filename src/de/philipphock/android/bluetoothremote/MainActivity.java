package de.philipphock.android.bluetoothremote;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateActor;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateChangeReactor;

public class MainActivity extends Activity implements BluetoothService.BluetoothServiceCallbacks, BluetoothStateChangeReactor{

	
	private EditText recvTxt;
	private EditText toSendTxt;
	private TextView connstatus;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothService btService;
	private BluetoothStateActor btStateActor;
	private boolean bluetoothEnabledBeforeAppStart=false;
	private TextView btStatus;
	private BluetoothDevice device;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        recvTxt = (EditText) findViewById(R.id.recvTxt);
        toSendTxt = (EditText) findViewById(R.id.toSend);
        connstatus = (TextView) findViewById(R.id.connStatus);
        btStatus = (TextView) findViewById(R.id.btStatus);
        
        btStateActor = new BluetoothStateActor(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();    
         
    }


    @Override
    protected void onResume() {
    	super.onResume();
    	btStateActor.register(this);
    	Intent serviceBind = new Intent(this,BluetoothService.class);
    	bindService(serviceBind, bluetoothServiceConnection,Context.BIND_AUTO_CREATE );
    	
    	bluetoothEnabledBeforeAppStart=mBluetoothAdapter.isEnabled();
    	onBluetoothEnabledChanged(bluetoothEnabledBeforeAppStart);
    	mBluetoothAdapter.enable();
    	connstatus.setText("unknown");
		connstatus.setTextColor(Color.YELLOW);
	
		

            
        
    }
    
	@Override
	protected void onPause() {
		super.onPause();
		btStateActor.unregister(this);
		btService.disconnect();
		if (!bluetoothEnabledBeforeAppStart){
			mBluetoothAdapter.disable();
		}
		unbindService(bluetoothServiceConnection);
	}
	    

    
    private final ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					btService = null;
					
					connstatus.setText("unknown");
					connstatus.setTextColor(Color.YELLOW);
					
				}
			});
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			BluetoothService.MyBinder b = (BluetoothService.MyBinder) service;
		    btService = b.getService();
		    btService.setCallback(MainActivity.this);
		    onConnection(btService.isConnected(), false);
			if(!btService.isConnected())
				doConnectToDefault();
		    
		}
	};
    
	
	

    private void doConnectToDefault(){
		Set<BluetoothDevice> bondedSet = BluetoothAdapter.getDefaultAdapter()
				.getBondedDevices();
		
		SharedPreferences prefs = this.getSharedPreferences(
			      "de.philipphock.android.bluetoothremote", Context.MODE_PRIVATE);
		String devicekey = "device";
		String mac = prefs.getString(devicekey, "");
			
		for(BluetoothDevice d:bondedSet){
			if (d.getAddress().equals(mac)){
				Log.d("debug", "found: "+d.getName());
				btService.doConnect(d);
				return;
			}
		}
		
    }

    
    //BluetoothService.BluetoothServiceCallbacks

    
    
    
	@Override
	public void onConnection(final boolean isConnected, boolean hasChanged) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (isConnected){
					connstatus.setText("connected");
					connstatus.setTextColor(Color.GREEN);
				}else{
					connstatus.setText("disconnected");
					connstatus.setTextColor(Color.RED);
				}
			}
		});  

		
	}


	@Override
	public void onRecv(final String data) {
		Log.d("debug", "recv "+data);
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				recvTxt.append(data);
				recvTxt.append("\n");
			}
		});  

		
	}


	@Override
	public void onError(final Exception e) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, "Error "+e.getMessage(), Toast.LENGTH_LONG).show();
			}
		});  
	    
	}


	//BluetoothStateChangeReactor
	
	@Override
	public void onBluetoothEnabledChanged(boolean isEnabled) {
		if (isEnabled){
			btStatus.setText("bluetooth on");
			btStatus.setTextColor(Color.GREEN);
		}else{
			btStatus.setText("bluetooth off");
			btStatus.setTextColor(Color.RED);
		}
	}


	@Override
	public void onBluetoothTurningOn() {
		btStatus.setText("bt turning on..");
		btStatus.setTextColor(Color.YELLOW);
	}


	@Override
	public void onBluetoothTurningOff() {
		btStatus.setText("bt turning off..");
		btStatus.setTextColor(Color.YELLOW);
	}


	@Override
	public void onBluetoothIsDiscoverable() {
		
	}


	@Override
	public void onBluetoothIsConnectable() {
		
	}


	@Override
	public void onBluetoothIsNotConnectableAndNotDiscoveralbe() {
		
	}


	@Override
	public void onBluetoothIsNotDiscoveralbe() {
		
	}


    
	//BluetoothService.BluetoothServiceCallbacks\\
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    MenuItem i = menu.findItem(R.id.connectTo);
	    i.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent a = new Intent(MainActivity.this,ConnectToDeviceActivity.class);
				startActivityForResult(a, 1);
				return true;
				
			}
		});
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("debug","result");
		if (requestCode == 1 && resultCode == RESULT_OK){
			Log.d("debug","cond");
			BluetoothDevice d = data.getParcelableExtra("device");
			
			btService.doConnect(d);
		}
	}
	
	public void doSend(View v){
		String s = toSendTxt.getText().toString();
    	btService.send(s);
    	toSendTxt.getText().clear();
	}
	
}
