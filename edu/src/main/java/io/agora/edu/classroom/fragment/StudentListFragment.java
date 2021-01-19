package io.agora.edu.classroom.fragment;

import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.agora.edu.R2;

import butterknife.BindView;
import io.agora.edu.R;
import io.agora.edu.base.BaseFragment;
import io.agora.edu.classroom.BaseClassActivity;
import io.agora.edu.classroom.adapter.StudentListAdapter;
import io.agora.edu.classroom.bean.group.GroupMemberInfo;

public class StudentListFragment extends BaseFragment implements OnItemChildClickListener {
    private static final String TAG = StudentListFragment.class.getSimpleName();

    @BindView(R2.id.rcv_students)
    RecyclerView rcvStudents;

    private String localUserUuid;
    private StudentListAdapter studentListAdapter;

    public StudentListFragment() {
    }

    public StudentListFragment(String localUserUuid) {
        this.localUserUuid = localUserUuid;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_studentlist_layout;
    }

    @Override
    protected void initData() {
        studentListAdapter = new StudentListAdapter(localUserUuid);
        studentListAdapter.setOnItemChildClickListener(this);
    }

    @Override
    protected void initView() {
        rcvStudents.setAdapter(studentListAdapter);
    }

    public void updateLocalUserUuid(String userUuid) {
        localUserUuid = userUuid;
        studentListAdapter.updateLocalUserUuid(userUuid);
    }

    public void updateStudentList(List<GroupMemberInfo> allStudent) {
        List<GroupMemberInfo> onlineStudents = new ArrayList<>();
        if (allStudent != null && allStudent.size() > 0) {
            /**本地用户始终在第一位*/
            if (!TextUtils.isEmpty(localUserUuid)) {
                for (int i = 0; i < allStudent.size(); i++) {
                    GroupMemberInfo memberInfo = allStudent.get(i);
                    if (memberInfo.getUuid().equals(localUserUuid)) {
                        if (i != 0) {
                            Collections.swap(allStudent, 0, i);
                            break;
                        }
                    }
                }
            }
            /**过滤掉不在线的人数*/
            for (GroupMemberInfo memberInfo : allStudent) {
                if (memberInfo.getOnline()) {
                    onlineStudents.add(memberInfo);
                }
            }
            if (rcvStudents.isComputingLayout()) {
                rcvStudents.postDelayed(() -> {
                    studentListAdapter.updateStudentList(onlineStudents);
                }, 300);
            } else {
                rcvStudents.post(() -> studentListAdapter.updateStudentList(onlineStudents));
            }
        }
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (context instanceof BaseClassActivity) {
            boolean isSelected = view.isSelected();
            int id = view.getId();
            if (id == R.id.iv_btn_mute_audio) {
                ((BaseClassActivity) context).muteLocalAudio(isSelected);
            } else if (id == R.id.iv_btn_mute_video) {
                ((BaseClassActivity) context).muteLocalVideo(isSelected);
            }
        }
    }
}
