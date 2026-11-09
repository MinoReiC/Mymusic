package com.musicforme.app.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

/**
 * Java utility class demonstrating Kotlin + Java interoperability.
 * Provides audio metadata extraction using Android's MediaMetadataRetriever.
 */
public class AudioMetadataHelper {

    public static class AudioInfo {
        private final String bitrate;
        private final String mimeType;
        private final long durationMs;

        public AudioInfo(String bitrate, String mimeType, long durationMs) {
            this.bitrate = bitrate;
            this.mimeType = mimeType;
            this.durationMs = durationMs;
        }

        public String getBitrate() {
            return bitrate != null ? bitrate : "N/A";
        }

        public String getMimeType() {
            return mimeType != null ? mimeType : "audio/mpeg";
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getFormattedBitrate() {
            if (bitrate == null || bitrate.isEmpty()) return "Unknown kbps";
            try {
                int kbps = Integer.parseInt(bitrate) / 1000;
                return kbps + " kbps";
            } catch (NumberFormatException e) {
                return bitrate;
            }
        }
    }

    public static AudioInfo extractAudioInfo(Context context, Uri audioUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, audioUri);
            String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            long durationMs = 0L;
            if (durationStr != null) {
                try {
                    durationMs = Long.parseLong(durationStr);
                } catch (NumberFormatException ignored) {}
            }

            return new AudioInfo(bitrate, mimeType, durationMs);
        } catch (Exception e) {
            return new AudioInfo(null, null, 0L);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
    }
}
