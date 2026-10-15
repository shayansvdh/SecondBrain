package com.secondbrain.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalendarScreen(
    vm: AppViewModel,
    onAdd: (Kind, Long) -> Unit,
    onEdit: (Item) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val items by vm.items.collectAsState()
    var offset by remember { mutableIntStateOf(0) }
    var sel by remember { mutableLongStateOf(today()) }
    val marks = remember(items) { items.mapNotNull { it.day }.toSet() }
    val dayItems = items.filter { it.day == sel }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "تقویم", Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {
                        offset = 0
                        sel = today()
                    },
                    label = { Text("امروز") },
                    leadingIcon = { Icon(Icons.Rounded.Today, null, Modifier.size(18.dp)) }
                )
            }
        }
        item {
            AppCard {
                MonthHeader(offset) { offset = it }
                MonthGrid(offset, sel, marks) { sel = it }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(longDay(sel), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Kind.TASK, Kind.PROJECT, Kind.JOURNAL, Kind.INCOME, Kind.EXPENSE, Kind.GOAL).forEach { k ->
                        AssistChip(
                            onClick = { onAdd(k, sel) },
                            label = { Text("${k.single} جدید") },
                            leadingIcon = { Icon(k.icon, null, Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
        if (dayItems.isEmpty()) item { EmptyState("برای این روز چیزی ثبت نشده") }
        items(dayItems, key = { it.id }) { it ->
            val k = Kind.valueOf(it.type)
            AppCard(onClick = { onEdit(it) }, pad = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (k.hasCheck) {
                        CheckCircle(it.done) { vm.toggleDone(it) }
                        Spacer(Modifier.width(10.dp))
                    } else {
                        Icon(k.icon, null, tint = cs.primary)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(it.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Pill(k.single)
                }
            }
        }
    }
}

@Composable
fun FocusScreen(vm: AppViewModel) {
    val t = vm.timer
    val cs = MaterialTheme.colorScheme
    var tab by remember { mutableIntStateOf(0) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("تمرکز", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("حالت کار عمیق را فعال کن", color = cs.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("پومودورو") },
                    leadingIcon = { Icon(Icons.Rounded.Timer, null, Modifier.size(18.dp)) })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("کرنومتر") },
                    leadingIcon = { Icon(Icons.Rounded.Timer, null, Modifier.size(18.dp)) })
            }
        }
        item { if (tab == 0) PomodoroCard(t) else StopwatchCard(t) }
    }
}

@Composable
private fun BigButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(76.dp).clip(androidx.compose.foundation.shape.CircleShape).background(accentBrush()).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, Modifier.size(38.dp), tint = cs.onPrimary) }
}

@Composable
private fun PomodoroCard(t: FocusTimer) {
    val cs = MaterialTheme.colorScheme
    val total = (if (t.mode == 0) t.focusMin else t.breakMin) * 60
    val prog = 1f - t.remaining / total.toFloat()
    AppCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Pill(if (t.mode == 0) "زمان تمرکز" else "استراحت", Icons.Rounded.Whatshot)
            Spacer(Modifier.height(16.dp))
            RingProgress(prog, dia = 250.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Ltr { Text(fa(mmss(t.remaining)), fontSize = 52.sp, fontWeight = FontWeight.Bold) }
                    Text("جلسهٔ ${fa(t.sessions)} انجام شد", color = cs.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                FilledTonalIconButton(onClick = { t.reset() }) { Icon(Icons.Rounded.Replay, null) }
                BigButton(if (t.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) { t.toggle() }
                FilledTonalIconButton(onClick = { t.next() }) { Icon(Icons.Rounded.SkipNext, null) }
            }
            Spacer(Modifier.height(18.dp))
            Stepper("مدت تمرکز", t.focusMin, { t.setFocus(t.focusMin - 5) }, { t.setFocus(t.focusMin + 5) })
            Stepper("مدت استراحت", t.breakMin, { t.setBreak(t.breakMin - 1) }, { t.setBreak(t.breakMin + 1) })
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        IconButton(onClick = onMinus) { Icon(Icons.Rounded.Remove, null) }
        Text("${fa(value)} دقیقه", fontWeight = FontWeight.Bold)
        IconButton(onClick = onPlus) { Icon(Icons.Rounded.Add, null) }
    }
}

private fun fmtMs(ms: Long): String =
    fa(String.format(java.util.Locale.US, "%02d:%02d.%02d", ms / 60000, ms / 1000 % 60, ms / 10 % 100))

@Composable
private fun StopwatchCard(t: FocusTimer) {
    val cs = MaterialTheme.colorScheme
    AppCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Ltr { Text(fmtMs(t.swMs), fontSize = 50.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                FilledTonalIconButton(onClick = { t.swReset() }) { Icon(Icons.Rounded.Replay, null) }
                BigButton(if (t.swRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) { t.swToggle() }
                FilledTonalIconButton(onClick = { t.swLap() }) { Icon(Icons.Rounded.Flag, null) }
            }
            Spacer(Modifier.height(14.dp))
            t.laps.forEachIndexed { i, ms ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text("دور ${fa(t.laps.size - i)}", Modifier.weight(1f), color = cs.onSurfaceVariant)
                    Ltr { Text(fmtMs(ms), fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}
