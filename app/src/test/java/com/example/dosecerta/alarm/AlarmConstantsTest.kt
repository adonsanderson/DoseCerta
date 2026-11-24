package com.example.dosecerta.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmConstantsTest {

    @Test
    fun `requestCodeForDose is deterministic, positive, and distinct across doses`() {
        val code1 = AlarmConstants.requestCodeForDose(1L)
        val code1Again = AlarmConstants.requestCodeForDose(1L)
        val code2 = AlarmConstants.requestCodeForDose(2L)
        val codeLarge = AlarmConstants.requestCodeForDose(987_654_321_012L)

        assertEquals(code1, code1Again)
        assertNotEquals(code1, code2)
        assertNotEquals(code2, codeLarge)
        assertTrue(code1 > 0)
        assertTrue(code2 > 0)
        assertTrue(codeLarge > 0)
    }

    @Test
    fun `actionRequestCodeForDose is distinct from alarm requestCodeForDose`() {
        val alarmCode = AlarmConstants.requestCodeForDose(42L)
        val actionCode = AlarmConstants.actionRequestCodeForDose(42L)

        assertNotEquals(alarmCode, actionCode)
        assertTrue(actionCode > 0)
    }
}
