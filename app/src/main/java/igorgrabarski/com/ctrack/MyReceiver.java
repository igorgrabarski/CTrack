package igorgrabarski.com.ctrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

       Intent intentReboot = new Intent(context, WorkService.class);
        context.startService(intentReboot);
    }

}
