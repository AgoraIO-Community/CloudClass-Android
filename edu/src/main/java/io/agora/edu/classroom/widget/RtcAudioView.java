package io.agora.edu.classroom.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.agora.edu.R;

public class RtcAudioView extends AppCompatImageView {

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

    public RtcAudioView(Context context) {
        this(context, null);
    }

    public RtcAudioView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RtcAudioView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setImageResource(this.imgResArray[this.showIndex]);
    }

    public void setState(@State int state) {
        if (this.state != state) {
            this.state = state;
            if (state == State.OPENED) {
                showIndex = 1;
            } else if (state == State.CLOSED) {
                showIndex = 0;
            }
            setImageResource(this.imgResArray[this.showIndex]);
        }
    }

    public int getState() {
        return state;
    }

}
