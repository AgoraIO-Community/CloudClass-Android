package io.agora.base.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class MapBean extends JsonBean {
    public Map<String, Object> toMap() {
        String json = toJsonString();
        return new Gson().fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}
