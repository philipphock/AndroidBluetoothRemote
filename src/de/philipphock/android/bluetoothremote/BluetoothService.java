package de.philipphock.android.bluetoothremote;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import de.philipphock.android.lib.bluetooth.BTClient;
import de.philipphock.android.lib.bluetooth.BTClient.BTClientCallback;

public class BluetoothService extends Service implements BTClientCallback{
	private final IBinder mBinder = new MyBinder();
	private final String serviceUUID = "94d6b2c0-8034-11e3-baa7-0800200c9a66";
	private volatile boolean isConnected = false;
	private BluetoothServiceCallbacks callbacks;
	private BTClient btClient; 
	
	public void setCallback(BluetoothServiceCallbacks callback){
		this.callbacks=callback;
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();

		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		
		return mBinder;
	}

	public class MyBinder extends Binder {
		BluetoothService getService() {
	      return BluetoothService.this;
	    }
	}
	
	
	public synchronized void doConnect(BluetoothDevice d){
		
		if (isConnected){
			Log.d("debug","isConnected");
			if (callbacks == null) return;
			callbacks.onConnection(isConnected, false);
			return;
		}

		try {
			if (btClient != null){
				Log.d("debug","cancel client");
				btClient.cancel();
			}else{
				Log.d("debug","client is null");
			}
			Log.d("debug","init new client");

			btClient = new BTClient(d, serviceUUID, this,true);
			Log.d("debug","start listening");

			btClient.startListeningForIncomingBytes();
		} catch (IOException e) {
			callbacks.onError(e);
		}
	}
	
	public synchronized void send(String data){
		try {
			if (btClient == null){
				Log.d("debug","bluetooth client is null");
				return;
			}
			if (data == null){
				Log.d("debug","data to send is null");
				return;
			}
			btClient.send(data.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnected(){
		return isConnected;
	}
	
	public interface BluetoothServiceCallbacks{
		public void onConnection(boolean isConnected,boolean hasChanged);
		public void onRecv(String data);
		public void onError(Exception e);
		
	}


	//BTClientCallback
	@Override
	public void onConnection(boolean isConnected) {
		Log.d("debug", "connection callback: "+isConnected);
		this.isConnected = isConnected;
		if (callbacks == null){
			Log.d("debug", "cannot tell callback :( he is null");
			return;
		}
		callbacks.onConnection(isConnected, true);
	}


	@Override
	public void onError(Exception e) {
		if (callbacks == null) return;
		callbacks.onError(e);
	}


	@Override
	public void onRecv(byte[] data, int length) {
		try {
			if (callbacks == null) return;
			callbacks.onRecv(new String(data,0,length,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	//BTClientCallback\\
	
	public void disconnect(){
		try {
			if (btClient != null)
				btClient.cancel();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
