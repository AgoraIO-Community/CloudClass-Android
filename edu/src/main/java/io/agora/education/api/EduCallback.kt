package io.agora.education.api

import io.agora.education.api.base.EduError

interface EduCallback<T>{
    fun onSuccess(res: T?)

    fun onFailure(error: EduError)

}
