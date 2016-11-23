package me.dawars.coffeetracker.setup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import me.dawars.coffeetracker.MainActivity;
import me.dawars.coffeetracker.R;
import me.dawars.coffeetracker.gcm.QuickstartPreferences;
import me.dawars.coffeetracker.gcm.RegistrationIntentService;
import me.dawars.coffeetracker.setup.adapter.ConnectionAdapter;

public class ConnectActivity extends AppCompatActivity {

    public static final String EXTRA_JUG_NAME = "jug_name";
    private static final String TAG = ConnectActivity.class.getSimpleName();
    private static final String JUG_URL_LIST = "http://192.168.4.1/wifi";
    private static final String JUG_URL_SAVE = "http://192.168.4.1/wifisave";
    //GCM
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    @Bind(R.id.connect_wifi_list)
    ListView mWifiListView;
    @Bind(R.id.empty_view)
    View mEmptyView;
    View mFooterView;
    private WifiManager mWifiManager;
    private List<WifiData> wifiScanList = new ArrayList<>();
    private ConnectionAdapter mAdapter;
    private Handler mHandler;
    private AsyncTask<Void, Void, List<WifiData>> task;
    private final Runnable wifiListRunnable = new Runnable() {
        @Override
        public void run() {
            task = new WifiListAsyncTask().execute();
        }
    };
    private String jugWifiName;
    private int savedWifiId;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        ButterKnife.bind(this);

        Intent intent_get = getIntent();
        jugWifiName = intent_get.getStringExtra(EXTRA_JUG_NAME);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int jugId = Integer.parseInt(jugWifiName.substring(PairActivity.JUG_WIFI_NAME.length() + 1));
        Log.v(TAG, "Jug id: " + jugId);
        editor.putInt(QuickstartPreferences.JUG_ID, jugId);
        editor.apply();


        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(jugWifiName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);

        //TODO: pre loader waiting for SmartJug wifi connection full screen?


        //ListAdapter
        mAdapter = new ConnectionAdapter();
        mWifiListView.setAdapter(mAdapter);
        mWifiListView.setEmptyView(mEmptyView);

        mFooterView = getLayoutInflater().inflate(R.layout.activity_connect_wifi_footer, null);
        mWifiListView.addFooterView(mFooterView);

        //Wifi connection
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        //Background thread
        HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());

        //GCM
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "broadcast receiver");


//                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    //TODO: hide loading
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
//                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
//                    mInformationTextView.setText(getString(R.string.token_error_message));

                    // Start IntentService to register this application with GCM.
                    Intent intent2 = new Intent(getApplicationContext(), RegistrationIntentService.class);
                    startService(intent2);
                }
            }
        };

        // Registering BroadcastReceiver
        registerReceiver();

    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices(); // download gcm if not available

        registerReceiver();
        mHandler.postDelayed(wifiListRunnable, 1000);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(wifiListRunnable);
        task.cancel(false);
        deleteWifiCongig();
        //GCM
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    @OnItemClick(R.id.connect_wifi_list)
    public void wifiSelected(int pos) {
        //Footer
        if (mAdapter.getCount() > pos) {
            WifiData item = mAdapter.getItem(pos);
            createWifiDialog(item.ssid, item.secutity);
        } else {
            createWifiDialog("", 1);//security checkbox
        }
    }

    private void createWifiDialog(final String ssid, int securiry) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final View myLayout = getLayoutInflater().inflate(R.layout.dialog_connect, null);
        alert.setView(myLayout);

        final EditText pwdEt = (EditText) myLayout.findViewById(R.id.dialog_pwd);
        final EditText ssidEt = (EditText) myLayout.findViewById(R.id.dialog_ssid);
        final CheckBox showPwdCB = (CheckBox) myLayout.findViewById(R.id.dialog_show_pwd);

        //InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);////????
        if (ssid.equals("")) {
            alert.setTitle(R.string.add_network);
            ssidEt.requestFocus();
            //keyboard.showSoftInput(ssidEt, 0);
        } else {
            alert.setTitle(ssid);
            ssidEt.setVisibility(View.GONE);
            ssidEt.setText(ssid);
            pwdEt.requestFocus();
            //keyboard.showSoftInput(pwdEt, 0);
        }

        if (securiry == 0) { //no pwd
            pwdEt.setVisibility(View.GONE);
            showPwdCB.setVisibility(View.GONE);
        }

//        final AlertDialog alertDialog = alert.create();

        alert.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int id) {
                String ssid = ssidEt.getText().toString();
                String pwd = pwdEt.getText().toString();
                new ConnectAsyncTask().execute(ssid, pwd);

            }
        });

        alert.setCancelable(true);
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        showPwdCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    pwdEt.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    pwdEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

//        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        alert.show();
    }

    private InputStream downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        URL url = new URL(myurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        int response = conn.getResponseCode();
        Log.d(TAG, "The response is: " + response);
        is = conn.getInputStream();

        return is;
    }

    private WifiConfiguration createAPConfiguration(String networkSSID) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        wifiConfiguration.SSID = "\"" + networkSSID + "\"";
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        return wifiConfiguration;
    }

    //Method for the AP connect:
    public int connectToAP(String networkSSID) {
        WifiConfiguration wifiConfiguration = createAPConfiguration(networkSSID);

        savedWifiId = mWifiManager.addNetwork(wifiConfiguration);
        Log.d(TAG, "# addNetwork returned " + savedWifiId);

        boolean b = mWifiManager.enableNetwork(savedWifiId, true);
        Log.d(TAG, "# enableNetwork returned " + b);

        mWifiManager.setWifiEnabled(true);

        return savedWifiId;
    }

    private void deleteWifiCongig() {
        mWifiManager.removeNetwork(savedWifiId);
        for (WifiConfiguration item : mWifiManager.getConfiguredNetworks()) {
            mWifiManager.enableNetwork(item.networkId, false);
        }
    }

    //GCM
    private void registerReceiver() {
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    //Input: conn credentials, Progress: Void, Res: conn savedWifiId
    class ConnectAsyncTask extends AsyncTask<String, Void, WifiData> {

        @Override
        protected WifiData doInBackground(String... params) {

            String ssid = params[0];
            String pwd = params[1];
            BufferedReader br = null;
            try {
                InputStream is = downloadUrl(JUG_URL_SAVE + "?s=" + ssid + "&p=" + pwd);
                //read back ok sign
                br = new BufferedReader(new InputStreamReader(is));
                String response = br.readLine();
                Log.v(TAG, "Response: " + response);

                if (response.equals("CONN")) {

                    mHandler.removeCallbacks(wifiListRunnable);
                    task.cancel(false);

                    deleteWifiCongig();
                    //TODO: Loader anim

                    // Start IntentService to register this application with GCM.
                    Intent intent = new Intent(getApplicationContext(), RegistrationIntentService.class);
                    startService(intent);

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null)
                        br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            return null;
        }
    }

    public class WifiData implements Comparable<WifiData> {
        private String ssid;
        private int strength;
        private int secutity;

        public WifiData(String ssid, int security, int strength) {
            this.ssid = ssid;
            this.secutity = security;
            this.strength = strength;
        }

        public int getStrength() {
            return strength;
        }

        public int getSecutity() {
            return secutity;
        }

        public String getSsid() {
            return ssid;
        }

        @Override
        public int compareTo(@NotNull WifiData another) {
            return this.strength - another.strength;
        }
    }

    private class WifiListAsyncTask extends AsyncTask<Void, Void, List<WifiData>> {
        @Override
        protected List<WifiData> doInBackground(Void... params) {
            if (isCancelled()) return null;

            //Check connection with SSID and connect
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

            if (!wifiInfo.getSSID().equals("\"" + jugWifiName + "\"")) {
                Log.v(TAG, "Connecting to: " + jugWifiName + " from " + wifiInfo.getSSID());

                if (!mWifiManager.isWifiEnabled())
                    mWifiManager.setWifiEnabled(true);

                connectToAP(jugWifiName);
            } else {
                Log.v(TAG, "Already connected to: " + jugWifiName);

                //Fetch Wifi list
                String line;
                BufferedReader buffer = null;

                try {
                    InputStream is = downloadUrl(JUG_URL_LIST);
                    buffer = new BufferedReader(new InputStreamReader(is));

                    boolean isDuplicate;
                    wifiScanList.clear();
                    Log.v(TAG, "Updating wifi list");

                    while ((line = buffer.readLine()) != null) {
                        String[] wifi = line.split("<");

                        String ssid = wifi[0];
                        int security = Integer.parseInt(wifi[1]);
                        int strength = Integer.parseInt(wifi[2]);

                        isDuplicate = false;
                        for (int i = 0; i < wifiScanList.size(); i++) {
                            WifiData id = wifiScanList.get(i);
                            if (id.ssid.equals(ssid)) {
                                id.strength = Math.max(id.strength, strength);
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate && !ssid.contains(PairActivity.JUG_WIFI_NAME)) {
                            WifiData wifiData = new WifiData(ssid, security, strength);
                            wifiScanList.add(wifiData);
                        }
                    }
                    //TODO: hide big loader and display list again, keep checking somehow
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (buffer != null)
                            buffer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Collections.sort(wifiScanList, Collections.reverseOrder());
                return wifiScanList;
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<WifiData> wifiDatas) {
            if (wifiDatas != null) {
                mAdapter.set(wifiDatas);
            } else {
                Log.d(TAG, "postExec is null");
            }
            mHandler.postDelayed(wifiListRunnable, 3000);
        }
    }
}