package io.agora.edu.classroom.widget.room;

import android.content.Context;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import io.agora.edu.R;

public class ClassContainer {
    public enum NetworkState {
        good, medium, bad
    }

    private IClassWindowListener mListener;

    private String mClassIdFormat;
    private String mClassNotBeginHint;
    private String mClassBeginHint;
    private String mNetworkGoodText;

    private final Context mContext;

    private final View mBaseLayout;
    private AppCompatTextView mClassIdText;
    private AppCompatImageView mClassTimeIcon;
    private AppCompatTextView mClassTimeText;
    private AppCompatTextView mNetworkStateText;
    private AppCompatImageView mNetworkStateIcon;

    private String[] mNetworkStateTexts;

    private final int[] mNetworkStateIcons = {
            R.drawable.classroom_icon_network_state_good,
            R.drawable.classroom_icon_network_state_medium,
            R.drawable.classroom_icon_network_state_bad
    };

    private final int[] mNetworkStateColors = {
            R.color.classroom_network_state_color_good,
            R.color.classroom_network_state_color_medium,
            R.color.classroom_network_state_color_bad,
    };

    public ClassContainer(Context context, View layout) {
        mContext = context;
        mBaseLayout = layout;
        init();
    }

    private void init() {
        mClassIdFormat = mContext.getResources().getString(R.string.class_window_class_id_format);
        mClassNotBeginHint = mContext.getResources().getString(R.string.class_window_class_not_start_hint);
        mClassBeginHint = mContext.getResources().getString(R.string.class_window_class_begins);
        mNetworkStateTexts = mContext.getResources().getStringArray(R.array.class_window_network_states);

        mClassIdText = mBaseLayout.findViewById(R.id.classroom_room_id_text);
        mClassTimeIcon = mBaseLayout.findViewById(R.id.classroom_class_time_icon);
        mClassTimeText = mBaseLayout.findViewById(R.id.classroom_class_time_text);
        mNetworkStateIcon = mBaseLayout.findViewById(R.id.classroom_network_state_icon);
        mNetworkStateText = mBaseLayout.findViewById(R.id.classroom_network_state_text);

        mClassIdText.setText(mClassIdFormat);
        setClassTime(false, "");
        setNetworkState(NetworkState.good);

        mBaseLayout.findViewById(R.id.classroom_customer_service_btn).setOnClickListener(view -> {
            if (mListener != null) mListener.onCustomerService();
        });

        mBaseLayout.findViewById(R.id.classroom_refresh_btn).setOnClickListener(view -> {
            if (mListener != null) mListener.onRefreshClassWindow();
        });

        mBaseLayout.findViewById(R.id.classroom_switch_camera_btn).setOnClickListener(view -> {
            if (mListener != null) mListener.onSwitchCamera();
        });

        mBaseLayout.findViewById(R.id.classroom_leave_btn_layout).setOnClickListener(view -> {
            if (mListener != null) mListener.onLeaveRoom();
        });
    }

    public void setClassWindowListener(IClassWindowListener listener) {
        mListener = listener;
    }

    public void setClassId(String classId) {
        mClassIdText.setText(String.format(mClassIdFormat, classId));
    }

    public void setClassTime(boolean started, String time) {
        String format = started ? mClassBeginHint : mClassNotBeginHint;
        String message = String.format(format, time);
        mClassTimeText.setText(message);
    }

    public void setNetworkState(NetworkState state) {
        int index = state.ordinal();
        if (index < 0 || index >= mNetworkStateIcons.length) return;
        mNetworkStateIcon.setImageResource(mNetworkStateIcons[index]);
        mNetworkStateText.setText(mNetworkStateTexts[index]);
        mNetworkStateText.setTextColor(mContext.getResources().getColor(mNetworkStateColors[index]));
    }
}
