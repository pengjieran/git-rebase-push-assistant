package com.examplecn.service

import com.intellij.openapi.components.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPOutputStream

/**
 * Holds the two variants of a generated hotfix script:
 * [full] includes existence + SHA-256 integrity checks (best for saving to a file),
 * [clipboard] omits the if/verification blocks so it pastes cleanly into a terminal.
 */
data class HotfixScripts(val full: String, val clipboard: String)

/**
 * Service for generating Arthas hotfix scripts from .class files.
 * The payload is gzip-compressed then Base64-encoded to reduce size,
 * and verified with SHA-256 on the target host.
 */
@Service(Service.Level.PROJECT)
class ArthasHotfixService {

    fun generateHotfixScript(classFile: File): HotfixScripts {
        if (!classFile.exists()) {
            throw IllegalArgumentException("Class file does not exist: ${classFile.absolutePath}")
        }
        if (!classFile.isFile || classFile.extension != "class") {
            throw IllegalArgumentException("Not a valid .class file: ${classFile.name}")
        }

        val className = classFile.nameWithoutExtension
        val timestamp = System.currentTimeMillis()

        val classBytes = classFile.readBytes()
        val sha256 = sha256Hex(classBytes)
        val gzipped = gzip(classBytes)
        val base64Content = Base64.getMimeEncoder(76, "\n".toByteArray()).encodeToString(gzipped)

        return HotfixScripts(
            full = buildScript(className, timestamp, base64Content, sha256, classBytes.size, gzipped.size, verify = true),
            clipboard = buildScript(className, timestamp, base64Content, sha256, classBytes.size, gzipped.size, verify = false)
        )
    }

    private fun gzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun buildScript(
        className: String,
        timestamp: Long,
        base64Content: String,
        sha256: String,
        rawSize: Int,
        gzipSize: Int,
        verify: Boolean
    ): String {
        val d = '$'
        val sb = StringBuilder()
        sb.appendLine("#!/bin/bash")
        sb.appendLine("# Arthas Hotfix Script for $className")
        sb.appendLine("# Generated: ${java.time.Instant.ofEpochMilli(timestamp)}")
        sb.appendLine("# Payload: gzip + Base64 (raw ${rawSize}B -> gzip ${gzipSize}B)")
        sb.appendLine("#")
        sb.appendLine("# Usage:")
        sb.appendLine("#   1. Upload this script to the target server")
        sb.appendLine("#   2. Execute: bash ${className}_hotfix_${timestamp}.sh")
        sb.appendLine("#   3. Attach Arthas to your Java process, then run the printed retransform command")
        sb.appendLine()
        sb.appendLine("set -e")
        sb.appendLine()
        sb.appendLine("TEMP_DIR=\"/tmp\"")
        sb.appendLine("CLASS_NAME=\"$className\"")
        sb.appendLine("TIMESTAMP=\"$timestamp\"")
        if (verify) {
            sb.appendLine("EXPECTED_SHA256=\"$sha256\"")
        }
        sb.appendLine("GZ_FILE=\"${d}{TEMP_DIR}/${d}{CLASS_NAME}_${d}{TIMESTAMP}.class.gz\"")
        sb.appendLine("CLASS_FILE=\"${d}{TEMP_DIR}/${d}{CLASS_NAME}_${d}{TIMESTAMP}.class\"")
        sb.appendLine()
        sb.appendLine("echo \"==> Arthas Hotfix Script for ${d}CLASS_NAME\"")
        sb.appendLine("echo \"==> Decoding Base64 + gunzip to ${d}CLASS_FILE...\"")
        sb.appendLine()
        sb.appendLine("# Decode Base64 -> gzip stream, then gunzip -> .class")
        sb.appendLine("base64 -d << 'EOF_B64' > \"${d}GZ_FILE\"")
        sb.appendLine(base64Content)
        sb.appendLine("EOF_B64")
        sb.appendLine()
        sb.appendLine("gunzip -c \"${d}GZ_FILE\" > \"${d}CLASS_FILE\"")
        sb.appendLine()
        if (verify) {
            sb.appendLine("if [ ! -f \"${d}CLASS_FILE\" ]; then")
            sb.appendLine("    echo \"ERROR: Failed to create .class file\"")
            sb.appendLine("    exit 1")
            sb.appendLine("fi")
            sb.appendLine()
            sb.appendLine("# Verify integrity against the source .class SHA-256")
            sb.appendLine("if command -v sha256sum >/dev/null 2>&1; then")
            sb.appendLine("    ACTUAL_SHA256=${d}(sha256sum \"${d}CLASS_FILE\" | awk '{print ${d}1}')")
            sb.appendLine("elif command -v shasum >/dev/null 2>&1; then")
            sb.appendLine("    ACTUAL_SHA256=${d}(shasum -a 256 \"${d}CLASS_FILE\" | awk '{print ${d}1}')")
            sb.appendLine("else")
            sb.appendLine("    ACTUAL_SHA256=\"\"")
            sb.appendLine("    echo \"WARN: no sha256sum/shasum found, skipping integrity check\"")
            sb.appendLine("fi")
            sb.appendLine()
            sb.appendLine("if [ -n \"${d}ACTUAL_SHA256\" ] && [ \"${d}ACTUAL_SHA256\" != \"${d}EXPECTED_SHA256\" ]; then")
            sb.appendLine("    echo \"ERROR: SHA-256 mismatch, file may be corrupted\"")
            sb.appendLine("    echo \"  expected: ${d}EXPECTED_SHA256\"")
            sb.appendLine("    echo \"  actual:   ${d}ACTUAL_SHA256\"")
            sb.appendLine("    exit 1")
            sb.appendLine("fi")
            sb.appendLine()
            sb.appendLine("CLASS_SIZE=${d}(stat -f%z \"${d}CLASS_FILE\" 2>/dev/null || stat -c%s \"${d}CLASS_FILE\" 2>/dev/null)")
            sb.appendLine("echo \"==> Class file ready (${d}CLASS_SIZE bytes, sha256 ok)\"")
        } else {
            sb.appendLine("echo \"==> Class file ready: ${d}CLASS_FILE\"")
        }
        sb.appendLine("echo \"\"")
        sb.appendLine("echo \"==> Next steps:\"")
        sb.appendLine("echo \"    1. Attach Arthas: java -jar arthas-boot.jar\"")
        sb.appendLine("echo \"    2. In Arthas, run: retransform ${d}CLASS_FILE\"")
        sb.appendLine("echo \"\"")
        sb.appendLine("echo \"==> Retransform command (copy into Arthas):\"")
        sb.appendLine("echo \"retransform ${d}CLASS_FILE\"")
        sb.appendLine("echo \"\"")
        sb.appendLine("echo \"==> Cleanup (optional, after successful retransform):\"")
        sb.appendLine("echo \"rm -f ${d}GZ_FILE ${d}CLASS_FILE\"")
        return sb.toString()
    }
}