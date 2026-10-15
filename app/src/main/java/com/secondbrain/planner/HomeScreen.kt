package com.secondbrain.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.util.Locale

val IncomeColor = Color(0xFF34D399)
val ExpenseColor = Color(0xFFF87171)

@Composable
fun ClockCard() {
    val s = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000)
        }
    }
    val blink = rememberInfiniteTransition(label = "blink")
    val colonAlpha by blink.animateFloat(
        1f, .25f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "colon"
    )
    val hh = fa(String.format(Locale.US, "%02d", now.hour))
    val mm = fa(String.format(Locale.US, "%02d", now.minute))
    val ss = fa(String.format(Locale.US, "%02d", now.second))
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(s.radius.value.dp))
            .background(heroBrush()).padding(vertical = 22.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Ltr {
                if (s.clockStyle.value == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClockTile(hh)
                        Text(
                            ":", fontSize = 44.sp, fontWeight = FontWeight.Bold,
                            color = cs.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp).alpha(colonAlpha)
                        )
                        ClockTile(mm)
                        Spacer(Modifier.width(10.dp))
                        Text(ss, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer.copy(alpha = .7f))
                    }
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$hh:$mm", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer)
                        Text(" $ss", fontSize = 22.sp, color = cs.onPrimaryContainer.copy(alpha = .7f), modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(longDay(today()), color = cs.onPrimaryContainer.copy(alpha = .85f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ClockTile(text: String) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.width(88.dp).height(98.dp).clip(RoundedCornerShape(20.dp)).background(cs.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 54.sp, fontWeight = FontWeight.Bold, color = cs.onPrimary)
    }
}

@Composable
private fun QuickTile(k: Kind, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(cs.primary.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) { Icon(k.icon, null, tint = cs.primary) }
        Spacer(Modifier.height(4.dp))
        Text(k.single, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    AppCard(modifier, pad = 12.dp) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onAdd: (Kind) -> Unit,
    onEdit: (Item) -> Unit,
    onOpen: (Kind) -> Unit,
    onProfile: () -> Unit
) {
    val s = vm.settings
    val cs = MaterialTheme.colorScheme
    val items by vm.items.collectAsState()
    val logs by vm.logs.collectAsState()
    val t = today()
    val showClock = s.showClock.value
    val showQuick = s.showQuick.value
    val showStats = s.showStats.value
    val showHabits = s.showHabits.value
    val showFinance = s.showFinance.value
    val showTasks = s.showTasks.value
    val showProjects = s.showProjects.value

    val habitDays = remember(logs) {
        logs.groupBy { it.habitId }.mapValues { e -> e.value.map { l -> l.day }.toSet() }
    }
    val tasks = items.filter { it.type == Kind.TASK.name }
    val todayTasks = tasks.filter { it.day == t }
    val openTasks = tasks.filter { !it.done && (it.day == null || it.day <= t) }
        .sortedWith(compareByDescending<Item> { it.priority }.thenBy { it.day ?: Long.MAX_VALUE })
    val habits = items.filter { it.type == Kind.HABIT.name }
    val best = habits.maxOfOrNull { streak(habitDays[it.id]) } ?: 0
    val mk = monthKey(t)
    val incomes = items.filter { it.type == Kind.INCOME.name }
    val expenses = items.filter { it.type == Kind.EXPENSE.name }
    val incM = incomes.filter { monthKey(it.day ?: t) == mk }.sumOf { it.amount }
    val expM = expenses.filter { monthKey(it.day ?: t) == mk }.sumOf { it.amount }
    val projects = items.filter { it.type == Kind.PROJECT.name && !it.done }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${greeting()}، ${vm.auth.name} 👋",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                    )
                    Text(longDay(t), color = cs.onSurfaceVariant)
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(accentBrush()).clickable(onClick = onProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(vm.auth.name.take(1), color = cs.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
        if (showClock) item { ClockCard() }
        if (showQuick) item {
            AppCard {
                SectionTitle("ساخت سریع", Icons.Rounded.AutoAwesome)
                Kind.entries.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { k -> QuickTile(k, Modifier.weight(1f)) { onAdd(k) } }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (showStats) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    Icons.Rounded.TaskAlt,
                    "${fa(todayTasks.count { it.done })}/${fa(todayTasks.size)}",
                    "کارهای امروز", Modifier.weight(1f)
                )
                StatCard(Icons.Rounded.Whatshot, fa(best), "بهترین استریک", Modifier.weight(1f))
                StatCard(Icons.Rounded.AccountBalanceWallet, money(incM - expM), "تراز این ماه", Modifier.weight(1.2f))
            }
        }
        if (showHabits) item {
            AppCard(onClick = { onOpen(Kind.HABIT) }) {
                SectionTitle("نمودار عملکرد عادت‌ها", Icons.Rounded.Repeat)
                if (habits.isEmpty()) {
                    EmptyState("هنوز عادتی نساخته‌ای. از «ساخت سریع» یکی بساز.")
                } else {
                    val counts = (6 downTo 0).map { k ->
                        habits.count { h -> habitDays[h.id]?.contains(t - k) == true }.toFloat()
                    }
                    val labels = (6 downTo 0).map { k -> faWeekShort[weekIdx(t - k)] }
                    BarChart(counts, labels)
                    Spacer(Modifier.height(10.dp))
                    habits.take(4).forEach { h ->
                        val on = habitDays[h.id]?.contains(t) == true
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(h.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Pill("🔥 ${fa(streak(habitDays[h.id]))}", color = cs.tertiary)
                            Spacer(Modifier.width(10.dp))
                            CheckCircle(on) { vm.toggleLog(h.id, t) }
                        }
                    }
                }
            }
        }
        if (showFinance) item {
            AppCard(onClick = { onOpen(Kind.EXPENSE) }) {
                SectionTitle("درآمد و هزینه", Icons.Rounded.AccountBalanceWallet)
                BarChart(
                    monthlySums(incomes), monthLabels(),
                    values2 = monthlySums(expenses), c1 = IncomeColor, c2 = ExpenseColor
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("درآمد: ${money(incM)}", Icons.Rounded.TrendingUp, IncomeColor)
                    Pill("هزینه: ${money(expM)}", Icons.Rounded.TrendingDown, ExpenseColor)
                }
            }
        }
        if (showTasks) item {
            AppCard(onClick = { onOpen(Kind.TASK) }) {
                SectionTitle("کارهای پیش رو", Icons.Rounded.TaskAlt)
                if (openTasks.isEmpty()) {
                    EmptyState("همه‌چیز انجام شده 🎉")
                } else {
                    openTasks.take(5).forEach { task ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onEdit(task) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheckCircle(task.done) { vm.toggleDone(task) }
                            Spacer(Modifier.width(12.dp))
                            Text(task.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (task.day != null && task.day < t) Pill("عقب‌افتاده", color = cs.error)
                            else if (task.priority == 2) Pill("مهم", color = cs.error)
                        }
                    }
                }
            }
        }
        if (showProjects) item {
            AppCard(onClick = { onOpen(Kind.PROJECT) }) {
                SectionTitle("پروژه‌های فعال", Icons.Rounded.Folder)
                if (projects.isEmpty()) {
                    EmptyState("پروژهٔ فعالی نداری")
                } else {
                    projects.take(4).forEach { p ->
                        Column(Modifier.fillMaxWidth().clickable { onEdit(p) }.padding(vertical = 6.dp)) {
                            Row {
                                Text(p.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${fa(p.progress)}٪", color = cs.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { p.progress / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                                color = cs.primary, trackColor = cs.outlineVariant.copy(alpha = .4f)
                            )
                        }
                    }
                }
            }
        }
    }
}
