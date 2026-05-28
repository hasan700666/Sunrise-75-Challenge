package com.example

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GoldenOrange
import com.example.ui.theme.SunriseYellow
import com.example.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ChallengeApp(viewModel: ChallengeViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Observe Toast Messages
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (state.currentScreen) {
            AppScreen.SPLASH -> SplashScreen()
            AppScreen.MAIN -> MainDashboardScreen(state = state, viewModel = viewModel)
            AppScreen.RESULT -> ResultScreen(state = state, viewModel = viewModel)
        }
    }
}

// 1. Splash Screen Component
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sunrise Glowing "75" Header
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .shadow(24.dp, CircleShape, ambientColor = GoldenOrange, spotColor = SunriseYellow)
                    .size(160.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GoldenOrange.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Text(
                    text = "75",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(SunriseYellow, GoldenOrange)
                        )
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "SUNRISE CHALLENGE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GoldenOrange,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Rise with the Sun. Achieve Discipline.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }
    }
}

// 2. Main Dashboard Component
@Composable
fun MainDashboardScreen(state: UiState, viewModel: ChallengeViewModel) {
    Scaffold(
        bottomBar = {
            SleekBottomAppBar()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Dashboard Badge (Sleek Streak Card replacing default circle)
            item {
                StreakCard(state = state)
            }

            // Challenge Active Day Status indicator
            item {
                SunriseActiveDayBanner(state)
            }

            // Main Area: Question Wizard or Completion Display
            item {
                ChallengeCompletionArea(state, viewModel)
            }

            // Developer Area
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sunrise 75 Challenge v1.0 • Offline Native Mode",
                        fontSize = 11.sp,
                        color = TextPrimary.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reset All (For Testing)",
                        fontSize = 11.sp,
                        color = ErrorColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.resetAllDataForTesting() }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// 2.a Beautiful Custom Streak Card matching Sleek HTML
@Composable
fun StreakCard(state: UiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            // Background blur glowing circle effect (using custom canvas drawing of radial color)
            Canvas(modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp)
            ) {
                drawCircle(
                    color = GoldenOrange.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.9f
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Streak".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldenOrange.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Big beautiful gradient streak text
                Text(
                    text = "${state.streakCount}",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(SunriseYellow, GoldenOrange)
                        )
                    ),
                    lineHeight = 80.sp
                )
                
                Text(
                    text = if (state.streakCount == 1) "Day Consistent" else "Days Consistent",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Beautiful timer capsule layout from the sleek template
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Pulse dot Indicator
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SunriseYellow)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next Day Starts: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = state.todaySunriseTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SunriseYellow
                        )
                    }
                }
            }
        }
    }
}

// 2.b Active Sunrise Time Status Banner
@Composable
fun SunriseActiveDayBanner(state: UiState) {
    val currentTime = LocalTime.now()
    val isAfterSunrise = currentTime.isAfter(state.todaySunriseTime) || currentTime == state.todaySunriseTime

    Card(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIVE CHALLENGE DATE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.activeChallengeDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A new day begins at local sunrise (${state.todaySunriseTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}).",
                    fontSize = 11.sp,
                    color = TextPrimary.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isAfterSunrise) GoldenOrange.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isAfterSunrise) Icons.Default.WbSunny else Icons.Default.Bedtime,
                    contentDescription = "Day status",
                    tint = if (isAfterSunrise) SunriseYellow else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 2.c Completion Area Selector (Interactive Checklist vs Already Complete)
@Composable
fun ChallengeCompletionArea(state: UiState, viewModel: ChallengeViewModel) {
    if (state.todayCompleted) {
        // Today's check-in has already been completed!
        Card(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.todaySuccessful) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(GoldenOrange.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SunriseYellow,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DAY COMPLETED!",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunriseYellow,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Excellent work. You completed today's 75 Challenge successfully after Sunrise. Stay focused!",
                        fontSize = 12.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (state.generatedBitmap != null) {
                        Text(
                            text = "COMPLETED ACTION PHOTO".uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Image(
                            bitmap = state.generatedBitmap.asImageBitmap(),
                            contentDescription = "Status preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, SunriseYellow.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateToResult() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(1.dp, GoldenOrange, RoundedCornerShape(12.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View", tint = GoldenOrange, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Detail", color = GoldenOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { viewModel.downloadGeneratedImage() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Download", tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Answered NO, Day failed & Streak Reset
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(ErrorColor.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Failed",
                            tint = ErrorColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CHALLENGE FAILED TODAY",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You registered a fail. Streak has been reset to 0. Real discipline lies in picking yourself up and waking early again tomorrow.",
                        fontSize = 12.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    } else {
        // Today is NOT yet checked in. Display the Step 1 Yes/No wizard!
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Daily Requirements".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary.copy(alpha = 0.4f),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            // Dynamic Sleek lists instead of grouped card
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.questions.forEach { question ->
                    RequirementRowItem(
                        question = question,
                        onCheckedChange = { checked -> viewModel.toggleQuestion(question.id, checked) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action / Complete Button area
            val allYes = state.questions.all { it.isChecked }
            val imageLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                uri?.let { viewModel.onImageSelected(it) }
            }

            if (state.isImageUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldenOrange, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Processing Streak Image...", fontSize = 12.sp, color = SunriseYellow)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Fail Button
                        Button(
                            onClick = { viewModel.onAnswerNo() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("challenge_no_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Clear, contentDescription = "No", tint = ErrorColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Answer No", color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Uploding / Gradient Action button
                        Button(
                            onClick = {
                                if (allYes) {
                                    imageLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } else {
                                    viewModel.onAnswerNo()
                                }
                            },
                            enabled = allYes,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(56.dp)
                                .shadow(
                                    elevation = if (allYes) 12.dp else 0.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    ambientColor = GoldenOrange.copy(alpha = 0.4f),
                                    spotColor = GoldenOrange
                                )
                                .testTag("challenge_yes_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (allYes) {
                                            Brush.horizontalGradient(
                                                colors = listOf(GoldenOrange, Color(0xFFFDB813))
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                colors = listOf(CardSurface.copy(alpha = 0.5f), CardSurface.copy(alpha = 0.5f))
                                            )
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (allYes) Color.Transparent else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Upload",
                                        tint = if (allYes) Color(0xFF121212) else Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Upload Image",
                                        color = if (allYes) Color(0xFF121212) else Color.White.copy(alpha = 0.2f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Missing any requirement resets active streak to 0.",
                        fontSize = 11.sp,
                        color = TextPrimary.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Beautiful Custom Requirement checklist row item matching Sleek
@Composable
fun RequirementRowItem(
    question: Question,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (question.isChecked) GoldenOrange.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onCheckedChange(!question.isChecked) },
        colors = CardDefaults.cardColors(
            containerColor = if (question.isChecked) CardSurface.copy(alpha = 0.9f) else CardSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = question.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (question.isChecked) Color.White else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Custom high-fidelity amber box visual replacing standard checkbox
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (question.isChecked) GoldenOrange.copy(alpha = 0.1f) else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (question.isChecked) GoldenOrange else Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                if (question.isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked",
                        tint = GoldenOrange,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// 2.d Custom Styled Visual Bottom Bar matching layout templates perfectly
@Composable
fun SleekBottomAppBar() {
    Column {
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(72.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Challenge (Active)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.clickable(onClick = {}) // Safe empty loop
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp, 30.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldenOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Challenge",
                        tint = GoldenOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Challenge",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldenOrange
                )
            }

            // Tab 2: History (Decorative/Disabled block)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .alpha(0.4f)
                    .clickable(onClick = {})
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "History",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Tab 3: Stats (Decorative/Disabled block)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .alpha(0.4f)
                    .clickable(onClick = {})
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Stats",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stats",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// 3. Result / Completion Image Detail View Component
@Composable
fun ResultScreen(state: UiState, viewModel: ChallengeViewModel) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepCharcoal)
                .padding(innerPadding)
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateToMain() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldenOrange)
                    }
                    Text(
                        text = "CHALLENGE COMPLETED",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    text = "See your physical streak image below",
                    fontSize = 12.sp,
                    color = TextPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Image Render area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.generatedBitmap != null) {
                    Image(
                        bitmap = state.generatedBitmap.asImageBitmap(),
                        contentDescription = "Computed streak picture",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SunriseYellow.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(CardSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No image completed", color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }

            // Actions panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Layout description",
                            tint = SunriseYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "This image has been divided vertically into 10 parts with custom modern 'Day' labels overlaid to mark your active streak accomplishments.",
                            fontSize = 11.sp,
                            color = TextPrimary.copy(alpha = 0.8f),
                            lineHeight = 15.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateToMain() },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.0f)
                            .height(48.dp)
                            .border(1.dp, GoldenOrange, RoundedCornerShape(12.dp))
                    ) {
                        Text("Dashboard", color = GoldenOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.downloadGeneratedImage() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.0f).height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Download", tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

val ErrorColor = Color(0xFFCF6679)
