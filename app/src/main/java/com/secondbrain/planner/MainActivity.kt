package com.secondbrain.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalSettings provides vm.settings) {
                AppTheme(vm.settings) { AppRoot(vm) }
            }
        }
    }
}

@Composable
fun Backdrop() {
    val cs = MaterialTheme.colorScheme
    val tr = rememberInfiniteTransition(label = "bd")
    val p = tr.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val v = p.value
        drawRect(Brush.verticalGradient(listOf(cs.primary.copy(alpha = .14f), Color.Transparent, cs.tertiary.copy(alpha = .10f))))
        val c1 = Offset(w * (0.95f - 0.25f * v), h * (0.08f + 0.06f * v))
        drawCircle(
            Brush.radialGradient(listOf(cs.primary.copy(alpha = .32f), Color.Transparent), center = c1, radius = w * .8f),
            radius = w * .8f, center = c1
        )
        val c2 = Offset(w * (0.1f + 0.25f * v), h * (0.92f - 0.08f * v))
        drawCircle(
            Brush.radialGradient(listOf(cs.tertiary.copy(alpha = .26f), Color.Transparent), center = c2, radius = w * .7f),
            radius = w * .7f, center = c2
        )
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides cs.onBackground) {
        Box(Modifier.fillMaxSize().background(cs.background)) {
            if (vm.settings.gradientBg.value) Backdrop()
            if (!vm.auth.loggedIn) AuthScreen(vm) else MainShell(vm)
        }
    }
}

private val tabs = listOf(
    Icons.Rounded.Home to "خانه",
    Icons.Rounded.CalendarMonth to "تقویم",
    Icons.Rounded.Timer to "تمرکز",
    Icons.Rounded.GridView to "بخش‌ها",
    Icons.Rounded.Settings to "تنظیمات"
)

@Composable
fun FloatingBar(sel: Int, onSel: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(68.dp)
            .shadow(16.dp, shape).clip(shape)
            .background(cs.surface.copy(alpha = .94f))
            .border(1.dp, cs.primary.copy(alpha = .25f), shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { i, (icon, label) ->
            val on = i == sel
            val w by animateFloatAsState(if (on) 1.9f else 1f, label = "w")
            Box(
                Modifier.weight(w).fillMaxHeight().clip(RoundedCornerShape(24.dp))
                    .then(if (on) Modifier.background(accentBrush()) else Modifier)
                    .clickable { onSel(i) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = if (on) cs.onPrimary else cs.onSurfaceVariant)
                    AnimatedVisibility(visible = on) {
                        Text(
                            " $label", color = cs.onPrimary, fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainShell(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var openKind by remember { mutableStateOf<Kind?>(null) }
    var editor by remember { mutableStateOf<Editor?>(null) }

    BackHandler(enabled = openKind != null || tab != 0) {
        if (openKind != null) openKind = null else tab = 0
    }

    Box(Modifier.fillMaxSize()) {
        val screen = openKind?.let { 100 + it.ordinal } ?: tab
        Crossfade(targetState = screen, modifier = Modifier.fillMaxSize().statusBarsPadding(), label = "screen") { sc ->
            when {
                sc >= 100 -> {
                    val k = Kind.entries[sc - 100]
                    ModuleScreen(
                        vm, k,
                        onBack = { openKind = null },
                        onAdd = { editor = Editor(k) },
                        onEdit = { editor = Editor(Kind.valueOf(it.type), it) }
                    )
                }
                sc == 0 -> HomeScreen(
                    vm,
                    onAdd = { editor = Editor(it) },
                    onEdit = { editor = Editor(Kind.valueOf(it.type), it) },
                    onOpen = { openKind = it },
                    onProfile = { tab = 4 }
                )
                sc == 1 -> CalendarScreen(
                    vm,
                    onAdd = { k, d -> editor = Editor(k, null, d) },
                    onEdit = { editor = Editor(Kind.valueOf(it.type), it) }
                )
                sc == 2 -> FocusScreen(vm)
                sc == 3 -> HubScreen(vm) { openKind = it }
                else -> SettingsScreen(vm)
            }
        }
        FloatingBar(
            sel = tab,
            onSel = {
                openKind = null
                tab = it
            },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
    }

    editor?.let { ed ->
        EditSheet(ed, vm) { editor = null }
    }
}
