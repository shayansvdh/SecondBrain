package com.secondbrain.planner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Editor(val kind: Kind, val item: Item? = null, val day: Long? = null)

@Composable
fun HubScreen(vm: AppViewModel, onOpen: (Kind) -> Unit) {
    val items by vm.items.collectAsState()
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("بخش‌ها", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("همهٔ بخش‌های ذهن دومت", color = cs.onSurfaceVariant)
        }
        Kind.entries.chunked(2).forEach { row ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { k ->
                        val count = items.count { it.type == k.name }
                        AppCard(Modifier.weight(1f), onClick = { onOpen(k) }) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(accentBrush()),
                                contentAlignment = Alignment.Center
                            ) { Icon(k.icon, null, tint = cs.onPrimary) }
                            Spacer(Modifier.height(10.dp))
                            Text(k.fa, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("${fa(count)} مورد", fontSize = 12.sp, color = cs.onSurfaceVariant)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ModuleScreen(
    vm: AppViewModel,
    kind: Kind,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Item) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val items by vm.items.collectAsState()
    val logs by vm.logs.collectAsState()
    val list = remember(items, kind) { items.filter { it.type == kind.name } }
    val habitDays = remember(logs) {
        logs.groupBy { it.habitId }.mapValues { e -> e.value.map { l -> l.day }.toSet() }
    }
    var q by remember(kind) { mutableStateOf("") }
    var filter by remember(kind) { mutableIntStateOf(0) }
    var cat by remember(kind) { mutableStateOf("") }
    var del by remember { mutableStateOf<Item?>(null) }

    val shown = sortItems(
        kind,
        list.filter {
            (q.isBlank() || it.title.contains(q, true) || it.note.contains(q, true)) &&
                    (filter == 0 || (filter == 1 && !it.done) || (filter == 2 && it.done)) &&
                    (cat.isEmpty() || it.category == cat)
        }
    )

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Icon(kind.icon, null, tint = cs.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                kind.fa, Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
            )
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(accentBrush()).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Add, null, tint = cs.onPrimary) }
            Spacer(Modifier.width(8.dp))
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ModuleSummary(kind, list) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = q, onValueChange = { q = it },
                        placeholder = { Text("جستجو...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50)
                    )
                    if (kind.hasCheck) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("همه", "فعال", "انجام‌شده").forEachIndexed { i, n ->
                                FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(n) })
                            }
                        }
                    }
                    if (kind.categories.isNotEmpty()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            kind.categories.forEach { c ->
                                FilterChip(
                                    selected = cat == c,
                                    onClick = { cat = if (cat == c) "" else c },
                                    label = { Text(c) }
                                )
                            }
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item { EmptyState("موردی پیدا نشد. با دکمهٔ + یکی بساز.") }
            }
            items(shown, key = { it.id }) { it ->
                if (kind == Kind.HABIT) {
                    HabitCard(vm, it, habitDays[it.id], { onEdit(it) }, { del = it })
                } else {
                    ItemCard(vm, it, kind, { onEdit(it) }, { del = it })
                }
            }
        }
    }
    if (del != null) {
        AlertDialog(
            onDismissRequest = { del = null },
            title = { Text("حذف شود؟") },
            text = { Text("«${del!!.title}» برای همیشه پاک می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(del!!)
                    del = null
                }) { Text("حذف", color = cs.error) }
            },
            dismissButton = { TextButton(onClick = { del = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun ModuleSummary(kind: Kind, list: List<Item>) {
    val cs = MaterialTheme.colorScheme
    val t = today()
    when (kind) {
        Kind.INCOME, Kind.EXPENSE -> {
            val col = if (kind == Kind.INCOME) IncomeColor else ExpenseColor
            val mk = monthKey(t)
            val total = list.filter { monthKey(it.day ?: t) == mk }.sumOf { it.amount }
            AppCard {
                Text("مجموع این ماه", color = cs.onSurfaceVariant, fontSize = 12.sp)
                Text("${money(total)} تومان", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = col)
                Spacer(Modifier.height(12.dp))
                BarChart(monthlySums(list), monthLabels(), c1 = col, c2 = col)
            }
        }
        Kind.WEIGHT -> {
            val sorted = list.sortedBy { it.day ?: 0L }
            AppCard {
                if (sorted.isNotEmpty()) {
                    val last = sorted.last().amount
                    val diff = last - sorted.first().amount
                    Text("آخرین وزن", color = cs.onSurfaceVariant, fontSize = 12.sp)
                    Text("${fa(fmtNum(last))} کیلوگرم", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (sorted.size > 1) Pill(
                        "تغییر کل: ${fa(fmtNum(diff))}",
                        if (diff <= 0) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp
                    )
                    Spacer(Modifier.height(10.dp))
                }
                LineChart(sorted.map { it.amount.toFloat() })
            }
        }
        Kind.MEAL -> {
            val cal = list.filter { it.day == t }.sumOf { it.amount }
            AppCard {
                Text("کالری امروز", color = cs.onSurfaceVariant, fontSize = 12.sp)
                Text("${fa(fmtNum(cal))} کالری", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        else -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("کل: ${fa(list.size)}", kind.icon)
                if (kind.hasCheck) Pill("انجام‌شده: ${fa(list.count { it.done })}", Icons.Rounded.Check, IncomeColor)
            }
        }
    }
}

@Composable
fun ItemCard(vm: AppViewModel, item: Item, kind: Kind, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AppCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (kind.hasCheck) {
                CheckCircle(item.done) { vm.toggleDone(item) }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) cs.onSurfaceVariant else cs.onSurface
                )
                if (item.note.isNotBlank()) {
                    Text(
                        item.note, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = cs.onSurfaceVariant)
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item.day?.let { Pill(fmtDay(it), Icons.Rounded.CalendarMonth) }
            if (item.category.isNotBlank()) Pill(item.category, Icons.Rounded.Label, cs.secondary)
            if (kind.hasPriority && item.priority == 2) Pill("اولویت بالا", color = cs.error)
            if (kind.hasPriority && item.priority == 0) Pill("اولویت کم", color = cs.outline)
            if (kind.amountLabel != null) {
                when (kind) {
                    Kind.INCOME -> Pill("+${money(item.amount)}", color = IncomeColor)
                    Kind.EXPENSE -> Pill("-${money(item.amount)}", color = ExpenseColor)
                    Kind.MEAL -> Pill("${fa(fmtNum(item.amount))} کالری", Icons.Rounded.LocalFireDepartment, cs.tertiary)
                    else -> Pill("${fa(fmtNum(item.amount))} کیلوگرم", Icons.Rounded.MonitorWeight, cs.tertiary)
                }
            }
        }
        if (kind.hasProgress) {
            val p by animateFloatAsState(item.progress / 100f, label = "prog")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
                    color = cs.primary, trackColor = cs.outlineVariant.copy(alpha = .4f)
                )
                Spacer(Modifier.width(10.dp))
                Text("${fa(item.progress)}٪", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.primary)
            }
        }
    }
}

@Composable
fun HabitCard(vm: AppViewModel, item: Item, days: Set<Long>?, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val t = today()
    AppCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (item.note.isNotBlank()) {
                    Text(item.note, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            Pill("🔥 ${fa(streak(days))} روز", color = cs.tertiary)
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (k in 6 downTo 0) {
                val d = t - k
                val on = days?.contains(d) == true
                val bg: Brush = if (on) accentBrush() else SolidColor(cs.outlineVariant.copy(alpha = .3f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(faWeekShort[weekIdx(d)], fontSize = 11.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(bg)
                            .clickable { vm.toggleLog(item.id, d) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (on) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = cs.onPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(ed: Editor, vm: AppViewModel, onDismiss: () -> Unit) {
    val k = ed.kind
    val old = ed.item
    val cs = MaterialTheme.colorScheme
    val s = LocalSettings.current
    val fieldShape = RoundedCornerShape((s.radius.value * 0.7f).dp)

    var title by remember { mutableStateOf(old?.title ?: "") }
    var note by remember { mutableStateOf(old?.note ?: "") }
    var day by remember {
        mutableStateOf<Long?>(old?.day ?: ed.day ?: if (k.defaultToday) today() else null)
    }
    var amount by remember {
        mutableStateOf(if (old != null && old.amount != 0.0) fmtNum(old.amount) else "")
    }
    var category by remember { mutableStateOf(old?.category ?: "") }
    var priority by remember { mutableIntStateOf(old?.priority ?: 1) }
    var progress by remember { mutableFloatStateOf((old?.progress ?: 0).toFloat()) }
    var picker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surface
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(k.icon, null, tint = cs.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (old == null) "${k.single} جدید" else "ویرایش ${k.single}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (old != null) {
                    TextButton(onClick = {
                        vm.delete(old)
                        onDismiss()
                    }) { Text("حذف", color = cs.error) }
                }
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text("عنوان") },
                singleLine = true, shape = fieldShape, modifier = Modifier.fillMaxWidth()
            )
            if (k.amountLabel != null) {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it }, label = { Text(k.amountLabel) },
                    singleLine = true, shape = fieldShape, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            if (k.hasDay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { picker = true }, shape = fieldShape) {
                        Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(day?.let { fmtDay(it) } ?: "انتخاب ${k.dayLabel}")
                    }
                    if (day != null && !k.defaultToday) {
                        IconButton(onClick = { day = null }) { Icon(Icons.Rounded.Close, null) }
                    }
                }
            }
            if (k.categories.isNotEmpty()) {
                Text("دسته‌بندی", fontWeight = FontWeight.Medium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    k.categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = if (category == c) "" else c },
                            label = { Text(c) }
                        )
                    }
                }
            }
            if (k.hasPriority) {
                Text("اولویت", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("کم", "متوسط", "بالا").forEachIndexed { i, n ->
                        FilterChip(selected = priority == i, onClick = { priority = i }, label = { Text(n) })
                    }
                }
            }
            if (k.hasProgress) {
                Text("پیشرفت: ${fa(progress.toInt())}٪", fontWeight = FontWeight.Medium)
                Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f)
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it }, label = { Text("توضیحات") },
                minLines = 3, shape = fieldShape, modifier = Modifier.fillMaxWidth()
            )
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(s.radius.value.dp))
                    .background(if (title.isNotBlank()) accentBrush() else SolidColor(cs.outlineVariant))
                    .clickable(enabled = title.isNotBlank()) {
                        val base = old ?: Item(type = k.name, title = "")
                        vm.save(
                            base.copy(
                                title = title.trim(),
                                note = note,
                                day = day,
                                amount = en(amount).toDoubleOrNull() ?: 0.0,
                                category = category,
                                priority = priority,
                                progress = progress.toInt(),
                                done = if (k.hasProgress) progress >= 100f else (old?.done ?: false)
                            )
                        )
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("ذخیره", color = cs.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
    if (picker) {
        PersianDatePicker(day, { picker = false }) {
            day = it
            picker = false
        }
    }
}
