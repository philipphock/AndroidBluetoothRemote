package de.philipphock.android.bluetoothremote;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateActor;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateChangeReactor;

public class MainActivity extends Activity implements BluetoothService.BluetoothServiceCallbacks, BluetoothStateChangeReactor{

	
	private EditText recvTxt;
	private TextView connstatus;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothService btService;
	private BluetoothStateActor btStateActor;
	private boolean bluetoothEnabledBeforeAppStart=false;
	private TextView btStatus;
	private enum BEFORE_CONNECT {NOT_CONNECTED_WITH_SERVICE,BLUETOOTH_OFF};
	private ProgressDialog progressToConnect;
	private Set<BEFORE_CONNECT> before_connect = new HashSet<MainActivity.BEFORE_CONNECT>();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
       
        connstatus = (TextView) findViewById(R.id.connStatus);
        btStatus = (TextView) findViewById(R.id.btStatus);
        
        btStateActor = new BluetoothStateActor(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
        
        before_connect.add(BEFORE_CONNECT.BLUETOOTH_OFF);
        before_connect.add(BEFORE_CONNECT.NOT_CONNECTED_WITH_SERVICE);
        
        
        SeekBar seek = (SeekBar)findViewById(R.id.volslider);
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
    	Intent serviceBind = new Intent(this,BluetoothService.class);
    	bindService(serviceBind, bluetoothServiceConnection,Context.BIND_AUTO_CREATE );
    	
    	bluetoothEnabledBeforeAppStart=mBluetoothAdapter.isEnabled();
    	onBluetoothEnabledChanged(bluetoothEnabledBeforeAppStart);
    	mBluetoothAdapter.enable();
	


            
        
    }
    
	@Override
	protected void onPause() {
		super.onPause();
		if (btService != null){
			btService.disconnect();	
		}
		
		if (!bluetoothEnabledBeforeAppStart){
			mBluetoothAdapter.disable();
		}
		btStateActor.unregister(this);
		unbindService(bluetoothServiceConnection);
		
		if (progressToConnect != null)
			progressToConnect.dismiss();
	}
	    

	
	private void tryConnect(){
		if (before_connect.size()==0){
			doConnectToDefault();
		}
	}
    
    private final ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			before_connect.add(BEFORE_CONNECT.NOT_CONNECTED_WITH_SERVICE);
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
			
			before_connect.remove(BEFORE_CONNECT.NOT_CONNECTED_WITH_SERVICE);
			tryConnect();
		    
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
	public void onRecv(final String data) {
		//handle recv
		
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
			before_connect.remove(BEFORE_CONNECT.BLUETOOTH_OFF);
			tryConnect();
			
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("debug","result");
		if (requestCode == 1 && resultCode == RESULT_OK){
			Log.d("debug","cond");
			BluetoothDevice d = data.getParcelableExtra("device");
			
			btService.doConnect(d);
		}
	}
	
	
	
	private void send(String s){
    	btService.send(s);
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
		Log.d("debug", "volume:"+percent);
		send("volume:"+percent);
	}
	
}
