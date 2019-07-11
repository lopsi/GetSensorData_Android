package es.csic.getsensordata; /**
 * Created by Antonio on 27/10/2016.
 */

import android.app.Application;
import com.estimote.sdk.BeaconManager;

public class MyApplication extends Application {

    private BeaconManager beaconManager;

    @Override
    public void onCreate() {
        super.onCreate();

        beaconManager = new BeaconManager(getApplicationContext());
    }
}
