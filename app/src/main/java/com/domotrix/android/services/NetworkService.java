package com.domotrix.android.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.domotrix.android.NetworkDiscovery;
import com.domotrix.android.R;
import com.domotrix.android.utils.RepeatableAsyncTask;

import javax.jmdns.ServiceInfo;

/**
 * Created by cougaro on 10/11/15.
 */
public class NetworkService extends Service {

    private IDomotrixService mService = null;
    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IDomotrixService.Stub.asInterface(service);
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    protected void connectToRemoteService(Context ctx) {
        if (!mIsBound) {
            Intent serviceIntent = new Intent(IDomotrixService.class.getName());
            //boolean bindResult = bindService(Utils.createExplicitFromImplicitIntent(getApplicationContext(), serviceIntent), mConnection, Context.BIND_AUTO_CREATE);
            boolean bindResult = bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = bindResult;
        }
    }

    protected void disconnectFromRemoteService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start Searching Network Task
        DiscoverNetworkTask task = new DiscoverNetworkTask(NetworkService.this);
        task.execute();

        connectToRemoteService(getApplicationContext());

        return  Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromRemoteService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class DiscoverNetworkTask extends RepeatableAsyncTask<Void, Void, Object> {
        Context mContext;

        public DiscoverNetworkTask(Context context) {
            super(10);
            mContext = context;
        }

        @Override
        protected Object repeatInBackground(Void... params) {
            final boolean[] isFound = {false};
            NetworkDiscovery discovery = new NetworkDiscovery(NetworkService.this);
            discovery.findServers(new NetworkDiscovery.OnFoundListener() {
                @Override
                public void onServiceAdded(ServiceInfo info) {
                    isFound[0] = true;
                    String[] addresses = info.getHostAddresses();
                    if (mService != null) {
                        try {
                            mService.remoteLog("REMOTELOG","DOMOTRIX IP FOUND AT:"+addresses[0]);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceRemoved(ServiceInfo info) {
                    isFound[0] = false;
                }
            });
            if (isFound[0] == true) {
                return isFound[0];
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Object v, Exception e) {
            if (v == null) {
                Intent intent = new Intent(getApplicationContext(), NetworkService.class);
                PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), intent, 0);
                // Build notification
                // Actions are just fake
                Notification noti = new Notification.Builder(mContext)
                        .setContentTitle("Not Found")
                        .setContentText("Subject").setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pIntent)
                        .build();
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                noti.flags |= Notification.FLAG_AUTO_CANCEL;
                notificationManager.notify(0, noti);
            }
        }
    }

}