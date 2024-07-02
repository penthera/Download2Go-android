package com.penthera.download2go1_9

import com.penthera.virtuososdk.client.IDASHManifestRenditionSelector
import com.penthera.virtuososdk.client.ISegmentedAsset

class ExampleCustomDASHTrackSelector : IDASHManifestRenditionSelector {
    override fun selectRendition(
        asset: ISegmentedAsset?,
        tracks: MutableList<IDASHManifestRenditionSelector.IDASHVideoRendition>
    ): IDASHManifestRenditionSelector.IDASHVideoRendition {
        // Implement your custom logic here for selecting a video track.
        // tracks will contain a list of all available video tracks in the manifest with information
        // including size, bandwidth, framerate, codec, mimetype.
        return tracks
            .filter{track -> track.codec == "avc1.42c01e"}
            .first()
    }

    companion object {
        @Volatile
        private var instance: ExampleCustomDASHTrackSelector? = null

        fun getInstance(): ExampleCustomDASHTrackSelector {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = ExampleCustomDASHTrackSelector()
                    }
                }
            }
            return instance!!
        }
    }
}