package com.ian.forcemultiplier.presentation.login.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var supabaseClient: SupabaseClient

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

// ─── Palette ─────────────────────────────────────────────────────────────────
private val BgDark       = Color(0xFF0B0F14)
private val SurfaceDark  = Color(0xFF151B23)
private val Surface2Dark = Color(0xFF1C2128)
private val BorderDark   = Color(0xFF30363D)
private val MutedText    = Color(0xFF8B949E)
private val GreenPrimary = Color(0xFF2ED573)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    supabaseClient: SupabaseClient,
    onLoginSuccess: () -> Unit
) {
    var email         by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var isLoading     by remember { mutableStateOf(false) }
    var isSignUp      by remember { mutableStateOf(false) }
    var showPassword  by remember { mutableStateOf(false) }
    var error         by remember { mutableStateOf<String?>(null) }
    var infoMessage   by remember { mutableStateOf<String?>(null) }
    val focusManager  = LocalFocusManager.current
    val scope         = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Subtle top gradient accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GreenPrimary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // ── Wordmark ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FM",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ForceMultiplier",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(52.dp))

            // ── Heading ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isSignUp) "Create account" else "Welcome back",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isSignUp)
                            "Join the force. Start multiplying your impact."
                        else
                            "Sign in to your workspace.",
                        color = MutedText,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Social Buttons ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Google
                OutlinedButton(
                    onClick = { /* TODO: Google OAuth */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDark,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // Apple
                Button(
                    onClick = { /* TODO: Apple OAuth */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_apple),
                            contentDescription = "Apple",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apple",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Divider ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BorderDark
                )
                Text(
                    text = "or",
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = MutedText,
                    fontSize = 13.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = BorderDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Email ─────────────────────────────────────────────────
            FMTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = "Email",
                placeholder = "you@example.com",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Password ──────────────────────────────────────────────
            FMTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = "Password",
                placeholder = "Min. 6 characters",
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                trailingIcon = {
                    TextButton(
                        onClick = { showPassword = !showPassword },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (showPassword) "Hide" else "Show",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            // ── Error / Info ──────────────────────────────────────────
            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error ?: "",
                    color = Color(0xFFFF4757),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            AnimatedVisibility(visible = infoMessage != null) {
                Text(
                    text = infoMessage ?: "",
                    color = GreenPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            if (!isSignUp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        color = MutedText,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { /* TODO: reset */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Primary CTA ───────────────────────────────────────────
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "Please fill in all fields."
                        return@Button
                    }
                    isLoading = true
                    error = null
                    infoMessage = null
                    scope.launch {
                        try {
                            if (isSignUp) {
                                supabaseClient.auth.signUpWith(Email) {
                                    this.email = email.trim()
                                    this.password = password
                                }
                                infoMessage = "Check your email to confirm your account."
                            } else {
                                supabaseClient.auth.signInWith(Email) {
                                    this.email = email.trim()
                                    this.password = password
                                }
                                onLoginSuccess()
                            }
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Authentication failed."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.Black
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isSignUp) "Create account" else "Sign in",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Toggle sign-in / sign-up ──────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text(
                    text = if (isSignUp) "Already have an account?  " else "Don't have an account?  ",
                    color = MutedText,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isSignUp) "Sign in" else "Sign up",
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        isSignUp = !isSignUp
                        error = null
                        infoMessage = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FMTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(text = placeholder, color = MutedText.copy(alpha = 0.5f), fontSize = 14.sp)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = GreenPrimary
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}
