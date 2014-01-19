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
	private BluetoothServiceCallbacks callbacks;
	private BTClient btClient; 
	
	public void setCallback(BluetoothServiceCallbacks callback){
		this.callbacks=callback;
	}
	
	
	
	@Override
	public synchronized IBinder  onBind(Intent intent) {
		
		if (btClient != null){
			
			btClient.cancel();
		}
			
		return mBinder;
	}

	public class MyBinder extends Binder {
		BluetoothService getService() {
	      return BluetoothService.this;
	    }
	}
	
	
	public synchronized void doConnect(BluetoothDevice d){
		
			if (btClient!=null && callbacks != null){
				callbacks.onConnection(btClient.isConnected(), false);
			}
			if (btClient != null){
				Log.d("debug","cancel client");
				btClient.cancel();
			}else{
				Log.d("debug","client is null");
			}

			try {
				btClient = new BTClient(d, serviceUUID, this,true);
				btClient.startListeningForIncomingBytes();
			} catch (IOException e) {
				e.printStackTrace();
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
			byte[] toSend = data.getBytes("UTF-8");
			if (toSend != null || btClient != null){
				btClient.send(toSend);
			}else{
				disconnect();
				onConnection(false);
			}
				
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnected(){
		if (btClient == null) return false;
		return btClient.isConnected();
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
		
		if (btClient != null)
			btClient.cancel();
		stopSelf();
	}
	
	@Override
	public void onDestroy() {
		if (btClient != null)
			btClient.cancel();
		super.onDestroy();
	}
}
