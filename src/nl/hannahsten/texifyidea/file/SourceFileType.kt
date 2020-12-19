package nl.hannahsten.texifyidea.file

import com.intellij.openapi.fileTypes.LanguageFileType
import nl.hannahsten.texifyidea.LatexLanguage
import nl.hannahsten.texifyidea.TexifyIcons

/**
 * @author Thomas
 */
object SourceFileType : LanguageFileType(LatexLanguage.INSTANCE) {

    override fun getName() = "LaTeX source file"

    override fun getDescription() = "LaTeX source file"

    override fun getDefaultExtension() = "dtx"

    override fun getIcon() = TexifyIcons.FILE
}
