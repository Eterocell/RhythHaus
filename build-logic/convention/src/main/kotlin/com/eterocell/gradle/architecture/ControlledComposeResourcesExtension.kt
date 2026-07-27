package com.eterocell.gradle.architecture

public open class ControlledComposeResourcesExtension(
    private val declareNamespace: (String) -> Unit,
) {
    private var declaredNamespace: String? = null

    public val namespace: String?
        get() = declaredNamespace

    public fun namespace(value: String) {
        val normalizedValue = value.trim()

        val existingNamespace = declaredNamespace
        when {
            existingNamespace == null -> {
                declaredNamespace = normalizedValue
                if (normalizedValue.isNotBlank()) {
                    declareNamespace(normalizedValue)
                }
            }
            existingNamespace == normalizedValue -> Unit
            else ->
                error(
                    "Compose resources namespace is already declared as " +
                        "'$existingNamespace' and cannot be redeclared as '$normalizedValue'.",
                )
        }
    }
}
