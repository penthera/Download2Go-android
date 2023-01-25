package com.penthera.download2go1_4;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.penthera.virtuososdk.client.ISettings;
import com.penthera.virtuososdk.client.Virtuoso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 */
public class SettingsViewActivity extends AppCompatActivity {

    // We can keep a single instance of the Virtuoso object in an application class, or create a
    // new copy in each activity.
    private Virtuoso virtuoso;

    /** Handle to the settings interface */
    private ISettings settings;

    // Settings controls

    /** Change the current setting for max storage */
    private EditText maxstorage;
    /** Change the current value for headroom */
    private EditText headroom;
    /** The battery level text */
    private TextView batteryDetail;
    /** The current value for battery threshold */
    private SeekBar batterythreshold;
    /** Change the current vlaue for cell quota */
    private EditText cellquota;
    /** The time the cell quota started from */
    private TextView cellquotastart;
    /** The connection timeout for HTTP transactions */
    private EditText connectionTimeout;
    /** The socket timeout for HTTP transactions */
    private EditText socketTimeout;
    /** The maximum number of concurrent download connections used by the downloader */
    private EditText maxConcurrentConnections;
    /** The current percent based progress rate used by Virtuoso */
    private SeekBar progressPercent;
    /** The percent level */
    private TextView progressPercentDetails;
    /** The current time based progress rate used by Virtuoso */
    private EditText progressTimed;

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        virtuoso = new Virtuoso(this);

        settings = virtuoso.getSettings();

        maxstorage = findViewById(R.id.max_storage_value);
        headroom = findViewById(R.id.headroom_value);
        batterythreshold = findViewById(R.id.battery_value);
        cellquota = findViewById(R.id.cellquota_value);
        cellquotastart = findViewById(R.id.cellquota_date_value);
        connectionTimeout = findViewById(R.id.edt_connection_timeout);
        socketTimeout = findViewById(R.id.edt_socket_timeout);
        maxConcurrentConnections = findViewById(R.id.edt_max_connections);
        progressPercent = findViewById(R.id.progress_percent_value);
        progressTimed = findViewById(R.id.progress_timed_value);

        batteryDetail = findViewById(R.id.battery_label);
        progressPercentDetails  = findViewById(R.id.progress_percent_label);

        SeekBar.OnSeekBarChangeListener seekProgressChangeListener = new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if(fromUser){
                    progressPercentDetails.setText(getString(R.string.report_progress_percent, progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

        };
        progressPercent.setOnSeekBarChangeListener(seekProgressChangeListener);

        SeekBar.OnSeekBarChangeListener seekChangeListener = new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if(fromUser){
                    batteryDetail.setText(getString(R.string.battery_threshold, progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        batterythreshold.setOnSeekBarChangeListener(seekChangeListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume();

        updateSettingsValues();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause();
    }


    public void onGlobalReset(View view) {
        settings
                .resetMaxStorageAllowed()
                .resetHeadroom()
                .resetBatteryThreshold()
                .resetCellularDataQuota()
                .resetCellularDataQuotaStart()
                .resetHTTPConnectionTimeout()
                .resetHTTPSocketTimeout()
                .resetMaxDownloadConnections()
                .resetProgressUpdateByPercent()
                .resetProgressUpdateByTime();
    }

    public void onApplySettings(View view) {
        try{
            settings
                    .setMaxStorageAllowed(Long.parseLong(maxstorage.getText().toString()))
                    .setHeadroom(Long.parseLong(headroom.getText().toString()))
                    .setBatteryThreshold(((float)batterythreshold.getProgress())/100)
                    .setCellularDataQuota(Long.parseLong(cellquota.getText().toString()))
                    .setHTTPConnectionTimeout(Integer.parseInt(connectionTimeout.getText().toString()))
                    .setHTTPSocketTimeout(Integer.parseInt(socketTimeout.getText().toString()))
                    .setMaxDownloadConnections(Integer.parseInt(maxConcurrentConnections.getText().toString()))
                    .setProgressUpdateByPercent(progressPercent.getProgress())
                    .setProgressUpdateByTime(Long.parseLong(progressTimed.getText().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSettingsValues() {

        maxstorage.setText(String.format(Locale.getDefault() , "%d",settings.getMaxStorageAllowed()));
        headroom.setText(String.format(Locale.getDefault() , "%d",settings.getHeadroom()));

        int val = (int)(settings.getBatteryThreshold() * 100);
        val = val < 0 ? 0: Math.min(val, 100);
        batterythreshold.setProgress(val);
        batteryDetail.setText(getString(R.string.battery_threshold, val));

        cellquota.setText(String.format(Locale.getDefault() , "%d",settings.getCellularDataQuota()));

        long quotaStart = settings.getCellularDataQuotaStart();
        cellquotastart.setText(dateFormatter.format(new Date(quotaStart*1000)));

        connectionTimeout.setText(String.format(Locale.getDefault() , "%d",settings.getHTTPConnectionTimeout()));
        socketTimeout.setText(String.format(Locale.getDefault() , "%d",settings.getHTTPSocketTimeout()));
        maxConcurrentConnections.setText(String.format(Locale.getDefault() , "%d",settings.getMaxDownloadConnections()));

        progressPercent.setProgress(settings.getProgressUpdateByPercent());
        progressPercentDetails.setText(getString(R.string.report_progress_percent,settings.getProgressUpdateByPercent()));
        progressTimed.setText(String.format(Locale.getDefault() , "%d",settings.getProgressUpdateByTime()));
    }


    // Reset Methods
    public void onMaxStorageReset(View view) {
        settings.resetMaxStorageAllowed();
    }

    public void onHeadroomReset(View view) {
        settings.resetHeadroom();
        updateSettingsValues();
    }

    public void onBatteryReset(View view) {
        settings.resetBatteryThreshold();
        updateSettingsValues();
    }

    public void onCellQuotaReset(View view) {
        settings.resetCellularDataQuota();
        updateSettingsValues();
    }

    public void onCellQuotaDateReset(View view) {
        settings.resetCellularDataQuotaStart();
        updateSettingsValues();
    }

    public void onConnectionTimeoutReset(View view) {
        settings.resetHTTPConnectionTimeout();
        updateSettingsValues();
    }

    public void onSocketTimeoutReset(View view) {
        settings.resetHTTPSocketTimeout();
        updateSettingsValues();
    }

    public void onMaxConnectionReset(View view) {
        settings.resetMaxDownloadConnections();
        updateSettingsValues();
    }

    public void onProgressPercentReset(View view) {
        settings.resetProgressUpdateByPercent();
        updateSettingsValues();
    }

    public void onProgressTimedReset(View view) {
        settings.resetProgressUpdateByTime();
        updateSettingsValues();
    }
}
