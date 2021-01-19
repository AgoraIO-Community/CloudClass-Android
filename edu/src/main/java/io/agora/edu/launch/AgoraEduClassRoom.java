package io.agora.edu.launch;

import android.app.Activity;
import android.util.Log;

import java.lang.ref.WeakReference;

import io.agora.edu.classroom.BaseClassActivity;
import io.agora.edu.classroom.ReplayActivity;

public class AgoraEduClassRoom extends AgoraEduReplay {
    private WeakReference<BaseClassActivity> baseClassActivityWeak;

    public AgoraEduClassRoom() {
        super();
    }

    public void add(Activity activity) {
        super.add(activity);
        if (activity instanceof BaseClassActivity) {
            baseClassActivityWeak = new WeakReference<>((BaseClassActivity) activity);
        }
    }

    public void destroy() throws IllegalStateException {
        super.destroy();
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
