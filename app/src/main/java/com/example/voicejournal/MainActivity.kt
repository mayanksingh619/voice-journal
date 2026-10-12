package com.example.voicejournal

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.voicejournal.data.JournalDatabase
import com.example.voicejournal.data.JournalEntry
import com.example.voicejournal.reminders.ReminderReceiver
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { VoiceJournalApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceJournalApp() {
    val context = LocalContext.current
    val dao = remember { JournalDatabase.get(context).journalDao() }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val entriesFlow = remember(query) {
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())
    }
    val entries by entriesFlow.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<JournalEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var source by remember { mutableStateOf("typed") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            editing = null
            draftText = spoken
            reminderAt = null
            source = "speech"
            showEditor = true
        } else if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "No speech was recognized. You can type your note instead.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startNew(text: String = "", inputSource: String = "typed") {
        editing = null
        draftText = text
        reminderAt = null
        source = inputSource
        showEditor = true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Voice Journal", fontWeight = FontWeight.SemiBold)
                    Text("Your memories, kept on this phone", style = MaterialTheme.typography.labelSmall)
                }
            })
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FloatingActionButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your journal entry")
                    }
                    try { speechLauncher.launch(intent) }
                    catch (_: Exception) { Toast.makeText(context, "Speech recognition isn't available. Type your note instead.", Toast.LENGTH_LONG).show(); startNew() }
                }) { Icon(Icons.Default.Mic, contentDescription = "Speak a journal entry") }
                FloatingActionButton(onClick = { startNew() }) { Icon(Icons.Default.Add, contentDescription = "Add a journal entry") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search your journal") },
                singleLine = true
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (query.isBlank()) "Recent entries" else "Search results", style = MaterialTheme.typography.titleMedium)
                Text("${entries.size} entries", style = MaterialTheme.typography.labelMedium)
            }
            if (entries.isEmpty()) {
                Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(if (query.isBlank()) "Start with a thought" else "No matching entries", style = MaterialTheme.typography.titleMedium)
                    Text(if (query.isBlank()) "Tap the microphone to speak, or + to type. Entries are saved locally." else "Try another keyword.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(entries, key = { it.id }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                editing = entry
                                draftText = entry.text
                                reminderAt = entry.reminderAt
                                source = entry.source
                                showEditor = true
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(entry.text, style = MaterialTheme.typography.bodyLarge, maxLines = 5)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(formatDate(entry.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (entry.reminderAt != null) {
                                            Icon(Icons.Default.Notifications, contentDescription = "Has reminder", tint = MaterialTheme.colorScheme.primary)
                                            Text(formatDate(entry.reminderAt), style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(onClick = { scope.launch { dao.deleteById(entry.id) } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text("Private by default · No custom server · Cloud AI not enabled in this MVP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "New journal entry" else "Edit journal entry") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What would you like to remember?") },
                        minLines = 4,
                        maxLines = 8
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        chooseDateTime(context, reminderAt) { reminderAt = it }
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Text(if (reminderAt == null) " Add reminder" else " Change reminder (${formatDate(reminderAt!!)})")
                    }
                    if (reminderAt != null) TextButton(onClick = { reminderAt = null }) { Text("Remove reminder") }
                    Text("Source: ${if (source == "speech") "speech transcript" else "typed note"}", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clean = draftText.trim()
                    if (clean.isBlank()) {
                        Toast.makeText(context, "Write or dictate a note first.", Toast.LENGTH_SHORT).show()
                    } else {
                        val now = System.currentTimeMillis()
                        val old = editing
                        val entry = JournalEntry(
                            id = old?.id ?: UUID.randomUUID().toString(),
                            text = clean,
                            createdAt = old?.createdAt ?: now,
                            updatedAt = now,
                            source = source,
                            reminderAt = reminderAt
                        )
                        scope.launch {
                            dao.upsert(entry)
                            reminderAt?.let { scheduleReminder(context, entry, it) }
                        }
                        showEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }
}

private fun formatDate(time: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(time))

private fun chooseDateTime(context: Context, current: Long?, onSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { if (current != null) timeInMillis = current else add(Calendar.HOUR_OF_DAY, 1) }
    DatePickerDialog(context, { _, year, month, day ->
        val picked = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
        }
        TimePickerDialog(context, { _, hour, minute ->
            picked.set(Calendar.HOUR_OF_DAY, hour); picked.set(Calendar.MINUTE, minute); picked.set(Calendar.SECOND, 0)
            onSelected(picked.timeInMillis)
        }, picked.get(Calendar.HOUR_OF_DAY), picked.get(Calendar.MINUTE), false).show()
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
}

private fun scheduleReminder(context: Context, entry: JournalEntry, at: Long) {
    if (at <= System.currentTimeMillis()) {
        Toast.makeText(context, "Reminder time is in the past; entry was saved without scheduling it.", Toast.LENGTH_LONG).show()
        return
    }
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", "Journal reminder")
        putExtra("body", entry.text.take(160))
        putExtra("notificationId", entry.id.hashCode())
    }
    val pending = PendingIntent.getBroadcast(context, entry.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }
}
