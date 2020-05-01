package com.example.marcqtan.samplemusic;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.DrawableRes;

public final class Samples {

    public static final class Sample {
        public final Uri uri;
        public final String mediaId;
        public final String title;
        public final String description;
        public final int bitmapResource;
        public final int duration;

        public Sample(
                String uri, String mediaId, String title, String description, int bitmapResource, int duration) {
            this.uri = Uri.parse(uri);
            this.mediaId = mediaId;
            this.title = title;
            this.description = description;
            this.bitmapResource = bitmapResource;
            this.duration = duration;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static final Sample[] SAMPLES = new Sample[]{
            new Sample(
                    "https://storage.googleapis.com/automotive-media/Jazz_In_Paris.mp3",
                    "audio_1",
                    "Jazz in Paris",
                    "Jazz for the masses",
                    R.drawable.album_art_1,
                    103),
            new Sample(
                    "https://storage.googleapis.com/automotive-media/The_Messenger.mp3",
                    "audio_2",
                    "The messenger",
                    "Hipster guide to London",
                    R.drawable.album_art_2,
                    132),
            new Sample(
                    "https://storage.googleapis.com/automotive-media/Talkies.mp3",
                    "audio_3",
                    "Talkies",
                    "If it talks like a duck and walks like a duck.",
                    R.drawable.album_art_3,
                    162),
    };

    private static Bitmap getBitmap(Context context, @DrawableRes int bitmapResource) {
        Bitmap bmp = ((BitmapDrawable) context.getResources().getDrawable(bitmapResource)).getBitmap();
        return Bitmap.createScaledBitmap(bmp, dipToPixels(context, 132), dipToPixels(context, 132), false);

    }

    static Bitmap getIconBitmap(Context context, @DrawableRes int bitmapResource) {
        return ((BitmapDrawable) context.getResources().getDrawable(bitmapResource)).getBitmap();
    }

    private static int dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics));
    }

    static MediaDescriptionCompat getMediaDescription(Context context, Sample sample, int currentMediaIndex) {
        Bundle extras = new Bundle();
        extras.putParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getBitmap(context, sample.bitmapResource));
        extras.putLong("currentMediaIndex", currentMediaIndex);
        extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, sample.duration * 1000);
        extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, sample.description);
        return new MediaDescriptionCompat.Builder()
                .setMediaId(sample.mediaId)
                //.setIconBitmap(bitmap)
                .setTitle(sample.title)
                .setSubtitle(sample.description)
                .setDescription(sample.description)
                .setExtras(extras)
                .build();
    }

}

