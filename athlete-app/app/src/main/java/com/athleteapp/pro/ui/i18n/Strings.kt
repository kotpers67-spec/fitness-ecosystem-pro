package com.athleteapp.pro.ui.i18n

enum class AthleteLanguage(val code: String, val title: String) {
    RU("ru", "Русский"),
    EN("en", "English")
}

object AthleteStrings {
    fun get(key: String, lang: String = "ru"): String {
        val isEn = lang == "en"
        return when (key) {
            "app_title" -> if (isEn) "ATHLETE PRO" else "ATHLETE PRO"
            "my_workout" -> if (isEn) "TODAY'S WORKOUT" else "МОЯ ТРЕНИРОВКА"
            "my_history" -> if (isEn) "MY PROGRESS" else "МОЙ ПРОГРЕСС"
            "my_profile" -> if (isEn) "PROFILE & SYNC" else "ПРОФИЛЬ И СИНХРОНИЗАЦИЯ"
            "sync_coach" -> if (isEn) "Sync with Coach" else "Синхронизация с тренером"
            "auto_update" -> if (isEn) "Auto-Update" else "Автообновление"
            "check_update" -> if (isEn) "Check for Updates" else "Проверить обновления"
            "rest_timer" -> if (isEn) "REST" else "ОТДЫХ"
            "done" -> if (isEn) "DONE" else "ГОТОВО"
            "weight" -> if (isEn) "Weight (kg)" else "Вес (кг)"
            "reps" -> if (isEn) "Reps" else "Повторы"
            else -> key
        }
    }
}
