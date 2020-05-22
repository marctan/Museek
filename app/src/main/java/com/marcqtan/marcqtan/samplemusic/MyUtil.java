package com.marcqtan.marcqtan.samplemusic;

import android.content.Context;
import android.graphics.PorterDuff;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

/**
 * Created by Marc Q. Tan on 22/05/2020.
 */

public final class MyUtil {

    public enum REPEAT_MODE {
        NONE(0),
        ONE(1),
        ALL(2);

        private int value = 0;

        public int getValue() {
            return value;
        }

        REPEAT_MODE(int value) {
            this.value = value;
        }
    }

    public static void updateRepeatDrawable(REPEAT_MODE rmode, ImageView repeat, Context ctx) {
        switch (rmode) {
            case ONE:
                repeat.setImageDrawable(ctx.getDrawable(R.drawable.repeat_one));
                repeat.setColorFilter(ContextCompat.getColor(ctx, R.color.active), PorterDuff.Mode.MULTIPLY);
                break;
            case ALL:
                repeat.setImageDrawable(ctx.getDrawable(R.drawable.repeat));
                repeat.setColorFilter(ContextCompat.getColor(ctx, R.color.active));
                break;
            case NONE:
            default:
                repeat.setImageDrawable(ctx.getDrawable(R.drawable.repeat));
                repeat.setColorFilter(ContextCompat.getColor(ctx, R.color.colorAccent));
                break;
        }
    }

    public static void updateShuffleDrawable(boolean enabled, ImageView shuffle, Context ctx) {
        if (enabled) {
            shuffle.setColorFilter(ContextCompat.getColor(ctx, R.color.active));
        } else {
            shuffle.setColorFilter(ContextCompat.getColor(ctx, R.color.colorAccent));
        }
    }

}
