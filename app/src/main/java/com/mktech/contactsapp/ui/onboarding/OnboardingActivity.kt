package com.mktech.contactsapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.mktech.contactsapp.BaseActivity
import com.mktech.contactsapp.MainActivity
import com.mktech.contactsapp.R
import com.mktech.contactsapp.ui.components.NativeAdCard
import com.mktech.contactsapp.ui.theme.ContactsAppTheme
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val iconTint: Color,
    val bgColor: Color
)

class OnboardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContactsAppTheme {
                OnboardingScreen(
                    onFinish = { finishOnboarding() }
                )
            }
        }
    }

    private fun finishOnboarding() {
        // Mark onboarding as done
        getSharedPreferences("app_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_done", true)
            .putBoolean("skip_splash", true)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

            // ── Pager ────────────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                OnboardingPageContent(
                    page      = pages[pageIndex],
                    pageIndex = pageIndex,
                    showAd    = pageIndex == pages.size - 1
                )
            }

            // ── Skip button ──────────────────────────────────────────────────
            this@Column.AnimatedVisibility(
                visible = !isLastPage,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                enter = fadeIn(),
                exit  = fadeOut()
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                        text  = stringResource(R.string.onboarding_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Bottom controls ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dot indicators
                PageIndicator(
                    pageCount   = pages.size,
                    currentPage = pagerState.currentPage
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Next / Get Started button
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        label       = "btn_label"
                    ) { last ->
                        Text(
                            text       = stringResource(
                                if (last) R.string.onboarding_get_started
                                else      R.string.onboarding_next
                            ),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp
                        )
                    }
                }
            }
        }

        // NativeAdCard sits here — outside the Box, no overlap, no padding applied
        NativeAdCard()
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    showAd: Boolean
) {
    // Animate icon scale on page entry
    val scale by animateFloatAsState(
        targetValue    = 1f,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 100.dp, bottom = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // Icon circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(page.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector       = page.icon,
                contentDescription = null,
                tint              = page.iconTint,
                modifier          = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text       = stringResource(page.titleRes),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            color      = Color.White
        )

        // Description
        Text(
            text      = stringResource(page.descRes),
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color     = Color.White,
            lineHeight = 24.sp
        )

    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue   = if (isSelected) 24.dp else 8.dp,
                animationSpec = tween(300),
                label         = "dot_width"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun rememberOnboardingPages(): List<OnboardingPage> {
    val primary       = MaterialTheme.colorScheme.primary
    val secondary     = MaterialTheme.colorScheme.secondary
    val tertiary      = MaterialTheme.colorScheme.tertiary
    val error         = MaterialTheme.colorScheme.error
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    return remember {
        listOf(
            OnboardingPage(
                titleRes  = R.string.onboarding_title_1,
                descRes   = R.string.onboarding_desc_1,
                icon      = Icons.Default.Call,
                iconTint  = primary,
                bgColor   = primaryContainer.copy(alpha = 0.4f)
            ),
//            OnboardingPage(
//                titleRes  = R.string.onboarding_title_2,
//                descRes   = R.string.onboarding_desc_2,
//                icon      = Icons.Default.ContactPhone,
//                iconTint  = secondary,
//                bgColor   = secondaryContainer.copy(alpha = 0.4f)
//            ),
            OnboardingPage(
                titleRes  = R.string.onboarding_title_3,
                descRes   = R.string.onboarding_desc_3,
                icon      = Icons.Default.SwipeLeft,
                iconTint  = error,
                bgColor   = error.copy(alpha = 0.12f)
            ),
            OnboardingPage(
                titleRes  = R.string.onboarding_title_4,
                descRes   = R.string.onboarding_desc_4,
                icon      = Icons.Default.Palette,
                iconTint  = tertiary,
                bgColor   = tertiary.copy(alpha = 0.12f)
            ),
            OnboardingPage(
                titleRes  = R.string.onboarding_title_5,
                descRes   = R.string.onboarding_desc_5,
                icon      = Icons.Default.SortByAlpha,
                iconTint  = primary,
                bgColor   = primaryContainer.copy(alpha = 0.4f)
            ),
            OnboardingPage(
                titleRes  = R.string.onboarding_title_6,
                descRes   = R.string.onboarding_desc_6,
                icon      = Icons.Default.StarBorder,
                iconTint  = secondary,
                bgColor   = secondaryContainer.copy(alpha = 0.4f)
            ),
        )
    }
}