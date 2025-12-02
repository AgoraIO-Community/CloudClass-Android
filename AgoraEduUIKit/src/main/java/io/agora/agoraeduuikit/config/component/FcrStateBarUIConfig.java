package io.agora.agoraeduuikit.config.component;

/**
 * author : felix
 * date : 2022/7/11
 * description : Head区域（状态栏组件）
 */
public class FcrStateBarUIConfig {
    public NetworkState networkState = new NetworkState();
    public RoomName roomName = new RoomName();
    public ScheduleTime scheduleTime = new ScheduleTime();

    /**
     * 网络状态
     */
    public static class NetworkState extends FcrBaseUIConfig {
    }

    /**
     * 房间名
     */
    public static class RoomName extends FcrBaseUIConfig {

    }

    /**
     * 课程时间
     */
    public static class ScheduleTime extends FcrBaseUIConfig {

    }
}
