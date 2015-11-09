package com.domotrix.android.receivers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NetworkChangeReceiver extends BasicRemoteReceiver {
  private final String TAG = "NetworkReceiver";

  @Override
  public void onReceive(final Context context, final Intent intent) {
    ServiceManager serviceManager = new ServiceManager(context);
    if (serviceManager.isNetworkAvailable()) {
        String networkName = serviceManager.getNetworkName();
        Log.d(TAG,"YOU ARE CONNECTED ON :"+networkName);
        if (isConnected()) {
        }
    }
  }
}
