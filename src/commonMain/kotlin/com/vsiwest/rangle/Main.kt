fun main() {
    // --- Simulate inducing schema from OpenAPI/Swagger nodes ---
    val openApiSpec = mapOf(
        "openapi" to "3.1.0",
        "info" to mapOf(
            "title" to "Schema Example",
            "version" to "0.1"
        ),
        "paths" to emptyMap<String, Any>(),
        "components" to mapOf(
            "schemas" to mapOf(
                "User" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "userId" to mapOf("type" to "integer", "format" to "int64", "example" to 12345),
                        "username" to mapOf("type" to "string", "example" to "john_doe"),
                        "email" to mapOf("type" to "string", "format" to "email", "example" to "john.doe@example.com"),
                    ),
                    "required" to listOf("userId", "username", "email")
                )
            )
        )
    )

    val structuralSignature = getStructuralSignatureWithTypes(openApiSpec)
    println("\n--- Structural Signature ---")
    println(structuralSignature)
}
