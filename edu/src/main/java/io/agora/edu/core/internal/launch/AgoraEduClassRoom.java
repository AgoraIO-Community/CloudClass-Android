package io.agora.edu.core.internal.launch;

import android.app.Activity;
import android.util.Log;

import java.lang.ref.WeakReference;

import io.agora.edu.sdk.app.activities.BaseClassActivity;

public class AgoraEduClassRoom {
    private static final String TAG = "AgoraEduClassRoom";

    private WeakReference<BaseClassActivity> baseClassActivityWeak;
    private AgoraEduEvent curState = AgoraEduEvent.AgoraEduEventDestroyed;

    public AgoraEduClassRoom() {
    }

    public boolean isIdle() {
        return !this.curState.equals(AgoraEduEvent.AgoraEduEventReady);
    }

    public void updateState(AgoraEduEvent state) {
        this.curState = state;
    }

    public void add(Activity activity) {
        if (activity instanceof BaseClassActivity) {
            baseClassActivityWeak = new WeakReference<>((BaseClassActivity) activity);
        }
    }

    public void destroy() throws IllegalStateException {
        Log.i(TAG, "AgoraEdu: destroy() have be called");
        if (curState != AgoraEduEvent.AgoraEduEventReady) {
            throw new IllegalStateException("curState is not AgoraEduEventReady, destroy() cannot be called");
        }
        if (baseClassActivityWeak != null) {
            if (baseClassActivityWeak.get() != null && !baseClassActivityWeak.get().isFinishing()
                    && !baseClassActivityWeak.get().isDestroyed()) {
                baseClassActivityWeak.get().finish();
            }
            baseClassActivityWeak.clear();
            baseClassActivityWeak = null;
        }
    }
}
