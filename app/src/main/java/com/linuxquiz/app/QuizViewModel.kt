package com.linuxquiz.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

enum class Screen { Home, Quiz, Result }

class QuizViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ScoreStore(app)

    val all: List<Question> = QuizRepository.load(app)
    val topics: List<String> = all.map { it.topic }.distinct()

    var screen by mutableStateOf(Screen.Home)
        private set

    // Настройки теста
    var selectedTopic by mutableStateOf<String?>(null)
    var questionCount by mutableStateOf(20) // ALL_QUESTIONS = все

    // Состояние текущего теста
    var session by mutableStateOf<List<Question>>(emptyList())
        private set
    var index by mutableStateOf(0)
        private set
    var selected by mutableStateOf<Int?>(null)
        private set
    var score by mutableStateOf(0)
        private set
    var mistakes by mutableStateOf<List<Mistake>>(emptyList())
        private set
    var history by mutableStateOf(store.all())
        private set

    fun countIn(topic: String): Int = all.count { it.topic == topic }

    fun start() {
        val pool = all.filter { selectedTopic == null || it.topic == selectedTopic }
        session = pool.shuffled().take(questionCount).map { q ->
            // Перемешиваем варианты при каждом прохождении
            val order = q.options.indices.shuffled()
            Question(
                id = q.id,
                topic = q.topic,
                text = q.text,
                options = order.map { q.options[it] },
                correct = order.indexOf(q.correct)
            )
        }
        index = 0
        score = 0
        selected = null
        mistakes = emptyList()
        screen = Screen.Quiz
    }

    fun answer(choice: Int) {
        if (selected != null) return
        selected = choice
        val q = session[index]
        if (choice == q.correct) {
            score += 1
        } else {
            mistakes = mistakes + Mistake(q.text, q.options[choice], q.options[q.correct])
        }
    }

    fun next() {
        if (index + 1 < session.size) {
            index += 1
            selected = null
        } else {
            finish()
        }
    }

    private fun finish() {
        store.add(Attempt(System.currentTimeMillis(), selectedTopic ?: "Все темы", score, session.size))
        history = store.all()
        screen = Screen.Result
    }

    fun toHome() {
        screen = Screen.Home
    }

    fun clearHistory() {
        store.clear()
        history = emptyList()
    }

    companion object {
        const val ALL_QUESTIONS = Int.MAX_VALUE
    }
}
