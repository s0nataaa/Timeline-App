package com.sonata.timeline

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.Instant
import java.time.ZoneOffset
import android.content.Intent

private val Bg = Color(0xFFF2F2F7)
private val Blue = Color(0xFF007AFF)
private val Indigo = Color(0xFF5856D6)
private val Green = Color(0xFF34C759)
private val Orange = Color(0xFFFF9500)
private val Red = Color(0xFFFF3B30)
private val Gray = Color(0xFF8E8E93)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.load(this)
        if (Store.items.isEmpty()) {
            val d = LocalDate.now()
            Store.items += Item(title = "Kumpulkan tugas web", type = Type.DEADLINE, due = d.minusDays(1), note = "Kedai Kopi Nusantara")
            Store.items += Item(title = "Presentasi Case Essay", type = Type.DEADLINE, due = d.plusDays(9))
            Store.items += Item(title = "Rapikan portofolio", type = Type.IMPORTANT)
            Store.items += Item(title = "Latihan hiragana", type = Type.DAILY)
            Store.save(this)
        }
        setContent { MaterialTheme { Screen() } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNav(window)
    }
}

private fun toggle(c: Context, it: Item) {
    val n = Store.items.indexOfFirst { x -> x.id == it.id }
    Store.items[n] = if (it.type == Type.DAILY)
        it.copy(lastDone = if (it.isDone()) null else LocalDate.now())
    else it.copy(done = !it.done)
    Store.save(c)
}

fun meta(i: Item): Pair<String, Color> = when (i.type) {
    Type.IMPORTANT -> "Penting, tanpa due date" to Orange
    Type.DAILY -> "Harian" to Indigo
    Type.DEADLINE -> {
        val d = i.daysLeft()!!
        val rel = when {
            d < 0 -> "Terlambat ${-d} hari"
            d == 0 -> "Hari ini"
            d == 1 -> "Besok"
            else -> "$d hari lagi"
        }
        val tgl = i.due!!.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id")))
        "$tgl · $rel" to (if (d < 0) Red else if (d <= 3) Orange else Blue)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen() {
    val ctx = LocalContext.current
    var filter by remember { mutableStateOf<Type?>(null) }
    var sheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }
    val options = listOf<Pair<String, Type?>>(
        "Semua" to null, "Deadline" to Type.DEADLINE, "Penting" to Type.IMPORTANT, "Harian" to Type.DAILY
    )
    val rows = Store.items.sortedTimeline().filter { filter == null || it.type == filter }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    "Timeline", fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    val f = java.io.File(ctx.filesDir, "items.json")
                    if (f.exists()) {
                        ctx.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, f.readText())
                            }, "Export"
                        ))
                    }
                }) { Text("Export", color = Blue) }
                TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString().orEmpty()
                    val ok = t.isNotBlank() && Store.importJson(ctx, t)
                    android.widget.Toast.makeText(
                        ctx, if (ok) "Import berhasil" else "Clipboard bukan data Timeline", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) { Text("Import", color = Blue) }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { n, (label, t) ->
                    SegmentedButton(
                        selected = filter == t,
                        onClick = { filter = t },
                        shape = SegmentedButtonDefaults.itemShape(n, options.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = Blue, activeContentColor = Color.White)
                    ) { Text(label, fontSize = 13.sp, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (rows.isEmpty()) {
                Text("Belum ada event", color = Gray, modifier = Modifier.padding(32.dp))
            } else {
                LazyColumn(
                    Modifier.weight(1f, fill = false).clip(RoundedCornerShape(14.dp)).background(Color.White)
                ) {
                    items(rows, key = { it.id }) { item ->
                        ItemRow(item, Modifier.animateItem(), onEdit = { editing = item; sheet = true }) {
                            toggle(ctx, item)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = null; sheet = true },
            containerColor = Blue, contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)
        ) { Text("+", fontSize = 28.sp) }
    }
    if (sheet) FormSheet(editing) { sheet = false }
}

@Composable
private fun ItemRow(item: Item, modifier: Modifier, onEdit: () -> Unit, onToggle: () -> Unit) {
    val done = item.isDone()
    val (text, color) = meta(item)
    val ring by animateColorAsState(if (done) Green else color)
    val fill by animateColorAsState(if (done) Green else Color.White)
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, ring, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (done) Text("✓", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).clickable(onClick = onEdit)) {
            Text(
                item.title, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                color = if (done) Gray else Color.Black,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(text, color = color, fontSize = 13.sp)
            // selalu ada walau kosong, supaya tinggi semua baris sama
            Text(
                item.note.ifEmpty { " " },
                color = Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormSheet(item: Item?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(item?.title ?: "") }
    var type by remember { mutableStateOf(item?.type ?: Type.DEADLINE) }
    var due by remember { mutableStateOf(item?.due) }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var pickDate by remember { mutableStateOf(false) }
    val canSave = title.isNotBlank() && (type != Type.DEADLINE || due != null)
    val opts = listOf("Deadline" to Type.DEADLINE, "Penting" to Type.IMPORTANT, "Harian" to Type.DAILY)

    ModalBottomSheet(onDismissRequest = onClose, containerColor = Bg) {
        val w = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        SideEffect { w?.let(::hideNav) }
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (item == null) "Event baru" else "Ubah event", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                title, { title = it }, label = { Text("Nama event") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                opts.forEachIndexed { n, (label, t) ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t },
                        shape = SegmentedButtonDefaults.itemShape(n, opts.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = Blue, activeContentColor = Color.White)
                    ) { Text(label, fontSize = 13.sp) }
                }
            }
            if (type == Type.DEADLINE) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(due?.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id"))) ?: "Pilih due date")
                }
            }
            OutlinedTextField(
                note, { note = it }, label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val d = if (type == Type.DEADLINE) due else null
                    if (item == null) {
                        Store.items += Item(title = title.trim(), type = type, due = d, note = note.trim())
                    } else {
                        val n = Store.items.indexOfFirst { it.id == item.id }
                        Store.items[n] = item.copy(title = title.trim(), type = type, due = d, note = note.trim())
                    }
                    Store.save(ctx); onClose()
                },
                enabled = canSave, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) { Text("Simpan") }
            if (item != null) {
                TextButton(
                    onClick = { Store.items.removeAll { it.id == item.id }; Store.save(ctx); onClose() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Hapus", color = Red) }
            }
        }
    }

    if (pickDate) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = st) }
    }
}

fun hideNav(w: android.view.Window) {
    val c = androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
    c.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    c.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
}