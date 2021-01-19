package io.agora.edu.classroom.adapter;

import android.app.Activity;
import android.text.TextUtils;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

import io.agora.edu.R;
import io.agora.education.api.stream.data.EduStreamInfo;
import io.agora.edu.classroom.BaseClassActivity;
import io.agora.edu.classroom.bean.group.StageStreamInfo;
import io.agora.edu.classroom.widget.StageVideoView;

public class StageVideoAdapter extends BaseQuickAdapter<StageStreamInfo, StageVideoAdapter.ViewHolder> {
    private static final String TAG = "StageVideoAdapter";

    private static String localUserUuid;
    private String rewardUuid;

    public StageVideoAdapter() {
        super(0);
        setDiffCallback(new DiffUtil.ItemCallback<StageStreamInfo>() {
            @Override
            public boolean areItemsTheSame(@NonNull StageStreamInfo oldItem, @NonNull StageStreamInfo newItem) {
                EduStreamInfo oldStream = oldItem.getStreamInfo();
                EduStreamInfo newStream = newItem.getStreamInfo();
                boolean a = oldStream.getHasVideo() == newStream.getHasVideo()
                        && oldStream.getHasAudio() == newStream.getHasAudio()
                        && oldStream.getStreamUuid().equals(newStream.getStreamUuid())
                        && oldStream.getStreamName().equals(newStream.getStreamName())
                        && oldStream.getPublisher().equals(newStream.getPublisher())
                        && oldStream.getVideoSourceType().equals(newStream.getVideoSourceType());
                return a;
            }

            @Override
            public boolean areContentsTheSame(@NonNull StageStreamInfo oldItem, @NonNull StageStreamInfo newItem) {
                EduStreamInfo oldStream = oldItem.getStreamInfo();
                EduStreamInfo newStream = newItem.getStreamInfo();
                boolean a = oldStream.getHasVideo() == newStream.getHasVideo()
                        && oldStream.getHasAudio() == newStream.getHasAudio()
                        && oldStream.getStreamUuid().equals(newStream.getStreamUuid())
                        && oldStream.getStreamName().equals(newStream.getStreamName())
                        && oldStream.getPublisher().equals(newStream.getPublisher())
                        && oldStream.getVideoSourceType().equals(newStream.getVideoSourceType());
                return a;
            }

            @Nullable
            @Override
            public Object getChangePayload(@NonNull StageStreamInfo oldItem, @NonNull StageStreamInfo newItem) {
                EduStreamInfo oldStream = oldItem.getStreamInfo();
                EduStreamInfo newStream = newItem.getStreamInfo();
                boolean a = oldStream.getHasVideo() == newStream.getHasVideo()
                        && oldStream.getHasAudio() == newStream.getHasAudio()
                        && oldStream.getStreamUuid().equals(newStream.getStreamUuid())
                        && oldStream.getStreamName().equals(newStream.getStreamName())
                        && oldStream.getPublisher().equals(newStream.getPublisher())
                        && oldStream.getVideoSourceType().equals(newStream.getVideoSourceType());
                if (a) {
                    return true;
                } else {
                    return null;
                }
            }
        });
    }

    @NonNull
    @Override
    protected ViewHolder onCreateDefViewHolder(@NonNull ViewGroup parent, int viewType) {
        StageVideoView item = new StageVideoView(getContext());
        item.init();
        int width = getContext().getResources().getDimensionPixelSize(R.dimen.dp_95);
        int height = parent.getMeasuredHeight() - parent.getPaddingTop() - parent.getPaddingBottom();
        item.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        return new ViewHolder(item);
    }

    @Override
    protected void convert(@NonNull ViewHolder viewHolder, StageStreamInfo item, @NonNull List<?> payloads) {
        super.convert(viewHolder, item, payloads);
//        if (payloads.size() > 0) {
//            viewHolder.convert(item);
//        }
        if (payloads.isEmpty()) {
            viewHolder.convert(item);
        } else {
            /*判断是否需要播放奖励动画，同时动画播放完成后就把播放标志置空*/
            boolean a = !TextUtils.isEmpty(item.getGroupUuid()) && item.getGroupUuid().equals(rewardUuid);
            if (a || item.getStreamInfo().getPublisher().getUserUuid().equals(rewardUuid)) {
                viewHolder.view.setReward(item.getReward());
                viewHolder.rewardAnim();
            }
            if (getItemPosition(item) == getData().size() - 1) {
                this.rewardUuid = null;
            }
        }
    }

    @Override
    protected void convert(@NonNull ViewHolder viewHolder, StageStreamInfo item) {
        viewHolder.convert(item);
        Activity activity = (Activity) viewHolder.view.getContext();
        if (item.getStreamInfo().getHasVideo() && activity instanceof BaseClassActivity) {
            ((BaseClassActivity) activity).renderStream(
                    ((BaseClassActivity) activity).getMainEduRoom(), item.getStreamInfo(),
                    viewHolder.view.getVideoLayout());
        }
    }

    public void setNewList(@Nullable List<StageStreamInfo> newData, String localUserUuid) {
        this.localUserUuid = localUserUuid;
        List<StageStreamInfo> list = new ArrayList<>();
        list.addAll(newData);
        ((Activity) getContext()).runOnUiThread(() -> {
            setDiffNewData(list);
        });
    }

    /**调用一次，奖励加一*/
    public void notifyRewardByUser(String userUuid) {
        List<StageStreamInfo> stageStreams = getData();
        for (int i = 0; i < stageStreams.size(); i++) {
            StageStreamInfo element = stageStreams.get(i);
            String uuid = element.getStreamInfo().getPublisher().getUserUuid();
            if (uuid.equals(userUuid)) {
                element.setReward(element.getReward() + 1);
                this.rewardUuid = uuid;
                final int finalI = i;
                ((Activity) getContext()).runOnUiThread(() -> notifyItemChanged(finalI, "notifyRewardByUser"));
            }
        }
    }

    /**调用一次，奖励加一*/
    public void notifyRewardByGroup(String groupUuid) {
        this.rewardUuid = groupUuid;
        int size = 0;
        List<StageStreamInfo> stageStreams = getData();
        for (int i = 0; i < stageStreams.size(); i++) {
            StageStreamInfo element = stageStreams.get(i);
            if (element.getGroupUuid().equals(groupUuid)) {
                element.setReward(element.getReward() + 1);
                size++;
            }
        }
        final int finalSize = size;
        ((Activity) getContext()).runOnUiThread(() ->
                notifyItemRangeChanged(0, finalSize, "notifyRewardByGroup"));
    }

    static class ViewHolder extends BaseViewHolder {
        private StageVideoView view;

        ViewHolder(StageVideoView view) {
            super(view);
            this.view = view;
        }

        void convert(StageStreamInfo item) {
            view.muteAudio(!item.getStreamInfo().getHasAudio());
            view.setName(item.getStreamInfo().getPublisher().getUserName());
            view.setReward(item.getReward());
            view.enableVideo(item.getStreamInfo().getHasVideo());
        }

        public void rewardAnim() {
            view.showRewardAnim();
        }
    }

}
