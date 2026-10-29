package com.desloth.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents daily deliverables and emergency micro-tasks that credit stimulation units upon completion.
 *
 * @param id Unique identifier
 * @param title Human-readable task description
 * @param unitValue Units awarded upon completion (e.g. +15 for 5-minute micro-tasks, +30/+50 for deep work)
 * @param isMicroTask True if this task can be deployed by the Circuit Breaker overlay (5-minute friction recovery)
 * @param completedToday True if completed during the current epoch day
 * @param lastCompletedEpochDay The epoch day index (LocalDate.toEpochDay()) when this task was last marked complete
 */
@Entity(tableName = "deliverables")
data class DeliverableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val unitValue: Int,
    val isMicroTask: Boolean,
    val completedToday: Boolean = false,
    val lastCompletedEpochDay: Long = 0L
)
