package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Automated architecture hard guardrails scanning the real packaged deliverable JAR:
 * <ol>
 *   <li>Dedicated Server isolation: non-client classes must have zero constant-pool references
 *       to {@code net/minecraft/client/} or client-only GUI / rendering classes.</li>
 *   <li>Mixin bidirectional closure: every class declared in {@code *.mixins.json} must exist
 *       in the packaged JAR, and every {@code @Mixin}-annotated source class must be registered.</li>
 *   <li>Clean deliverable packaging: zero {@code rinlib} and zero {@code com/terraformersmc/modmenu}
 *       compile-time stub leakage in the final JAR.</li>
 * </ol>
 */
final class ArchitectureContractTest {
    private static final List<String> FORBIDDEN_SERVER_CLIENT_PREFIXES = List.of(
        "net/minecraft/client/",
        "dev/ftb/mods/ftblibrary/ui/",
        "dev/ftb/mods/ftblibrary/icon/",
        "com/mojang/blaze3d/",
        "org/lwjgl/"
    );

    private static final Pattern PACKAGE_DECL = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern MIXIN_ANNOTATION = Pattern.compile("(?m)^\\s*@Mixin\\b");
    private static final Pattern TYPE_DECL = Pattern.compile(
        "(?m)\\b(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?(?:class|interface)\\s+(\\w+)\\b"
    );
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_ARRAY_STRING = Pattern.compile("\"([^\"]+)\"");

    @Test
    void dedicatedServerIsolationForbidsClientReferencesOutsideClientClasses() throws IOException {
        Path jarPath = resolveBuiltJar();
        try (ZipFile jar = new ZipFile(jarPath.toFile())) {
            MixinManifest manifest = parseMixinsFromJar(jar);
            Set<String> clientOnlyMixinClasses = manifest.clientMixinInternalNames();
            List<String> violations = new ArrayList<>();
            int scannedServerClasses = 0;

            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                if (!name.startsWith("dev/rinchan/paperplane/") && !name.startsWith("_platform/")) {
                    continue;
                }
                if (isAllowedClientClass(name, clientOnlyMixinClasses)) {
                    continue;
                }

                scannedServerClasses++;
                byte[] bytecode = jar.getInputStream(entry).readAllBytes();
                List<String> utf8Entries = readConstantPoolUtf8s(bytecode);
                for (String constant : utf8Entries) {
                    for (String forbidden : FORBIDDEN_SERVER_CLIENT_PREFIXES) {
                        if (constant.contains(forbidden)) {
                            violations.add(name + " references forbidden client symbol '" + constant + "'");
                        }
                    }
                }
            }

            assertTrue(scannedServerClasses > 0, "Expected to scan server/common classes in " + jarPath);
            assertTrue(
                violations.isEmpty(),
                "Dedicated server isolation violations in " + jarPath.getFileName() + ":\n  - "
                    + String.join("\n  - ", violations)
            );
        }
    }

    @Test
    void mixinConfigurationAndAnnotatedSourcesFormExactBidirectionalClosure() throws IOException {
        Path jarPath = resolveBuiltJar();
        try (ZipFile jar = new ZipFile(jarPath.toFile())) {
            MixinManifest manifest = parseMixinsFromJar(jar);
            assertFalse(manifest.allDeclaredInternalNames().isEmpty(), "Expected at least one Mixin in *.mixins.json");

            // 1. Forward closure: every declared mixin in *.mixins.json MUST exist in the final JAR.
            List<String> missingInJar = new ArrayList<>();
            for (String internalName : manifest.allDeclaredInternalNames()) {
                String classEntry = internalName + ".class";
                if (jar.getEntry(classEntry) == null) {
                    missingInJar.add(classEntry);
                }
            }
            assertTrue(
                missingInJar.isEmpty(),
                "Mixins declared in *.mixins.json but missing from " + jarPath.getFileName() + ": " + missingInJar
            );

            // 2. Common mixins MUST NOT reference client classes.
            List<String> clientLeakInCommonMixin = new ArrayList<>();
            for (String internalName : manifest.commonMixinInternalNames()) {
                ZipEntry entry = jar.getEntry(internalName + ".class");
                assertNotNull(entry, "Missing common mixin class: " + internalName);
                for (String constant : readConstantPoolUtf8s(jar.getInputStream(entry).readAllBytes())) {
                    for (String forbidden : FORBIDDEN_SERVER_CLIENT_PREFIXES) {
                        if (constant.contains(forbidden)) {
                            clientLeakInCommonMixin.add(internalName + " -> " + constant);
                        }
                    }
                }
            }
            assertTrue(
                clientLeakInCommonMixin.isEmpty(),
                "Common mixins must not reference client classes: " + clientLeakInCommonMixin
            );

            // 3. Reverse closure: every @Mixin-annotated source class MUST be declared in *.mixins.json.
            Set<String> sourceMixins = findAnnotatedMixinsInSources();
            assertFalse(sourceMixins.isEmpty(), "Expected to discover @Mixin classes in main Java sources");

            Set<String> unregistered = new LinkedHashSet<>(sourceMixins);
            unregistered.removeAll(manifest.allDeclaredInternalNames());
            assertTrue(
                unregistered.isEmpty(),
                "@Mixin classes present in source but NOT registered in *.mixins.json: " + unregistered
            );

            Set<String> undeclaredSource = new LinkedHashSet<>(manifest.allDeclaredInternalNames());
            undeclaredSource.removeAll(sourceMixins);
            assertTrue(
                undeclaredSource.isEmpty(),
                "Mixins registered in *.mixins.json but missing @Mixin source file: " + undeclaredSource
            );
        }
    }

    @Test
    void packagedJarHasZeroRinlibOrModMenuStubLeakage() throws IOException {
        Path jarPath = resolveBuiltJar();
        try (ZipFile jar = new ZipFile(jarPath.toFile())) {
            List<String> violations = new ArrayList<>();
            int totalEntries = 0;

            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                totalEntries++;
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);

                if (lower.contains("rinlib")) {
                    violations.add("Forbidden rinlib entry in JAR: " + name);
                }
                if (lower.startsWith("com/terraformersmc/modmenu/")) {
                    violations.add("Compile-only Mod Menu stub leaked into JAR: " + name);
                }
                if (name.endsWith(".class") && name.startsWith("dev/rinchan/")) {
                    for (String constant : readConstantPoolUtf8s(jar.getInputStream(entry).readAllBytes())) {
                        if (constant.toLowerCase(Locale.ROOT).contains("rinlib")) {
                            violations.add("Forbidden rinlib constant-pool reference in " + name + ": " + constant);
                        }
                    }
                }
            }

            assertTrue(totalEntries > 0, "Packaged JAR is empty: " + jarPath);
            assertTrue(
                violations.isEmpty(),
                "Packaging cleanliness violations in " + jarPath.getFileName() + ":\n  - "
                    + String.join("\n  - ", violations)
            );
        }
    }

    private static boolean isAllowedClientClass(String classEntryName, Set<String> clientOnlyMixinClasses) {
        if (classEntryName.startsWith("dev/rinchan/paperplane/client/")) {
            return true;
        }
        String internalName = classEntryName.substring(0, classEntryName.length() - ".class".length());
        if (clientOnlyMixinClasses.contains(internalName)) {
            return true;
        }
        // Loader-specific client entrypoints (gated by Dist.CLIENT or fabric.mod.json "client" / "modmenu")
        String simpleName = internalName.substring(internalName.lastIndexOf('/') + 1);
        int dollar = simpleName.indexOf('$');
        String outerSimple = dollar >= 0 ? simpleName.substring(0, dollar) : simpleName;
        return outerSimple.endsWith("Client")
            || outerSimple.endsWith("ClientNetwork")
            || outerSimple.endsWith("ModMenu");
    }

    private static Path resolveBuiltJar() throws IOException {
        String configured = System.getProperty("paperplane.jar");
        if (configured != null && !configured.isBlank()) {
            Path p = Path.of(configured);
            assertTrue(Files.isRegularFile(p), "Configured paperplane.jar does not exist: " + p);
            return p;
        }

        // Fallback when invoked outside Gradle property injection
        Path libs = Path.of("build/libs");
        assertTrue(Files.isDirectory(libs), "Missing build/libs directory: " + libs.toAbsolutePath());
        try (var stream = Files.list(libs)) {
            List<Path> candidates = stream
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString();
                    return n.startsWith("paper_plane-")
                        && n.endsWith(".jar")
                        && !n.endsWith("-sources.jar")
                        && !n.endsWith("-dev.jar")
                        && !n.endsWith("-javadoc.jar");
                })
                .sorted()
                .toList();
            assertFalse(candidates.isEmpty(), "No built paper_plane JAR found in " + libs.toAbsolutePath());
            return candidates.stream()
                .filter(p -> p.getFileName().toString().endsWith("-all.jar"))
                .findFirst()
                .orElse(candidates.get(candidates.size() - 1));
        }
    }

    private static Set<String> findAnnotatedMixinsInSources() throws IOException {
        String configured = System.getProperty("paperplane.sourceDirs");
        List<Path> roots = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            for (String part : configured.split(Pattern.quote(File.pathSeparator))) {
                if (!part.isBlank()) {
                    Path dir = Path.of(part);
                    if (Files.isDirectory(dir)) {
                        roots.add(dir);
                    }
                }
            }
        } else {
            for (String rel : List.of("src/main/java", "../common/src/main/java", "../../shared/core", "../../shared/modern")) {
                Path dir = Path.of(rel);
                if (Files.isDirectory(dir)) {
                    roots.add(dir);
                }
            }
        }

        Set<String> result = new LinkedHashSet<>();
        for (Path root : roots) {
            try (var stream = Files.walk(root)) {
                for (Path file : stream.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String src = Files.readString(file, StandardCharsets.UTF_8);
                    if (!MIXIN_ANNOTATION.matcher(src).find()) {
                        continue;
                    }
                    Matcher pkgMatcher = PACKAGE_DECL.matcher(src);
                    Matcher typeMatcher = TYPE_DECL.matcher(src);
                    if (pkgMatcher.find() && typeMatcher.find()) {
                        String pkg = pkgMatcher.group(1).replace('.', '/');
                        String typeName = typeMatcher.group(1);
                        result.add(pkg + "/" + typeName);
                    }
                }
            }
        }
        return result;
    }

    private static MixinManifest parseMixinsFromJar(ZipFile jar) throws IOException {
        Set<String> common = new LinkedHashSet<>();
        Set<String> client = new LinkedHashSet<>();
        Set<String> server = new LinkedHashSet<>();

        Enumeration<? extends ZipEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.endsWith(".mixins.json") || name.contains("/")) {
                continue;
            }
            String json = new String(jar.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher pkgMatcher = JSON_STRING_FIELD.matcher(json);
            assertTrue(pkgMatcher.find(), "Mixin config missing \"package\" field: " + name);
            String basePkg = pkgMatcher.group(1).replace('.', '/');

            for (String rel : extractJsonStringArray(json, "mixins")) {
                common.add(basePkg + "/" + rel.replace('.', '/'));
            }
            for (String rel : extractJsonStringArray(json, "client")) {
                client.add(basePkg + "/" + rel.replace('.', '/'));
            }
            for (String rel : extractJsonStringArray(json, "server")) {
                server.add(basePkg + "/" + rel.replace('.', '/'));
            }
        }
        return new MixinManifest(common, client, server);
    }

    private static List<String> extractJsonStringArray(String json, String fieldName) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        Matcher item = JSON_ARRAY_STRING.matcher(m.group(1));
        while (item.find()) {
            values.add(item.group(1));
        }
        return values;
    }

    /**
     * Parses a standard JVM ClassFile constant pool (JVMS §4.4) and returns all CONSTANT_Utf8 strings.
     */
    private static List<String> readConstantPoolUtf8s(byte[] classBytes) throws IOException {
        List<String> utf8s = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IOException("Invalid class file magic: 0x" + Integer.toHexString(magic));
            }
            in.readUnsignedShort(); // minor_version
            in.readUnsignedShort(); // major_version
            int cpCount = in.readUnsignedShort();
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8s.add(in.readUTF()); // CONSTANT_Utf8
                    case 3, 4 -> in.skipBytes(4); // Integer, Float
                    case 5, 6 -> { // Long, Double (occupies two CP entries)
                        in.skipBytes(8);
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> in.skipBytes(2); // Class, String, MethodType, Module, Package
                    case 9, 10, 11, 12, 17, 18 -> in.skipBytes(4); // Fieldref, Methodref, InterfaceMethodref, NameAndType, Dynamic, InvokeDynamic
                    case 15 -> in.skipBytes(3); // MethodHandle
                    default -> throw new IOException("Unknown constant pool tag: " + tag);
                }
            }
        }
        return utf8s;
    }

    private record MixinManifest(
        Set<String> commonMixinInternalNames,
        Set<String> clientMixinInternalNames,
        Set<String> serverMixinInternalNames
    ) {
        Set<String> allDeclaredInternalNames() {
            Set<String> all = new LinkedHashSet<>();
            all.addAll(commonMixinInternalNames);
            all.addAll(clientMixinInternalNames);
            all.addAll(serverMixinInternalNames);
            return all;
        }
    }
}
