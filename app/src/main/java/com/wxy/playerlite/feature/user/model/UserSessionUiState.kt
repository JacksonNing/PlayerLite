package com.wxy.playerlite.feature.user.model

import com.wxy.playerlite.user.model.LoginState

internal data class UserSessionUiState(
    val isLoggedIn: Boolean = false,
    val isBusy: Boolean = false,
    val title: String = "未登录",
    val summary: String = "本地可用，登录后开启在线播放",
    val avatarUrl: String? = null
)

internal fun LoginState.toUserSessionUiState(isBusy: Boolean): UserSessionUiState {
    return when (this) {
        LoginState.LoggedOut -> UserSessionUiState(
            isLoggedIn = false,
            isBusy = isBusy,
            title = "未登录",
            summary = "本地可用，登录后开启在线播放",
            avatarUrl = null
        )

        is LoginState.LoggedIn -> {
            val userInfo = session.userInfo
            val summary = buildString {
                append(userInfo.accountIdentity ?: "在线账户")
                userInfo.level?.let { append(" · Lv.$it") }
                append(" · Cookie 已就绪")
            }
            UserSessionUiState(
                isLoggedIn = true,
                isBusy = isBusy,
                title = userInfo.nickname,
                summary = summary,
                avatarUrl = userInfo.avatarUrl.takeIf { it.isNotBlank() }
            )
        }
    }
}
