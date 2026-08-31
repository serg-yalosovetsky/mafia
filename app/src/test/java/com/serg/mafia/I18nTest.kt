package com.serg.mafia

import com.serg.mafia.model.Role
import com.serg.mafia.ui.Lang
import com.serg.mafia.ui.STRINGS
import com.serg.mafia.ui.tr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Перевод без дыр: недостающий ключ виден только на живом столе, и это поздно. */
class I18nTest {

    @Test
    fun `во всех языках одинаковый набор ключей`() {
        val uk = STRINGS[Lang.UK]!!.keys
        Lang.entries.forEach { lang ->
            val keys = STRINGS[lang]!!.keys
            assertEquals("нет ключей в ${lang.code}: " + (uk - keys), emptySet<String>(), uk - keys)
            assertEquals("лишние ключи в ${lang.code}: " + (keys - uk), emptySet<String>(), keys - uk)
        }
    }

    @Test
    fun `у каждой роли есть название и подсказка на каждом языке`() {
        Role.entries.forEach { role ->
            Lang.entries.forEach { lang ->
                assertTrue(role.titleKey in STRINGS[lang]!!)
                assertTrue(role.hintKey in STRINGS[lang]!!)
            }
        }
    }

    @Test
    fun `украинский — язык по умолчанию`() {
        assertEquals(Lang.UK, Lang.of(null))
        assertEquals(Lang.UK, Lang.of("xx"))
        assertEquals(Lang.RU, Lang.of("ru"))
        assertEquals(Lang.EN, Lang.of("en"))
    }

    @Test
    fun `подстановки работают`() {
        assertEquals("Гравець 3", tr(Lang.UK, "player_n", 3))
        assertEquals("Игрок 3", tr(Lang.RU, "player_n", 3))
        assertEquals("Player 3", tr(Lang.EN, "player_n", 3))
    }

    @Test
    fun `неизвестный ключ возвращает сам ключ, а не падает`() {
        assertEquals("no_such_key", tr(Lang.UK, "no_such_key"))
    }
}
