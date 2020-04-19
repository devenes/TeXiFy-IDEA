package nl.hannahsten.texifyidea.inspections.latex

import nl.hannahsten.texifyidea.file.LatexFileType
import nl.hannahsten.texifyidea.inspections.TexifyInspectionTestBase

internal class LatexEscapeUnderscoreInspectionTest : TexifyInspectionTestBase(LatexEscapeUnderscoreInspection()) {

    fun `test unescaped special character warning`() {
        myFixture.configureByText(LatexFileType, """
            \begin{document}
                some text <warning descr="Escape character \ expected">_</warning> with unescaped special character
            \end{document}
        """.trimIndent())
        myFixture.checkHighlighting(true, false, false, false)
    }

    fun `test unescaped special character quick fix for _`() {
        testQuickFix("""
            \begin{document}
                some _ text
            \end{document}
        """.trimIndent(), """
            \begin{document}
                some \_ text
            \end{document}
        """.trimIndent())
    }
}
