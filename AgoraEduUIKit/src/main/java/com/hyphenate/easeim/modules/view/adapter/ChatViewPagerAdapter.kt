package com.hyphenate.easeim.modules.view.adapter

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

/**
 * viewPager adapter
 */
class ChatViewPagerAdapter(private val mList: List<View>):PagerAdapter() {

    override fun getCount(): Int = mList.size
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
         return view == `object`
    }

    override fun getItemPosition(`object`: Any): Int {
        //第一种方法是直接返回POSITION_NONE
        //第二种就是先判断是否发生了修改再判断
        val index = mList.indexOf(`object`)
        if (index == -1) {
            return PagerAdapter.POSITION_NONE
        }
        return PagerAdapter.POSITION_UNCHANGED
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        container.addView(mList[position])
        return mList[position]
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(mList[position])
    }
}