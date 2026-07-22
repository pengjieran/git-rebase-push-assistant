package com.examplecn.service

import com.intellij.openapi.components.Service
import java.io.File
import java.util.Base64

/**
 * Service for generating Arthas hotfix scripts from .class files
 */
@Service(Service.Level.PROJECT)
class ArthasHotfixService {

    /**
     * Generate an Arthas hotfix shell script for the given .class file
     *
     * @param classFile The compiled .class file
     * @return Shell script content for Arthas retransform
     */
    fun generateHotfixScript(classFile: File): String {
        if (!classFile.exists()) {
            throw IllegalArgumentException("Class file does not exist: ${classFile.absolutePath}")
        }

        if (!classFile.isFile || classFile.extension != "class") {
            throw IllegalArgumentException("Not a valid .class file: ${classFile.name}")
        }

        val className = classFile.nameWithoutExtension
        val timestamp = System.currentTimeMillis()

        // Read and encode the .class file to Base64
        val classBytes = classFile.readBytes()
        val base64Content = Base64.getEncoder().encodeToString(classBytes)

        // Generate the shell script
        return buildScript(className, timestamp, base64Content)
    }

    private fun buildScript(className: String, timestamp: Long, base64Content: String): String {
        return """#!/bin/bash
# Arthas Hotfix Script for $className
# Generated: ${java.time.Instant.ofEpochMilli(timestamp)}
#
# Usage:
#   1. Upload this script to the target server
#   2. Run: chmod +x ${className}_hotfix_${timestamp}.sh
#   3. Execute: ./${className}_hotfix_${timestamp}.sh
#   4. The script will decode the class file and prepare the retransform command
#   5. Attach Arthas to your Java process and run the retransform command

set -e

TEMP_DIR="/tmp"
CLASS_NAME="$className"
TIMESTAMP="$timestamp"
TXT_FILE="${'$'}{TEMP_DIR}/${'$'}{CLASS_NAME}_${'$'}{TIMESTAMP}.txt"
CLASS_FILE="${'$'}{TEMP_DIR}/${'$'}{CLASS_NAME}_${'$'}{TIMESTAMP}.class"

echo "==> Arthas Hotfix Script for ${'$'}CLASS_NAME"
echo "==> Decoding Base64 content to ${'$'}TXT_FILE..."

# Write Base64 content to temporary text file
cat > "${'$'}TXT_FILE" << 'EOF_BASE64'
$base64Content
EOF_BASE64

echo "==> Decoding to .class file at ${'$'}CLASS_FILE..."
base64 -d < "${'$'}TXT_FILE" > "${'$'}CLASS_FILE"

if [ ! -f "${'$'}CLASS_FILE" ]; then
    echo "ERROR: Failed to create .class file"
    exit 1
fi

CLASS_SIZE=${'$'}(stat -f%z "${'$'}CLASS_FILE" 2>/dev/null || stat -c%s "${'$'}CLASS_FILE" 2>/dev/null)
echo "==> Class file created successfully (Size: ${'$'}CLASS_SIZE bytes)"
echo ""
echo "==> Next steps:"
echo "    1. Attach Arthas to your Java process:"
echo "       java -jar arthas-boot.jar"
echo ""
echo "    2. Run the retransform command in Arthas:"
echo "       retransform ${'$'}CLASS_FILE"
echo ""
echo "    3. Verify the retransform was successful"
echo ""
echo "==> Retransform command (copy and paste into Arthas):"
echo "retransform ${'$'}CLASS_FILE"
echo ""
echo "==> Cleanup (optional, after successful retransform):"
echo "rm -f ${'$'}TXT_FILE ${'$'}CLASS_FILE"
"""
    }
}