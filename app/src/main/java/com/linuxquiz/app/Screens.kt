package com.linuxquiz.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GreenBg = Color(0xFFC8E6C9)
private val RedBg = Color(0xFFFFCDD2)
private val DarkText = Color(0xFF1B1B1B)

@Composable
fun QuizApp(vm: QuizViewModel) {
    BackHandler(enabled = vm.screen != Screen.Home) { vm.toHome() }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (vm.screen) {
            Screen.Home -> HomeScreen(vm)
            Screen.Quiz -> QuizScreen(vm)
            Screen.Result -> ResultScreen(vm)
        }
    }
}

// ───────────────────────── Главный экран ─────────────────────────

@Composable
fun HomeScreen(vm: QuizViewModel) {
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val counts = listOf(10, 20, 50, 100, QuizViewModel.ALL_QUESTIONS)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Text("🐧 Linux Квиз", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${vm.all.size} вопросов · 4 варианта ответа", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { SectionTitle("Тема") }
        item {
            TopicRow("Все темы (${vm.all.size})", vm.selectedTopic == null) { vm.selectedTopic = null }
        }
        items(vm.topics) { topic ->
            TopicRow("$topic (${vm.countIn(topic)})", vm.selectedTopic == topic) { vm.selectedTopic = topic }
        }
        item {
            Column {
                SectionTitle("Количество вопросов")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    counts.forEach { c ->
                        FilterChip(
                            selected = vm.questionCount == c,
                            onClick = { vm.questionCount = c },
                            label = { Text(if (c == QuizViewModel.ALL_QUESTIONS) "Все" else c.toString()) }
                        )
                    }
                }
            }
        }
        item {
            Button(onClick = { vm.start() }, modifier = Modifier.fillMaxWidth()) {
                Text("Начать викторину")
            }
        }
        if (vm.history.isNotEmpty()) {
            item {
                val best = vm.history.maxByOrNull { it.score * 100 / it.total }!!
                Column {
                    SectionTitle("Мои результаты")
                    Text(
                        "Лучший: ${best.score} из ${best.total} (${best.score * 100 / best.total}%)",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            items(vm.history.take(10)) { a ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.topic, style = MaterialTheme.typography.bodyMedium)
                            Text(fmt.format(Date(a.time)), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${a.score} / ${a.total}", fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                OutlinedButton(onClick = { vm.clearHistory() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Очистить историю")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun TopicRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp))
    }
}

// ───────────────────────── Экран вопроса ─────────────────────────

@Composable
fun QuizScreen(vm: QuizViewModel) {
    if (vm.session.isEmpty()) return
    val q = vm.session[vm.index]
    val selected = vm.selected
    val answered = selected != null
    val total = vm.session.size

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Вопрос ${vm.index + 1} из $total", style = MaterialTheme.typography.titleSmall)
            Text("Баллы: ${vm.score}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (vm.index + if (answered) 1 else 0).toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(q.topic, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(q.text, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            q.options.forEachIndexed { i, option ->
                val isCorrect = i == q.correct
                val isChosen = i == selected
                val (bg, fg, label) = when {
                    !answered -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, option)
                    isCorrect -> Triple(GreenBg, DarkText, "✓  $option")
                    isChosen -> Triple(RedBg, DarkText, "✗  $option")
                    else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), option)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !answered) { vm.answer(i) }
                ) {
                    Text(label, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (answered) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.index + 1 < total) "Дальше" else "Завершить")
            }
        }
    }
}

// ───────────────────────── Итоги ─────────────────────────

@Composable
fun ResultScreen(vm: QuizViewModel) {
    val total = vm.session.size
    val percent = if (total == 0) 0 else vm.score * 100 / total
    val message = when {
        percent >= 90 -> "Отлично! Вы настоящий гуру Linux 🐧"
        percent >= 70 -> "Хороший результат!"
        percent >= 50 -> "Неплохо, но есть что повторить"
        else -> "Стоит подтянуть основы — всё получится!"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Викторина завершена", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("${vm.score} / $total", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("баллов · $percent%", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("Результат сохранён в истории", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { vm.start() }, modifier = Modifier.weight(1f)) { Text("Ещё раз") }
                OutlinedButton(onClick = { vm.toHome() }, modifier = Modifier.weight(1f)) { Text("На главную") }
            }
        }
        if (vm.mistakes.isNotEmpty()) {
            item { SectionTitle("Работа над ошибками (${vm.mistakes.size})") }
            items(vm.mistakes) { m ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(m.question, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Ваш ответ: ${m.yourAnswer}", color = MaterialTheme.colorScheme.error)
                        Text("Правильно: ${m.correctAnswer}", color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}
