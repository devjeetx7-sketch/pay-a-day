package com.dailywork.attedance.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.data.UserPreferencesRepository
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val content: @Composable (Float) -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    repository: UserPreferencesRepository,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pages = remember {
        getOnboardingPages(onNext = {
            scope.launch {
                repository.saveOnboardingCompleted(true)
                onComplete()
            }
        })
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle background motion
        AnimatedBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageOffset = (
                        (pagerState.currentPage - page) + pagerState
                            .currentPageOffsetFraction
                        ).absoluteValue

                OnboardingPageContent(
                    page = pages[page],
                    offset = pageOffset
                )
            }

            // Bottom Navigation Area
            OnboardingBottomBar(
                pagerState = pagerState,
                pageCount = pages.size,
                onSkip = {
                    scope.launch {
                        repository.saveOnboardingCompleted(true)
                        onComplete()
                    }
                },
                onNext = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        scope.launch {
                            repository.saveOnboardingCompleted(true)
                            onComplete()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAnim"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f).blur(100.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF13B28A).copy(alpha = 0.2f), Color.Transparent),
                center = center.copy(x = center.x * (0.5f + animValue * 0.5f), y = center.y * (0.5f - animValue * 0.2f)),
                radius = size.minDimension
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFEF4444).copy(alpha = 0.15f), Color.Transparent),
                center = center.copy(x = center.x * (1.5f - animValue * 0.5f), y = center.y * (1.5f + animValue * 0.2f)),
                radius = size.minDimension
            )
        )
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    offset: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            page.content(offset)
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - (offset * 2f).coerceIn(0f, 1f)
                translationY = offset * 100f
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - (offset * 3f).coerceIn(0f, 1f)
                translationY = offset * 150f
            }
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingBottomBar(
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageCount: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip Button
        if (pagerState.currentPage < pageCount - 1) {
            TextButton(onClick = onSkip) {
                Text(
                    "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Spacer(modifier = Modifier.width(64.dp))
        }

        // Indicators
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                    label = "indicatorWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        // Next/Go Button
        if (pagerState.currentPage < pageCount - 1) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

fun getOnboardingPages(onNext: () -> Unit): List<OnboardingPage> {
    return listOf(
        OnboardingPage(
            title = "Find Daily Work Faster",
            subtitle = "Discover nearby jobs, contractors, and opportunities in one powerful platform.",
            content = { offset -> Page1Visual(offset) }
        ),
        OnboardingPage(
            title = "For Personal Users",
            subtitle = "Explore daily jobs, apply instantly, and track your work history with ease.",
            content = { offset -> Page2Visual(offset) }
        ),
        OnboardingPage(
            title = "For Contractors",
            subtitle = "Post jobs, hire workers, and manage your projects with premium business tools.",
            content = { offset -> Page3Visual(offset) }
        ),
        OnboardingPage(
            title = "Unlock Premium Power",
            subtitle = "Get advanced insights, priority visibility, and smart statistics to grow faster.",
            content = { offset -> Page4Visual(offset) }
        ),
        OnboardingPage(
            title = "Ready To Start?",
            subtitle = "Join Work Daily and start working smarter today.",
            content = { offset ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Page5Visual(offset)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                alpha = 1f - offset
                                translationY = offset * 200f
                            },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Continue as Personal", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                alpha = 1f - offset
                                translationY = offset * 250f
                            },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Continue as Contractor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    )
}

@Composable
fun Page1Visual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "p1")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p1Float"
    )

    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        // Background Glow
        Box(
            modifier = Modifier
                .size(200.dp)
                .blur(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
        )

        // Main Icon Card
        Card(
            modifier = Modifier
                .size(120.dp)
                .offset(y = floatAnim.dp)
                .graphicsLayer {
                    rotationZ = offset * 45f
                    scaleX = 1f - offset
                    scaleY = 1f - offset
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🔍", fontSize = 48.sp)
            }
        }

        // Floating Icons
        val items = listOf("👷" to (-80 to -80), "🏗️" to (80 to -60), "🔧" to (-70 to 70), "🏢" to (70 to 80))
        items.forEachIndexed { index, (emoji, pos) ->
            val itemAnim by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500 + index * 200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "p1Item$index"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = pos.first.dp + (offset * (index * 20 - 40)).dp,
                        y = pos.second.dp + (itemAnim * 10).dp
                    )
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .graphicsLayer {
                        alpha = 1f - offset.absoluteValue
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun Page2Visual(offset: Float) {
    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        val features = listOf(
            "Daily Jobs" to "📍",
            "Instant Apply" to "⚡",
            "Work History" to "📊"
        )

        features.forEachIndexed { index, (text, emoji) ->
            val yOffset = (index - 1) * 70
            val xOffset = index * 20 - 20

            Card(
                modifier = Modifier
                    .width(180.dp)
                    .height(60.dp)
                    .offset(
                        x = (xOffset + offset * (index * 100)).dp,
                        y = (yOffset - offset * (index * 50)).dp
                    )
                    .graphicsLayer {
                        rotationZ = -5f + index * 5f + offset * 15f
                        alpha = 1f - offset
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun Page3Visual(offset: Float) {
    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        // Center Business Dashboard Visual
        Card(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    rotationX = offset * 30f
                    rotationY = offset * -30f
                    scaleX = 1f - offset * 0.2f
                    scaleY = 1f - offset * 0.2f
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("👨‍💼", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Box(modifier = Modifier.width(80.dp).height(8.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f), CircleShape))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.width(40.dp).height(6.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.05f), CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(45.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(45.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(45.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(8.dp)))
            }
        }

        // Floating Action Badges
        val badges = listOf("Hire" to (-80 to -80), "Post" to (80 to 80))
        badges.forEachIndexed { index, (label, pos) ->
            Box(
                modifier = Modifier
                    .offset(x = pos.first.dp - (offset * 50).dp, y = pos.second.dp)
                    .background(
                        if(index == 0) MaterialTheme.colorScheme.primary else Color(0xFFEF4444),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun Page4Visual(offset: Float) {
    var isUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(offset) {
        if (offset < 0.1f) {
            kotlinx.coroutines.delay(500)
            isUnlocked = true
        } else {
            isUnlocked = false
        }
    }

    val lockRotation by animateFloatAsState(if (isUnlocked) 0f else -15f)
    val lockScale by animateFloatAsState(if (isUnlocked) 1.2f else 1f)

    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        // Glowing background
        Box(
            modifier = Modifier
                .size(220.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        rotationZ = lockRotation + offset * 45f
                        scaleX = lockScale - offset
                        scaleY = lockScale - offset
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (isUnlocked) "🔓" else "🔒", fontSize = 72.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("PREMIUM", color = Color(0xFFB8860B), fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun Page5Visual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "p5")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p5Scale"
    )

    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .graphicsLayer {
                    alpha = 1f - offset
                }
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFEF4444))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🚀", fontSize = 80.sp)
        }

        // Particle effects
        repeat(6) { i ->
            val angle = i * 60f
            val rad = Math.toRadians(angle.toDouble())
            val x = (120 * Math.cos(rad)).toFloat()
            val y = (120 * Math.sin(rad)).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        }
    }
}
