package com.example.breathingapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ResourceConflictMarkersTest {

    @Test
    fun resourceXmlFilesShouldNotContainMergeMarkersOrCodexBranchText() {
        val projectRoot = File(System.getProperty("user.dir"))
        val resDir = File(projectRoot, "app/src/main/res")
        assertTrue("Resource directory not found: ${resDir.path}", resDir.exists())

        val suspiciousTokens = listOf("<<<<<<<", "=======", ">>>>>>>", "codex/")

        val offenders = resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .mapNotNull { file ->
                val content = file.readText()
                val matched = suspiciousTokens.filter { token -> content.contains(token) }
                if (matched.isNotEmpty()) "${file.path} -> ${matched.joinToString()}" else null
            }
            .toList()

        assertTrue(
            "Found suspicious conflict tokens in resource XML files:\n${offenders.joinToString("\n")}",
            offenders.isEmpty()
        )
    }
}
