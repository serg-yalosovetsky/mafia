package com.serg.mafia.model

/** Кого приложение считает победившим и как комиссар видит игрока. */
enum class Faction { RED, BLACK, MANIAC }

enum class Role(
    /** Ключ перевода названия роли — текст живёт в словаре, а не в модели. */
    val titleKey: String,
    val faction: Faction,
    /** Как выглядит при проверке комиссара. Маньяк для комиссара — чёрный. */
    val checksAs: Faction = faction,
) {
    CIVILIAN("role_civilian", Faction.RED),
    DOCTOR("role_doctor", Faction.RED),
    SHERIFF("role_sheriff", Faction.RED),
    BUTTERFLY("role_butterfly", Faction.RED),
    MAFIA("role_mafia", Faction.BLACK),
    DON("role_don", Faction.BLACK),
    MANIAC("role_maniac", Faction.MANIAC, checksAs = Faction.BLACK);

    val isBlack: Boolean get() = faction == Faction.BLACK

    /** Ключ короткой подсказки на карточке игрока при раздаче. */
    val hintKey: String
        get() = when (this) {
            CIVILIAN -> "hint_civilian"
            DOCTOR -> "hint_doctor"
            SHERIFF -> "hint_sheriff"
            BUTTERFLY -> "hint_butterfly"
            MAFIA -> "hint_mafia"
            DON -> "hint_don"
            MANIAC -> "hint_maniac"
        }
}

/** Состав партии: сколько кого раздать. */
data class Setup(
    val playerCount: Int = 10,
    val names: List<String> = defaultNames(10),
    val withDon: Boolean = true,
    val withDoctor: Boolean = true,
    val withSheriff: Boolean = true,
    val withButterfly: Boolean = false,
    val withManiac: Boolean = false,
    /** Ведущий может сам добавить или убрать чёрных — партия станет сложнее или проще. */
    val mafiaOverride: Int? = null,
    /** Первая ночь заранее назначена промахом: мафия стреляет в воздух, красным легче. */
    val firstNightMiss: Boolean = false,
    val lang: String = "uk",
    val introSpeechSeconds: Int = 30,
    val daySpeechSeconds: Int = 60,
) {
    /** Классическая треть стола — то, что приложение предлагает по умолчанию. */
    val autoMafia: Int get() = Math.round(playerCount / 3f).toInt().coerceAtLeast(1)

    val maxMafia: Int get() = ((playerCount - 1) / 2).coerceAtLeast(1)

    /** Чёрных всего (дон входит в это число): ручное значение ведущего или авто. */
    val mafiaTotal: Int get() = (mafiaOverride ?: autoMafia).coerceIn(1, maxMafia)

    val mafiaIsManual: Boolean get() = mafiaOverride != null && mafiaOverride != autoMafia

    /** Насколько тяжело городу: сколько мирных приходится на одного убийцу. */
    val redsPerKiller: Float
        get() {
            val killers = mafiaTotal + if (withManiac) 1 else 0
            val reds = playerCount - killers
            return if (killers == 0) 0f else reds / killers.toFloat()
        }

    /** Ключ подсказки ведущему на экране настройки: насколько тяжело городу. */
    val difficultyKey: String
        get() = when {
            redsPerKiller >= 2.6f -> "diff_easy"
            redsPerKiller >= 2.1f -> "diff_easier"
            redsPerKiller >= 1.8f -> "diff_normal"
            redsPerKiller >= 1.5f -> "diff_hard"
            else -> "diff_very_hard"
        }

    val roleCounts: Map<Role, Int>
        get() {
            val m = LinkedHashMap<Role, Int>()
            val don = if (withDon && mafiaTotal >= 1) 1 else 0
            if (don > 0) m[Role.DON] = 1
            val plainMafia = mafiaTotal - don
            if (plainMafia > 0) m[Role.MAFIA] = plainMafia
            if (withSheriff) m[Role.SHERIFF] = 1
            if (withDoctor) m[Role.DOCTOR] = 1
            if (withButterfly) m[Role.BUTTERFLY] = 1
            if (withManiac) m[Role.MANIAC] = 1
            val special = m.values.sum()
            val civilians = playerCount - special
            if (civilians > 0) m[Role.CIVILIAN] = civilians
            return m
        }

    val assignedTotal: Int get() = roleCounts.values.sum()

    /** Не сходится состав — партию не начинаем (спец-ролей больше, чем людей за столом). */
    val isValid: Boolean get() = assignedTotal == playerCount && playerCount in 4..20

    /** Ключ ошибки состава и его параметры (нужны для подстановки в перевод). */
    val validationKey: Pair<String, List<Any>>?
        get() = when {
            playerCount < 4 -> "err_min_players" to listOf(4)
            playerCount > 20 -> "err_max_players" to listOf(20)
            assignedTotal > playerCount ->
                "err_too_many_roles" to listOf(assignedTotal, playerCount)
            else -> null
        }

    /**
     * Мафия сильно впереди по стартовому балансу — предложим ведущему
     * первую ночь «в воздух», чтобы усилить красных.
     */
    val suggestsFirstNightMiss: Boolean
        get() {
            val extraKillers = mafiaTotal + if (withManiac) 1 else 0
            val reds = playerCount - mafiaTotal - if (withManiac) 1 else 0
            return reds < extraKillers * 2
        }

    fun deal(): List<Role> {
        val pool = ArrayList<Role>(playerCount)
        roleCounts.forEach { (role, count) -> repeat(count) { pool += role } }
        pool.shuffle()
        return pool
    }
}

/** Имена по умолчанию задаёт UI на языке интерфейса; здесь — запасной вариант. */
fun defaultNames(n: Int, pattern: String = "Player %d"): List<String> =
    (1..n).map { String.format(pattern, it) }
