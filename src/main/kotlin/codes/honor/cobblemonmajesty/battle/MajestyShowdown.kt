package codes.honor.cobblemonmajesty.battle

import codes.honor.cobblemonmajesty.CobblemonMajesty
import codes.honor.cobblemonmajesty.Guard
import codes.honor.cobblemonmajesty.config.MajestyConfig
import codes.honor.cobblemonmajesty.feature.MajestyFeature
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.runner.ShowdownService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object MajestyShowdown {
    const val BOOTSTRAP_JS: String = """
(() => {
  if (globalThis.__cobblemonMajesty) return globalThis.__cobblemonMajesty;
  const multipliersByUuid = new Map();
  const uuidsByBattle = new Map();
  const majestyEffect = { id: 'cobblemonmajesty', name: 'Majesty', effectType: 'Rule' };
  const majestyCallback = function(damage, source, target, move) {
    if (!source || !move || !move.type || move.type === '???') return;
    const mult = multipliersByUuid.get(source.uuid);
    if (mult === undefined || mult === 1) return;
    const naturalTypes = source.getTypes(false, true);
    if (Array.isArray(naturalTypes) && naturalTypes.includes(move.type)) {
      this.chainModify(mult);
    }
  };
  const BattleProto = require('./sim/battle').Battle.prototype;
  const origFind = BattleProto.findBattleEventHandlers;
  BattleProto.findBattleEventHandlers = function(callbackName, getKey) {
    const handlers = origFind.call(this, callbackName, getKey);
    if (callbackName === 'onModifyDamage' && multipliersByUuid.size > 0) {
      handlers.push(this.resolvePriority({
        effect: majestyEffect,
        callback: majestyCallback,
        state: null,
        end: null,
        effectHolder: this
      }, callbackName));
    }
    return handlers;
  };
  globalThis.__cobblemonMajesty = {
    registerBattle(battleId, uuidsCsv, multiplier) {
      const id = String(battleId);
      this.clearBattle(id);
      const mult = Number(multiplier);
      if (!uuidsCsv || !(mult > 1)) return 0;
      const list = String(uuidsCsv).split(',').map(s => s.trim()).filter(Boolean);
      if (list.length === 0) return 0;
      uuidsByBattle.set(id, list);
      for (const u of list) multipliersByUuid.set(u, mult);
      return list.length;
    },
    clearBattle(battleId) {
      const id = String(battleId);
      const prev = uuidsByBattle.get(id);
      if (prev) {
        for (const u of prev) multipliersByUuid.delete(u);
        uuidsByBattle.delete(id);
      }
    },
    clearAll() {
      multipliersByUuid.clear();
      uuidsByBattle.clear();
    },
    activeCount() {
      return multipliersByUuid.size;
    }
  };
  return globalThis.__cobblemonMajesty;
})()
"""

    val activeBattles = ConcurrentHashMap<UUID, Set<UUID>>()
    val registeredBattlesCount = AtomicLong()

    private val startGuard = Guard("Majesty Showdown battle start")
    private val endGuard = Guard("Majesty Showdown battle end")

    fun register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.LOW) { event ->
            startGuard.run { onBattleStarting(event.battle) }
        }
        CobblemonEvents.BATTLE_ENDED.subscribe(Priority.NORMAL) { event ->
            endGuard.run { onBattleEnd(event.battle.battleId) }
        }
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL) { event ->
            endGuard.run { onBattleEnd(event.battle.battleId) }
        }
        CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL) { event ->
            endGuard.run { onBattleEnd(event.battle.battleId) }
        }
    }

    @JvmStatic
    fun onShowdownBoot(service: Any) {
        try {
            ensureBootstrapLoaded(service)
        } catch (e: Exception) {
            CobblemonMajesty.LOGGER.error("Failed to install Cobblemon Majesty Showdown hook on boot", e)
        }
    }

    @JvmStatic
    fun onBattleEnd(battleId: UUID) {
        activeBattles.remove(battleId) ?: return
        try {
            val bridge = ensureBootstrapLoaded(ShowdownService.service) ?: return
            invokePolyglotMember(bridge, "clearBattle", battleId.toString())
        } catch (e: Exception) {
            CobblemonMajesty.LOGGER.error("Failed to clear Cobblemon Majesty Showdown state for {}", battleId, e)
        }
    }

    fun clearAll() {
        activeBattles.clear()
        try {
            val bridge = ensureBootstrapLoaded(ShowdownService.service) ?: return
            invokePolyglotMember(bridge, "clearAll")
        } catch (_: Exception) {
        }
    }

    private fun onBattleStarting(battle: PokemonBattle) {
        if (!MajestyConfig.combat.damageBonusEnabled.get()) {
            onBattleEnd(battle.battleId)
            return
        }
        val multiplier = MajestyConfig.combat.damageMultiplier.get()
        if (multiplier <= 1.0) {
            onBattleEnd(battle.battleId)
            return
        }

        val majesticUuids = LinkedHashSet<UUID>()
        for (actor in battle.actors) {
            for (bp in actor.pokemonList) {
                if (MajestyFeature.isMajestic(bp.effectedPokemon) || MajestyFeature.isMajestic(bp.originalPokemon)) {
                    majesticUuids.add(bp.effectedPokemon.uuid)
                    majesticUuids.add(bp.originalPokemon.uuid)
                }
            }
        }
        if (majesticUuids.isEmpty()) return

        val bridge = ensureBootstrapLoaded(ShowdownService.service) ?: return
        val csv = majesticUuids.joinToString(",") { it.toString() }
        invokePolyglotMember(bridge, "registerBattle", battle.battleId.toString(), csv, multiplier)
        activeBattles[battle.battleId] = majesticUuids
        registeredBattlesCount.incrementAndGet()
    }

    private fun ensureBootstrapLoaded(service: Any): Any? {
        val contextField = service.javaClass.fields.firstOrNull { it.name == "context" }
            ?: service.javaClass.declaredFields.firstOrNull { it.name == "context" }?.apply { isAccessible = true }
            ?: return null
        val polyglotContext = contextField.get(service) ?: return null
        val evalMethod = polyglotContext.javaClass.methods.firstOrNull {
            it.name == "eval" && it.parameterCount == 2 &&
                it.parameterTypes[0] == String::class.java &&
                it.parameterTypes[1] == CharSequence::class.java
        } ?: return null
        return evalMethod.invoke(polyglotContext, "js", BOOTSTRAP_JS)
    }

    private fun invokePolyglotMember(receiver: Any, member: String, vararg args: Any): Any? {
        val invokeMethod = receiver.javaClass.methods.firstOrNull {
            it.name == "invokeMember" && it.parameterCount == 2 && it.parameterTypes[0] == String::class.java
        } ?: return null
        return invokeMethod.invoke(receiver, member, args)
    }
}
