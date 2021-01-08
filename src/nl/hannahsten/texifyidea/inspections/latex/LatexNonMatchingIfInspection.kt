package nl.hannahsten.texifyidea.inspections.latex

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import nl.hannahsten.texifyidea.insight.InsightGroup
import nl.hannahsten.texifyidea.inspections.TexifyInspectionBase
import nl.hannahsten.texifyidea.psi.LatexCommands
import nl.hannahsten.texifyidea.util.Magic
import nl.hannahsten.texifyidea.util.files.commandsInFile
import nl.hannahsten.texifyidea.util.magic.CommandMagic
import nl.hannahsten.texifyidea.util.magic.GeneralMagic
import nl.hannahsten.texifyidea.util.magic.PatternMagic
import nl.hannahsten.texifyidea.util.matches
import nl.hannahsten.texifyidea.util.previousCommand
import java.util.*

/**
 * @author Hannah Schellekens
 */
open class LatexNonMatchingIfInspection : TexifyInspectionBase() {

    override val inspectionGroup = InsightGroup.LATEX

    override fun getDisplayName() = "Open if-then-else control sequence"

    override val inspectionId = "NonMatchingIf"

    override fun inspectFile(file: PsiFile, manager: InspectionManager, isOntheFly: Boolean): List<ProblemDescriptor> {
        val descriptors = descriptorList()

        // Find matches.
        val stack = ArrayDeque<LatexCommands>()
        val commands = file.commandsInFile().sortedBy { it.textOffset }
        for (command in commands) {
            val name = command.name
            if (command.name in CommandMagic.endIfs) {
                // Non-opened fi.
                if (stack.isEmpty()) {
                    descriptors.add(
                        manager.createProblemDescriptor(
                            command,
                            "No matching \\if-command found",
                            GeneralMagic.noQuickFix,
                            ProblemHighlightType.GENERIC_ERROR,
                            isOntheFly
                        )
                    )
                    continue
                }

                stack.pop()
            }
            else if (PatternMagic.ifCommand.matches(name) && name !in CommandMagic.ignoredIfs && command.previousCommand()?.name != "\\newif") {
                stack.push(command)
            }
        }

        // Mark unclosed ifs.
        for (cmd in stack) {
            descriptors.add(
                manager.createProblemDescriptor(
                    cmd,
                    "If statement should probably be closed with \\fi",
                    GeneralMagic.noQuickFix,
                    ProblemHighlightType.WARNING,
                    isOntheFly
                )
            )
        }

        return descriptors
    }
}