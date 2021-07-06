// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: src/users.proto

package io.agora.report.v2.protobuf;

public interface ApaasUserJoinOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.protobuf.ApaasUserJoin)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * 时间戳，必须存在
   * </pre>
   *
   * <code>int64 lts = 1;</code>
   * @return The lts.
   */
  long getLts();

  /**
   * <pre>
   * vid
   * </pre>
   *
   * <code>int32 vid = 2;</code>
   * @return The vid.
   */
  int getVid();

  /**
   * <pre>
   * apaas版本号
   * </pre>
   *
   * <code>string ver = 3;</code>
   * @return The ver.
   */
  String getVer();
  /**
   * <pre>
   * apaas版本号
   * </pre>
   *
   * <code>string ver = 3;</code>
   * @return The bytes for ver.
   */
  com.google.protobuf.ByteString
      getVerBytes();

  /**
   * <pre>
   * apaas场景，如education/meeting/entertainment
   * </pre>
   *
   * <code>string scenario = 4;</code>
   * @return The scenario.
   */
  String getScenario();
  /**
   * <pre>
   * apaas场景，如education/meeting/entertainment
   * </pre>
   *
   * <code>string scenario = 4;</code>
   * @return The bytes for scenario.
   */
  com.google.protobuf.ByteString
      getScenarioBytes();

  /**
   * <pre>
   * 异常码，若有
   * </pre>
   *
   * <code>int32 errorCode = 5;</code>
   * @return The errorCode.
   */
  int getErrorCode();

  /**
   * <pre>
   * apaas用户id，同RTM uid
   * </pre>
   *
   * <code>string uid = 6;</code>
   * @return The uid.
   */
  String getUid();
  /**
   * <pre>
   * apaas用户id，同RTM uid
   * </pre>
   *
   * <code>string uid = 6;</code>
   * @return The bytes for uid.
   */
  com.google.protobuf.ByteString
      getUidBytes();

  /**
   * <pre>
   * 用户名，用于显示
   * </pre>
   *
   * <code>string userName = 7;</code>
   * @return The userName.
   */
  String getUserName();
  /**
   * <pre>
   * 用户名，用于显示
   * </pre>
   *
   * <code>string userName = 7;</code>
   * @return The bytes for userName.
   */
  com.google.protobuf.ByteString
      getUserNameBytes();

  /**
   * <pre>
   * rtc流id
   * </pre>
   *
   * <code>int64 streamUid = 8;</code>
   * @return The streamUid.
   */
  long getStreamUid();

  /**
   * <pre>
   * rtc流id
   * </pre>
   *
   * <code>string streamSuid = 9;</code>
   * @return The streamSuid.
   */
  String getStreamSuid();
  /**
   * <pre>
   * rtc流id
   * </pre>
   *
   * <code>string streamSuid = 9;</code>
   * @return The bytes for streamSuid.
   */
  com.google.protobuf.ByteString
      getStreamSuidBytes();

  /**
   * <pre>
   * apaas角色
   * </pre>
   *
   * <code>string role = 10;</code>
   * @return The role.
   */
  String getRole();
  /**
   * <pre>
   * apaas角色
   * </pre>
   *
   * <code>string role = 10;</code>
   * @return The bytes for role.
   */
  com.google.protobuf.ByteString
      getRoleBytes();

  /**
   * <pre>
   * rtc sid
   * </pre>
   *
   * <code>string streamSid = 11;</code>
   * @return The streamSid.
   */
  String getStreamSid();
  /**
   * <pre>
   * rtc sid
   * </pre>
   *
   * <code>string streamSid = 11;</code>
   * @return The bytes for streamSid.
   */
  com.google.protobuf.ByteString
      getStreamSidBytes();

  /**
   * <pre>
   * rtm sid
   * </pre>
   *
   * <code>string rtmSid = 12;</code>
   * @return The rtmSid.
   */
  String getRtmSid();
  /**
   * <pre>
   * rtm sid
   * </pre>
   *
   * <code>string rtmSid = 12;</code>
   * @return The bytes for rtmSid.
   */
  com.google.protobuf.ByteString
      getRtmSidBytes();

  /**
   * <pre>
   * apaas房间id，与rtc/rtm channelName相同
   * </pre>
   *
   * <code>string roomId = 13;</code>
   * @return The roomId.
   */
  String getRoomId();
  /**
   * <pre>
   * apaas房间id，与rtc/rtm channelName相同
   * </pre>
   *
   * <code>string roomId = 13;</code>
   * @return The bytes for roomId.
   */
  com.google.protobuf.ByteString
      getRoomIdBytes();

  /**
   * <pre>
   * room create timestamp
   * </pre>
   *
   * <code>int64 roomCreateTs = 14;</code>
   * @return The roomCreateTs.
   */
  long getRoomCreateTs();
}
