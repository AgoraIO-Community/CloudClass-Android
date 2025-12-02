package io.agora.agoraeduuikit.impl.users

enum class RosterType(private val value: Int) {
    SmallClass(0), LargeClass(1);

    fun value(): Int {
        return this.value
    }
}