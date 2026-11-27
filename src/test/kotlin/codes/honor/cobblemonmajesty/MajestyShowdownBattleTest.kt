package codes.honor.cobblemonmajesty

import codes.honor.cobblemonmajesty.battle.MajestyShowdown
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipInputStream
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MajestyShowdownBattleTest {
    private lateinit var showdownRoot: Path

    @BeforeAll
    fun prepareShowdown(@TempDir tempDir: Path) {
        val propDir = System.getProperty("cobblemon_majesty.showdownDir")
        if (!propDir.isNullOrBlank() && File(propDir, "index.js").isFile) {
            showdownRoot = File(propDir).toPath()
            return
        }

        val cobblemonJar = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map(::File)
            .firstOrNull { it.isFile && it.name.lowercase().contains("cobblemon") && it.name.endsWith(".jar") }
            ?: error("Cobblemon jar not found on test classpath")

        val dest = tempDir.resolve("showdown")
        Files.createDirectories(dest)
        JarFile(cobblemonJar).use { jar ->
            val zipEntry = jar.getJarEntry("data/cobblemon/showdown.zip")
                ?: error("data/cobblemon/showdown.zip not found in ${cobblemonJar.name}")
            ZipInputStream(jar.getInputStream(zipEntry)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outPath = dest.resolve(entry.name).normalize()
                    check(outPath.startsWith(dest))
                    if (entry.isDirectory) {
                        Files.createDirectories(outPath)
                    } else {
                        Files.createDirectories(outPath.parent)
                        Files.copy(zis, outPath)
                    }
                    entry = zis.nextEntry
                }
            }
        }
        showdownRoot = dest
    }

    private fun withShowdownContext(block: (Context) -> Unit) {
        val ctx = Context.newBuilder("js")
            .allowIO(true)
            .allowExperimentalOptions(true)
            .allowPolyglotAccess(PolyglotAccess.ALL)
            .allowHostAccess(HostAccess.ALL)
            .option("engine.WarnInterpreterOnly", "false")
            .option("js.commonjs-require", "true")
            .option("js.commonjs-require-cwd", showdownRoot.toAbsolutePath().toString())
            .option(
                "js.commonjs-core-modules-replacements",
                "buffer:buffer/,crypto:crypto-browserify,path:path-browserify"
            )
            .build()

        ctx.use {
            it.eval("js", "globalThis.process = { cwd: function() { return ''; } };")
            it.eval("js", Files.readString(showdownRoot.resolve("index.js")))
            it.eval("js", MajestyShowdown.BOOTSTRAP_JS)
            block(it)
        }
    }

    private fun simulateSingleHitDamage(
        ctx: Context,
        majesticArmed: Boolean,
        multiplier: Double = 1.1,
        attackerPacked: String,
        defenderPacked: String,
        moveChoice: String = "move 1"
    ): Int {
        val script = """
            (attackerPacked, defenderPacked, moveChoice, majesticArmed, multiplier) => {
              const Battle = require('./sim/battle').Battle;
              const Teams = require('./sim/teams').Teams;
              const p1Team = Teams.unpack(attackerPacked);
              const p2Team = Teams.unpack(defenderPacked);
              if (majesticArmed) {
                globalThis.__cobblemonMajesty.registerBattle('test-battle', p1Team[0].uuid, multiplier);
              } else {
                globalThis.__cobblemonMajesty.clearBattle('test-battle');
              }
              const battle = new Battle({
                formatid: 'gen9customgame',
                seed: [11, 22, 33, 44]
              });
              battle.setPlayer('p1', { name: 'Attacker', team: p1Team });
              battle.setPlayer('p2', { name: 'Defender', team: p2Team });
              if (battle.requestState === 'teampreview') {
                battle.makeChoices('team 1', 'team 1');
              }
              const defMon = battle.p2.active[0];
              const startHp = defMon.hp;
              battle.makeChoices(moveChoice, 'move 1');
              const dealt = startHp - defMon.hp;
              globalThis.__cobblemonMajesty.clearBattle('test-battle');
              return dealt;
            }
        """.trimIndent()
        val fn = ctx.eval("js", script)
        return fn.execute(attackerPacked, defenderPacked, moveChoice, majesticArmed, multiplier).asInt()
    }

    @Test
    fun boostsBothDualTypesAfterStabAndLeavesOffTypeUnchanged() {
        withShowdownContext { ctx ->
            val lucarioUuid = "11111111-2222-3333-4444-555555555555"
            val defenderUuid = "99999999-8888-7777-6666-555555555555"
            val lucario = "lucario||$lucarioUuid|300||-1||innerfocus|flashcannon,aurasphere,darkpulse,seismictoss|16/16,20/20,15/15,20/20|modest|0,0,0,252,0,0|M|31,31,31,31,31,31||100|255,pokeball,,,,Steel,"
            val blissey = "mew||$defenderUuid|400||-1||synchronize|splash|40/40|calm|252,0,252,0,252,0||31,31,31,31,31,31||100|255,pokeball,,,,Psychic,"

            val steelBase = simulateSingleHitDamage(ctx, false, 1.1, lucario, blissey, "move 1")
            val steelMajestic = simulateSingleHitDamage(ctx, true, 1.1, lucario, blissey, "move 1")
            assertTrue(steelBase > 0)
            assertEquals(steelBase * 1.1, steelMajestic.toDouble(), 1.5)
            assertTrue(steelMajestic > steelBase, "Expected Steel STAB move to gain 1.1x: $steelBase -> $steelMajestic")

            val fightingBase = simulateSingleHitDamage(ctx, false, 1.1, lucario, blissey, "move 2")
            val fightingMajestic = simulateSingleHitDamage(ctx, true, 1.1, lucario, blissey, "move 2")
            assertTrue(fightingMajestic > fightingBase, "Expected Fighting STAB move to gain 1.1x: $fightingBase -> $fightingMajestic")

            val darkOffTypeBase = simulateSingleHitDamage(ctx, false, 1.1, lucario, blissey, "move 3")
            val darkOffTypeMajestic = simulateSingleHitDamage(ctx, true, 1.1, lucario, blissey, "move 3")
            assertEquals(darkOffTypeBase, darkOffTypeMajestic, "Off-type Dark Pulse must not receive Majestic bonus")

            val seismicBase = simulateSingleHitDamage(ctx, false, 1.1, lucario, blissey, "move 4")
            val seismicMajestic = simulateSingleHitDamage(ctx, true, 1.1, lucario, blissey, "move 4")
            assertEquals(100, seismicBase)
            assertEquals(100, seismicMajestic, "Fixed-damage Seismic Toss must stay exactly 100")
        }
    }

    @Test
    fun stacksWithAdaptabilityIncludesProteanAndExcludesTeraAddedType() {
        withShowdownContext { ctx ->
            val atkUuid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
            val defUuid = "ffffffff-0000-1111-2222-333333333333"
            val adaptabilityLucario = "lucario||$atkUuid|300||-1||adaptability|aurasphere|20/20|modest|0,0,0,252,0,0|M|31,31,31,31,31,31||100|255,pokeball,,,,Fighting,"
            val target = "snorlax||$defUuid|520||-1||immunity|splash|40/40|careful|252,0,0,0,252,0|M|31,31,31,31,31,31||100|255,pokeball,,,,Normal,"

            val adaptBase = simulateSingleHitDamage(ctx, false, 1.1, adaptabilityLucario, target, "move 1")
            val adaptMajestic = simulateSingleHitDamage(ctx, true, 1.1, adaptabilityLucario, target, "move 1")
            assertTrue(adaptMajestic > adaptBase, "Expected Adaptability (2.0x) + Majesty (1.1x) = 2.2x: $adaptBase -> $adaptMajestic")
            assertEquals(adaptBase * 1.1, adaptMajestic.toDouble(), 2.0)

            val proteanGreninja = "greninja||$atkUuid|280||-1||protean|icebeam|15/15|timid|0,0,0,252,0,0|M|31,31,31,31,31,31||100|255,pokeball,,,,Water,"
            val proteanBase = simulateSingleHitDamage(ctx, false, 1.1, proteanGreninja, target, "move 1")
            val proteanMajestic = simulateSingleHitDamage(ctx, true, 1.1, proteanGreninja, target, "move 1")
            assertTrue(proteanMajestic > proteanBase, "Protean active natural type shift should receive 1.1x Majesty bonus")

            val fireTeraLucario = "lucario||$atkUuid|300||-1||innerfocus|flamethrower,aurasphere|15/15,20/20|modest|0,0,0,252,0,0|M|31,31,31,31,31,31||100|255,pokeball,,,,Fire,"
            val teraFlamethrowerBase = simulateSingleHitDamage(ctx, false, 1.1, fireTeraLucario, target, "move 1 terastallize")
            val teraFlamethrowerMajestic = simulateSingleHitDamage(ctx, true, 1.1, fireTeraLucario, target, "move 1 terastallize")
            assertEquals(
                teraFlamethrowerBase,
                teraFlamethrowerMajestic,
                "Tera-added Fire type must NOT receive Majestic 1.1x bonus"
            )

            val teraAuraSphereBase = simulateSingleHitDamage(ctx, false, 1.1, fireTeraLucario, target, "move 2 terastallize")
            val teraAuraSphereMajestic = simulateSingleHitDamage(ctx, true, 1.1, fireTeraLucario, target, "move 2 terastallize")
            assertTrue(
                teraAuraSphereMajestic > teraAuraSphereBase,
                "Natural Fighting type must still receive Majestic 1.1x bonus even while Terastallized into Fire"
            )
        }
    }
}
