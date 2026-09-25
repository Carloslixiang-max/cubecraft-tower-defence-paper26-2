package dev.cubecrafttd.testing

import kotlin.test.Test
import kotlin.test.assertTrue

class DomainFixtureSuiteTest {
    @Test
    fun allDomainFixturesPass() {
        val results = DomainFixtureSuite.runAll()
        val failed = results.filterNot { it.passed }
        assertTrue(
            failed.isEmpty(),
            buildString {
                append("Failed ${failed.size}/${results.size} domain fixtures")
                if (failed.isNotEmpty()) {
                    append(": ")
                    append(
                        failed.joinToString { result ->
                            if (result.details.isEmpty()) result.id
                            else "${result.id} (${result.details.joinToString()})"
                        }
                    )
                }
            }
        )
    }

    @Test
    fun fixtureSuiteDoesNotAccidentallyShrink() {
        val count = DomainFixtureSuite.runAll().size
        assertTrue(
            count >= 315,
            "Expected at least the v32 baseline of 315 fixtures, got $count"
        )
    }
}
