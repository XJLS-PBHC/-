package com.example.ocr

data class RequisitionOcrResult(
    val patientName: String,
    val inpatientNo: String = "",
    val bodyParts: String,
    val partCount: Int = 1,
    val appointmentTime: String = "",
    val durationMinutes: Int = 15,
    val rawText: String = "",
    val isAiRecognized: Boolean = false
)

data class SampleRequisition(
    val title: String,
    val description: String,
    val patientName: String,
    val inpatientNo: String,
    val bodyParts: String,
    val partCount: Int,
    val appointmentTime: String,
    val simulatedSlipText: String
)
