package com.wxy.playerlite.feature.user

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.wxy.playerlite.ui.theme.PlayerLiteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultScreen_shouldShowFlatBrandIntroAndPrimaryActionHierarchy() {
        composeRule.setContent {
            PlayerLiteTheme {
                LoginScreen(
                    state = LoginUiState(),
                    onLoginMethodSelected = {},
                    onPhoneChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmitLogin = {},
                    onSkip = {},
                    onLogout = {}
                )
            }
        }

        composeRule.onNodeWithTag("login_brand_header").assertIsDisplayed()
        composeRule.onNodeWithTag("login_intro_block").assertIsDisplayed()
        composeRule.onNodeWithTag("login_form_section").assertIsDisplayed()
        composeRule.onNodeWithTag("login_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("login_scroll_content").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("login_primary_button").assertIsDisplayed()
        val phoneIndicatorBounds = composeRule
            .onNodeWithTag("login_method_phone_indicator", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val phoneIndicatorWidth = with(composeRule.density) {
            phoneIndicatorBounds.width.toDp()
        }
        assertTrue(
            "Expected a compact selected-tab indicator, but was $phoneIndicatorWidth",
            phoneIndicatorWidth in 47.dp..49.dp
        )
        composeRule
            .onNodeWithTag("login_password_visibility_button")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("显示密码")
            .performClick()
            .assertContentDescriptionEquals("隐藏密码")
    }
}
