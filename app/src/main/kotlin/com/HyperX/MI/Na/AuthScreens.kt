package com.HyperX.MI.Na

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle

/*
 * 全屏沉浸式 登录 / 注册 页面。
 *
 * 设计约定：
 *  - 页面铺满：statusBarsPadding + navigationBarsPadding + imePadding，键盘弹出不遮挡输入框；
 *    顶部只有「返回按钮（左对齐）+ 标题」，无底栏、无其它 chrome。
 *  - 背景：液态玻璃模式保持透明（壁纸由外层父层透出，这里不再画一层壁纸），玻璃卡片采样 backdrop；
 *    MD3 模式使用 MaterialTheme.colorScheme.surfaceContainer 作为整页底色。
 *  - 双风格：按 LocalMd3Style.current 分支 —— 液态用真实 backdrop 玻璃输入框 + SearchLiquidButton，
 *    MD3 用 OutlinedTextField（带 label，容器 surfaceContainerHigh）+ material3.Button。
 *  - 校验仅为 UX 层（服务端才是权威）；网络请求仍走 viewModel.login / viewModel.register。
 */

// ---------------------------------------------------------------------------
// UX 层校验（服务端才是权威）
// ---------------------------------------------------------------------------

private const val AUTH_PASSWORD_MIN_LENGTH = 8
private const val AUTH_NICKNAME_MAX_LENGTH = 24
private val AUTH_USERNAME_PATTERN = Regex("^[A-Za-z0-9_]{3,32}$")

/** 登录页：只要求非空（宽松，具体规则交给服务端）。 */
internal fun authLoginUsernameError(raw: String): String? =
    if (raw.trim().isEmpty()) "请输入用户名" else null

internal fun authLoginPasswordError(raw: String): String? =
    if (raw.isEmpty()) "请输入密码" else null

/** 注册页：用户名 3-32 位字母数字下划线。 */
internal fun authUsernameError(raw: String): String? {
    val v = raw.trim()
    return when {
        v.isEmpty() -> "请输入用户名"
        !AUTH_USERNAME_PATTERN.matches(v) -> "用户名需为 3-32 位字母、数字或下划线"
        else -> null
    }
}

/** 注册页：密码 ≥8 且同时含字母与数字。 */
internal fun authPasswordError(raw: String): String? = when {
    raw.isEmpty() -> "请输入密码"
    raw.length < AUTH_PASSWORD_MIN_LENGTH -> "密码至少 $AUTH_PASSWORD_MIN_LENGTH 位"
    !raw.any { it.isLetter() } || !raw.any { it.isDigit() } -> "密码需同时包含字母和数字"
    else -> null
}

internal fun authConfirmPasswordError(password: String, confirm: String): String? = when {
    confirm.isEmpty() -> "请再次输入密码"
    confirm != password -> "两次输入的密码不一致"
    else -> null
}

/** 注册页：昵称 1-24。 */
internal fun authNicknameError(raw: String): String? {
    val v = raw.trim()
    return when {
        v.isEmpty() -> "请输入昵称"
        v.length > AUTH_NICKNAME_MAX_LENGTH -> "昵称最多 $AUTH_NICKNAME_MAX_LENGTH 个字符"
        else -> null
    }
}

// ---------------------------------------------------------------------------
// 入口：登录 / 注册 流程
// ---------------------------------------------------------------------------

/**
 * 登录 / 注册全屏流程。注册页由本流程内部切换（带回退：注册页返回键回到登录页，登录页返回键退出流程）。
 * 注册成功 = 已自动登录（viewModel.register 内部会写入 token 与登录态），因此直接回调 onAuthSuccess 返回。
 */
@Composable
fun AuthFlowScreen(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit = onBack
) {
    var isRegister by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (isRegister) isRegister = false else onBack()
    }

    AnimatedContent(
        targetState = isRegister,
        transitionSpec = {
            val direction = if (targetState) 1 else -1
            (fadeIn(Motion.enter()) + slideInHorizontally(Motion.enter()) { direction * it / 10 }) togetherWith
                    (fadeOut(Motion.exit()) + slideOutHorizontally(Motion.exit()) { -direction * it / 10 })
        },
        label = "AuthModeTransition",
        modifier = Modifier.fillMaxSize()
    ) { register ->
        if (register) {
            AuthRegisterScreen(
                viewModel = viewModel,
                backdrop = backdrop,
                onBack = { isRegister = false },
                onSwitchToLogin = { isRegister = false },
                onSuccess = onAuthSuccess
            )
        } else {
            AuthLoginScreen(
                viewModel = viewModel,
                backdrop = backdrop,
                onBack = onBack,
                onSwitchToRegister = { isRegister = true },
                onSuccess = onAuthSuccess
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 登录页
// ---------------------------------------------------------------------------

@Composable
fun AuthLoginScreen(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit,
    onSwitchToRegister: () -> Unit,
    onSuccess: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var serverMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val usernameError = authLoginUsernameError(username)
    val passwordError = authLoginPasswordError(password)
    val formValid = usernameError == null && passwordError == null

    fun submit() {
        if (isLoading) return
        showErrors = true
        focusManager.clearFocus()
        if (usernameError != null || passwordError != null) return
        isLoading = true
        serverMessage = ""
        viewModel.login(username.trim(), password) { ok, msg ->
            isLoading = false
            if (ok) onSuccess() else serverMessage = msg.ifBlank { "登录失败，请稍后重试" }
        }
    }

    AuthPageScaffold(title = "登录", onBack = onBack) {
        AuthHero(title = "欢迎回来", subtitle = "登录后同步你的收藏、歌单与播放记录")
        Spacer(Modifier.height(20.dp))

        AuthFormContainer(backdrop = backdrop) {
            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = "用户名",
                placeholder = "3-32 位字母、数字或下划线",
                leadingIcon = Icons.Rounded.Person,
                backdrop = backdrop,
                error = if (username.isNotEmpty() || showErrors) usernameError else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "密码",
                placeholder = "请输入密码",
                leadingIcon = Icons.Rounded.Lock,
                backdrop = backdrop,
                error = if (password.isNotEmpty() || showErrors) passwordError else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibleToggle(
                        visible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible }
                    )
                }
            )

            AuthErrorBanner(serverMessage)

            AuthPrimaryButton(
                label = "登录",
                icon = Icons.AutoMirrored.Rounded.Login,
                backdrop = backdrop,
                ready = formValid,
                loading = isLoading,
                onClick = { submit() }
            )

            AuthSwitchAction(
                text = "没有账号？去注册",
                enabled = !isLoading,
                onClick = onSwitchToRegister
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 注册页
// ---------------------------------------------------------------------------

@Composable
fun AuthRegisterScreen(
    viewModel: PlayerViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onSuccess: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var serverMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val usernameError = authUsernameError(username)
    val passwordError = authPasswordError(password)
    val confirmPasswordError = authConfirmPasswordError(password, confirmPassword)
    val nicknameError = authNicknameError(nickname)
    val formValid = usernameError == null && passwordError == null &&
            confirmPasswordError == null && nicknameError == null

    fun submit() {
        if (isLoading) return
        showErrors = true
        focusManager.clearFocus()
        if (usernameError != null || passwordError != null ||
            confirmPasswordError != null || nicknameError != null
        ) return
        isLoading = true
        serverMessage = ""
        viewModel.register(username.trim(), password, nickname.trim()) { ok, msg ->
            isLoading = false
            if (ok) onSuccess() else serverMessage = msg.ifBlank { "注册失败，请稍后重试" }
        }
    }

    AuthPageScaffold(title = "注册", onBack = onBack) {
        AuthHero(title = "创建账号", subtitle = "注册成功后会自动登录并返回")
        Spacer(Modifier.height(20.dp))

        AuthFormContainer(backdrop = backdrop) {
            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = "用户名",
                placeholder = "3-32 位字母、数字或下划线",
                leadingIcon = Icons.Rounded.Person,
                backdrop = backdrop,
                error = if (username.isNotEmpty() || showErrors) usernameError else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "密码",
                placeholder = "至少 8 位，含字母与数字",
                leadingIcon = Icons.Rounded.Lock,
                backdrop = backdrop,
                error = if (password.isNotEmpty() || showErrors) passwordError else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibleToggle(
                        visible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible }
                    )
                }
            )

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "确认密码",
                placeholder = "请再次输入密码",
                leadingIcon = Icons.Rounded.Lock,
                backdrop = backdrop,
                error = if (confirmPassword.isNotEmpty() || showErrors) confirmPasswordError else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation()
            )

            AuthTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = "昵称",
                placeholder = "1-24 个字符",
                leadingIcon = Icons.Rounded.Badge,
                backdrop = backdrop,
                error = if (nickname.isNotEmpty() || showErrors) nicknameError else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )

            AuthErrorBanner(serverMessage)

            AuthPrimaryButton(
                label = "注册",
                icon = Icons.Rounded.PersonAdd,
                backdrop = backdrop,
                ready = formValid,
                loading = isLoading,
                onClick = { submit() }
            )

            AuthSwitchAction(
                text = "已有账号？去登录",
                enabled = !isLoading,
                onClick = onSwitchToLogin
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 页面骨架（全屏 + 沉浸式）
// ---------------------------------------------------------------------------

@Composable
private fun AuthPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val isMd3 = LocalMd3Style.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 液态玻璃：透明，让外层壁纸透出；MD3：surfaceContainer 整页底色
            .then(
                if (isMd3) Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            AuthTopBar(title = title, onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 大屏 / 横屏下限制表单宽度，避免输入框被拉得过长
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    content = content
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** 顶部只有返回按钮（左对齐）+ 标题，无其它 chrome。 */
@Composable
private fun AuthTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 20.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AuthHero(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 表单容器：
 *  - MD3：surfaceContainerHigh 卡片（M3 色调层次，输入框直接落在容器上）
 *  - 液态玻璃：不加外层容器，让每个输入框自带 glass 卡片（backdrop 采样更准确）
 */
@Composable
private fun AuthFormContainer(
    backdrop: Backdrop,
    content: @Composable ColumnScope.() -> Unit
) {
    val isMd3 = LocalMd3Style.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMd3)
                    Modifier.background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedRectangle(28f.dp)
                    )
                else Modifier
            )
            .padding(if (isMd3) 20.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(if (isMd3) 18.dp else 14.dp),
        content = content
    )
}

// ---------------------------------------------------------------------------
// 输入框（双风格）
// ---------------------------------------------------------------------------

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    backdrop: Backdrop,
    error: String?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 需求：液态玻璃模式下输入框也统一使用 MD3 样式
        val useMd3Field = true
        if (useMd3Field) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = {
                    Text(
                        placeholder,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = trailingIcon,
                isError = error != null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(20f.dp) },
                        effects = {
                            vibrancy()
                            // 无折射（lens）更清晰
                            blur(9f.dp.toPx())
                        },
                        highlight = { Highlight.Plain }
                    )
                    .clip(RoundedRectangle(20f.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                leadingIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Box {
                                    if (value.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                            trailingIcon?.invoke()
                        }
                    }
                )
            }
        }

        // 输入框下方小字错误提示（error 配色）
        if (error != null) {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = error,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PasswordVisibleToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = if (visible) "隐藏密码" else "显示密码",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// 按钮 / 提示（双风格）
// ---------------------------------------------------------------------------

/**
 * 主提交按钮：
 *  - 液态玻璃：SearchLiquidButton（真实 backdrop 玻璃，自动适配）
 *  - MD3：material3.Button（M3 规范，提交中禁用并内联进度）
 * 提交中（loading）按钮不可再次触发，并显示进度。
 */
@Composable
private fun AuthPrimaryButton(
    label: String,
    icon: ImageVector,
    backdrop: Backdrop,
    ready: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    // 登录/注册按钮统一走液态玻璃按钮：此前 MD3 模式用的是普通 Button、没有液态质感，
    // 用户明确要求两种模式下都要有液态效果。
    SearchLiquidButton(
        onClick = { if (!loading) onClick() },
        backdrop = backdrop,
        shape = RoundedCornerShape(20.dp),
        label = if (loading) "${label}中…" else label,
        icon = icon,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (loading) 0.7f else if (ready) 1f else 0.88f)
    )
}

@Composable
private fun AuthSwitchAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    }
}

/** 服务端返回的错误提示（校验只是 UX 层，服务端才是权威）。 */
@Composable
private fun AuthErrorBanner(message: String) {
    if (message.isEmpty()) return

    val containerColor = if (LocalMd3Style.current)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedRectangle(16f.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error
        )
    }
}
