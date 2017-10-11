package nl.rubensten.texifyidea.index

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import nl.rubensten.texifyidea.util.documentClassFile
import nl.rubensten.texifyidea.util.findRootFile
import nl.rubensten.texifyidea.util.referencedFiles

/**
 * @author Ruben Schellekens
 */
abstract class IndexUtilBase<T : PsiElement>(

        /**
         * The class of the elements that are stored in the index.
         */
        val elementClass: Class<T>,

        /**
         * The key of the index.
         */
        val indexKey: StubIndexKey<String, T>
) {

    /**
     * Get all the items in the index in the given file set.
     *
     * @param baseFile
     *          The file from which to look.
     */
    fun getItemsInFileSet(baseFile: PsiFile): Collection<T> {
        // Setup search set.
        val project = baseFile.project
        val searchFiles = baseFile.referencedFiles()
                .map { it.virtualFile }
                .toMutableSet()
        searchFiles.add(baseFile.virtualFile)

        // Add document class.
        val root = baseFile.findRootFile()
        val documentClass = root.documentClassFile()
        if (documentClass != null) {
            searchFiles.add(documentClass.virtualFile)
        }

        // Search index.
        val scope = GlobalSearchScope.filesScope(project, searchFiles)
        return getItems(project, scope)
    }

    /**
     * Get all the items in the index that are in the given file.
     */
    fun getItems(file: PsiFile) = getItems(file.project, GlobalSearchScope.fileScope(file))

    /**
     * Get all the items in the index that are in the given project.
     */
    fun getItems(project: Project) = getItems(project, GlobalSearchScope.projectScope(project))

    /**
     * Get all the items in the index.
     *
     * @param project
     *          The project instance.
     * @param scope
     *          The scope in which to search for the items.
     */
    fun getItems(project: Project, scope: GlobalSearchScope): Collection<T> {
        val result = ArrayList<T>()
        for (key in getKeys(project)) {
            result.addAll(getItemsByName(key, project, scope))
        }
        return result
    }

    /**
     * Get all the items in the index that have the given name.
     *
     * @param name
     *          The name of the items to get.
     * @param project
     *          The project instance.
     * @param scope
     *          The scope in which to search for the items.
     */
    fun getItemsByName(name: String, project: Project, scope: GlobalSearchScope): Collection<T> {
        return StubIndex.getElements(indexKey, name, project, scope, elementClass)
    }

    /**
     * Get all the keys in the index.
     *
     * @param project
     *          The project instance.
     */
    fun getKeys(project: Project) = StubIndex.getInstance().getAllKeys(indexKey, project).toTypedArray()

    /**
     * Get the key of the index.
     */
    fun key() = indexKey
}