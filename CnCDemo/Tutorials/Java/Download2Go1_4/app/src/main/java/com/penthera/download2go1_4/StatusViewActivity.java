package com.penthera.download2go1_4;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.penthera.VirtuosoSDK;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.IService;
import com.penthera.virtuososdk.client.Observers;
import com.penthera.virtuososdk.client.QueueObserver;
import com.penthera.virtuososdk.client.Virtuoso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 *
 */
public class StatusViewActivity extends AppCompatActivity {

    // We can keep a single instance of the Virtuoso object in an application class, or create a
    // new copy in each activity.
    private Virtuoso virtuoso;

    private IService download2GoService;

    private RecyclerView statusList;

    private StatusArrayAdapter statusArrayAdapter;

    private List<StatusValue> statusValues;

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a", Locale.US);

    private int engineStatus = 0;

    private double currentThroughput = 0.0;

    private double overallThroughput = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_activity);

        statusList = findViewById(R.id.status_list);
        statusValues = new ArrayList<>();
        statusList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        statusList.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        statusArrayAdapter = new StatusArrayAdapter(statusValues);
        statusList.setAdapter(statusArrayAdapter);

        virtuoso = new Virtuoso(this);

        // This assigns a new instance of the service client, it is a thin wrapper around an Android service binding.
        download2GoService = virtuoso.getService();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume();
        virtuoso.addObserver(engineStatusObserver);
        virtuoso.addObserver(throughputObserver);
        download2GoService.setConnectionObserver(serviceConnectionObserver);
        download2GoService.bind();

        updateStatusDetails();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the Download2Go SDK on activity pause
        virtuoso.removeObserver(engineStatusObserver);
        virtuoso.removeObserver(throughputObserver);
        download2GoService.unbind();
        download2GoService.setConnectionObserver(null);
        virtuoso.onPause();
    }

    public void updateStatusDetails() {
        // populate values into statusValues
        boolean valueSetChanged = false;
        if (statusValues.size() == 0) {
            populateKeys();
            valueSetChanged = true;
        }

        updateValues();

        // force list to update
        if (valueSetChanged) statusArrayAdapter.notifyDataSetChanged();
        else statusArrayAdapter.notifyItemRangeChanged(0, 11);
    }

    private void populateKeys() {
        // 0. SDK Version
        StatusValue sdkVersionValue = new StatusValue();
        sdkVersionValue.key = getString(R.string.sdk_version);
        sdkVersionValue.value = VirtuosoSDK.FULL_VERSION;
        statusValues.add(sdkVersionValue);

        // 1. Engine Status
        StatusValue engineStatusValue = new StatusValue();
        engineStatusValue.key = getString(R.string.engine_status);
        engineStatusValue.value = "";
        statusValues.add(engineStatusValue);

        // 2. Network Status
        StatusValue networkStatusValue = new StatusValue();
        networkStatusValue.key = getString(R.string.network_status);
        networkStatusValue.value = "";
        statusValues.add(networkStatusValue);

        // 3. Storage Status
        StatusValue storageStatusValue = new StatusValue();
        storageStatusValue.key = getString(R.string.storage_status);
        storageStatusValue.value = "";
        statusValues.add(storageStatusValue);

        // 4. Power Status
        StatusValue powerStatusValue = new StatusValue();
        powerStatusValue.key = getString(R.string.power_status);
        powerStatusValue.value = "";
        statusValues.add(powerStatusValue);

        // 5. Cell Quota Status
        StatusValue cellStatusValue = new StatusValue();
        cellStatusValue.key = getString(R.string.cell_quota_status);
        cellStatusValue.value = "";
        statusValues.add(cellStatusValue);

        // 6. Disk Usage
        StatusValue diskUsageValue = new StatusValue();
        diskUsageValue.key = getString(R.string.disk_usage);
        diskUsageValue.value = "";
        statusValues.add(diskUsageValue);

        // 7. Available Storage
        StatusValue availableStorageValue = new StatusValue();
        availableStorageValue.key = getString(R.string.available_storage);
        availableStorageValue.value = "";
        statusValues.add(availableStorageValue);

        // 8. Authentication Status
        StatusValue authenticationValue = new StatusValue();
        authenticationValue.key = getString(R.string.authentication_status);
        authenticationValue.value = "";
        statusValues.add(authenticationValue);

        // 9. Current Throughput
        StatusValue currentThroughputValue = new StatusValue();
        currentThroughputValue.key = getString(R.string.current_throughput);
        currentThroughputValue.value = "";
        statusValues.add(currentThroughputValue);

        // 10. Overall Throughput
        StatusValue overallThroughputValue = new StatusValue();
        overallThroughputValue.key = getString(R.string.overall_throughput);
        overallThroughputValue.value = "";
        statusValues.add(overallThroughputValue);

        // 11. Secure Time
        StatusValue secureTimeValue = new StatusValue();
        secureTimeValue.key = getString(R.string.secure_time);
        secureTimeValue.value = "";
        statusValues.add(secureTimeValue);
    }

    @SuppressLint("DefaultLocale")
    private void updateValues() {

        if(download2GoService != null && download2GoService.isBound()){
            try{
                engineStatus = download2GoService.getStatus();
                //this should only be available for testing....!!
                currentThroughput = download2GoService.getCurrentThroughput();
                overallThroughput = download2GoService.getOverallThroughput();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        // 1. Engine Status
        statusValues.get(1).value = getStatusString(engineStatus);

        // 2. Network Status
        statusValues.get(2).value = getOkBlockedString(virtuoso.isNetworkStatusOK());

        // 3. Storage Status
        statusValues.get(3).value = getOkBlockedString(virtuoso.isDiskStatusOK());

        // 4. Power Status
        statusValues.get(4).value = getOkBlockedString(virtuoso.isPowerStatusOK());

        // 5. Cell Quota Status
        statusValues.get(5).value = getOkBlockedString(virtuoso.isCellularDataQuotaOK());

        // 6. Disk Usage
        statusValues.get(6).value = String.format("%d MB", virtuoso.getStorageUsed());

        // 7. Available Storage
        statusValues.get(7).value = String.format("%d MB", virtuoso.getAllowableStorageRemaining());

        // 8. Authentication Status
        statusValues.get(8).value = authenticationString(virtuoso.getBackplane().getAuthenticationStatus());

        // 9. Current Throughput
        statusValues.get(9).value = String.format("%.2f" ,currentThroughput);

        // 10. Overall Throughput
        statusValues.get(10).value = String.format("%.2f" ,overallThroughput);

        // 11. Secure Time
        statusValues.get(11).value = dateFormatter.format(virtuoso.getCurrentVirtuosoTime());
    }

    private void updateThroughput() {
        try{
            currentThroughput = download2GoService.getCurrentThroughput();
            overallThroughput = download2GoService.getOverallThroughput();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        // 9. Current Throughput
        statusValues.get(9).value = String.format(Locale.US,"%.2f" ,currentThroughput);

        // 10. Overall Throughput
        statusValues.get(10).value = String.format(Locale.US,"%.2f" ,overallThroughput);

        statusArrayAdapter.notifyItemRangeChanged(9, 2);
    }

    private String getOkBlockedString(boolean ok) {
        return getString(ok ? R.string.status_ok : R.string.status_blocked);
    }

    private String getStatusString(int engineStatus) {
        String statusString;
        switch(engineStatus){
            case Common.EngineStatus.AUTH_FAILURE:
                statusString = "AUTH_FAILURE";
                break;
            case Common.EngineStatus.ERROR:
                statusString = "ERROR";
                break;
            case Common.EngineStatus.BLOCKED:{
                statusString = "BLOCKED";
            }
            break;
            case Common.EngineStatus.DISABLED:
                statusString = "DISABLED";
                break;
            case Common.EngineStatus.PAUSED:
                statusString = "PAUSED";
                break;
            default:
                statusString = "OKAY";
                break;
        }
        return statusString;
    }

    private String authenticationString(int status) {
        String statusString = "UNSET";
        switch(status){
            case Common.AuthenticationStatus.AUTHENTICATED:
                statusString = "AUTHENTICATED";
                break;
            case Common.AuthenticationStatus.NOT_AUTHENTICATED:
                statusString = "NOT_AUTHENTICATED";
                break;
            case Common.AuthenticationStatus.AUTHENTICATION_EXPIRED:
                statusString = "AUTHENTICATION_EXPIRED";
                break;
            case Common.AuthenticationStatus.INVALID_LICENSE:
                statusString = "INVALID_LICENSE";
                break;
            case Common.AuthenticationStatus.SHUTDOWN:
                statusString = "LOGGED OUT";
                break;
        }
        return statusString;
    }


    /**
     * Connection observer monitors when the service is bound
     */
    protected IService.IConnectionObserver serviceConnectionObserver = new IService.IConnectionObserver(){

        @Override
        public void connected() {
            // Update UI once we know connection is bound.
            if (download2GoService != null && download2GoService.isBound()) {
                updateStatusDetails();
            }
        }

        @Override
        public void disconnected() {
        }

    };

    /**
     * This class observes the SDK engine status changes. Note that callbacks are not on
     * the main thread.
     */
    private Observers.IEngineObserver engineStatusObserver = new EngineObserver() {
        @Override
        public void engineStatusChanged(int status) {
            runOnUiThread(() -> updateStatusDetails());
        }
    };

    /**
     * This simple extension of the queue observer is used for demonstration purposes as a simple
     * way to update the throughput values as a download progresses. Note that callbacks are not on
     * the main thread.
     */
    private Observers.IQueueObserver throughputObserver = new QueueObserver() {

        @Override
        public void enginePerformedProgressUpdateDuringDownload(IIdentifier aAsset) {
            runOnUiThread(() -> updateThroughput());
        }

        @Override
        public void engineCompletedDownloadingAsset(IIdentifier aAsset) {
        }

        @Override
        public  void engineEncounteredErrorDownloadingAsset(IIdentifier aAsset) {
        }
    };

    static class StatusValue {
        public String key;
        public String value;
    }

    // A very simple adapter for demonstration purposes which populates a number of key-value status rows into
    // a recyclerview
    static class StatusArrayAdapter extends RecyclerView.Adapter<StatusArrayAdapter.StatusArrayAdapterViewHolder> {

        private List<StatusValue> values;

        public StatusArrayAdapter(List<StatusValue> values) {
            this.values = values;

        }

        @NonNull
        @Override
        public StatusArrayAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new StatusArrayAdapterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.status_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull StatusArrayAdapterViewHolder holder, int position) {
            holder.bind(values.get(position));
        }

        @Override
        public int getItemCount() {
            return values.size();
        }

        class StatusArrayAdapterViewHolder extends RecyclerView.ViewHolder {

            TextView keyTextView, valueTextView;

            public StatusArrayAdapterViewHolder(@NonNull View itemView) {
                super(itemView);
                keyTextView = itemView.findViewById(R.id.status_key);
                valueTextView = itemView.findViewById(R.id.status_value);
            }

            public void bind(StatusValue value) {
                keyTextView.setText(value.key);
                valueTextView.setText(value.value);
            }
        }
    }
}
