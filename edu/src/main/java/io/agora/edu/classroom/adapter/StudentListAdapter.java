package io.agora.edu.classroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import io.agora.edu.R2;

import com.chad.library.adapter.base.listener.OnItemChildClickListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.edu.R;
import io.agora.edu.classroom.bean.group.GroupMemberInfo;

public class StudentListAdapter extends RecyclerView.Adapter<StudentListAdapter.ViewHolder> {
    private static final String TAG = StudentListAdapter.class.getSimpleName();
    private final int layoutId = R.layout.item_student_layout;
    private List<GroupMemberInfo> students = new ArrayList<>();

    public List<GroupMemberInfo> getStudents() {
        return students;
    }

    private String localUserUuid;

    private OnItemChildClickListener onItemChildClickListener;

    public StudentListAdapter(String localUserUuid) {
        this.localUserUuid = localUserUuid;
    }

    public void setOnItemChildClickListener(OnItemChildClickListener onItemChildClickListener) {
        this.onItemChildClickListener = onItemChildClickListener;
    }

    public void updateLocalUserUuid(String userUuid) {
        localUserUuid = userUuid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupMemberInfo memberInfo = students.get(position);
//        holder.textView.setText(memberInfo.getUserName() + (memberInfo.getOnline() ? "" : R.string.offline_state));
        holder.textView.setText(memberInfo.getUserName());
        if (memberInfo.getUuid().equals(localUserUuid) && memberInfo.getOnStage()) {
            holder.muteAudio.setImageResource(R.drawable.ic_audio_green);
            holder.muteVideo.setImageResource(R.drawable.ic_video_green);
            holder.muteAudio.setClickable(true);
            holder.muteVideo.setClickable(true);
            holder.muteAudio.setOnClickListener(v -> {
                if (onItemChildClickListener != null) {
                    onItemChildClickListener.onItemChildClick(null, holder.muteAudio, position);
                }
            });
            holder.muteVideo.setOnClickListener(v -> {
                if (onItemChildClickListener != null) {
                    onItemChildClickListener.onItemChildClick(null, holder.muteVideo, position);
                }
            });
        } else {
            holder.muteAudio.setImageResource(R.drawable.ic_audio_gray);
            holder.muteVideo.setImageResource(R.drawable.ic_video_gray);
            holder.muteAudio.setClickable(false);
            holder.muteVideo.setClickable(false);
        }
        holder.muteAudio.setSelected(memberInfo.getOnStage() && memberInfo.getEnableAudio());
        holder.muteVideo.setSelected(memberInfo.getOnStage() && memberInfo.getEnableVideo());
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public void updateStudentList(List<GroupMemberInfo> data) {
        this.students = data;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.textView)
        AppCompatTextView textView;

        @BindView(R2.id.iv_btn_mute_audio)
        AppCompatImageView muteAudio;

        @BindView(R2.id.iv_btn_mute_video)
        AppCompatImageView muteVideo;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
