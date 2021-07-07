package io.agora.edu.launch;

public enum AgoraEduRoleType {
   AgoraEduRoleTypeStudent(2);

   private int value;

   public final int getValue() {
      return this.value;
   }

   public final void setValue(int var1) {
      this.value = var1;
   }

   private AgoraEduRoleType(int value) {
      this.value = value;
   }

   public static final boolean isValid(int type) {
      return type == AgoraEduRoleTypeStudent.value;
   }
}