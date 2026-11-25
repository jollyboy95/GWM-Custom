package com.gwmcustom.app

import org.junit.Assert.*
import org.junit.Test

class DemoProfileTest {
    @Test
    fun noOpDoesNotCreateHistory() {
        val profile = DemoProfile()
        assertSame(profile, profile.edit(Setting.STARTUP_ANIMATION, true, 1))
    }

    @Test
    fun snapshotRestoresBothModulesAndPreservesHistory() {
        val initial = DemoProfile().edit(Setting.TOUCH_SOUNDS, true, 1)
        val saved = initial.save(2)
        val backup = saved.backups.single()
        val edited = saved.edit(Setting.TOUCH_SOUNDS, false, 3)
            .edit(Setting.WELCOME_LIGHTS, false, 4)
        val restored = edited.restore(backup, 5)

        assertTrue(restored.value(Setting.TOUCH_SOUNDS))
        assertTrue(restored.value(Setting.WELCOME_LIGHTS))
        assertFalse(edited.value(Setting.WELCOME_LIGHTS))
        assertEquals(Setting.entries.size, backup.values.size)
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), restored.history.map { it.time })
        assertEquals(saved.backups, restored.backups)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotRestoreASnapshotFromAnotherProfile() {
        val backup = DemoProfile().save(1).backups.single()
        DemoProfile().restore(backup, 2)
    }
}
