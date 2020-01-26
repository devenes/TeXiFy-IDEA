package nl.hannahsten.texifyidea.run.latex.ui

import com.intellij.diagnostic.logging.AdditionalTabComponent
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.util.Key
import com.intellij.ui.components.JBList
import nl.hannahsten.texifyidea.util.substringEnd
import org.apache.commons.collections.buffer.CircularFifoBuffer
import java.awt.BorderLayout
import java.awt.ScrollPane
import javax.swing.DefaultListModel
import javax.swing.JComponent

/**
 * Runner tab component displaying LaTeX log messages in a more readable and navigatable format.
 *
 * @param startedProcess The LaTeX compile process.
 *
 * @author Sten Wessel
 */
class LatexLogTabComponent(startedProcess: ProcessHandler) : AdditionalTabComponent(BorderLayout()) {

    private val listModel = DefaultListModel<String>()

    init {

        add(ScrollPane().apply { add(JBList(listModel)) }, BorderLayout.CENTER)

        startedProcess.addProcessListener(LatexOutputListener(), this)
    }

    override fun getTabTitle() = "Log messages"

    override fun dispose() {

    }

    override fun getPreferredFocusableComponent() = component

    override fun getToolbarActions(): ActionGroup? = null

    override fun getToolbarContextComponent(): JComponent? = null

    override fun getToolbarPlace(): String? = null

    override fun getSearchComponent(): JComponent? = null

    override fun isContentBuiltIn() = false

    private inner class LatexOutputListener(val lineWidth: Int = 79) : ProcessListener {

        /**
         * Window of the last two log output messages.
         */
        val window = CircularFifoBuffer(2)

        var isCollectingMessage = false
        var currentMessageText: String? = null

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            if (outputType !is ProcessOutputType) return

            // We look for specific strings in the log output for which we know it was probably an error message
            // (it might be that it is actually not, since anything can be typed to the LaTeX output log, but we ignore
            // that for now and assume the log is of reasonable format).
            // Standard log output is only 79 characters wide, meaning these strings might occur over two lines.
            // We assume the match tokens will always fit over two lines.
            // Therefore, we maintain a window of two log output lines to detect the error messages.
            //
            // We assume that if the first line of the error/warning is exactly the maximum line width, it will continue
            // on the next line. This may not be accurate, but there is no way of distinguishing this.

            val newText = event.text.trimEnd('\n', '\r')
            window.add(newText)
            val text = window.joinToString(separator = "")

            // Check if we are currently in the process of collecting the full message of a matched message of interest
            if (isCollectingMessage) {
                collectMessageLine(newText)
            }
            else {
                // If now the first entry in window is an empty line: ignore this and skip to the next line
                if ((window.firstOrNull() as? String).isNullOrEmpty()) return

                // Check if we have found an error
                ERROR_REGEX.find(text)?.apply {
                    val message = groups["message"]?.value?.substringEnd(newText.length) ?: ""

                    if (text.substringEnd(newText.length).length >= lineWidth) {
                        // Keep on collecting output for this message
                        currentMessageText = message
                        isCollectingMessage = true
                        collectMessageLine(newText)
                    }
                    else {
                        addMessageToLog(message)
                    }
                }

                // Check if we have found a warning
                if (TEX_WARNINGS.any { text.startsWith(it) }) {
                    val message = text.substringEnd(newText.length)

                    if (message.length >= lineWidth) {
                        // Keep on collecting output for this message
                        currentMessageText = message
                        isCollectingMessage = true
                        collectMessageLine(newText)
                    }
                    else {
                        addMessageToLog(message)
                    }
                }
            }

        }

        private fun collectMessageLine(newText: String) {
            currentMessageText += newText

            if (newText.length < lineWidth) {
                isCollectingMessage = false
                addMessageToLog(currentMessageText!!)
            }
        }

        private fun addMessageToLog(message: String) {
            listModel.addElement(message)
        }

        override fun processTerminated(event: ProcessEvent) {

        }

        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {

        }

        override fun startNotified(event: ProcessEvent) {

        }
    }

    companion object {
        private val ERROR_REGEX = """^(?<file>.+)?:(?<line>\d+): (?<message>.+)$""".toRegex()
        private val TEX_WARNINGS = listOf(
                "LaTeX Warning: ",
                "LaTeX Font Warning: ",
                "AVAIL list clobbered at",
                "Double-AVAIL list clobbered at",
                "Doubly free location at",
                "Bad flag at",
                "Runaway definition",
                "Runaway argument",
                "Runaway text",
                "Missing character: There is no",
                "No pages of output.",
                "Underfull \\hbox",
                "Overfull \\hbox",
                "Loose \\hbox",
                "Tight \\hbox",
                "Underfull \\vbox",
                "Overfull \\vbox",
                "Loose \\vbox",
                "Tight \\vbox"
        )
    }

    private enum class LatexLogMessageType { ERROR, PACKAGE_ERROR, WARNING, FONT_WARNING }
}