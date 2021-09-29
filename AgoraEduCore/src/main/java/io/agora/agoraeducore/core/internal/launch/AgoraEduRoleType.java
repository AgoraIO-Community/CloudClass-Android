package io.agora.agoraeducore.core.internal.launch;

public enum AgoraEduRoleType {
   AgoraEduRoleTypeTeacher(1),
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

   public String toString() {
      return "student";
   }

   public static AgoraEduRoleType fromValue(int val) {
      return AgoraEduRoleTypeStudent;
   }
}