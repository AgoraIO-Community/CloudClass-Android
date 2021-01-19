package io.agora.record;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.BusinessException;
import io.agora.base.network.RetrofitManager;
import io.agora.edu.common.bean.ResponseBody;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;

import static io.agora.edu.BuildConfig.API_BASE_URL;

public class ReplayImpl implements Replay {

    @Override
    public void replayList(String appId, String roomId, int next, EduCallback<ReplayRes> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, ReplayService.class)
                .record(appId, roomId, next)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<ReplayRes>>() {
                    @Override
                    public void onSuccess(@Nullable ResponseBody<ReplayRes> res) {
                        if (res != null && res.data != null) {
                            ReplayRes replayRes = res.data;
                            callback.onSuccess(replayRes);
                        }
                    }

                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        if(throwable instanceof BusinessException) {
                            BusinessException e = (BusinessException) throwable;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(throwable.getMessage()));
                        }
                    }
                }));
    }

    private int nextId = 0, total = 0;
    private List<ReplayRes.RecordDetail> recordDetails = new ArrayList<>();

    @Override
    public void allReplayList(String appId, String roomId, int next, EduCallback<List<ReplayRes.RecordDetail>> callback) {
        RetrofitManager.instance().getService(API_BASE_URL, ReplayService.class)
                .record(appId, roomId, next)
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<ReplayRes>>() {
                    @Override
                    public void onSuccess(@Nullable ResponseBody<ReplayRes> res) {
                        if (res != null && res.data != null) {
                            total = res.data.total;
                            nextId = res.data.nextId;
                            recordDetails.addAll(res.data.list);
                            if (recordDetails.size() < total) {
                                allReplayList(appId, roomId, nextId, callback);
                            } else {
                                nextId = total = 0;
                                List<ReplayRes.RecordDetail> list = new ArrayList<>();
                                for (ReplayRes.RecordDetail element : recordDetails) {
                                    try {
                                        list.add(element.clone());
                                    }
                                    catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                recordDetails.clear();
                                callback.onSuccess(list);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        if(throwable instanceof BusinessException) {
                            BusinessException e = (BusinessException) throwable;
                            callback.onFailure(new EduError(e.getCode(), e.getMessage()));
                        } else {
                            callback.onFailure(EduError.Companion.customMsgError(throwable.getMessage()));
                        }
                    }
                }));
    }
}
