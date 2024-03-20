package io.agora.education.utils

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.education.setting.FcrWebviewActivity


/**
 * author : felix
 * date : 2023/7/28
 * description :
 */
object FcrPrivateProtocolUtils {
    val text1 = "用户协议"
    val text2 = "隐私协议"
    var text3 = "我已阅读并同意"
    var text4 = "和"
    var text5 = "为了更好地保障您的合法权益，请您阅读并同意以下协议"

    //个人信息收集公示
    var dataCollectionUrl = "https://solutions-apaas.agora.io/static/assets/data_collection.html"

    //第三方信息数据共享
    val dataShareUrl = "https://solutions-apaas.agora.io/static/assets/third_sdk.html"

    // 用户服务协议
    var userAgreement = "https://solutions-apaas.agora.io/static/assets/user_agreement.html"

    // 灵动课堂隐私政策
    var privacyPolicy = "https://solutions-apaas.agora.io/static/assets/privacy_policy.html"

    // 用户协议（海外）
    var userService = "https://www.agora.io/en/terms-of-service/"

    fun getPrivateProtocol(context: Context, onClickTextListener: (() -> Unit)): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        builder.append(getSpannableNormalText({
            onClickTextListener.invoke()
        }, context.getString(R.string.fcr_login_free_option_read_agree)))

        // 国内外协议不一样
        val region = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
        if (AgoraEduRegion.cn == region) {
            // 国内：用户协议+隐私协议
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(context, context.getString(R.string.fcr_login_label_terms_of_service),userAgreement)
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
            builder.append(context.getString(R.string.fcr_login_free_option_read_agree_and))
            builder.append(getSpannableText({
                // 隐私协议
                FcrWebviewActivity.startWebView(context,context.getString(R.string.fcr_login_label_privacy_policy), privacyPolicy)
            }, context.getString(R.string.fcr_login_label_privacy_policy)))
        } else {
            builder.append(" ")
            // 国外：用户协议
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(context,context.getString(R.string.fcr_login_label_terms_of_service), userService)
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
        }
        return builder
    }

    fun getPrivateProtocolInfo(context: Context, onClickTextListener: (() -> Unit)): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        builder.append(getSpannableNormalText({
            onClickTextListener.invoke()
        }, context.getString(R.string.fcr_login_popup_window_label_content1)))

        // 国内外协议不一样
        val region = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
        //val region = AgoraEduRegion.na
        if (AgoraEduRegion.cn == region) {
            // 国内：用户协议+隐私协议
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_terms_of_service),
                    userAgreement
                )
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))

            builder.append(context.getString(R.string.fcr_login_free_option_read_agree_and))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 隐私协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_privacy_policy),
                    privacyPolicy
                )
            }, context.getString(R.string.fcr_login_label_privacy_policy)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))
        } else {
            // 国外：用户协议
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_terms_of_service),
                    userService
                )
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))
        }
        builder.append(getSpannableNormalText({
            onClickTextListener.invoke()
        }, context.getString(R.string.fcr_login_popup_window_label_content2)))
        if (AgoraEduRegion.cn == region) {
            // 国内：用户协议+隐私协议
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_terms_of_service),
                    userAgreement
                )
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))
            builder.append(context.getString(R.string.fcr_login_free_option_read_agree_and))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 隐私协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_privacy_policy),
                    privacyPolicy
                )
            }, context.getString(R.string.fcr_login_label_privacy_policy)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))
        } else {
            // 国外：用户协议
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_terms_of_service),
                    userService
                )
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))
        }
        builder.append(getSpannableNormalText({
            onClickTextListener.invoke()
        }, context.getString(R.string.fcr_login_popup_window_label_content3)))
        return builder
    }

    fun getSpannableNormalText(onSpannableListener: (() -> Unit), text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        spannable.setSpan(object : MyClickableSpan() {
            override fun onClick(widget: android.view.View) {
                onSpannableListener.invoke()
            }
        }, 0, text.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    fun getSpannableText(onSpannableListener: (() -> Unit)? = null, text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        spannable.setSpan(object : MyClickableSpan() {
            override fun onClick(widget: android.view.View) {
                onSpannableListener?.invoke()
            }
        }, 0, text.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#4262FF")),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    fun showAgreeDialog(context: Context, onAgreeListener: (Boolean) -> Unit) {
        AgoraUIDialogBuilder(context)
            .setShowClose(true)
            .title(context.resources.getString(R.string.fcr_login_popup_window_label_title_again))
            .message(getAgainAgree(context))
            .negativeText(context.resources.getString(R.string.fcr_login_popup_window_again_button_disagree))
            .positiveText(context.resources.getString(R.string.fcr_login_popup_window_again_button_agree))
            .positiveClick {
                onAgreeListener.invoke(true)
            }
            .negativeClick {
                onAgreeListener.invoke(false)
            }
            .build()
            .show()
    }

    /**
     * 只有国内才有
     */
    fun getAgainAgree(context: Context): SpannableStringBuilder{
        val builder = SpannableStringBuilder()
        builder.append(context.getString(R.string.fcr_login_popup_window_label_content_again1))

        // 国内外协议不一样
        val region = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn)
        //val region = AgoraEduRegion.na
        if (AgoraEduRegion.cn == region) {
            // 国内：用户协议+隐私协议
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_terms_of_service),
                    userAgreement
                )
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))

            builder.append(context.getString(R.string.fcr_login_free_option_read_agree_and))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 隐私协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_privacy_policy),
                    privacyPolicy
                )
            }, context.getString(R.string.fcr_login_label_privacy_policy)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))
        } else {
            // 国外：用户协议
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content4)))
            builder.append(getSpannableText({
                // 用户协议
                FcrWebviewActivity.startWebView(
                    context,
                    context.getString(R.string.fcr_login_label_terms_of_service),
                    userService
                )
            }, context.getString(R.string.fcr_login_label_terms_of_service)))
            builder.append(getSpannableText(null, context.getString(R.string.fcr_login_popup_window_label_content5)))
        }
        builder.append(context.getString(R.string.fcr_login_popup_window_label_content_again2))
        return builder
    }

    fun showAgreeDialog2(context: Context) {
        AgoraUIDialogBuilder(context)
            .setShowClose(true)
            .title("服务协议及隐私保护")
            //.title(context.resources.getString(R.string.fcr_room_class_leave_class_title))
            .message(getAgreeDialog2())
            .negativeText("拒绝")
            .positiveText("同意并继续")
            .positiveClick {

            }
            .negativeClick {

            }
            .build()
            .show()
    }

    fun getAgreeDialog2(): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        builder.append(text5)
        builder.append(getSpannableText({
            // 用户协议

        }, text1))
        builder.append(text4)
        builder.append(getSpannableText({
            // 隐私协议

        }, text2))
        return builder
    }


    abstract class MyClickableSpan : ClickableSpan() {
        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
        }
    }
}