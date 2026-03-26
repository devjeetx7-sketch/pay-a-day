package com.dailywork.attedance.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.R
import com.dailywork.attedance.viewmodel.AuthViewModel
import com.dailywork.attedance.viewmodel.LoginState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import androidx.compose.ui.composed

fun Modifier.pressScaleEffect() = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f)

    this
        .scale(scale)
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    selectedLanguage: String,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    val loginState by authViewModel.loginState.collectAsState()
    val context = LocalContext.current

    // Simple language dictionary for Login Screen based on selected language
    val isHindi = selectedLanguage == "hi"
    val isBengali = selectedLanguage == "bn"
    val isMarathi = selectedLanguage == "mr"
    val isTamil = selectedLanguage == "ta"
    val isTelugu = selectedLanguage == "te"

    val titleText = when {
        isHindi -> "नमस्ते, शुरू करें"
        isBengali -> "নমস্কার, শুরু করা যাক"
        isMarathi -> "नमस्कार, सुरुवात करूया"
        isTamil -> "வணக்கம், தொடங்குவோம்"
        isTelugu -> "నమస్కారం, ప్రారంభిద్దాం"
        else -> "Namaste, Let's get started"
    }

    val subtitleText = when {
        isHindi -> "अपने कार्यबल का कुशलतापूर्वक प्रबंधन करें।"
        isBengali -> "আপনার কর্মীবাহিনী দক্ষতার সাথে পরিচালনা করুন।"
        isMarathi -> "आपल्या कर्मचाऱ्यांचे प्रभावीपणे व्यवस्थापन करा."
        isTamil -> "உங்கள் பணியாளர்களை திறமையாக நிர்வகிக்கவும்."
        isTelugu -> "మీ వర్క్‌ఫోర్స్‌ను సమర్థవంతంగా నిర్వహించండి."
        else -> "Manage your workforce efficiently."
    }

    val nameLabel = if (isHindi) "पूरा नाम" else "Full Name"
    val emailLabel = if (isHindi) "ईमेल पता" else "Email Address"
    val passwordLabel = if (isHindi) "पासवर्ड" else "Password"

    val loginText = if (isHindi) "लॉग इन" else "Login"
    val registerText = if (isHindi) "खाता बनाएँ" else "Create Account"

    val googleText = if (isHindi) "Google से साइन इन करें" else "Sign in with Google"

    val alreadyAccountText = if (isHindi) "क्या आपके पास पहले से खाता है?" else "Already have an account?"
    val noAccountText = if (isHindi) "क्या आपके पास खाता नहीं है?" else "Don't have an account?"
    val signUpText = if (isHindi) "साइन अप करें" else "Sign Up"

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                authViewModel.loginWithGoogleCredential(token)
            } ?: run {
                authViewModel.setLoginError("Google Sign-In failed: No ID Token found")
            }
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                10 -> "Developer Error (10): Verify SHA-1 and Web Client ID"
                12501 -> "Sign-in cancelled by user"
                7 -> "Network Error"
                else -> "Google Sign-In failed: ${e.message}"
            }
            authViewModel.setLoginError(errorMessage)
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon and Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BusinessCenter,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = titleText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitleText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Form fields
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isRegistering) {
                CustomTextField(
                    label = nameLabel,
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "John Doe"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            CustomTextField(
                label = emailLabel,
                value = email,
                onValueChange = { email = it },
                placeholder = "name@example.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                label = passwordLabel,
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        if (loginState is LoginState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (loginState as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                if (isRegistering) {
                    authViewModel.registerWithEmail(email, password, name.ifBlank { email.substringBefore("@") })
                } else {
                    authViewModel.loginWithEmail(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pressScaleEffect(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            ),
            enabled = loginState !is LoginState.Loading
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isRegistering) registerText else loginText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Google Auth Button
        OutlinedButton(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                launcher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pressScaleEffect(),
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            enabled = loginState !is LoginState.Loading
        ) {
            Text(
                text = googleText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Toggle Register/Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRegistering) alreadyAccountText else noAccountText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRegistering) loginText else signUpText,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isRegistering = !isRegistering
                }
            )
        }
    }
}

@Composable
fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                innerTextField()
            }
        )
    }
}
