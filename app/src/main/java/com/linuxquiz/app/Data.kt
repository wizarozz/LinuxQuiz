package com.linuxquiz.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Question(
    val id: Int,
    val topic: String,
    val text: String,
    val options: List<String>,
    val correct: Int
)

data class Attempt(val time: Long, val topic: String, val score: Int, val total: Int)

data class Mistake(val question: String, val yourAnswer: String, val correctAnswer: String)

object QuizRepository {
    fun load(context: Context): List<Question> {
        val json = context.assets.open("questions.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val opts = o.getJSONArray("options")
            Question(
                id = o.getInt("id"),
                topic = o.getString("topic"),
                text = o.getString("q"),
                options = List(opts.length()) { opts.getString(it) },
                correct = o.getInt("correct")
            )
        }
    }
}

/** Хранит историю результатов (баллы) в SharedPreferences. */
class ScoreStore(context: Context) {
    private val prefs = context.getSharedPreferences("quiz_scores", Context.MODE_PRIVATE)

    fun all(): List<Attempt> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Attempt(o.getLong("time"), o.getString("topic"), o.getInt("score"), o.getInt("total"))
        }
    }

    fun add(attempt: Attempt) {
        val list = (listOf(attempt) + all()).take(MAX_ITEMS)
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("time", it.time)
                    .put("topic", it.topic)
                    .put("score", it.score)
                    .put("total", it.total)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "history"
        const val MAX_ITEMS = 100
    }
}
