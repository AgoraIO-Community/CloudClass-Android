package io.agora.edu.classroom.adapter;

import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import io.agora.edu.R2;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.agora.edu.R;
import io.agora.edu.classroom.bean.group.GroupInfo;
import io.agora.edu.classroom.bean.group.GroupMemberInfo;

public class StudentGroupAdapter extends RecyclerView.Adapter<StudentGroupAdapter.ViewHolder> {
    private static final String TAG = "StudentGroupAdapter";

    private List<GroupInfo> groupInfoList = new ArrayList<>();
    private List<GroupMemberInfo> allMemberList = new ArrayList<>();
    private final int layoutId = R.layout.item_studentgroup_layout;
    private int scrollViewWidth;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupInfo groupInfo = groupInfoList.get(position);
        holder.groupNameTextView.setText(String.format(holder.itemView.getContext().getString(
                R.string.groupname), groupInfo.getGroupName(), groupInfo.getMembers().size()));
        holder.coVideoing.setVisibility(groupInfo.getOnStage() ? View.VISIBLE : View.GONE);
        List<GroupMemberInfo> curGroupMembers = new ArrayList<>();
        for (GroupMemberInfo memberInfo : allMemberList) {
            if (groupInfo.getMembers().contains(memberInfo.getUuid())) {
                curGroupMembers.add(memberInfo);
            }
        }
        Log.e(TAG, new Gson().toJson(curGroupMembers));
        GroupMemberAdapter memberAdapter = new GroupMemberAdapter(curGroupMembers);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(holder.itemView.getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        holder.membersRecyclerView.setLayoutManager(linearLayoutManager);
        /*分割线判空，否则随着刷新，分割间隔将逐渐变大*/
        if (holder.membersRecyclerView.getItemDecorationCount() == 0) {
            holder.membersRecyclerView.addItemDecoration(new AdapterDecoration(18));
        }
        holder.membersRecyclerView.setAdapter(memberAdapter);
        holder.membersRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                holder.leftMargin += dx;
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.scrollView.getLayoutParams();
                layoutParams.leftMargin = holder.leftMargin;
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == getItemCount() - 1) {
                    layoutParams.rightMargin = 0;
                } else if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    holder.leftMargin = 0;
                    layoutParams.leftMargin = holder.leftMargin;
                }
                layoutParams.width = scrollViewWidth;
                holder.scrollView.setLayoutParams(layoutParams);
            }
        });
        holder.scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scrollViewWidth = holder.scrollView.getRight() - holder.scrollView.getLeft();
                        holder.scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return groupInfoList.size();
    }

    public void updateGroupList(List<GroupInfo> newGroupInfos, List<GroupMemberInfo> newAllMembers) {
        this.groupInfoList = newGroupInfos;
        this.allMemberList = newAllMembers;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.groupName_TextView)
        AppCompatTextView groupNameTextView;
        @BindView(R2.id.coVideoing)
        AppCompatTextView coVideoing;
        @BindView(R2.id.view0)
        View view0;
        @BindView(R2.id.members_RecyclerView)
        RecyclerView membersRecyclerView;
        @BindView(R2.id.scrollView)
        View scrollView;

        private int leftMargin;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private class AdapterDecoration extends RecyclerView.ItemDecoration {
        private int left;

        public AdapterDecoration(int left) {
            this.left = left;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.left = left;
            }
            outRect.top = 0;
            outRect.right = 0;
            outRect.bottom = 0;
        }
    }
}
