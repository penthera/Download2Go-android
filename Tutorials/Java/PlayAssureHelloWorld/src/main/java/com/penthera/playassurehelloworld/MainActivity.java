package com.penthera.playassurehelloworld;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

/**
 *
 */
public class MainActivity extends AppCompatActivity {

    // DEMO Server details
    private static final String BACKPLANE_URL = "https://demo.penthera.com/";
    private static final String BACKPLANE_PUBLIC_KEY = ;
    private static final String BACKPLANE_PRIVATE_KEY = ;

    // This is the test asset to play
    private static final String ASSET_URL = "https://virtuoso-demo-content.s3.amazonaws.com/Steve/steve.m3u8";

    // Use this as the user ID for demonstration
    private static String userId = UUID.randomUUID().toString();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.playNormal).setOnClickListener(v -> {
            streamAsset();
        });
        findViewById(R.id.playAssured).setOnClickListener(v -> {
            playAssureAsset();
        });
    }

    private void streamAsset() {
        VideoPlayerActivity.playStream(ASSET_URL, this);
    }

    private void playAssureAsset() {
        // Substitute the proper backplane url and key credentials for your implementation
        // Provide an appropriate unique user id.  We use a random id here for convenience.
        VideoPlayerActivity.playAssure(ASSET_URL, this,
                BACKPLANE_URL, BACKPLANE_PUBLIC_KEY, BACKPLANE_PRIVATE_KEY, userId);
    }
}
