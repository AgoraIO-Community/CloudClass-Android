package io.agora.edu.launch;

public enum AgoraEduRoomType {
   AgoraEduRoomType1V1(0),
   AgoraEduRoomTypeSmall(1),
   AgoraEduRoomTypeBig(2);

   private int value;

   public final int getValue() {
      return this.value;
   }

   public final void setValue(int var1) {
      this.value = var1;
   }

   private AgoraEduRoomType(int value) {
      this.value = value;
   }

   public static final boolean isValid(int type) {
      return type == AgoraEduRoomType1V1.getValue() || type == AgoraEduRoomTypeSmall.getValue() ||
              type == AgoraEduRoomTypeBig.getValue();
   }
}