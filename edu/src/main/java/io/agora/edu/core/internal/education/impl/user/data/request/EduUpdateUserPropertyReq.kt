package io.agora.edu.core.internal.education.impl.user.data.request

class EduUpdateUserPropertyReq(
        val value: String,
        val cause: MutableMap<String, String>
) {}