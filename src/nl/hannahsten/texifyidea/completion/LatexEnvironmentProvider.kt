package nl.hannahsten.texifyidea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.FileBasedIndex
import nl.hannahsten.texifyidea.TexifyIcons
import nl.hannahsten.texifyidea.index.LatexDefinitionIndex
import nl.hannahsten.texifyidea.index.file.LatexExternalEnvironmentIndex
import nl.hannahsten.texifyidea.lang.DefaultEnvironment
import nl.hannahsten.texifyidea.lang.Dependend
import nl.hannahsten.texifyidea.lang.Environment
import nl.hannahsten.texifyidea.lang.SimpleEnvironment
import nl.hannahsten.texifyidea.util.Kindness
import nl.hannahsten.texifyidea.util.magic.CommandMagic
import nl.hannahsten.texifyidea.util.requiredParameter
import java.util.*

/**
 * Provide environments for the autocompletion.
 */
object LatexEnvironmentProvider {

    fun addIndexedEnvironments(result: CompletionResultSet, parameters: CompletionParameters) {
        result.addAllElements(
            FileBasedIndex.getInstance().getAllKeys(LatexExternalEnvironmentIndex.id, parameters.editor.project ?: return)
                .flatMap { envText ->
                    Environment.lookupInIndex(envText, parameters.editor.project ?: return).map { env ->
                        createEnvironmentLookupElement(env)
                    }
                }
        )
    }

    private fun createEnvironmentLookupElement(env: Environment): LookupElementBuilder {
        return LookupElementBuilder.create(env, env.environmentName)
            .withPresentableText(env.environmentName)
            .bold()
            .withTailText(env.getArgumentsDisplay() + " " + packageName(env), true)
            .withIcon(TexifyIcons.DOT_ENVIRONMENT)
    }

    fun addEnvironments(result: CompletionResultSet, parameters: CompletionParameters) {
        // Find all environments.
        val environments: MutableList<Environment> = ArrayList()
        Collections.addAll(environments, *DefaultEnvironment.values())
        LatexDefinitionIndex.getItemsInFileSet(parameters.originalFile).stream()
            .filter { cmd -> CommandMagic.environmentDefinitions.contains(cmd.name) }
            .map { cmd -> cmd.requiredParameter(0) }
            .filter { obj -> Objects.nonNull(obj) }
            .map { environmentName -> SimpleEnvironment(environmentName!!) }
            .forEach { e: SimpleEnvironment -> environments.add(e) }

        // Create autocomplete elements.
        result.addAllElements(
            ContainerUtil.map2List(environments) { env: Environment ->
                createEnvironmentLookupElement(env)
            }
        )
        result.addLookupAdvertisement(Kindness.getKindWords())
    }

    fun packageName(dependend: Dependend): String {
        val name = dependend.dependency.name
        return if ("" == name) {
            ""
        }
        else " ($name)"
    }
}