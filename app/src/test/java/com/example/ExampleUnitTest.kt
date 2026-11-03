package com.example

import com.promy.irontrack.data.model.PersonalRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `calculate estimated 1RM correctly`() {
    val singleRep = PersonalRecord.calculate1RM(100.0, 1)
    assertEquals(100.0, singleRep, 0.01)

    val fiveReps = PersonalRecord.calculate1RM(100.0, 5)
    assertEquals(116.67, fiveReps, 0.05)
    assertTrue(fiveReps > 100.0)
  }

  @Test
  fun `PRs link to exercise by exerciseId regardless of name change`() {
    val exerciseId = 18L
    val oldPr = PersonalRecord(
        exerciseId = exerciseId,
        exerciseName = "Barbell Bicep Curl",
        maxWeight = 40.0,
        maxRepsAtMaxWeight = 10,
        bestEstimated1RM = 53.33,
        maxVolumeSet = 400.0,
        totalSetsLogged = 5,
        lastPerformed = 1000L
    )

    // Updating the exercise name to "Bicep curl" preserves the same exerciseId and records
    val updatedPr = oldPr.copy(exerciseName = "Bicep curl")
    assertEquals(exerciseId, updatedPr.exerciseId)
    assertEquals(40.0, updatedPr.maxWeight, 0.01)
    assertEquals("Bicep curl", updatedPr.exerciseName)
  }
}
