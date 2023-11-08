package com.penthera.playassurehelloworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.penthera.playassurehelloworld.databinding.ActivityMainBinding
import java.util.*

/**
 * Launch activity to start player with or without Play Assure
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Use this as the userId for demonstration
    private val userId = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playNormal.setOnClickListener { streamAsset() }

        binding.playAssured.setOnClickListener { playAssureAsset() }
    }

    private fun streamAsset() {
        VideoPlayerActivity.playStream(ASSET_URL, this)
    }

    private fun playAssureAsset() {

        // Substitute the proper backplane url and key credentials for your implementation
        // Provide an appropriate unique user id.  We use a random id here for convenience.
        VideoPlayerActivity.playAssure(ASSET_URL, this,
            BACKPLANE_URL, BACKPLANE_PUBLIC_KEY, BACKPLANE_PRIVATE_KEY, userId)
    }

    companion object {
        const val ASSET_URL: String = "https://virtuoso-demo-content.s3.amazonaws.com/Steve/steve.m3u8"

        const val BACKPLANE_URL = "https://demo.penthera.com"
        const val BACKPLANE_PUBLIC_KEY =  "a382d4a927dee20c21af4442a85adfc8366b99b485547d7364818839509ac7cb"
        const val BACKPLANE_PRIVATE_KEY = "517ba3db3fb8127d33b342716b69bdbdb030425737a3ade7d2224b86fdf8bf19"
    }
}