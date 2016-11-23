package me.dawars.coffeetracker.setup;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import me.dawars.coffeetracker.R;

public class PairActivity extends AppCompatActivity {

    public static final CharSequence JUG_WIFI_NAME = "SmartJug";
    private static final String TAG = PairActivity.class.getSimpleName();
    @Bind(R.id.pair_wifi_list)
    ListView mWifiListView;

    @Bind(R.id.logo_holder)
    View mLogoHolder;

    @Bind(R.id.empty_view)
    View mEmptyView;

    WifiManager mWifiManager;
    WifiReceiver mWifiReceiver = new WifiReceiver();
    private List<String> mWifiNames;

    @Override
    @TargetApi(18)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);

        ButterKnife.bind(this);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (!mWifiManager.isWifiEnabled()) {
            if (currentapiVersion >= 18) {
                if (!mWifiManager.isScanAlwaysAvailable())
                    mWifiManager.setWifiEnabled(true);
            } else {
                mWifiManager.setWifiEnabled(true);
            }
        }

        mWifiListView.addHeaderView(new View(getApplicationContext()));
        mWifiListView.setEmptyView(mEmptyView);
    }

    protected void onPause() {
        unregisterReceiver(mWifiReceiver);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
        super.onResume();
    }

    @OnClick(R.id.pair_refresh_btn)
    public void refresh() {
        mWifiManager.startScan();
        mEmptyView.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.pair_no_jug_btn)
    public void noJug() {
        //TODO: show instructions or link
    }

    @OnItemClick(R.id.pair_wifi_list)
    public void wifiSelected(int pos) {

        Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
        intent.putExtra(ConnectActivity.EXTRA_JUG_NAME, mWifiNames.get(pos - 1)); // -1 for empty header view
        String transitionLogoHolder = getString(R.string.transition_logo_holder);

        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(PairActivity.this,
                        Pair.create(mLogoHolder, transitionLogoHolder)

                );
        ActivityCompat.startActivity(PairActivity.this, intent, options.toBundle());
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method is called when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = mWifiManager.getScanResults();
            mWifiNames = new ArrayList<>();

            for (int i = 0; i < wifiScanList.size(); i++) {
                String ssid = wifiScanList.get(i).SSID;
                if (ssid.contains(JUG_WIFI_NAME))
                    mWifiNames.add(ssid);
            }
            mWifiListView.setAdapter(new ArrayAdapter<>(getApplicationContext(), R.layout.pair_wifi_item, R.id.pair_item, mWifiNames));
        }
    }
}
