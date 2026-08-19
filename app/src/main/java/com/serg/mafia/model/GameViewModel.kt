package com.serg.mafia.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Вся партия живёт здесь: ViewModel переживает поворот и сворачивание,
 * а телефон в мафии крутится по рукам постоянно.
 */
class GameViewModel : ViewModel() {

    var s by mutableStateOf(GameState())
        private set

    // ------------------------------------------------------------- настройка
    fun updateSetup(block: (Setup) -> Setup) {
        val old = s.setup
        val new = block(old)
        val names = if (new.playerCount != old.names.size) {
            List(new.playerCount) { i -> new.names.getOrNull(i) ?: "Игрок ${i + 1}" }
        } else {
            new.names
        }
        s = s.copy(setup = new.copy(names = names))
    }

    fun renamePlayer(index: Int, name: String) {
        val names = s.setup.names.toMutableList()
        if (index in names.indices) names[index] = name
        s = s.copy(setup = s.setup.copy(names = names))
    }

    // ------------------------------------------------------------- раздача
    fun startDeal() {
        val roles = s.setup.deal()
        val players = s.setup.names.mapIndexed { i, name ->
            Player(id = i, name = name.ifBlank { "Игрок ${i + 1}" }, role = roles[i])
        }
        s = GameState(
            setup = s.setup,
            players = players,
            phase = Phase.DEAL,
            dealIndex = 0,
            log = listOf("Партия началась: ${players.size} игроков"),
        )
    }

    fun dealNext() {
        val next = s.dealIndex + 1
        s = if (next >= s.players.size) {
            s.copy(phase = Phase.INTRO_NIGHT, introStepIndex = 0, dealIndex = next)
        } else {
            s.copy(dealIndex = next)
        }
    }

    // ------------------------------------------------------------- знакомство
    fun introNext() {
        val next = s.introStepIndex + 1
        s = if (next >= s.introSteps.size) {
            startSpeechRound(Phase.INTRO_DAY, s.copy(dayNumber = 0))
        } else {
            s.copy(introStepIndex = next)
        }
    }

    // ------------------------------------------------------------- речи
    private fun startSpeechRound(phase: Phase, base: GameState = s): GameState {
        val fresh = base.copy(phase = phase, spokenIds = emptyList(), speakerId = null)
        return nextSpeakerState(fresh)
    }

    /** Ищет следующего говорящего, сжигая одноразовый пропуск речи за 2 фола. */
    private fun nextSpeakerState(state: GameState): GameState {
        var st = state
        while (true) {
            val queue = st.speechQueue()
            val candidate = queue.firstOrNull() ?: return st.copy(speakerId = null)
            if (candidate.speechSkipPending) {
                st = st.copy(
                    players = st.players.map {
                        if (it.id == candidate.id) it.copy(speechSkipPending = false) else it
                    },
                    spokenIds = st.spokenIds + candidate.id,
                    log = st.log + "${candidate.name} пропускает речь (2 фола)",
                )
                continue
            }
            return st.copy(speakerId = candidate.id)
        }
    }

    /** Речь закончена — передаём слово дальше, а если круг закрыт, идём в следующую фазу. */
    fun finishSpeech() {
        val speaker = s.speakerId ?: return
        var st = s.copy(spokenIds = s.spokenIds + speaker)
        st = nextSpeakerState(st)
        s = if (st.speakerId == null) {
            when (s.phase) {
                Phase.INTRO_DAY -> beginNight(st)
                // Круг закрыт: если никого не выставили — голосовать не за кого, сразу ночь.
                Phase.DAY -> if (st.nominations.isEmpty()) {
                    beginNight(st.copy(log = st.log + "Никого не выставили — день без голосования"))
                } else {
                    st.copy(phase = Phase.VOTE, votes = st.nominations.keys.associateWith { 0 })
                }
                else -> st
            }
        } else {
            st
        }
    }

    fun skipSpeaker() = finishSpeech()

    // ------------------------------------------------------------- ночь
    private fun beginNight(base: GameState = s): GameState {
        val number = base.nightNumber + 1
        // Промах первой ночи, назначенный в настройках, не надо переспрашивать у ведущего.
        val plannedMiss = number == 1 && base.setup.firstNightMiss
        val lines = buildList {
            add("— Ночь $number —")
            if (plannedMiss) add("Первая ночь: мафия стреляет в воздух (настройка стола)")
        }
        return base.copy(
            phase = Phase.NIGHT,
            nightNumber = number,
            nightStepIndex = 0,
            night = NightActions(mafiaMissed = plannedMiss),
            lastNight = NightOutcome(),
            firstNightMissDecided = plannedMiss,
            log = base.log + lines,
        )
    }

    fun goToNight() {
        s = beginNight()
    }

    fun setFirstNightMiss(miss: Boolean) {
        s = s.copy(
            night = s.night.copy(mafiaMissed = miss),
            firstNightMissDecided = true,
            log = if (miss) s.log + "Первая ночь: мафия стреляет в воздух" else s.log,
        )
    }

    fun chooseNightTarget(role: Role, targetId: Int?) {
        val n = s.night
        val updated = when (role) {
            Role.BUTTERFLY -> n.copy(butterflyTarget = targetId)
            Role.MAFIA -> n.copy(mafiaTarget = targetId)
            Role.DON -> n.copy(donCheck = targetId)
            Role.DOCTOR -> n.copy(doctorTarget = targetId)
            Role.SHERIFF -> n.copy(sheriffCheck = targetId)
            Role.MANIAC -> n.copy(maniacTarget = targetId)
            else -> n
        }
        s = s.copy(night = updated)
    }

    /** Заблокирован ли этот игрок бабочкой (бабочка ходит первой, поэтому известно сразу). */
    fun isBlocked(playerId: Int?): Boolean =
        playerId != null && s.night.butterflyTarget == playerId && s.aliveHolders(Role.BUTTERFLY).isNotEmpty()

    fun nightNext() {
        val next = s.nightStepIndex + 1
        s = if (next >= s.nightSteps.size) resolveNight() else s.copy(nightStepIndex = next)
    }

    fun nightBack() {
        if (s.nightStepIndex > 0) s = s.copy(nightStepIndex = s.nightStepIndex - 1)
    }

    private fun resolveNight(): GameState {
        val n = s.night
        val blocked = n.butterflyTarget?.takeIf { s.aliveHolders(Role.BUTTERFLY).isNotEmpty() }

        val doctorAlive = s.aliveHolders(Role.DOCTOR).firstOrNull()
        val doctorWorks = doctorAlive != null && doctorAlive.id != blocked
        val healed = if (doctorWorks) n.doctorTarget else null

        // Мафия стреляет коллективно: блокировка гасит выстрел, только если
        // заблокирован единственный оставшийся чёрный.
        val blackAlive = s.aliveBlack
        val mafiaBlocked = blocked != null && blackAlive.size == 1 && blackAlive.first().id == blocked
        val mafiaShoots = !n.mafiaMissed && !mafiaBlocked && n.mafiaTarget != null

        val maniacAlive = s.aliveHolders(Role.MANIAC).firstOrNull()
        val maniacWorks = maniacAlive != null && maniacAlive.id != blocked && n.maniacTarget != null

        val killed = LinkedHashSet<Int>()
        if (mafiaShoots && n.mafiaTarget != healed) killed += n.mafiaTarget!!
        if (maniacWorks && n.maniacTarget != healed) killed += n.maniacTarget!!

        val sheriffAlive = s.aliveHolders(Role.SHERIFF).firstOrNull()
        val sheriffWorks = sheriffAlive != null && sheriffAlive.id != blocked && n.sheriffCheck != null
        val sheriffResult = if (sheriffWorks) {
            val target = s.player(n.sheriffCheck!!)
            target.id to target.role.checksAs
        } else null

        val donFound = n.donCheck != null && s.playerOrNull(n.donCheck)?.role == Role.SHERIFF &&
            s.aliveHolders(Role.DON).firstOrNull()?.id != blocked

        val savedByDoctor = if (healed != null &&
            ((mafiaShoots && n.mafiaTarget == healed) || (maniacWorks && n.maniacTarget == healed))
        ) healed else null

        val players = s.players.map {
            if (it.id in killed) it.copy(alive = false, deathReason = "убит ночью", deathDay = s.nightNumber) else it
        }

        val lines = ArrayList<String>()
        if (blocked != null) lines += "Бабочка заблокировала ${s.player(blocked).name}"
        if (killed.isEmpty()) lines += "Ночь ${s.nightNumber}: жертв нет"
        else lines += "Ночь ${s.nightNumber}: погиб(ла) " + killed.joinToString { s.player(it).name }

        val newState = s.copy(
            players = players,
            phase = Phase.MORNING,
            lastNight = NightOutcome(
                killed = killed.toList(),
                savedByDoctor = savedByDoctor,
                blocked = blocked,
                sheriffResult = sheriffResult,
                donFoundSheriff = donFound,
            ),
            log = s.log + lines,
        )
        return withWinCheck(newState)
    }

    // ------------------------------------------------------------- день
    fun startDay() {
        val base = s.copy(
            dayNumber = s.dayNumber + 1,
            nominations = emptyMap(),
            votes = emptyMap(),
            log = s.log + "— День ${s.dayNumber + 1} —",
        )
        s = startSpeechRound(Phase.DAY, base)
    }

    fun nominate(byId: Int, targetId: Int) {
        if (s.nominations.containsKey(targetId)) return
        s = s.copy(
            nominations = s.nominations + (targetId to byId),
            log = s.log + "${s.player(byId).name} выставляет ${s.player(targetId).name}",
        )
    }

    fun cancelNomination(targetId: Int) {
        s = s.copy(nominations = s.nominations - targetId)
    }

    // ------------------------------------------------------------- фолы
    fun addFoul(playerId: Int) {
        val p = s.player(playerId)
        val fouls = p.fouls + 1
        var updated = p.copy(fouls = fouls)
        val lines = ArrayList<String>()
        when {
            fouls == 1 -> lines += "${p.name}: фол 1 — предупреждение"
            fouls == 2 -> {
                updated = updated.copy(speechSkipPending = true)
                lines += "${p.name}: фол 2 — пропускает ближайшую речь"
            }
            fouls >= 3 -> {
                updated = updated.copy(alive = false, deathReason = "3 фола", deathDay = s.dayNumber)
                lines += "${p.name}: фол 3 — выбывает из игры"
            }
        }
        var st = s.copy(
            players = s.players.map { if (it.id == playerId) updated else it },
            log = s.log + lines,
        )
        if (fouls >= 3) {
            st = st.copy(nominations = st.nominations - playerId)
            if (st.speakerId == playerId) st = nextSpeakerState(st.copy(spokenIds = st.spokenIds + playerId))
        }
        s = withWinCheck(st)
    }

    fun removeFoul(playerId: Int) {
        val p = s.player(playerId)
        if (p.fouls == 0) return
        val fouls = p.fouls - 1
        val updated = p.copy(
            fouls = fouls,
            speechSkipPending = if (fouls < 2) false else p.speechSkipPending,
            alive = if (p.deathReason == "3 фола") true else p.alive,
            deathReason = if (p.deathReason == "3 фола") null else p.deathReason,
        )
        s = s.copy(
            players = s.players.map { if (it.id == playerId) updated else it },
            log = s.log + "${p.name}: фол снят (осталось $fouls)",
        )
    }

    // ------------------------------------------------------------- голосование
    fun setVote(candidateId: Int, count: Int) {
        s = s.copy(votes = s.votes + (candidateId to count.coerceAtLeast(0)))
    }

    val voteLeaders: List<Int>
        get() {
            val max = s.votes.values.maxOrNull() ?: return emptyList()
            if (max <= 0) return emptyList()
            return s.votes.filterValues { it == max }.keys.toList()
        }

    fun eliminate(playerId: Int) {
        val p = s.player(playerId)
        val st = s.copy(
            players = s.players.map {
                if (it.id == playerId) it.copy(alive = false, deathReason = "голосование", deathDay = s.dayNumber) else it
            },
            log = s.log + "Голосованием выбывает ${p.name} (${p.role.title})",
        )
        val checked = withWinCheck(st)
        s = if (checked.winner != null) checked else beginNight(checked)
    }

    fun skipVote() {
        val checked = withWinCheck(s.copy(log = s.log + "Голосование: никто не выбыл"))
        s = if (checked.winner != null) checked else beginNight(checked)
    }

    // ------------------------------------------------------------- победа
    private fun withWinCheck(state: GameState): GameState {
        val alive = state.players.filter { it.alive }
        val black = alive.count { it.role.isBlack }
        val maniac = alive.count { it.role == Role.MANIAC }
        val red = alive.size - black - maniac
        val winner = when {
            black == 0 && maniac == 0 -> Faction.RED
            black == 0 && maniac > 0 && alive.size <= 2 -> Faction.MANIAC
            black > 0 && black >= red + maniac -> Faction.BLACK
            else -> null
        }
        return if (winner != null) {
            state.copy(phase = Phase.GAME_OVER, winner = winner, log = state.log + winnerText(winner))
        } else {
            state
        }
    }

    /** Прервать партию и вернуться к настройкам, сохранив стол (случайный «старт» — не приговор). */
    fun backToSetup() {
        s = GameState(setup = s.setup)
    }

    fun newGameSameTable() {
        s = GameState(setup = s.setup)
    }

    fun reset() {
        s = GameState()
    }
}
