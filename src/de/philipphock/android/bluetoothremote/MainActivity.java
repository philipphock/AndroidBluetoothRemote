package de.philipphock.android.bluetoothremote;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import de.philipphock.android.lib.bluetooth.BTClient;
import de.philipphock.android.lib.bluetooth.BTClient.BTClientCallback;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateActor;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateChangeReactor;

public class MainActivity extends Activity implements BTClientCallback, BluetoothStateChangeReactor{

	
	private TextView connstatus;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothStateActor btStateActor;
	private boolean bluetoothEnabledBeforeAppStart=false;
	private TextView btStatus;
	private enum BEFORE_CONNECT {BLUETOOTH_OFF,ALREADY_CONNECTED};
	private ProgressDialog progressToConnect;
	private Set<BEFORE_CONNECT> before_connect = Collections.synchronizedSet(new HashSet<MainActivity.BEFORE_CONNECT>());
	private BTClient btClient; 
	private final String serviceUUID = "94d6b2c0-8034-11e3-baa7-0800200c9a66";
	private SeekBar seek;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
       
        connstatus = (TextView) findViewById(R.id.connStatus);
        btStatus = (TextView) findViewById(R.id.btStatus);
        
        btStateActor = new BluetoothStateActor(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
        
        before_connect.add(BEFORE_CONNECT.BLUETOOTH_OFF);
        
        
        seek = (SeekBar)findViewById(R.id.volslider);
        seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				float progress = seekBar.getProgress();
				send_Volume(progress/1000f);
						
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
			
						
			}
		});
         
    }


    @Override
    protected void onResume() {
    	super.onResume();
    	
    	progressToConnect = ProgressDialog.show(this, "connecting...",
    		    "waiting for connection", true);
    	btStateActor.register(this);
    	
    	bluetoothEnabledBeforeAppStart=mBluetoothAdapter.isEnabled();
    	onBluetoothEnabledChanged(bluetoothEnabledBeforeAppStart);
    	mBluetoothAdapter.enable();
    	doConnectToDefault();


            
        
    }
    
	@Override
	protected void onPause() {
		super.onPause();
		btClient.cancel("app paused");
		
		if (!bluetoothEnabledBeforeAppStart){
			mBluetoothAdapter.disable();
		}
		btStateActor.unregister(this);
		
		if (progressToConnect != null)
			progressToConnect.dismiss();
	}
	    

	
	
    

		
		
    
	
	

    private synchronized void  doConnectToDefault(){
    	if (before_connect.size()==0){
    		before_connect.add(BEFORE_CONNECT.ALREADY_CONNECTED);
		
			Set<BluetoothDevice> bondedSet = BluetoothAdapter.getDefaultAdapter()
					.getBondedDevices();
			
			SharedPreferences prefs = this.getSharedPreferences(
				      "de.philipphock.android.bluetoothremote", Context.MODE_PRIVATE);
			String devicekey = "device";
			String mac = prefs.getString(devicekey, "");
				
			for(BluetoothDevice d:bondedSet){
				if (d.getAddress().equals(mac)){
					try {
						btClient = new BTClient(d,serviceUUID,MainActivity.this,true);
						btClient.startListeningForIncomingBytes();

					} catch (IOException e) {
						e.printStackTrace();
					}
					return;
				}
			}
    	}
    }

    
    //BluetoothService.BluetoothServiceCallbacks

    
    
    
	
	


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
			before_connect.remove(BEFORE_CONNECT.BLUETOOTH_OFF);
			doConnectToDefault();
			
		}else{
			before_connect.add(BEFORE_CONNECT.BLUETOOTH_OFF);
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
	
	
	
	
	
	private void send(String s){
    	try {
			btClient.send(s.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	
	//commands by ui
	
	public void send_standby(View v){
		send("standby");
	}
	
	
	public void send_mute(View v){
		send("togglemute");
	}
	
	public void send_next(View v){
		send("musicplayer:next");
	}
	public void send_prev(View v){
		send("musicplayer:prev");
	}

	public void send_playpause(View v){
		send("musicplayer:playpause");
	}

	
	private void send_Volume(float percent){
		send("volume:"+percent);
	}


	@Override
	public synchronized void onConnection(final boolean isConnected) {
		if (!isConnected){
			before_connect.remove(BEFORE_CONNECT.ALREADY_CONNECTED);
		}else{
			send("request_status");
		}
			runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (isConnected){
					connstatus.setText("connected");
					connstatus.setTextColor(Color.GREEN);
					if (progressToConnect != null)
						progressToConnect.dismiss();
				}else{
					connstatus.setText("disconnected");
					connstatus.setTextColor(Color.RED);
				}
			}
		});  		
	}


	@Override
	public void onRecv(byte[] data, int length) {
		//handle recv
		
		
		try {
			final String recv = new String(data,0,length,"UTF-8");
			try {
				JSONObject o = new JSONObject(recv);
				double vol = o.getDouble("vol");
				seek.setProgress((int)(vol*1000));
				return;
			} catch (JSONException e) {
				
			}
			
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, "recv "+recv, Toast.LENGTH_SHORT).show();
				}
			});
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}
	
}
