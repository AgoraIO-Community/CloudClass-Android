package io.agora.edu.launch;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

abstract class ActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {

    @Override
    public abstract void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState);

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public abstract void onActivityDestroyed(@NonNull Activity activity);
}
