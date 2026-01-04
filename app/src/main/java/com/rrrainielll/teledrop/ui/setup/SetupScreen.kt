package com.rrrainielll.teledrop.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.res.painterResource
import com.rrrainielll.teledrop.R
import com.rrrainielll.teledrop.ui.theme.Motion
import com.rrrainielll.teledrop.utils.PermissionHelper
import kotlinx.coroutines.delay

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    var permissionsGranted by remember { mutableStateOf(PermissionHelper.hasMediaPermissions(context)) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }
    
    // Request permissions on first load
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(PermissionHelper.getRequiredMediaPermissions())
        }
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    var showTutorial by remember { mutableStateOf(false) }
    
    // Staggered animation states
    var showLogo by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showTutorialButton by remember { mutableStateOf(false) }
    var showTokenField by remember { mutableStateOf(false) }
    var showChatIdField by remember { mutableStateOf(false) }
    var showConnectButton by remember { mutableStateOf(false) }
    
    // Trigger staggered animations
    LaunchedEffect(Unit) {
        delay(100)
        showLogo = true
        delay(100)
        showTitle = true
        delay(80)
        showSubtitle = true
        delay(120)
        showTutorialButton = true
        delay(100)
        showTokenField = true
        delay(80)
        showChatIdField = true
        delay(100)
        showConnectButton = true
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSetupComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Logo
                    val logoAlpha by animateFloatAsState(
                        targetValue = if (showLogo) 1f else 0f,
                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                        label = "LogoAlpha"
                    )
                    val logoScale by animateFloatAsState(
                        targetValue = if (showLogo) 1f else 0.8f,
                        animationSpec = Motion.BouncySpring,
                        label = "LogoScale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer {
                                alpha = logoAlpha
                                scaleX = logoScale
                                scaleY = logoScale
                            }
                            .clip(RoundedCornerShape(44.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Animated Title
                    val titleAlpha by animateFloatAsState(
                        targetValue = if (showTitle) 1f else 0f,
                        animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                        label = "TitleAlpha"
                    )
                    val titleTranslationY by animateFloatAsState(
                        targetValue = if (showTitle) 0f else 20f,
                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                        label = "TitleTranslationY"
                    )
                    
                    Text(
                        text = "TeleDrop",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer {
                            alpha = titleAlpha
                            translationY = titleTranslationY
                        }
                    )
                    
                    // Animated Subtitle
                    val subtitleAlpha by animateFloatAsState(
                        targetValue = if (showSubtitle) 1f else 0f,
                        animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                        label = "SubtitleAlpha"
                    )
                    val subtitleTranslationY by animateFloatAsState(
                        targetValue = if (showSubtitle) 0f else 16f,
                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                        label = "SubtitleTranslationY"
                    )
                    
                    Text(
                        text = "Sync your photos and videos to Telegram",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = subtitleAlpha
                            translationY = subtitleTranslationY
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Animated Tutorial Section
                    AnimatedVisibility(
                        visible = showTutorial,
                        enter = expandVertically(
                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate)
                        ) + fadeIn(
                            animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedAccelerate)
                        ) + fadeOut(
                            animationSpec = tween(Motion.Duration.Short4)
                        )
                    ) {
                        Column {
                            TutorialCard { showTutorial = false }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                    
                    // Animated Tutorial Button
                    AnimatedVisibility(
                        visible = !showTutorial && showTutorialButton,
                        enter = fadeIn(
                            animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate)
                        ) + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(Motion.Duration.Short4)
                        )
                    ) {
                        TextButton(onClick = { showTutorial = true }) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("How to create a Bot?")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Token Field
                    val tokenFieldAlpha by animateFloatAsState(
                        targetValue = if (showTokenField) 1f else 0f,
                        animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                        label = "TokenFieldAlpha"
                    )
                    val tokenFieldTranslationY by animateFloatAsState(
                        targetValue = if (showTokenField) 0f else 24f,
                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                        label = "TokenFieldTranslationY"
                    )
                    
                    OutlinedTextField(
                        value = uiState.botToken,
                        onValueChange = { 
                            viewModel.onTokenChanged(it)
                            if (it.length > 20) viewModel.fetchBotUsername()
                        },
                        label = { Text("Bot Token") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = tokenFieldAlpha
                                translationY = tokenFieldTranslationY
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = MaterialTheme.shapes.extraLarge,
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = uiState.botUsername != null,
                                enter = fadeIn(tween(Motion.Duration.Short4)) + 
                                    slideInVertically(animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate)),
                                exit = fadeOut(tween(Motion.Duration.Short3))
                            ) {
                                Icon(
                                    Icons.Default.Check, 
                                    contentDescription = "Valid", 
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    
                    // Animated "Open Bot" Button
                    AnimatedVisibility(
                        visible = uiState.botUsername != null,
                        enter = expandVertically(
                            animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate)
                        ) + fadeIn(
                            animationSpec = tween(Motion.Duration.Medium2)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(Motion.Duration.Short4)
                        ) + fadeOut()
                    ) {
                        var isPressed by remember { mutableStateOf(false) }
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.97f else 1f,
                            animationSpec = Motion.SnappySpring,
                            label = "OpenBotButtonScale"
                        )
                        
                        Button(
                            onClick = { 
                                uiState.botUsername?.let { username ->
                                    uriHandler.openUri("https://t.me/$username") 
                                }
                            },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .scale(buttonScale)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            tryAwaitRelease()
                                            isPressed = false
                                        }
                                    )
                                },
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Open @${uiState.botUsername}")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground), 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Chat ID Field
                    val chatIdFieldAlpha by animateFloatAsState(
                        targetValue = if (showChatIdField) 1f else 0f,
                        animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                        label = "ChatIdFieldAlpha"
                    )
                    val chatIdFieldTranslationY by animateFloatAsState(
                        targetValue = if (showChatIdField) 0f else 24f,
                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                        label = "ChatIdFieldTranslationY"
                    )
                    
                    OutlinedTextField(
                        value = uiState.chatId,
                        onValueChange = viewModel::onChatIdChanged,
                        label = { Text("Chat ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = chatIdFieldAlpha
                                translationY = chatIdFieldTranslationY
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = MaterialTheme.shapes.extraLarge,
                        trailingIcon = {
                            TextButton(onClick = { 
                                focusManager.clearFocus()
                                viewModel.autoDetectChatId() 
                            }) {
                                Text("Find Me")
                            }
                        }
                    )
                    
                    // Animated Info Message
                    AnimatedVisibility(
                        visible = uiState.infoMessage != null,
                        enter = fadeIn(tween(Motion.Duration.Short4)) + 
                            expandVertically(animationSpec = tween(Motion.Duration.Medium2)),
                        exit = fadeOut(tween(Motion.Duration.Short3)) + 
                            shrinkVertically(animationSpec = tween(Motion.Duration.Short4))
                    ) {
                        Text(
                            text = uiState.infoMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                        )
                    }

                    // Animated Error Message
                    AnimatedVisibility(
                        visible = uiState.error != null,
                        enter = fadeIn(tween(Motion.Duration.Short4)) + 
                            expandVertically(animationSpec = tween(Motion.Duration.Medium2)) +
                            slideInVertically(initialOffsetY = { -it / 2 }),
                        exit = fadeOut(tween(Motion.Duration.Short3)) + 
                            shrinkVertically(animationSpec = tween(Motion.Duration.Short4))
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Animated Connect Button / Loading
                    val connectButtonAlpha by animateFloatAsState(
                        targetValue = if (showConnectButton) 1f else 0f,
                        animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                        label = "ConnectButtonAlpha"
                    )
                    val connectButtonTranslationY by animateFloatAsState(
                        targetValue = if (showConnectButton) 0f else 24f,
                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                        label = "ConnectButtonTranslationY"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = connectButtonAlpha
                                translationY = connectButtonTranslationY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                        } else {
                            var isPressed by remember { mutableStateOf(false) }
                            val buttonScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.96f else 1f,
                                animationSpec = Motion.SnappySpring,
                                label = "ConnectButtonScale"
                            )
                            
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.verifyAndSave()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .scale(buttonScale)
                                    .pointerInput(uiState.botToken.isNotBlank()) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                tryAwaitRelease()
                                                isPressed = false
                                            }
                                        )
                                    },
                                enabled = uiState.botToken.isNotBlank(),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Text(
                                    "Connect",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialCard(onClose: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(50)
        isVisible = true
    }
    
    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = Motion.EmphasizedSpring,
        label = "TutorialCardScale"
    )
    
    Card(
        modifier = Modifier
            .scale(cardScale),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "How to Connect:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val uriHandler = LocalUriHandler.current
            val annotatedText = buildAnnotatedString {
                append("1. Open Telegram and search for ")
                pushStringAnnotation(tag = "URL", annotation = "https://telegram.me/BotFather")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("@BotFather")
                }
                pop()
                append(".")
            }
            
            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "2. Type /newbot and follow the instructions to create a bot.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "3. Copy the Bot Token (it looks like: 123456:ABC-DEF...).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "4. Paste it above.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Getting Chat ID:", 
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "1. Send /start to your new bot.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "2. Click 'Find Me' to auto-detect your Chat ID.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            var isPressed by remember { mutableStateOf(false) }
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = Motion.SnappySpring,
                label = "GotItButtonScale"
            )
            
            OutlinedButton(
                onClick = onClose, 
                modifier = Modifier
                    .align(Alignment.End)
                    .scale(buttonScale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    }
            ) {
                Text("Got it")
            }
        }
    }
}

