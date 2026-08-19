package com.serg.mafia.model

enum class Phase {
    SETUP,        // ведущий собирает стол
    DEAL,         // раздача ролей, телефон идёт по кругу
    INTRO_NIGHT,  // знакомство: фракции открывают глаза по очереди
    INTRO_DAY,    // первое знакомство: речь по 30 секунд
    NIGHT,        // ночные ходы
    MORNING,      // итог ночи
    DAY,          // речи и выставление на голосование
    VOTE,         // голосование
    GAME_OVER,
}

data class Player(
    val id: Int,
    val name: String,
    val role: Role,
    val alive: Boolean = true,
    val fouls: Int = 0,
    /** Второй фол снимает ОДНУ ближайшую речь и после этого сгорает. */
    val speechSkipPending: Boolean = false,
    val deathReason: String? = null,
    val deathDay: Int = 0,
)

/** Что роли выбрали этой ночью. Заполняется по шагам, применяется в конце ночи. */
data class NightActions(
    val butterflyTarget: Int? = null,
    val mafiaTarget: Int? = null,
    val mafiaMissed: Boolean = false,
    val donCheck: Int? = null,
    val doctorTarget: Int? = null,
    val sheriffCheck: Int? = null,
    val maniacTarget: Int? = null,
)

/** Один шаг ночи: кого будим и что он выбирает. */
data class NightStep(
    val role: Role,
    val title: String,
    val prompt: String,
    /** Показывать ли ведущему принадлежность каждого игрока прямо в списке. */
    val revealFactions: Boolean = false,
    /** Показывать ли, кто из списка комиссар (шаг дона). */
    val revealSheriff: Boolean = false,
    val allowSkip: Boolean = true,
)

data class NightOutcome(
    val killed: List<Int> = emptyList(),
    val savedByDoctor: Int? = null,
    val blocked: Int? = null,
    val sheriffResult: Pair<Int, Faction>? = null,
    val donFoundSheriff: Boolean = false,
)

data class GameState(
    val setup: Setup = Setup(),
    val players: List<Player> = emptyList(),
    val phase: Phase = Phase.SETUP,
    val dayNumber: Int = 0,
    val nightNumber: Int = 0,

    val dealIndex: Int = 0,
    val introStepIndex: Int = 0,
    val speakerId: Int? = null,
    val spokenIds: List<Int> = emptyList(),

    val nightStepIndex: Int = 0,
    val night: NightActions = NightActions(),
    val lastNight: NightOutcome = NightOutcome(),
    val firstNightMissDecided: Boolean = false,

    /** кандидат -> кто выставил */
    val nominations: Map<Int, Int> = emptyMap(),
    val votes: Map<Int, Int> = emptyMap(),

    val log: List<String> = emptyList(),
    val winner: Faction? = null,
) {
    val alivePlayers: List<Player> get() = players.filter { it.alive }
    fun player(id: Int): Player = players.first { it.id == id }
    fun playerOrNull(id: Int?): Player? = id?.let { pid -> players.firstOrNull { it.id == pid } }
    fun holders(role: Role): List<Player> = players.filter { it.role == role }
    fun aliveHolders(role: Role): List<Player> = players.filter { it.role == role && it.alive }

    val aliveBlack: List<Player> get() = players.filter { it.alive && it.role.isBlack }

    /** Шаги текущей ночи: порядок бабочка → мафия → дон → врач → комиссар → маньяк. */
    val nightSteps: List<NightStep>
        get() {
            val steps = ArrayList<NightStep>(6)
            if (aliveHolders(Role.BUTTERFLY).isNotEmpty()) {
                steps += NightStep(
                    Role.BUTTERFLY,
                    "Просыпается бабочка",
                    "Кого бабочка блокирует этой ночью?",
                )
            }
            if (aliveBlack.isNotEmpty()) {
                steps += NightStep(
                    Role.MAFIA,
                    "Просыпается мафия",
                    "Кого мафия убивает?",
                )
            }
            if (aliveHolders(Role.DON).isNotEmpty() && holders(Role.SHERIFF).isNotEmpty()) {
                steps += NightStep(
                    Role.DON,
                    "Просыпается дон",
                    "Кого дон проверяет на комиссара?",
                    revealSheriff = true,
                )
            }
            if (aliveHolders(Role.DOCTOR).isNotEmpty()) {
                steps += NightStep(
                    Role.DOCTOR,
                    "Просыпается врач",
                    "Кого врач лечит?",
                )
            }
            if (aliveHolders(Role.SHERIFF).isNotEmpty()) {
                steps += NightStep(
                    Role.SHERIFF,
                    "Просыпается комиссар",
                    "Кого комиссар проверяет?",
                    revealFactions = true,
                )
            }
            if (aliveHolders(Role.MANIAC).isNotEmpty()) {
                steps += NightStep(
                    Role.MANIAC,
                    "Просыпается маньяк",
                    "Кого маньяк убивает?",
                )
            }
            return steps
        }

    /** Шаги знакомства: мафия знакомится первой, дальше остальные по одному. */
    val introSteps: List<NightStep>
        get() {
            val steps = ArrayList<NightStep>(6)
            if (aliveBlack.isNotEmpty()) {
                steps += NightStep(
                    Role.MAFIA,
                    "Мафия знакомится",
                    "Чёрные открывают глаза и видят друг друга. Сверься со списком.",
                )
            }
            if (holders(Role.DON).isNotEmpty()) {
                steps += NightStep(Role.DON, "Дон", "Дон показывает себя мафии.")
            }
            if (holders(Role.BUTTERFLY).isNotEmpty()) {
                steps += NightStep(Role.BUTTERFLY, "Бабочка", "Бабочка открывает глаза.")
            }
            if (holders(Role.DOCTOR).isNotEmpty()) {
                steps += NightStep(Role.DOCTOR, "Врач", "Врач открывает глаза.")
            }
            if (holders(Role.SHERIFF).isNotEmpty()) {
                steps += NightStep(Role.SHERIFF, "Комиссар", "Комиссар открывает глаза.")
            }
            if (holders(Role.MANIAC).isNotEmpty()) {
                steps += NightStep(Role.MANIAC, "Маньяк", "Маньяк открывает глаза.")
            }
            return steps
        }

    /** Кто должен открыть глаза на этом шаге знакомства. */
    fun introActors(step: NightStep): List<Player> = when (step.role) {
        Role.MAFIA -> players.filter { it.role.isBlack }
        else -> holders(step.role)
    }

    val isFirstNight: Boolean get() = nightNumber == 1

    /** Кто ещё не говорил в этом круге (мёртвые и «пропускающие речь» отсеиваются отдельно). */
    fun speechQueue(): List<Player> {
        val start = if (players.isEmpty()) 0 else (dayNumber - 1).coerceAtLeast(0) % players.size
        val ordered = players.drop(start) + players.take(start)
        return ordered.filter { it.alive && it.id !in spokenIds }
    }
}

fun winnerText(f: Faction): String = when (f) {
    Faction.RED -> "Победил город"
    Faction.BLACK -> "Победила мафия"
    Faction.MANIAC -> "Победил маньяк"
}
