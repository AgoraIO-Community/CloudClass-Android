package io.agora.edu.classroom.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.agora.edu.R;

public class StageAudioView extends AppCompatImageView {

    @IntDef({State.CLOSED, State.OPENED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
        int CLOSED = 0; // closed
        int OPENED = 1; // opened
    }

    private int[] imgResArray = {
            R.drawable.ic_audio_off,
            R.drawable.ic_audio_on,
    };
    private int showIndex = 0;
    private int state = State.CLOSED;

    private Runnable runnable = () -> {
        setImageResource(this.imgResArray[this.showIndex]);
        if (this.state == 2) {
            this.showIndex++;
            postDelayed(this.runnable, 500);
        }
    };

    public StageAudioView(Context context) {
        this(context, null);
    }

    public StageAudioView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StageAudioView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        runnable.run();
    }

    public void setState(@State int state) {
        if (this.state != state) {
            this.state = state;
            if (state == State.OPENED) {
                showIndex = 1;
            } else if (state == State.CLOSED) {
                showIndex = 0;
            }
            runnable.run();
        }
    }

    public int getState() {
        return state;
    }

}
