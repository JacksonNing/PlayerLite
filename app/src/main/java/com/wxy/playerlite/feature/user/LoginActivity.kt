package com.wxy.playerlite.feature.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wxy.playerlite.R
import com.wxy.playerlite.ui.theme.PlayerLiteAppTheme
import com.wxy.playerlite.ui.theme.applyInitialPlayerLiteSystemBars

internal const val LOGIN_WELCOME_TITLE = "登录后解锁在线播放"
internal const val LOGIN_WELCOME_SUBTITLE = "本地播放仍可直接使用"

class LoginActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyInitialPlayerLiteSystemBars()

        setContent {
            val state = viewModel.uiStateFlow.collectAsStateWithLifecycle().value
            LaunchedEffect(state.loginSucceeded) {
                if (state.loginSucceeded) {
                    viewModel.consumeLoginSuccess()
                    finish()
                }
            }
            LaunchedEffect(state.skipRequested) {
                if (state.skipRequested) {
                    viewModel.consumeSkipRequested()
                    finish()
                }
            }
            PlayerLiteAppTheme {
                LoginScreen(
                    state = state,
                    onLoginMethodSelected = viewModel::selectLoginMethod,
                    onPhoneChanged = viewModel::updatePhone,
                    onEmailChanged = viewModel::updateEmail,
                    onPasswordChanged = viewModel::updatePassword,
                    onSubmitLogin = viewModel::submitLogin,
                    onSkip = viewModel::skipLogin,
                    onLogout = viewModel::logout
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }
}

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onLoginMethodSelected: (LoginMethod) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitLogin: () -> Unit,
    onSkip: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AccountPageBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .testTag("login_scroll_content")
                    .padding(
                        start = AccountVisualStyle.contentHorizontalPadding,
                        top = 12.dp,
                        end = AccountVisualStyle.contentHorizontalPadding,
                        bottom = 28.dp
                    ),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginTopBar(
                    onSkip = onSkip,
                    skipEnabled = !state.isBusy
                )
                Spacer(modifier = Modifier.height(64.dp))
                LoginIntro()
                Spacer(modifier = Modifier.height(44.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = AccountVisualStyle.contentMaxWidth)
                        .testTag("login_form_section"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoginFormSection(
                        state = state,
                        onLoginMethodSelected = onLoginMethodSelected,
                        onPhoneChanged = onPhoneChanged,
                        onEmailChanged = onEmailChanged,
                        onPasswordChanged = onPasswordChanged,
                        onSubmitLogin = onSubmitLogin,
                        onLogout = onLogout
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LoginTopBar(
    onSkip: () -> Unit,
    skipEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = AccountVisualStyle.contentMaxWidth)
            .height(48.dp)
            .testTag("login_brand_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_playerlite_note_brand),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "PlayerLite",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        TextButton(
            onClick = onSkip,
            enabled = skipEnabled,
            modifier = Modifier.testTag("login_skip_button")
        ) {
            Text(
                text = "跳过",
                style = MaterialTheme.typography.bodyLarge,
                color = AccountVisualTheme.accent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LoginIntro() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = AccountVisualStyle.contentMaxWidth)
            .testTag("login_intro_block"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = LOGIN_WELCOME_TITLE,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("login_welcome_title")
        )
        Text(
            text = LOGIN_WELCOME_SUBTITLE,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("login_welcome_subtitle")
        )
    }
}

@Composable
private fun LoginFormSection(
    state: LoginUiState,
    onLoginMethodSelected: (LoginMethod) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitLogin: () -> Unit,
    onLogout: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LoginMethodToggle(
        selected = state.loginMethod,
        onSelected = onLoginMethodSelected
    )

    if (state.loginMethod == LoginMethod.PHONE) {
        LoginInputField(
            value = state.phone,
            onValueChange = onPhoneChanged,
            label = "手机号",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null
                )
            },
            enabled = !state.isBusy
        )
    } else {
        LoginInputField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = "邮箱",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AlternateEmail,
                    contentDescription = null
                )
            },
            enabled = !state.isBusy,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
        )
    }

    LoginInputField(
        value = state.password,
        onValueChange = onPasswordChanged,
        label = "密码",
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null
            )
        },
        enabled = !state.isBusy,
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible },
                enabled = !state.isBusy,
                modifier = Modifier.testTag("login_password_visibility_button")
            ) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Rounded.VisibilityOff
                    } else {
                        Icons.Rounded.Visibility
                    },
                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    )

    AccountPrimaryButton(
        text = if (state.isBusy) "登录中..." else "登录",
        onClick = onSubmitLogin,
        enabled = !state.isBusy,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_primary_button")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = state.statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }

    if (state.isLoggedIn) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onLogout,
                enabled = !state.isBusy
            ) {
                Text(
                    text = "退出登录",
                    color = AccountVisualTheme.accent
                )
            }
        }
    }
}

@Composable
private fun LoginMethodToggle(
    selected: LoginMethod,
    onSelected: (LoginMethod) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LoginMethodOption(
                modifier = Modifier.weight(1f),
                label = "手机号",
                selected = selected == LoginMethod.PHONE,
                onClick = { onSelected(LoginMethod.PHONE) }
            )
            LoginMethodOption(
                modifier = Modifier.weight(1f),
                label = "邮箱",
                selected = selected == LoginMethod.EMAIL,
                onClick = { onSelected(LoginMethod.EMAIL) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
        )
    }
}

@Composable
private fun LoginMethodOption(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.testTag(
            if (label == "手机号") "login_method_phone_tab" else "login_method_email_tab"
        ),
        onClick = onClick,
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        AccountVisualTheme.accent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .testTag(
                        if (label == "手机号") {
                            "login_method_phone_indicator"
                        } else {
                            "login_method_email_indicator"
                        }
                    )
                    .background(
                        if (selected) {
                            AccountVisualTheme.accent
                        } else {
                            Color.Transparent
                        }
                    )
            )
        }
    }
}

@Composable
private fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccountVisualTheme.accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.36f),
            focusedLabelColor = AccountVisualTheme.accent,
            focusedLeadingIconColor = AccountVisualTheme.accent,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}
