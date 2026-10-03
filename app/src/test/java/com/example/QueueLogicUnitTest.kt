package com.example

import com.example.ocr.RequisitionOcrService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueLogicUnitTest {

    @Test
    fun testDurationCalculationPerPart() {
        // Requirement: 默认一个部位15分钟
        val partCount1 = 1
        val duration1 = partCount1 * 15
        assertEquals(15, duration1)

        val partCount2 = 2
        val duration2 = partCount2 * 15
        assertEquals(30, duration2)

        val partCount3 = 3
        val duration3 = partCount3 * 15
        assertEquals(45, duration3)
    }

    @Test
    fun testSavedTimeAccumulation() {
        val plannedMinutes = 30
        val actualMinutes = 18
        val saved = (plannedMinutes - actualMinutes).coerceAtLeast(0)
        assertEquals(12, saved)

        var accumulatedSavedMinutes = 0
        accumulatedSavedMinutes += saved
        assertEquals(12, accumulatedSavedMinutes)

        // Requirement: 累加超过10分钟提醒可插号
        val shouldAlert = accumulatedSavedMinutes >= 10
        assertTrue("Accumulated saved time >= 10 should trigger insert alert", shouldAlert)
    }

    @Test
    fun testOcrTextParsing() {
        val ocrService = RequisitionOcrService()
        val slipText = "医院检查申请单\n姓名：张建国  住院号：ZY20261088\n检查项目及部位：胸部CT平扫\n预约检查时间：10:15"
        val result = ocrService.parseFromText(slipText)

        assertEquals("张建国", result.patientName)
        assertEquals("ZY20261088", result.inpatientNo)
        assertEquals("10:15", result.appointmentTime)
        assertEquals(15, result.durationMinutes)
    }
}
