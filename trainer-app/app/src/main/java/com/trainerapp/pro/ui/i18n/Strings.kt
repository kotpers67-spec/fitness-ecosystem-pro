package com.trainerapp.pro.ui.i18n

enum class AppLanguage(val code: String, val title: String) {
    RU("ru", "Русский"),
    EN("en", "English")
}

object AppStrings {
    fun get(key: String, lang: String = "ru"): String {
        val isEn = lang == "en"
        return when (key) {
            "app_title" -> if (isEn) "TRAINER PRO" else "TRAINER PRO"
            "current_client" -> if (isEn) "CURRENT ATHLETE" else "ТЕКУЩИЙ ПОДОПЕЧНЫЙ"
            "select_client" -> if (isEn) "Select athlete" else "Выберите подопечного"
            "athlete_card" -> if (isEn) "ATHLETE CARD" else "КАРТОЧКА АТЛЕТА"
            "active" -> if (isEn) "Active" else "Активен"
            "goal" -> if (isEn) "Goal" else "Цель"
            "notes" -> if (isEn) "Notes" else "Заметки"
            "workout" -> if (isEn) "WORKOUT" else "ТРЕНИРОВКА"
            "history_stats" -> if (isEn) "HISTORY & CHARTS" else "ИСТОРИЯ И ГРАФИКИ"
            "settings_title" -> if (isEn) "SETTINGS & DATABASE" else "НАСТРОЙКИ И БАЗА"
            "color_theme" -> if (isEn) "COLOR THEME (5 PALETTES)" else "ЦВЕТОВАЯ ТЕМА (5 ПАЛИТР)"
            "layout_style" -> if (isEn) "LAYOUT & DESIGN STYLE" else "ДИЗАЙН И КОМПОНОВКА ЭКРАНА"
            "clients_list" -> if (isEn) "ATHLETES LIST" else "СПИСОК ИМЕН (КЛИЕНТЫ)"
            "exercises_list" -> if (isEn) "EXERCISES LIST" else "СПИСОК УПРАЖНЕНИЙ"
            "language" -> if (isEn) "LANGUAGE / ЯЗЫК" else "ЯЗЫК ИНТЕРФЕЙСА"
            "cloud_sync" -> if (isEn) "SYNC" else "СИНХРОНИЗАЦИЯ"
            "auto_update" -> if (isEn) "AUTO-UPDATE" else "АВТООБНОВЛЕНИЕ"
            "check_update" -> if (isEn) "Check for Updates" else "Проверить обновления"
            "push_to_cloud" -> if (isEn) "Sync" else "Синхронизация"
            "pull_from_cloud" -> if (isEn) "Pull from Cloud" else "Загрузить из облака"
            "sets" -> if (isEn) "SETS" else "ПОДХОДЫ"
            "weight_kg" -> if (isEn) "WEIGHT (KG)" else "ВЕС (КГ)"
            "reps" -> if (isEn) "REPS" else "ПОВТОРЫ"
            "done" -> if (isEn) "DONE" else "ГОТОВО"
            "add_set" -> if (isEn) "+ ADD SET" else "+ ДОБАВИТЬ ПОДХОД"
            "rest_timer" -> if (isEn) "REST BETWEEN SETS" else "ОТДЫХ МЕЖДУ СЕТАМИ"
            "body_weight_dynamic" -> if (isEn) "BODY WEIGHT DYNAMICS (KG)" else "ДИНАМИКА ВЕСА ТЕЛА (КГ)"
            "exercise_progress" -> if (isEn) "STRENGTH PROGRESS BY EXERCISE" else "ПРОГРЕСС СИЛОВЫХ ПО УПРАЖНЕНИЮ"
            "sync_hub" -> if (isEn) "SYNC" else "СИНХРОНИЗАЦИЯ"
            else -> key
        }
    }
}
