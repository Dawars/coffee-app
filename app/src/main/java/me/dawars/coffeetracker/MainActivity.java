package me.dawars.coffeetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.dawars.coffeetracker.gcm.QuickstartPreferences;
import me.dawars.coffeetracker.setup.WelcomeActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    @Bind(R.id.temperature)
    TextView tvTemp;
    @Bind(R.id.level)
    TextView tvLvl;
    @Bind(R.id.pour)
    TextView tvPour;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isPaired = sharedPreferences.getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);

        if (!isPaired) {
            startActivity(new Intent(getApplicationContext(), WelcomeActivity.class));
            finish();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        updateData(sharedPreferences);


        //GCM
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "broadcast receiver");

                updateData(sharedPreferences);


            }
        };

        // Registering BroadcastReceiver
        registerReceiver();
    }

    private void updateData(SharedPreferences sharedPreferences) {

        tvTemp.setText(sharedPreferences.getFloat(QuickstartPreferences.TEMPERATURE, -1.0f) + " °C");
        tvLvl.setText(((int) sharedPreferences.getFloat(QuickstartPreferences.LEVEL, 0) + 0) * 100 / 4 + "%");
        tvPour.setText((int) sharedPreferences.getFloat(QuickstartPreferences.POUR, -1) + "");

    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {


            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Delete jug?");
            alert.setMessage("The jug will be unpaired and it'll have to be reseted");
            alert.setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, int id) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    sharedPreferences.edit().remove(QuickstartPreferences.JUG_ID).apply();
                    sharedPreferences.edit().remove(QuickstartPreferences.TOKEN).apply();
                    sharedPreferences.edit().remove(QuickstartPreferences.SENT_TOKEN_TO_SERVER).apply();
                    sharedPreferences.edit().remove(QuickstartPreferences.REGISTRATION_COMPLETE).apply();
                    sharedPreferences.edit().remove(QuickstartPreferences.TEMPERATURE).apply();
                    sharedPreferences.edit().remove(QuickstartPreferences.LEVEL).apply();
                    sharedPreferences.edit().remove(QuickstartPreferences.POUR).apply();

                    startActivity(new Intent(getApplicationContext(), WelcomeActivity.class));
                    finish();
                }
            });

            alert.setCancelable(true);
            alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            alert.show();
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    //GCM
    private void registerReceiver() {
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(QuickstartPreferences.DATA));
            isReceiverRegistered = true;
        }
    }
}
