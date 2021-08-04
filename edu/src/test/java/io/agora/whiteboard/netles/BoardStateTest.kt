package io.agora.whiteboard.netles

import io.agora.edu.common.bean.board.BoardState
import org.junit.Test

class BoardStateTest {
    @Test
    fun stateTest() {
        val state = BoardState()
        assert(state.userDefinedPropertyEquals(null))

        val state1 = BoardState()
        val map1 = mutableMapOf<String, Any>()
        map1["property1"] = 1
        map1["property2"] = "value1"
        state1.userDefinedProperties = map1
        assert(!state.userDefinedPropertyEquals(state1))
        assert(!state1.userDefinedPropertyEquals(null))

        val map = mutableMapOf<String, Any>()
        map["property1"] = 1
        map["property2"] = "value1"
        state.userDefinedProperties = map
        assert(state.userDefinedPropertyEquals(state1))

        val submap = mutableMapOf<String, Any>()
        submap["property12"] = mutableListOf<Any>()
        map["property3"] = submap
        assert(!state.userDefinedPropertyEquals(state1))

        val submap1 = mutableMapOf<String, Any>()
        submap1["property12"] = mutableListOf<Any>()
        map1["property3"] = submap1
        assert(state.userDefinedPropertyEquals(state1))

        map["property1"] = "String"
        assert(!state.userDefinedPropertyEquals(state1))
    }
}