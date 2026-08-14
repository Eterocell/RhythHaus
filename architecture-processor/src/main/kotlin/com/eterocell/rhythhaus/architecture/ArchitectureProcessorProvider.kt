package com.eterocell.rhythhaus.architecture

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import java.io.File
import java.nio.file.Path

public class ArchitectureProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor =
        ArchitectureProcessor(
            modulePath = environment.options.getValue("architecture.module"),
            packageRoots =
                environment.options
                    .getValue("architecture.packageRoots")
                    .split(',')
                    .filter(String::isNotBlank),
            sourceRoots =
                environment.options
                    .getValue("architecture.sourceRoots")
                    .split(File.pathSeparator)
                    .filter(String::isNotBlank)
                    .map(::File),
            environment = environment,
        )
}

private class ArchitectureProcessor(
    private val modulePath: String,
    private val packageRoots: List<String>,
    private val sourceRoots: List<File>,
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    private var processedInitialInput: Boolean = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processedInitialInput) return emptyList()
        processedInitialInput = true
        val diagnostics: MutableMap<String, KSNode> = sortedMapOf()
        resolver
            .getNewFiles()
            .filter { it.origin == Origin.KOTLIN && it.isProductionSource() }
            .sortedBy(KSFile::filePath)
            .forEach { file ->
                validatePackage(file, diagnostics)
                file.declarations
                    .flatMap(KSDeclaration::allDeclarations)
                    .forEach {
                        validateKDoc(it, diagnostics)
                        validateLibraryPublicCallableContract(it, diagnostics)
                    }
            }
        diagnostics.forEach { (message, node) ->
            environment.logger.error(message, node)
        }
        return emptyList()
    }

    private fun validatePackage(
        file: KSFile,
        diagnostics: MutableMap<String, KSNode>
    ) {
        val packageName = file.packageName.asString()
        if (packageRoots.none {
            packageName == it || packageName.startsWith("$it.")
        }) {
            report(
                diagnostics,
                "ARCH-PACKAGE $modulePath:${relativePath(file)} ($packageName)",
                file)
        }
    }

    private fun validateKDoc(
        declaration: KSDeclaration,
        diagnostics: MutableMap<String, KSNode>
    ) {
        if (declaration.origin == Origin.SYNTHETIC) return
        if (Modifier.PUBLIC in declaration.modifiers &&
            declaration.qualifiedName != null &&
            declaration.docString.isNullOrBlank()) {
            report(
                diagnostics,
                "ARCH-KDOC $modulePath:${declaration.identity()}",
                declaration)
        }
    }

    @Suppress("ComplexCondition")
    private fun validateLibraryPublicCallableContract(
        declaration: KSDeclaration,
        diagnostics: MutableMap<String, KSNode>,
    ) {
        if (modulePath != ":feature:library:impl" ||
            declaration !is KSFunctionDeclaration ||
            declaration.origin == Origin.SYNTHETIC ||
            Modifier.PUBLIC !in declaration.modifiers ||
            declaration.qualifiedName == null)
            return

        declaration.parameters.forEach { parameter ->
            val name = parameter.name?.asString() ?: return@forEach
            if (!declaration.docString
                .orEmpty()
                .contains(Regex("@param\\s+$name(?:\\s|$)"))) {
                report(
                    diagnostics,
                    "ARCH-KDOC $modulePath:${declaration.identity()} missing @param $name",
                    declaration,
                )
            }
            if (parameter.hasDefault &&
                !isAllowedLibraryDefault(declaration, name)) {
                report(
                    diagnostics,
                    "ARCH-DEFAULT $modulePath:${declaration.identity()} ($name)",
                    declaration,
                )
            }
        }
    }

    private fun isAllowedLibraryDefault(
        declaration: KSFunctionDeclaration,
        parameterName: String
    ): Boolean {
        val owner = declaration.parentDeclaration?.qualifiedName?.asString()
        val callable = declaration.qualifiedName?.asString()
        return (owner == "com.eterocell.rhythhaus.library.ScanProgress" &&
            parameterName in setOf("session", "latestItem")) ||
            (callable ==
                "com.eterocell.rhythhaus.library.LibraryScanner.scan" &&
                parameterName in setOf("isCancelled", "onProgress")) ||
            (callable ==
                "com.eterocell.rhythhaus.library.impl.audioCandidateForSourceFile" &&
                parameterName in
                    setOf(
                        "metadataAudioSource",
                        "cleanupMetadataAudioSource",
                        "sizeBytes",
                        "modifiedAtEpochMillis",
                    )) ||
            (owner ==
                "com.eterocell.rhythhaus.library.PlatformFolderPickResult.Failure" &&
                parameterName == "cause")
    }

    private fun report(
        diagnostics: MutableMap<String, KSNode>,
        message: String,
        node: KSNode
    ) {
        diagnostics.putIfAbsent(message, node)
    }

    private fun relativePath(file: KSFile): String {
        val sourceRoot = file.sourceRoot() ?: return "<unsupported>"
        val filePath = File(file.filePath).absoluteFile.toPath().normalize()
        return sourceRoot
            .relativize(filePath)
            .toString()
            .replace(File.separatorChar, '/')
    }

    private fun KSFile.isProductionSource(): Boolean =
        sourceRoot()?.let { !it.isGeneratedSourceRoot() } == true

    private fun KSFile.sourceRoot(): Path? {
        val filePath = File(filePath).absoluteFile.toPath().normalize()
        return sourceRoots
            .map { it.absoluteFile.toPath().normalize() }
            .filter { filePath.startsWith(it) }
            .maxByOrNull { it.nameCount }
    }

    private fun Path.isGeneratedSourceRoot(): Boolean =
        zipWithNext().any { (parent, child) ->
            parent.toString() == "build" && child.toString() == "generated"
        }

    private fun KSDeclaration.identity(): String {
        val file =
            containingFile
                ?: return "<unsupported>:?:? (${qualifiedName?.asString().orEmpty()})"
        val position = location as? FileLocation
        return "${relativePath(file)}:${position?.lineNumber ?: "?"} (${qualifiedName?.asString().orEmpty()})"
    }
}

private fun KSDeclaration.allDeclarations(): Sequence<KSDeclaration> =
    sequence {
        yield(this@allDeclarations)
        if (this@allDeclarations is KSClassDeclaration) {
            declarations.forEach { declaration ->
                yieldAll(declaration.allDeclarations())
            }
        }
    }
