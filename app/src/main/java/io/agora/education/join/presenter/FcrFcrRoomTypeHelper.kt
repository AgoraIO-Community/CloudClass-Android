package io.agora.education.join.presenter

import android.content.Context
import com.hyphenate.easeimgroup.modules.view.ui.widget.RoomType
import io.agora.education.R
import io.agora.education.join.FcrSelectRoomTypeDialog

/**
 * author : felix
 * date : 2023/8/8
 * description :
 */
object FcrFcrRoomTypeHelper {
    private val list = ArrayList<FcrSelectRoomTypeDialog.FcrRoomType>()

    fun getDefRoomType(context: Context): FcrSelectRoomTypeDialog.FcrRoomType {
        return getRoomTypeList(context)[1]
    }

    fun getRoomTypeList(context: Context): List<FcrSelectRoomTypeDialog.FcrRoomType> {
        if (list.isEmpty()) {
            list.add(
                FcrSelectRoomTypeDialog.FcrRoomType(
                    RoomType.LARGE_CLASS,
                    context.resources.getString(R.string.fcr_login_free_class_mode_option_lecture_hall),
                )
            )
            list.add(
                FcrSelectRoomTypeDialog.FcrRoomType(
                    RoomType.SMALL_CLASS,
                    context.resources.getString(R.string.fcr_login_free_class_mode_option_small_classroom),
                )
            )
            list.add(
                FcrSelectRoomTypeDialog.FcrRoomType(
                    RoomType.ONE_ON_ONE,
                    context.resources.getString(R.string.fcr_login_free_class_mode_option_1on1)
                )
            )
        }
        return list
    }
}