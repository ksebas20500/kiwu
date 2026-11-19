package app.kiwu.android.ui.onboarding

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiwu.android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// PALETA DE COLORES ADAPTATIVA (MODO CLARO Y OSCURO)
// ==========================================
class OnboardingThemeColors(val isDark: Boolean) {
    val Background = if (isDark) Color(0xFF16101E) else Color(0xFFFBFAF8)
    val CardBackground = if (isDark) Color(0xFF251C30) else Color.White
    val CardBorder = if (isDark) Color(0xFF382C48) else Color(0xFFEEE9EF)
    val CardShadow = if (isDark) Color(0x66000000) else Color(0x1F362648)

    val PrimaryPurple = Color(0xFF7540A1)
    val PrimaryPurpleDark = Color(0xFF7743A0)
    val EyebrowPurple = if (isDark) Color(0xFFC28DF2) else Color(0xFF8C54AC)
    val PurpleSoftText = if (isDark) Color(0xFFC08FFF) else Color(0xFF74409C)
    val PurpleAccentLight = if (isDark) Color(0xFFB382D4) else Color(0xFF8E5BA8)
    val PurpleSoftBg = if (isDark) Color(0xFF3B284C) else Color(0xFFEADCF1)

    val AccentGreen = Color(0xFF55AD82)
    val AccentGreenDark = if (isDark) Color(0xFF63C293) else Color(0xFF4F9C72)
    val AccentGreenBg = if (isDark) Color(0xFF183D2A) else Color(0xFFDFF3E8)
    val AccentGreenIconBg = Color(0xFF58AA7B)
    val AccentGreenText = if (isDark) Color(0xFFA2E8C4) else Color(0xFF286443)
    val AccentGreenBoldText = if (isDark) Color(0xFFB8F5D4) else Color(0xFF237446)

    val AccentTerracotta = Color(0xFFD07A5B)
    val AccentTerracottaLight = Color(0xFFD88765)
    val AccentTerracottaBg = if (isDark) Color(0xFF3B261D) else Color(0xFFF4E7DA)
    val AccentTerracottaText = if (isDark) Color(0xFFFFB394) else Color(0xFF996349)
    val AccentTerracottaBadge = if (isDark) Color(0xFF3E231C) else Color(0xFFF8EFE9)
    val AccentTerracottaBadgeText = if (isDark) Color(0xFFFFB399) else Color(0xFFA7624E)
    val WarningBadgeBg = Color(0xFFD57F61)
    val WarningText = if (isDark) Color(0xFFFFB399) else Color(0xFF9B6552)

    val TextTitle = if (isDark) Color(0xFFF2EDF8) else Color(0xFF261F30)
    val TextSubtitle = if (isDark) Color(0xFFB5ADBD) else Color(0xFF746E79)
    val TextSkip = if (isDark) Color(0xFFC4BCCB) else Color(0xFF726B7B)
    val TextMuted = if (isDark) Color(0xFFA59CB0) else Color(0xFF918A96)
    val TextCalendarDay = if (isDark) Color(0xFFBDB5C5) else Color(0xFF837A88)
    val TextCalendarMonth = if (isDark) Color(0xFFA9A0B2) else Color(0xFF877E8B)
    val TextCalendarHeader = if (isDark) Color(0xFFB3AAB9) else Color(0xFF9C949E)

    val DotInactive = if (isDark) Color(0xFF423850) else Color(0xFFD7CFDC)
    val StarGold = Color(0xFFF9B73C)
    val StarGoldSecondary = Color(0xFFEFB94D)

    val OrbPeachStart = if (isDark) Color(0xFF3D2732) else Color(0xFFF9E6DD)
    val OrbPeachEnd = if (isDark) Color(0xFF2B213D) else Color(0xFFEFE0F4)
    val AmbientPurple = if (isDark) Color(0x3D7540A1) else Color(0x6BE4D2F2)
    val AmbientGreen = if (isDark) Color(0x352E8B57) else Color(0x73D6EEE2)
    val BorderLight = if (isDark) Color(0xFF382C48) else Color(0xFFEEE9EF)
    val ProgressTrack = if (isDark) Color(0xFF352B40) else Color(0xFFEAE4EC)
    val ProgressValueStart = Color(0xFF9A67B5)
    val ProgressValueEnd = Color(0xFF70409C)
}

// ==========================================
// COMPOSABLE PRINCIPAL (ONBOARDING SCREEN)
// ==========================================
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val colors = remember(isDark) { OnboardingThemeColors(isDark) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.Background)
    ) {
        // Ambient Glows adaptativos de fondo
        Box(
            modifier = Modifier
                .offset(x = 120.dp, y = 60.dp)
                .size(260.dp)
                .align(Alignment.TopEnd)
                .background(colors.AmbientPurple, CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-40).dp)
                .size(220.dp)
                .align(Alignment.BottomStart)
                .background(colors.AmbientGreen, CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header TopBar con insets de barra de estado
            OnboardingTopBar(
                colors = colors,
                currentPage = pagerState.currentPage,
                onSkipClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(3)
                    }
                },
                onLogoClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
            )

            // Pager interactivo deslizable
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val isActive = pagerState.currentPage == page
                when (page) {
                    0 -> OnboardingStep1(colors = colors, isActive = isActive)
                    1 -> OnboardingStep2(colors = colors, isActive = isActive)
                    2 -> OnboardingStep3(colors = colors, isActive = isActive)
                    3 -> OnboardingStep4(colors = colors, isActive = isActive)
                }
            }

            // Controles inferiores con insets
            OnboardingBottomControls(
                colors = colors,
                currentPage = pagerState.currentPage,
                pageCount = 4,
                onNextClick = {
                    if (pagerState.currentPage < 3) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                onDotClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    }
}

// ==========================================
// TOP BAR CON INSETS DE SISTEMA (STATUS BARS)
// ==========================================
@Composable
private fun OnboardingTopBar(
    colors: OnboardingThemeColors,
    currentPage: Int,
    onSkipClick: () -> Unit,
    onLogoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onLogoClick() }
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.kiwu_icon),
                contentDescription = "Kiwu Logo",
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "kiwu",
                color = colors.TextTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        if (currentPage < 3) {
            Text(
                text = "Saltar",
                color = colors.TextSkip,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSkipClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

// ==========================================
// PASO 1: Tareas
// ==========================================
@Composable
fun OnboardingStep1(
    colors: OnboardingThemeColors,
    isActive: Boolean = true
) {
    OnboardingStepLayout(
        colors = colors,
        scene = { TasksScene(colors = colors, isActive = isActive) },
        eyebrow = "TU SEMESTRE, MÁS SIMPLE",
        title = "Organiza tu semestre sin estrés",
        subtitle = "Crea listas por materia, gestiona entregas con fechas límite inteligentes y mantén tu racha al día."
    )
}

@Composable
private fun TasksScene(
    colors: OnboardingThemeColors,
    isActive: Boolean
) {
    val mascotScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.85f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mascotScale"
    )

    val card1Alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 150),
        label = "card1Alpha"
    )
    val card1OffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 600, delayMillis = 150),
        label = "card1OffsetY"
    )

    val card2Alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 300),
        label = "card2Alpha"
    )
    val card2OffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 600, delayMillis = 300),
        label = "card2OffsetY"
    )

    val checkPopScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkPopScale"
    )

    val chipOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 20.dp,
        animationSpec = tween(durationMillis = 500, delayMillis = 450),
        label = "chipOffsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        ConfettiParticles(isActive = isActive)

        Box(
            modifier = Modifier
                .size(220.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.OrbPeachStart, colors.OrbPeachEnd)
                    ),
                    shape = CircleShape
                )
        )

        MascotWithGlow(
            colors = colors,
            scale = mascotScale,
            modifier = Modifier.offset(y = (-8).dp)
        )

        // Tarjeta flotante 1
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = 50.dp + card1OffsetY)
                .graphicsLayer {
                    alpha = card1Alpha
                    rotationZ = -3f
                }
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(15.dp), spotColor = colors.CardShadow)
                .background(colors.CardBackground, RoundedCornerShape(15.dp))
                .border(1.dp, colors.CardBorder, RoundedCornerShape(15.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(checkPopScale)
                        .background(colors.AccentGreen, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = "Check",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Column {
                    Text(
                        text = "Entregar ensayo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.TextTitle,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = "Literatura",
                        fontSize = 9.sp,
                        color = colors.TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Tarjeta flotante 2
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-12).dp, y = (-90).dp + card2OffsetY)
                .graphicsLayer {
                    alpha = card2Alpha
                    rotationZ = 3f
                }
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(15.dp), spotColor = colors.CardShadow)
                .background(colors.CardBackground, RoundedCornerShape(15.dp))
                .border(1.dp, colors.CardBorder, RoundedCornerShape(15.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(1.5.dp, if (colors.isDark) Color(0xFF635672) else Color(0xFFC5BDC9), RoundedCornerShape(6.dp))
                )
                Column {
                    Text(
                        text = "Quiz de cálculo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.TextTitle
                    )
                    Text(
                        text = "Viernes · 9:00",
                        fontSize = 9.sp,
                        color = colors.TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Chip Vence mañana
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp + chipOffsetY)
                .shadow(elevation = 10.dp, shape = CircleShape, spotColor = colors.CardShadow)
                .background(colors.CardBackground, CircleShape)
                .border(1.dp, colors.CardBorder, CircleShape)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "●", color = colors.AccentTerracotta, fontSize = 9.sp)
                Text(
                    text = "Vence mañana · Modelado",
                    color = colors.TextSubtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConfettiParticles(isActive: Boolean) {
    if (!isActive) return
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "confettiOffset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        val confettiColors = listOf(Color(0xFFF2B646), Color(0xFF70B990), Color(0xFF9A67BC), Color(0xFFE9876D))
        confettiColors.forEachIndexed { i, color ->
            Box(
                modifier = Modifier
                    .offset(x = (40 + i * 35).dp, y = (60 + (i % 2) * 20).dp + (offsetY * (i + 1) * 0.4f).dp)
                    .size(5.dp, 8.dp)
                    .rotate(25f * i)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

// ==========================================
// PASO 2: Horarios
// ==========================================
@Composable
fun OnboardingStep2(
    colors: OnboardingThemeColors,
    isActive: Boolean = true
) {
    OnboardingStepLayout(
        colors = colors,
        scene = { ScheduleScene(colors = colors, isActive = isActive) },
        eyebrow = "SIEMPRE A TIEMPO",
        title = "Tu horario y asistencia bajo control",
        subtitle = "Visualiza tus clases de la semana y lleva la cuenta exacta de tus faltas para cuidar cada materia."
    )
}

@Composable
private fun ScheduleScene(
    colors: OnboardingThemeColors,
    isActive: Boolean
) {
    val dayPopScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.6f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dayPopScale"
    )

    val classOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 30.dp,
        animationSpec = tween(durationMillis = 600, delayMillis = 150),
        label = "classOffsetY"
    )
    val classAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 150),
        label = "classAlpha"
    )

    var mostrarFaltaUno by remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            delay(600)
            mostrarFaltaUno = true
        } else {
            mostrarFaltaUno = false
        }
    }

    val peekOffsetX by animateDpAsState(
        targetValue = if (isActive) 0.dp else 40.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "peekOffsetX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Clock,
                    contentDescription = null,
                    tint = colors.AccentTerracottaText,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "PRÓXIMA CLASE",
                    color = colors.AccentTerracottaText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.3.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("L", "M", "X", "J", "V", "S", "D")
                days.forEach { day ->
                    val isSelected = day == "X"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .scale(dayPopScale)
                                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), spotColor = colors.PrimaryPurpleDark)
                                        .background(colors.PrimaryPurpleDark, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                        .background(colors.CardBackground, RoundedCornerShape(12.dp))
                                        .border(1.dp, colors.BorderLight, RoundedCornerShape(12.dp))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            color = if (isSelected) Color.White else colors.TextCalendarDay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = classOffsetY)
                    .graphicsLayer { alpha = classAlpha }
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp), spotColor = colors.CardShadow)
                    .background(colors.CardBackground, RoundedCornerShape(22.dp))
                    .border(1.dp, colors.CardBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-20).dp)
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(colors.AccentTerracottaLight, RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "08:00 — 10:00",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.AccentTerracottaText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Modelado 3D",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.TextTitle,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(colors.AccentTerracottaBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "B-204",
                                color = colors.AccentTerracottaText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.BorderLight))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            AvatarCircle(text = "K", bg = if (colors.isDark) Color(0xFF4A3459) else Color(0xFFE5D7EB), tint = if (colors.isDark) Color(0xFFD8BBEB) else Color(0xFF744B88))
                            AvatarCircle(text = "M", bg = if (colors.isDark) Color(0xFF28483B) else Color(0xFFD7EBE1), tint = if (colors.isDark) Color(0xFFA3E1C5) else Color(0xFF437B61), offsetX = (-6).dp)
                            AvatarCircle(text = "+8", bg = if (colors.isDark) Color(0xFF4A3459) else Color(0xFFE5D7EB), tint = if (colors.isDark) Color(0xFFD8BBEB) else Color(0xFF744B88), offsetX = (-12).dp)
                        }

                        Box(
                            modifier = Modifier
                                .background(colors.AccentTerracottaBadge, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            AnimatedContent(
                                targetState = mostrarFaltaUno,
                                transitionSpec = {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith (fadeOut())
                                },
                                label = "AbsenceCountAnim"
                            ) { esUno ->
                                Text(
                                    text = if (esUno) "1/4 faltas" else "0/4 faltas",
                                    color = colors.AccentTerracottaBadgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = mostrarFaltaUno,
                enter = fadeIn() + scaleIn()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .background(colors.WarningBadgeBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Falta registrada",
                        color = colors.WarningText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp + peekOffsetX, y = 4.dp)
                .size(68.dp)
                .rotate(7f)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp), spotColor = colors.CardShadow)
                .background(colors.CardBackground, RoundedCornerShape(22.dp))
                .border(1.dp, colors.CardBorder, RoundedCornerShape(22.dp))
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.kiwu_mascot),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// ==========================================
// PASO 3: Notas
// ==========================================
@Composable
fun OnboardingStep3(
    colors: OnboardingThemeColors,
    isActive: Boolean = true
) {
    OnboardingStepLayout(
        colors = colors,
        scene = { GradesScene(colors = colors, isActive = isActive) },
        eyebrow = "SIN MÁS CÁLCULOS MENTALES",
        title = "Calcula tu nota antes del examen final",
        subtitle = "Registra tus notas y porcentajes. Kiwu te dice exactamente cuánto necesitas para aprobar."
    )
}

@Composable
private fun GradesScene(
    colors: OnboardingThemeColors,
    isActive: Boolean
) {
    val progressAnim by animateFloatAsState(
        targetValue = if (isActive) 0.35f else 0f,
        animationSpec = tween(durationMillis = 1200, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "progressAnim"
    )

    val pillOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else 20.dp,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "pillOffsetY"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "pillAlpha"
    )

    val needCardScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.9f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "needCardScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gradeStars")
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "starScale"
    )
    val starRotate by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "starRotate"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUÍMICA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    color = colors.TextMuted
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "4.2",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.AccentGreenDark,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(text = "promedio", fontSize = 9.sp, color = colors.TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Progreso del curso", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.TextTitle)
                Text(text = "35% evaluado", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.PurpleSoftText)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .background(colors.ProgressTrack, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(colors.ProgressValueStart, colors.ProgressValueEnd)
                            ),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = pillOffsetY)
                    .graphicsLayer { alpha = pillAlpha },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GradePillCard(colors = colors, modifier = Modifier.weight(1f), title = "Parcial 1", score = "4.90", weight = "20%")
                GradePillCard(colors = colors, modifier = Modifier.weight(1f), title = "Talleres", score = "3.50", weight = "15%")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(needCardScale)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0x244C8F69))
                    .background(colors.AccentGreenBg, RoundedCornerShape(18.dp))
                    .border(1.dp, if (colors.isDark) Color(0xFF285E42) else Color.Transparent, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .background(colors.AccentGreenIconBg, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Chart, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    Column {
                        Text(
                            text = "PARA APROBAR CON 3.0",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.7.sp,
                            color = colors.AccentGreenText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "Necesitas ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.AccentGreenText)
                            Text(text = "2.14", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.AccentGreenBoldText)
                            Text(text = " en el resto", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.AccentGreenText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✦",
                    color = colors.StarGoldSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .scale(starScale)
                        .rotate(starRotate)
                )
                Text(
                    text = "✦",
                    color = colors.StarGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .scale(starScale * 0.9f)
                        .rotate(-starRotate)
                )
            }
        }
    }
}

// ==========================================
// PASO 4: Calendario
// ==========================================
@Composable
fun OnboardingStep4(
    colors: OnboardingThemeColors,
    isActive: Boolean = true
) {
    OnboardingStepLayout(
        colors = colors,
        scene = { CalendarScene(colors = colors, isActive = isActive) },
        eyebrow = "TODO CONECTADO",
        title = "Todo tu mundo académico en un solo lugar",
        subtitle = "Accede a tus tareas, notas y horarios en cualquier momento. ¡Listo para empezar!"
    )
}

@Composable
private fun CalendarScene(
    colors: OnboardingThemeColors,
    isActive: Boolean
) {
    val celebrateScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "celebrateScale"
    )
    val celebrateRotate by animateFloatAsState(
        targetValue = if (isActive) 0f else -15f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "celebrateRotate"
    )

    val gridAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 100),
        label = "gridAlpha"
    )
    val gridOffsetY by animateDpAsState(
        targetValue = if (isActive) 0.dp else (-12).dp,
        animationSpec = tween(durationMillis = 600, delayMillis = 100),
        label = "gridOffsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOVIEMBRE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = colors.PurpleAccentLight
                    )
                    Text(
                        text = "Tu mes, de un vistazo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.TextTitle
                    )
                }
                Text(text = "2026", fontSize = 10.sp, color = colors.TextCalendarMonth)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                    Text(
                        text = d,
                        fontSize = 8.sp,
                        color = colors.TextCalendarHeader,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val eventDays = setOf(4, 9, 17, 23)
            val today = 12

            Column(
                modifier = Modifier
                    .offset(y = gridOffsetY)
                    .graphicsLayer { alpha = gridAlpha },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val dayNumber = (row * 7) + col + 1
                            val isToday = dayNumber == today
                            val hasEvent = dayNumber in eventDays

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .then(
                                        if (isToday) {
                                            Modifier
                                                .shadow(elevation = 6.dp, shape = RoundedCornerShape(9.dp), spotColor = colors.PrimaryPurpleDark)
                                                .background(colors.PrimaryPurpleDark, RoundedCornerShape(9.dp))
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isToday) Color.White else colors.TextTitle
                                    )
                                    if (hasEvent && !isToday) {
                                        Box(
                                            modifier = Modifier
                                                .size(3.dp)
                                                .background(colors.AccentTerracotta, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sync lines animadas
            SyncLines(isActive = isActive)

            // Mini barra de navegación
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp)
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(18.dp), spotColor = colors.CardShadow)
                    .background(colors.CardBackground, RoundedCornerShape(18.dp))
                    .border(1.dp, colors.CardBorder, RoundedCornerShape(18.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniNavItem(colors = colors, icon = Icons.Check, label = "Tareas", isActive = false)
                    MiniNavItem(colors = colors, icon = Icons.Chart, label = "Notas", isActive = true)
                    MiniNavItem(colors = colors, icon = Icons.Clock, label = "Horario", isActive = false)
                }
            }
        }

        // Mascota celebrando
        val infiniteTransition = rememberInfiniteTransition(label = "celebrateSparkle")
        val starSparkleScale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.35f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
            label = "starSparkleScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = 0.dp)
                .size(76.dp)
                .scale(celebrateScale)
                .rotate(celebrateRotate),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = colors.CardShadow)
                    .background(colors.CardBackground, RoundedCornerShape(20.dp))
                    .border(4.dp, colors.Background, RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.kiwu_mascot),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = "✦",
                color = colors.StarGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 0.dp)
                    .scale(starSparkleScale)
            )
        }
    }
}

// Líneas de conexión animadas
@Composable
private fun SyncLines(isActive: Boolean) {
    val lineHeight by animateDpAsState(
        targetValue = if (isActive) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "syncLineHeight"
    )

    Box(
        modifier = Modifier
            .width(210.dp)
            .height(22.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (lineHeight > 0.dp) {
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFFAC7BC2), Color.Transparent)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(lineHeight)
                        .rotate(42f)
                        .background(gradientBrush)
                )
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(lineHeight)
                        .background(gradientBrush)
                )
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(lineHeight)
                        .rotate(-42f)
                        .background(gradientBrush)
                )
            }
        }
    }
}

// ==========================================
// CONTROLES INFERIORES Y DOTS
// ==========================================
@Composable
private fun OnboardingBottomControls(
    colors: OnboardingThemeColors,
    currentPage: Int,
    pageCount: Int,
    onNextClick: () -> Unit,
    onDotClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = (currentPage == pageCount - 1),
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + scaleIn()) togetherWith (fadeOut(animationSpec = tween(200)) + scaleOut())
            },
            label = "BottomControlsAnimation"
        ) { isLastStep ->
            if (isLastStep) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(18.dp), spotColor = colors.PrimaryPurple)
                        .background(colors.PrimaryPurple, RoundedCornerShape(18.dp))
                        .clickable { onNextClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "¡Empezar ahora!",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.ArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OnboardingDotsIndicator(
                        colors = colors,
                        currentPage = currentPage,
                        pageCount = pageCount,
                        onDotClick = onDotClick
                    )

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = colors.PrimaryPurple)
                            .background(colors.PrimaryPurple, RoundedCornerShape(20.dp))
                            .clickable { onNextClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.ArrowRight,
                            contentDescription = "Siguiente",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingDotsIndicator(
    colors: OnboardingThemeColors,
    currentPage: Int,
    pageCount: Int,
    onDotClick: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val targetWidth = if (isActive) 25.dp else 7.dp
            val animatedWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "DotWidthAnimation"
            )

            Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(animatedWidth)
                    .clip(CircleShape)
                    .background(if (isActive) colors.PurpleSoftText else colors.DotInactive)
                    .clickable { onDotClick(index) }
            )
        }
    }
}

// ==========================================
// UTILS Y AUXILIARES
// ==========================================
@Composable
private fun OnboardingStepLayout(
    colors: OnboardingThemeColors,
    scene: @Composable () -> Unit,
    eyebrow: String,
    title: String,
    subtitle: String
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        scene()

        Spacer(modifier = Modifier.height(4.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = eyebrow,
                color = colors.EyebrowPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                color = colors.TextTitle,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp,
                letterSpacing = (-1.1).sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                color = colors.TextSubtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MascotWithGlow(
    colors: OnboardingThemeColors,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkles")
    val sparkScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "sparkScale"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(Color(0xFFFFCC5C).copy(alpha = if (colors.isDark) 0.15f else 0.25f), CircleShape)
        )

        Image(
            painter = painterResource(id = R.drawable.kiwu_mascot),
            contentDescription = "Kiwu el gato mago",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "✦",
            color = colors.StarGold,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-10).dp, y = (-20).dp)
                .scale(sparkScale)
        )
        Text(
            text = "✦",
            color = colors.StarGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 5.dp, y = 18.dp)
        )
    }
}

@Composable
private fun AvatarCircle(text: String, bg: Color, tint: Color, offsetX: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(
        modifier = Modifier
            .offset(x = offsetX)
            .size(24.dp)
            .border(2.dp, Color.White, CircleShape)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = tint, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GradePillCard(colors: OnboardingThemeColors, modifier: Modifier = Modifier, title: String, score: String, weight: String) {
    Box(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(15.dp), spotColor = colors.CardShadow)
            .background(colors.CardBackground, RoundedCornerShape(15.dp))
            .border(1.dp, colors.CardBorder, RoundedCornerShape(15.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = title, fontSize = 9.sp, color = colors.TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text = score, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.TextTitle, fontFamily = FontFamily.SansSerif)
                Text(text = weight, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.PurpleSoftText)
            }
        }
    }
}

@Composable
private fun MiniNavItem(colors: OnboardingThemeColors, icon: ImageVector, label: String, isActive: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(25.dp, 22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActive) colors.PurpleSoftBg else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) colors.PurpleSoftText else colors.TextMuted,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) colors.PurpleSoftText else colors.TextMuted
        )
    }
}

// ==========================================
// ICONOS EN VECTOR NATIVO
// ==========================================
private object Icons {
    val ArrowRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                lineTo(19f, 12f)
                moveTo(13f, 6f)
                lineTo(19f, 12f)
                lineTo(13f, 6f)
                moveTo(13f, 18f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                lineTo(9f, 16f)
                lineTo(19f, 6f)
            }
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 4f)
                curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
                curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
                moveTo(12f, 7f)
                lineTo(12f, 12f)
                lineTo(15f, 14f)
            }
        }.build()
    }

    val Chart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 19f)
                lineTo(5f, 9f)
                moveTo(12f, 19f)
                lineTo(12f, 5f)
                moveTo(19f, 19f)
                lineTo(19f, 12f)
            }
        }.build()
    }
}

// ==========================================
// PREVIEWS EN ANDROID STUDIO (MODO CLARO Y OSCURO)
// ==========================================
@Preview(name = "Modo Claro", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingScreenLightPreview() {
    OnboardingScreen(isDark = false)
}

@Preview(name = "Modo Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingScreenDarkPreview() {
    OnboardingScreen(isDark = true)
}
