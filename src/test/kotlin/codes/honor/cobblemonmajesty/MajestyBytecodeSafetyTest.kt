package codes.honor.cobblemonmajesty

import java.io.File
import java.nio.file.Files
import java.util.jar.JarFile
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class MajestyBytecodeSafetyTest {
    @Test
    fun serverClassesNeverReferenceClientTypes() {
        val classesDirs = listOf(
            File("build/classes/kotlin/main"),
            File("build/classes/java/main")
        ).filter { it.isDirectory }
        assertTrue(classesDirs.isNotEmpty(), "Expected compiled main class directories")

        val forbiddenPrefixes = listOf(
            "net/minecraft/client/",
            "com/mojang/blaze3d/",
            "com/cobblemon/mod/common/client/",
            "net/neoforged/neoforge/client/"
        )

        val violations = mutableListOf<String>()

        for (dir in classesDirs) {
            Files.walk(dir.toPath()).use { stream ->
                stream.filter { it.toString().endsWith(".class") }.forEach { classPath ->
                    val bytes = Files.readAllBytes(classPath)
                    val reader = ClassReader(bytes)
                    val className = reader.className
                    if (className.contains("/client/") || className.endsWith("/MajestyClient")) {
                        return@forEach
                    }

                    fun checkRef(text: String?) {
                        if (text == null) return
                        for (prefix in forbiddenPrefixes) {
                            if (text.contains(prefix)) {
                                violations.add("$className references client type in $text")
                            }
                        }
                    }

                    reader.accept(object : ClassVisitor(Opcodes.ASM9) {
                        override fun visit(
                            version: Int,
                            access: Int,
                            name: String?,
                            signature: String?,
                            superName: String?,
                            interfaces: Array<out String>?
                        ) {
                            checkRef(superName)
                            checkRef(signature)
                            interfaces?.forEach(::checkRef)
                        }

                        override fun visitField(
                            access: Int,
                            name: String?,
                            descriptor: String?,
                            signature: String?,
                            value: Any?
                        ): FieldVisitor? {
                            checkRef(descriptor)
                            checkRef(signature)
                            return null
                        }

                        override fun visitMethod(
                            access: Int,
                            name: String?,
                            descriptor: String?,
                            signature: String?,
                            exceptions: Array<out String>?
                        ): MethodVisitor {
                            checkRef(descriptor)
                            checkRef(signature)
                            exceptions?.forEach(::checkRef)
                            return object : MethodVisitor(Opcodes.ASM9) {
                                override fun visitTypeInsn(opcode: Int, type: String?) = checkRef(type)
                                override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
                                    checkRef(owner)
                                    checkRef(descriptor)
                                }
                                override fun visitMethodInsn(
                                    opcode: Int,
                                    owner: String?,
                                    name: String?,
                                    descriptor: String?,
                                    isInterface: Boolean
                                ) {
                                    checkRef(owner)
                                    checkRef(descriptor)
                                }
                            }
                        }
                    }, 0)
                }
            }
        }

        assertTrue(violations.isEmpty(), "Dedicated-server safety violations:\n" + violations.joinToString("\n"))
    }

    @Test
    fun cobblemonMixinTargetsMatchJarSignatures() {
        val jarProp = System.getProperty("cobblemon_majesty.cobblemonJar")
        val targetJar = if (!jarProp.isNullOrBlank() && File(jarProp).isFile) {
            File(jarProp)
        } else {
            System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map(::File)
                .firstOrNull { it.isFile && it.name.lowercase().contains("cobblemon") && it.name.endsWith(".jar") }
        }
        assertNotNull(targetJar, "Could not locate Cobblemon jar on classpath for Mixin verification")

        val expectedMethods = mapOf(
            "com/cobblemon/mod/common/battles/runner/graal/GraalShowdownService.class" to listOf(
                "openConnection:()V",
                "endBattle:(Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;)V"
            ),
            "com/cobblemon/mod/common/client/render/pokemon/PokemonRenderer.class" to listOf(
                "render:(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                "resolveBaseLabel:(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            "com/cobblemon/mod/common/client/render/models/blockbench/PosableModel.class" to listOf(
                "render:(Lcom/cobblemon/mod/common/client/render/models/blockbench/repository/RenderContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            ),
            "com/cobblemon/mod/common/client/render/models/blockbench/repository/VaryingModelRepository.class" to listOf(
                "getTexture:(Lnet/minecraft/resources/ResourceLocation;Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableState;)Lnet/minecraft/resources/ResourceLocation;",
                "getTextureNoSubstitute:(Lnet/minecraft/resources/ResourceLocation;Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableState;)Lnet/minecraft/resources/ResourceLocation;"
            ),
            "com/cobblemon/mod/common/client/entity/PokemonClientDelegate.class" to listOf(
                "tick:(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V"
            ),
            "com/cobblemon/mod/common/client/gui/summary/Summary.class" to listOf(
                "render:(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            ),
            "com/cobblemon/mod/common/client/gui/battle/BattleOverlay.class" to listOf(
                "drawBattleTile"
            )
        )

        val expectedFields = mapOf(
            "com/cobblemon/mod/common/client/gui/summary/Summary.class" to listOf(
                setOf("selectedPokemon:Lcom/cobblemon/mod/common/pokemon/Pokemon;")
            ),
            "com/cobblemon/mod/common/battles/runner/graal/GraalShowdownService.class" to listOf(
                setOf(
                    "context:Lcom/cobblemon/mod/relocations/graalvm/polyglot/Context;",
                    "context:Lorg/graalvm/polyglot/Context;"
                )
            )
        )

        JarFile(targetJar!!).use { jar ->
            for ((entryName, methods) in expectedMethods) {
                val entry = jar.getJarEntry(entryName)
                assertNotNull(entry, "Missing Mixin target class $entryName in ${targetJar.name}")
                val foundMethods = mutableSetOf<String>()
                val foundMethodNames = mutableSetOf<String>()
                val foundFields = mutableSetOf<String>()

                val reader = ClassReader(jar.getInputStream(entry))
                reader.accept(object : ClassVisitor(Opcodes.ASM9) {
                    override fun visitMethod(
                        access: Int,
                        name: String,
                        descriptor: String,
                        signature: String?,
                        exceptions: Array<out String>?
                    ): MethodVisitor? {
                        foundMethods.add("$name:$descriptor")
                        foundMethodNames.add(name)
                        return null
                    }

                    override fun visitField(
                        access: Int,
                        name: String,
                        descriptor: String,
                        signature: String?,
                        value: Any?
                    ): FieldVisitor? {
                        foundFields.add("$name:$descriptor")
                        return null
                    }
                }, ClassReader.SKIP_CODE)

                for (expected in methods) {
                    if (expected.contains(":")) {
                        assertTrue(
                            expected in foundMethods,
                            "Missing method $expected on $entryName (found: $foundMethods)"
                        )
                    } else {
                        assertTrue(
                            expected in foundMethodNames,
                            "Missing method name $expected on $entryName"
                        )
                    }
                }

                for (candidates in expectedFields[entryName].orEmpty()) {
                    assertTrue(
                        candidates.any { it in foundFields },
                        "Missing field $candidates on $entryName (found: $foundFields)"
                    )
                }
            }
        }
    }
}
