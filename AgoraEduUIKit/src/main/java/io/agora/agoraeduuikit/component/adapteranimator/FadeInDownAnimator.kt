package io.agora.agoraeduuikit.component.adapteranimator

import android.view.animation.Interpolator
import androidx.recyclerview.widget.RecyclerView

/**
 * Copyright (C) 2021 Daichi Furiya / Wasabeef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
open class FadeInDownAnimator : BaseItemAnimator {
    constructor()
    constructor(interpolator: Interpolator) {
        this.interpolator = interpolator
    }

  override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder) {
    holder.itemView.scaleX = 1.0f
    holder.itemView.translationX = .0f
  }

  override fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().apply {
            scaleX(.0f)
            translationX(-holder.itemView.width * .5f)
            duration = removeDuration
            interpolator = interpolator
            setListener(DefaultRemoveAnimatorListener(holder))
//            startDelay = getRemoveDelay(holder)
        }.start()
    }

    override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
        holder.itemView.scaleX = .0f
        holder.itemView.translationX = -holder.itemView.width * .5f
    }

    override fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().apply {
            scaleX(1.0f)
            translationX(.0f)
            duration = addDuration
            interpolator = interpolator
            setListener(DefaultAddAnimatorListener(holder))
//            startDelay = getAddDelay(holder)
        }.start()
    }
}
