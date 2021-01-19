package io.agora.edu.classroom.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.agora.edu.R2;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.edu.R;
import io.agora.edu.R2;
import io.agora.edu.classroom.bean.group.GroupMemberInfo;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.ViewHolder> {
    private static final String TAG = "GroupMemberAdapter";

    private List<GroupMemberInfo> memberList = new ArrayList<>();
    private final int layoutId = R.layout.item_groupmember_layout;

    public GroupMemberAdapter(List<GroupMemberInfo> memberList) {
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupMemberInfo info = memberList.get(position);
        Log.e(TAG, new Gson().toJson(info));
        if (info.getOnStage()) {
            if (info.getOnline()) {
                holder.coverView.setVisibility(View.VISIBLE);
            } else {
                holder.coverView.setVisibility(View.GONE);
                holder.portraitImageView.setAlpha(0.5f);
            }
        } else {
            if (info.getOnline()) {
                holder.coverView.setVisibility(View.GONE);
            } else {
                holder.coverView.setVisibility(View.GONE);
                holder.portraitImageView.setAlpha(0.5f);
            }
        }
        holder.memberNameTextView.setText(info.getUserName());
        holder.memberNameTextView.setTextColor(holder.itemView.getResources().getColor(R.color.gray_191919));
        holder.integralTextView.setText(String.valueOf(info.getReward()));
        holder.integralImageView.setImageResource(
                info.getOnline() ? R.drawable.ic_integral_1 : R.drawable.ic_integral_0);
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.portrait_ImageView)
        AppCompatImageView portraitImageView;
        @BindView(R2.id.cover_View)
        AppCompatImageView coverView;
        @BindView(R2.id.memberName_TextView)
        AppCompatTextView memberNameTextView;
        @BindView(R2.id.integral_ImageView)
        AppCompatImageView integralImageView;
        @BindView(R2.id.reward_TextView)
        AppCompatTextView integralTextView;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
