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
    val deathReasonKey: String? = null,
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

/** Запись журнала: ключ перевода и подстановки — язык выбирается при показе. */
data class LogEntry(val key: String, val args: List<Any> = emptyList())

/** Один шаг ночи: кого будим и что он выбирает (тексты — ключи перевода). */
data class NightStep(
    val role: Role,
    val titleKey: String,
    val promptKey: String,
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

    /** кто выставил -> кого выставил; у каждого игрока свой записанный выбор */
    val nominationsBy: Map<Int, Int> = emptyMap(),
    val votes: Map<Int, Int> = emptyMap(),

    val log: List<LogEntry> = emptyList(),
    val winner: Faction? = null,
) {
    val alivePlayers: List<Player> get() = players.filter { it.alive }

    /** Кандидаты дня в порядке появления. */
    val candidates: List<Int> get() = nominationsBy.values.distinct()

    /** Кто выставил этого кандидата. */
    fun nominatedBy(candidateId: Int): List<Int> =
        nominationsBy.filterValues { it == candidateId }.keys.toList()
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
                    "step_butterfly",
                    "step_butterfly_q",
                )
            }
            if (aliveBlack.isNotEmpty()) {
                steps += NightStep(
                    Role.MAFIA,
                    "step_mafia",
                    "step_mafia_q",
                )
            }
            if (aliveHolders(Role.DON).isNotEmpty() && holders(Role.SHERIFF).isNotEmpty()) {
                steps += NightStep(
                    Role.DON,
                    "step_don",
                    "step_don_q",
                    revealSheriff = true,
                )
            }
            if (aliveHolders(Role.DOCTOR).isNotEmpty()) {
                steps += NightStep(
                    Role.DOCTOR,
                    "step_doctor",
                    "step_doctor_q",
                )
            }
            if (aliveHolders(Role.SHERIFF).isNotEmpty()) {
                steps += NightStep(
                    Role.SHERIFF,
                    "step_sheriff",
                    "step_sheriff_q",
                    revealFactions = true,
                )
            }
            if (aliveHolders(Role.MANIAC).isNotEmpty()) {
                steps += NightStep(
                    Role.MANIAC,
                    "step_maniac",
                    "step_maniac_q",
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
                    "intro_mafia",
                    "intro_mafia_q",
                )
            }
            if (holders(Role.DON).isNotEmpty()) {
                steps += NightStep(Role.DON, "intro_don", "intro_don_q")
            }
            if (holders(Role.BUTTERFLY).isNotEmpty()) {
                steps += NightStep(Role.BUTTERFLY, "intro_butterfly", "intro_butterfly_q")
            }
            if (holders(Role.DOCTOR).isNotEmpty()) {
                steps += NightStep(Role.DOCTOR, "intro_doctor", "intro_doctor_q")
            }
            if (holders(Role.SHERIFF).isNotEmpty()) {
                steps += NightStep(Role.SHERIFF, "intro_sheriff", "intro_sheriff_q")
            }
            if (holders(Role.MANIAC).isNotEmpty()) {
                steps += NightStep(Role.MANIAC, "intro_maniac", "intro_maniac_q")
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

fun winnerKey(f: Faction): String = when (f) {
    Faction.RED -> "win_red"
    Faction.BLACK -> "win_black"
    Faction.MANIAC -> "win_maniac"
}
