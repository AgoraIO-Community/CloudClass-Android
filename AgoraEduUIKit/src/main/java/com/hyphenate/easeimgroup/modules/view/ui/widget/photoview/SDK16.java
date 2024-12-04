package com.hyphenate.easeimgroup.modules.view.ui.widget.photoview;

import android.annotation.TargetApi;
import android.view.View;

@TargetApi(16)
class SDK16 {

	public static void postOnAnimation(View view, Runnable r) {
		view.postOnAnimation(r);
	}
	
}
