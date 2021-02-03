package io.agora.edu.classroom.widget.room;

import android.graphics.Color;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import io.agora.edu.R;

public class ClassTitleBar {
    public enum NetworkState {
        good, medium, bad
    }

    public interface ClassTitleBarListener {
        void onLeaveRoom();

        void onSwitchCamera();

        void onCustomerService();
    }

    private final int[] mNetworkStateIconRes = {
            R.drawable.classroom_icon_network_state_good,
            R.drawable.classroom_icon_network_state_medium,
            R.drawable.classroom_icon_network_state_bad,
    };

    private final int[] mNetworkStateTextColor = {
            Color.rgb(212, 239, 118),
            Color.rgb(231, 169, 83),
            Color.rgb(223, 88, 65)
    };

    private final AppCompatTextView mRoomIdTextView;
    private final String mRoomIdHintFormat;
    private final AppCompatImageView mTimeIcon;
    private final AppCompatTextView mClassStateText;
    private final AppCompatTextView mTimeText;
    private final AppCompatImageView mNetworkStateIcon;
    private final AppCompatTextView mNetworkStateText;

    private String[] mNetworkStateStrings;

    private ClassTitleBarListener mListener;

    public ClassTitleBar(ViewGroup layout) {
        layout.findViewById(R.id.classroom_customer_service_btn)
                .setOnClickListener(view -> {
                    if (mListener != null) mListener.onCustomerService();
                });

        layout.findViewById(R.id.classroom_switch_camera_btn)
                .setOnClickListener(view -> {
                    if (mListener != null) mListener.onSwitchCamera();
                });

        layout.findViewById(R.id.classroom_leave_btn_layout)
                .setOnClickListener(view -> {
                    if (mListener != null) mListener.onLeaveRoom();
                });

        mRoomIdTextView = layout.findViewById(R.id.classroom_room_id_text);
        mTimeIcon = layout.findViewById(R.id.classroom_class_time_icon);
        mTimeText = layout.findViewById(R.id.classroom_class_time_text);
        mClassStateText = layout.findViewById(R.id.classroom_class_state_text);
        mNetworkStateIcon = layout.findViewById(R.id.classroom_network_state_icon);
        mNetworkStateText = layout.findViewById(R.id.classroom_network_state_text);

        mNetworkStateStrings = layout.getContext().getResources()
                .getStringArray(R.array.class_window_network_states);

        setNetworkState(NetworkState.medium);
        setClassStarted(true);

        mRoomIdHintFormat = layout.getContext().getResources()
                .getString(R.string.class_window_class_id_format);
    }

    public void setClassId(String id) {
        String idText = String.format(mRoomIdHintFormat, id);
        mRoomIdTextView.setText(idText);
    }

    public void setNetworkState(NetworkState state) {
        int index = state.ordinal();
        mNetworkStateIcon.setImageResource(mNetworkStateIconRes[index]);
        mNetworkStateText.setText(mNetworkStateStrings[index]);
        mNetworkStateText.setTextColor(mNetworkStateTextColor[index]);
    }

    public void setClassStarted(boolean started) {
        int resource = started ? R.string.class_window_class_begins
                : R.string.class_window_class_not_start_hint;
        mClassStateText.setText(resource);
    }

    public void setClassTitleBarListener(@NonNull ClassTitleBarListener listener) {
        mListener = listener;
    }
}