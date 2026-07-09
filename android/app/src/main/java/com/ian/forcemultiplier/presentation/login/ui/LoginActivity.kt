package com.ian.forcemultiplier.presentation.login.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.ian.forcemultiplier.MainActivity
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.session.SessionManager
import com.ian.forcemultiplier.core.theme.ForceMultiplierTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supabaseClient.auth.currentUserOrNull() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContent {
            ForceMultiplierTheme {
                LoginScreen(
                    supabaseClient = supabaseClient,
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgBlack      = Color(0xFF080808)
private val SurfaceInput = Color(0xFF111111)
private val BorderInput  = Color(0xFF262626)
private val MutedText    = Color(0xFF6B6B6B)
private val BodyText     = Color(0xFFAAAAAA)
private val GreenPrimary = Color(0xFF2ED573)
private val White        = Color(0xFFFFFFFF)

@Composable
fun LoginScreen(
    supabaseClient: SupabaseClient,
    onLoginSuccess: () -> Unit
) {
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var isSignUp     by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var error        by remember { mutableStateOf<String?>(null) }
    var infoMessage  by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val scope        = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {

        // ── Bokeh orbs ──────────────────────────────────────────────────────
        // Top-right dark blob
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = 140.dp, y = (-90).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E1E1E), Color.Transparent),
                        center = Offset(180f, 180f),
                        radius = 420f
                    ),
                    CircleShape
                )
        )
        // Bottom-left dark blob
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1A1A), Color.Transparent),
                        center = Offset(160f, 160f),
                        radius = 360f
                    ),
                    CircleShape
                )
        )
        // Faint center ambient
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.Center)
                .offset(y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0D0D0D), Color.Transparent),
                        radius = 700f
                    ),
                    CircleShape
                )
        )

        // ── Content ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Wordmark — just text, italic style
            Text(
                text = "ForceMultiplier",
                color = White,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Headline ──────────────────────────────────────────────
            AnimatedContent(
                targetState = isSignUp,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                },
                label = "headline"
            ) { signUp ->
                Text(
                    text = if (signUp) "Hello,\nSign Up" else "Hello,\nLog In",
                    color = White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-1.2).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // ── Email ─────────────────────────────────────────────────
            FieldLabel("Email")
            Spacer(modifier = Modifier.height(8.dp))
            PillTextField(
                value = email,
                onValueChange = { email = it; error = null },
                placeholder = "Enter your email",
                leadingIcon = R.drawable.ic_email_outline,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Password ──────────────────────────────────────────────
            FieldLabel("Password")
            Spacer(modifier = Modifier.height(8.dp))
            PillTextField(
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = "Enter your password",
                leadingIcon = R.drawable.ic_lock,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(if (showPassword) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                            contentDescription = "Toggle visibility",
                            tint = MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )

            // ── Confirm password (sign up only) ───────────────────────
            AnimatedVisibility(visible = isSignUp, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                var confirm by remember { mutableStateOf("") }
                var showConfirm by remember { mutableStateOf(false) }
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    FieldLabel("Confirm password")
                    Spacer(modifier = Modifier.height(8.dp))
                    PillTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        placeholder = "Confirm your password",
                        leadingIcon = R.drawable.ic_lock,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    painter = painterResource(if (showConfirm) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                                    contentDescription = "Toggle visibility",
                                    tint = MutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }

            // ── Forgot password (login only) ──────────────────────────
            AnimatedVisibility(visible = !isSignUp, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = {},
                            modifier = Modifier.size(18.dp),
                            colors = CheckboxDefaults.colors(
                                uncheckedColor = BorderInput,
                                checkmarkColor = Color.Black,
                                checkedColor = GreenPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remember me", color = BodyText, fontSize = 12.sp)
                    }
                    Text(
                        "Forgot Password?",
                        color = BodyText,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {}
                    )
                }
            }

            // Error / info
            AnimatedVisibility(visible = error != null) {
                Text(error ?: "", color = Color(0xFFFF4757), fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
            AnimatedVisibility(visible = infoMessage != null) {
                Text(infoMessage ?: "", color = GreenPrimary, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Primary CTA button ────────────────────────────────────
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) { error = "Please fill in all fields."; return@Button }
                    isLoading = true; error = null; infoMessage = null
                    scope.launch {
                        try {
                            if (isSignUp) {
                                supabaseClient.auth.signUpWith(Email) { this.email = email.trim(); this.password = password }
                                infoMessage = "Check your email to confirm your account."
                            } else {
                                supabaseClient.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
                                onLoginSuccess()
                            }
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Authentication failed."
                        } finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = White),
                border = BorderStroke(1.dp, Color(0xFF333333)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Text(if (isSignUp) "Sign Up" else "Login", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.2.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Divider ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E1E1E))
                Text("  Or continue with  ", color = MutedText, fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E1E1E))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Social buttons ────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Google
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color(0xFF282828)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF111111), contentColor = White)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(painterResource(R.drawable.ic_google), "Google", Modifier.size(17.dp), tint = Color.Unspecified)
                        Spacer(Modifier.width(7.dp))
                        Text("Google", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BodyText)
                    }
                }
                // Apple
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color(0xFF282828)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF111111), contentColor = White)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(painterResource(R.drawable.ic_apple), "Apple", Modifier.size(17.dp), tint = White)
                        Spacer(Modifier.width(7.dp))
                        Text("Apple", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BodyText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── Toggle ────────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MutedText, fontSize = 13.sp)) {
                        append(if (isSignUp) "Already have an account? " else "Don't have an account? ")
                    }
                    withStyle(SpanStyle(color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)) {
                        append(if (isSignUp) "Login" else "Create an account")
                    }
                },
                modifier = Modifier
                    .clickable { isSignUp = !isSignUp; error = null; infoMessage = null }
                    .padding(bottom = 52.dp)
            )
        }
    }
}

// ─── Field label ──────────────────────────────────────────────────────────────
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = BodyText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth()
    )
}

// ─── Pill text field ──────────────────────────────────────────────────────────
@Composable
private fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: Int,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MutedText, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(17.dp)
            )
        },
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor    = SurfaceInput,
            unfocusedContainerColor  = SurfaceInput,
            focusedBorderColor       = Color(0xFF333333),
            unfocusedBorderColor     = BorderInput,
            focusedTextColor         = White,
            unfocusedTextColor       = White,
            cursorColor              = GreenPrimary
        ),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}
