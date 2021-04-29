package io.agora.education;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import io.agora.edu.launch.AgoraEduSDK;

import static io.agora.education.Constants.KEY_SP;
import static io.agora.education.Constants.POLICYURL;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = SettingActivity.class.getSimpleName();

    @BindView(R.id.switch_eye_care)
    protected Switch switch_eye_care;
    @BindView(R.id.version_TextView)
    protected TextView versionTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        switch_eye_care.setChecked(PreferenceManager.get(KEY_SP, false));
        versionTextView.setText(String.format(getString(R.string.version), BuildConfig.VERSION_NAME,
                AgoraEduSDK.version()));
    }

    @OnClick({R.id.iv_back, R.id.layout_policy})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.layout_policy:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(POLICYURL));
                startActivity(intent);
                break;
            case R.id.layout_log:
//                /**有bug,暂时隐藏不使用*/
//                /**上传log*/
//                getManager().uploadDebugItem(DebugItem.LOG, new EduCallback<String>() {
//                    @Override
//                    public void onSuccess(@Nullable String res) {
//                        Log.e(TAG, "日志上传成功->" + res);
//                    }
//
//                    @Override
//                    public void onFailure(@NotNull EduError error) {
//                        Log.e(TAG, "日志上传错误->code:" + error.getType() + ", reason:" + error.getMsg());
//                    }
//                });
                break;
        }
    }

    @OnCheckedChanged(R.id.switch_eye_care)
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        PreferenceManager.put(KEY_SP, isChecked);
    }

}
