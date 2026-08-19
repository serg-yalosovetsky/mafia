package com.serg.mafia.model

/** Кого приложение считает победившим и как комиссар видит игрока. */
enum class Faction { RED, BLACK, MANIAC }

enum class Role(
    val title: String,
    val faction: Faction,
    /** Как выглядит при проверке комиссара. Маньяк для комиссара — чёрный. */
    val checksAs: Faction = faction,
) {
    CIVILIAN("Мирный житель", Faction.RED),
    DOCTOR("Врач", Faction.RED),
    SHERIFF("Комиссар", Faction.RED),
    BUTTERFLY("Бабочка", Faction.RED),
    MAFIA("Мафия", Faction.BLACK),
    DON("Дон", Faction.BLACK),
    MANIAC("Маньяк", Faction.MANIAC, checksAs = Faction.BLACK);

    val isBlack: Boolean get() = faction == Faction.BLACK

    /** Короткая подсказка на карточке игрока при раздаче. */
    val hint: String
        get() = when (this) {
            CIVILIAN -> "Ты за город. Слушай, думай, вычисляй чёрных."
            DOCTOR -> "Ночью лечишь одного игрока. Можешь спасти и себя."
            SHERIFF -> "Ночью проверяешь одного игрока — чёрный он или красный."
            BUTTERFLY -> "Ночью блокируешь одного игрока: его ночное действие не сработает."
            MAFIA -> "Ночью вместе с другими чёрными выбираешь жертву."
            DON -> "Главный в мафии. Ночью ищешь комиссара."
            MANIAC -> "Ты сам за себя. Ночью убиваешь одного. Побеждаешь, оставшись один."
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

    /** Подсказка ведущему на экране настройки. */
    val difficultyLabel: String
        get() = when {
            redsPerKiller >= 2.6f -> "Городу легко"
            redsPerKiller >= 2.1f -> "Городу проще обычного"
            redsPerKiller >= 1.8f -> "Классический баланс"
            redsPerKiller >= 1.5f -> "Городу тяжело"
            else -> "Городу очень тяжело"
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

    val validationMessage: String?
        get() = when {
            playerCount < 4 -> "Минимум 4 игрока"
            playerCount > 20 -> "Максимум 20 игроков"
            assignedTotal > playerCount ->
                "Ролей больше, чем игроков (${assignedTotal} на ${playerCount}). Выключи лишние."
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

fun defaultNames(n: Int): List<String> = (1..n).map { "Игрок $it" }
