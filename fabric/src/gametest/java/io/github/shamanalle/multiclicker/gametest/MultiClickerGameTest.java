package io.github.shamanalle.multiclicker.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.config.Preset;
import io.github.shamanalle.multiclicker.gui.ConfigScreen;
import io.github.shamanalle.multiclicker.gui.ListEditScreen;
import io.github.shamanalle.multiclicker.gui.ProfilesScreen;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.clicker.ClickChannel;
import io.github.shamanalle.multiclicker.module.clicker.ClickerModule;
import io.github.shamanalle.multiclicker.module.visual.HighlightModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Plays MultiClicker in a real singleplayer world: every scenario sets up a situation, turns the
 * mod on and checks what actually happened in the game. Failures are collected so one run reports
 * every broken scenario; screenshots of the menus and the HUD are saved for visual review.
 */
public class MultiClickerGameTest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("MultiClicker/GameTest");

    private static final List<String> TRANSLATIONS = List.of(
            "uk_ua", "de_de", "fr_fr", "es_es", "pt_br", "pl_pl", "it_it", "tr_tr", "zh_cn", "ja_jp", "ko_kr");

    private final List<String> failures = new ArrayList<>();
    private final List<String> passed = new ArrayList<>();

    @FunctionalInterface
    private interface Scenario {
        void run(ClientGameTestContext context, TestServerContext server) throws Exception;
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        menuScreenshots(context, "title");

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            TestServerContext server = world.getServer();
            server.runCommand("time set noon");
            server.runCommand("tp @a 0.5 -60 0.5 0 0");
            TestCompat.waitForChunks(world);

            scenario(context, server, "attack waits for the cooldown and kills", this::attackCooldown);
            scenario(context, server, "target filter skips named mobs and armor stands", this::targetFilter);
            scenario(context, server, "looting finishing blow", this::lootingSwap);
            scenario(context, server, "auto eat eats without touching the chest in front", this::autoEat);
            scenario(context, server, "fighting and eating take turns", this::fightAndEat);
            scenario(context, server, "mining, auto tool and auto eat take turns", this::mineAndEat);
            scenario(context, server, "offhand priorities", this::offhand);
            scenario(context, server, "toggle sprint and toggle sneak stay intact", this::toggleKeys);
            scenario(context, server, "mining with auto tool, also without mouse focus", this::mining);
            scenario(context, server, "a screen pauses the clicker", this::screenPauses);
            scenario(context, server, "safety stops on low health", this::safety);
            scenario(context, server, "config, profiles and presets", this::config);
            scenario(context, server, "auto fish casts and catches", this::autoFish);
            scenario(context, server, "hud and highlight", this::hudScreenshots);

            reset(context, server);
            menuScreenshots(context, "world");
            context.getInput().resizeWindow(1920, 1080);
            context.waitTicks(2);
            menuScreenshots(context, "1080p");
            context.getInput().resizeWindow(640, 480);
            context.waitTicks(2);
            menuScreenshots(context, "640x480");
            context.getInput().resizeWindow(854, 480);
            context.waitTicks(2);

            // Russian strings are longer than English ones: check the layout with them too.
            setLanguage(context, "ru_ru");
            menuScreenshots(context, "ru");
            reset(context, server);
            hud(context, server, "ru_");
            deactivate(context);

            // The pictures on the project page, 1280x720 (GUI scale 3), in English and Russian.
            context.getInput().resizeWindow(1280, 720);
            context.waitTicks(2);
            readmeScreenshots(context, server, "ru");
            setLanguage(context, "en_us");
            readmeScreenshots(context, server, "en");
            context.getInput().resizeWindow(854, 480);
            context.waitTicks(2);

            // Every other translation: the densest pages, to catch strings that do not fit. Menus only:
            // a dozen resource reloads already strain the software renderer of the CI machine.
            for (String code : TRANSLATIONS) {
                setLanguage(context, code);
                languageScreenshots(context, code);
            }
            context.setScreen(() -> null);
        }
        setLanguage(context, "en_us");

        LOGGER.info("MultiClicker game tests: {} passed, {} failed", passed.size(), failures.size());
        passed.forEach(name -> LOGGER.info("[PASS] {}", name));
        failures.forEach(failure -> LOGGER.error("[FAIL] {}", failure));
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.size() + " MultiClicker scenario(s) failed:\n - " + String.join("\n - ", failures));
        }
    }

    private void scenario(ClientGameTestContext context, TestServerContext server, String name, Scenario scenario) {
        LOGGER.info("[RUN ] {}", name);
        try {
            reset(context, server);
            scenario.run(context, server);
            passed.add(name);
            LOGGER.info("[PASS] {}", name);
        } catch (Throwable t) {
            failures.add(name + ": " + t.getMessage());
            LOGGER.error("[FAIL] {}", name, t);
            context.takeScreenshot("fail_" + name.replaceAll("[^a-z]+", "_"));
        } finally {
            context.runOnClient(mc -> MultiClicker.get().setActive(false, null));
        }
    }

    // --- Scenarios ------------------------------------------------------------------------------

    private void attackCooldown(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.IRON_SWORD));
        server.runCommand("summon minecraft:husk 0.5 -60 2.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
        context.waitTicks(5);
        check(context.computeOnClient(mc -> mc.hitResult instanceof EntityHitResult hit && hit.getEntity().getType() == EntityType.HUSK),
                "the husk is not under the crosshair, the scenario setup is wrong");

        activate(context);
        waitUntil(context, "the husk to die", 300, mc -> husk(mc) == null);
        int attacks = context.computeOnClient(mc -> MultiClicker.get().stats().attacks());
        // An iron sword needs 4 fully charged hits; spam clicking without the cooldown would need dozens.
        check(attacks >= 3 && attacks <= 6, "expected 3-6 charged hits, got " + attacks);
        context.waitTicks(5);
        check(context.computeOnClient(mc -> MultiClicker.get().stats().kills()) == 1, "the kill was not counted");
    }

    private void targetFilter(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.IRON_SWORD));
        server.runCommand("summon minecraft:husk 0.5 -60 2.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,CustomName:\"Bob\"}");
        context.waitTicks(5);
        activate(context);
        context.waitTicks(40);
        check(context.computeOnClient(mc -> MultiClicker.get().stats().attacks()) == 0, "attacked a named mob");
        deactivate(context);

        server.runCommand("kill @e[type=!minecraft:player]");
        server.runCommand("summon minecraft:armor_stand 0.5 -60 2.5 {NoGravity:1b}");
        context.waitTicks(5);
        activate(context);
        context.waitTicks(40);
        check(context.computeOnClient(mc -> MultiClicker.get().stats().attacks()) == 0, "attacked an armor stand");
    }

    private void lootingSwap(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.WOODEN_SWORD));
        give(server, 1, enchanted(server, Items.STONE_SWORD, Enchantments.LOOTING, 3));
        server.runCommand("summon minecraft:husk 0.5 -60 2.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:3f}");
        context.waitTicks(5);
        context.runOnClient(mc -> MultiClicker.get().clicker().lootingSwap.set(true));

        activate(context);
        boolean[] usedLootingSlot = {false};
        waitUntil(context, "the husk to die", 100, mc -> {
            usedLootingSlot[0] |= Slots.selected(mc.player.getInventory()) == 1;
            return husk(mc) == null;
        });
        check(usedLootingSlot[0], "the Looting sword was never selected");
        waitUntil(context, "the original slot to be selected again", 20,
                mc -> Slots.selected(mc.player.getInventory()) == 0);
        check(context.computeOnClient(mc -> MultiClicker.get().stats().attacks()) == 1, "expected a single finishing blow");
    }

    private void autoEat(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.IRON_SWORD));
        give(server, 4, new ItemStack(Items.BREAD, 4));
        server.runCommand("setblock 0 -60 1 minecraft:chest");
        server.runCommand("tp @a 0.5 -60 0.5 0 45");
        server.runOnServer(s -> player(s).getFoodData().setFoodLevel(6));
        context.waitTicks(5);
        check(context.computeOnClient(mc -> mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK),
                "the chest is not under the crosshair, the scenario setup is wrong");
        context.runOnClient(mc -> MultiClicker.get().autoEat().enabledSetting().set(true));

        activate(context);
        boolean[] openedContainer = {false};
        waitUntil(context, "the player to eat", 120, mc -> {
            openedContainer[0] |= mc.screen instanceof AbstractContainerScreen<?>;
            return mc.player.getFoodData().getFoodLevel() > 6 && !MultiClicker.get().autoEat().isBusy();
        });
        check(!openedContainer[0], "eating opened the chest in front of the player");
        check(context.computeOnClient(mc -> Slots.selected(mc.player.getInventory())) == 0, "the sword slot was not selected again");
        check(context.computeOnClient(mc -> mc.player.getInventory().getItem(4).getCount()) < 4, "no bread was eaten");
    }

    private void fightAndEat(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.WOODEN_SWORD));
        give(server, 5, new ItemStack(Items.COOKED_BEEF, 4));
        server.runCommand("summon minecraft:husk 0.5 -60 2.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1024f,attributes:[{id:\"minecraft:max_health\",base:1024}]}");
        server.runOnServer(s -> player(s).getFoodData().setFoodLevel(6));
        context.runOnClient(mc -> MultiClicker.get().autoEat().enabledSetting().set(true));
        context.waitTicks(5);

        activate(context);
        int[] lastAttacks = {0};
        String[] badAttack = {null};
        waitUntil(context, "the player to eat while fighting", 200, mc -> {
            int attacks = MultiClicker.get().stats().attacks();
            if (attacks > lastAttacks[0] && !mc.player.getMainHandItem().is(Items.WOODEN_SWORD)) {
                badAttack[0] = mc.player.getMainHandItem().toString();
            }
            lastAttacks[0] = attacks;
            return mc.player.getFoodData().getFoodLevel() > 14;
        });
        check(badAttack[0] == null, "attacked while holding " + badAttack[0]);
        int attacksAfterEating = context.computeOnClient(mc -> MultiClicker.get().stats().attacks());
        waitUntil(context, "attacks to continue after eating", 60,
                mc -> MultiClicker.get().stats().attacks() > attacksAfterEating);
        check(context.computeOnClient(mc -> Slots.selected(mc.player.getInventory())) == 0, "the sword is not selected");
    }

    private void mineAndEat(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.WOODEN_SWORD));
        give(server, 2, new ItemStack(Items.STONE_PICKAXE));
        give(server, 5, new ItemStack(Items.BREAD, 4));
        server.runCommand("fill 0 -60 2 0 -59 5 minecraft:stone");
        server.runOnServer(s -> player(s).getFoodData().setFoodLevel(6));
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.clicker().attack.mode.set(ClickChannel.Mode.HOLD);
            mod.clicker().attackTarget.set(ClickerModule.AttackTarget.ENTITIES_AND_BLOCKS);
            mod.autoTool().enabledSetting().set(true);
            mod.autoEat().enabledSetting().set(true);
        });
        context.waitTicks(3);

        activate(context);
        waitUntil(context, "the player to eat while mining", 200, mc -> mc.player.getFoodData().getFoodLevel() > 14);
        waitUntil(context, "the pickaxe to be selected again", 40, mc -> mc.player.getMainHandItem().is(Items.STONE_PICKAXE));
        int broken = context.computeOnClient(mc -> countAir(mc, 0, -59, 2, 5));
        waitUntil(context, "mining to continue after eating", 100, mc -> countAir(mc, 0, -59, 2, 5) > broken);
    }

    private void offhand(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.IRON_PICKAXE));
        give(server, 3, new ItemStack(Items.BREAD, 8));
        give(server, 20, new ItemStack(Items.TOTEM_OF_UNDYING));
        server.runOnServer(s -> player(s).getFoodData().setFoodLevel(10));
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.offhand().enabledSetting().set(true);
            mod.offhand().totemHealth.set(0);
            mod.offhand().foodHunger.set(19);
            mod.autoEat().enabledSetting().set(true);
            mod.autoEat().hunger.set(1); // enabled, but not hungry enough to eat now
            mod.clicker().attack.enabled.set(false);
        });
        activate(context);
        context.waitTicks(30);
        check(context.computeOnClient(mc -> mc.player.getOffhandItem().isEmpty()),
                "took the hotbar food that auto eat needs");

        give(server, 15, new ItemStack(Items.COOKED_BEEF, 4));
        waitUntil(context, "cooked beef in the offhand", 40, mc -> mc.player.getOffhandItem().is(Items.COOKED_BEEF));

        context.runOnClient(mc -> MultiClicker.get().offhand().totemHealth.set(100));
        waitUntil(context, "the totem in the offhand", 40, mc -> mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING));
        check(context.computeOnClient(mc -> mc.player.getMainHandItem().is(Items.IRON_PICKAXE)), "the held item changed");
    }

    private void toggleKeys(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(mc -> {
            mc.options.toggleSprint().set(true);
            mc.options.toggleCrouch().set(true);
            MultiClicker mod = MultiClicker.get();
            mod.clicker().attack.enabled.set(false);
            mod.autoWalk().enabledSetting().set(true);
            mod.autoWalk().sprint.set(true);
            mod.autoWalk().jumpObstacles.set(false);
        });
        activate(context);
        int[] sprintKeyUp = {0};
        for (int i = 0; i < 30; i++) {
            context.waitTick();
            if (!context.computeOnClient(mc -> mc.options.keySprint.isDown() && mc.options.keyUp.isDown())) {
                sprintKeyUp[0]++;
            }
        }
        check(sprintKeyUp[0] == 0, "sprint/forward was released on " + sprintKeyUp[0] + " of 30 ticks");
        check(context.computeOnClient(mc -> mc.player.isSprinting()), "the player is not sprinting");
        deactivate(context);
        context.waitTicks(2);
        check(context.computeOnClient(mc -> !mc.options.keySprint.isDown() && !mc.options.keyUp.isDown()),
                "sprint or forward stayed pressed after stopping");

        // The player's own toggled sprint must survive the mod.
        context.runOnClient(mc -> mc.options.keySprint.setDown(true));
        check(context.computeOnClient(mc -> mc.options.keySprint.isDown()), "could not toggle sprint on");
        activate(context);
        context.waitTicks(10);
        deactivate(context);
        context.waitTicks(2);
        check(context.computeOnClient(mc -> mc.options.keySprint.isDown()), "the player's toggled sprint was lost");
        context.runOnClient(mc -> mc.options.keySprint.setDown(true)); // toggle off again

        // Anti-AFK sneak with toggle sneak must not leave the player sneaking.
        server.runCommand("tp @a 0.5 -60 0.5 0 0");
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.autoWalk().enabledSetting().set(false);
            mod.antiAfk().enabledSetting().set(true);
            mod.antiAfk().minInterval.set(5);
            mod.antiAfk().maxInterval.set(5);
            mod.antiAfk().jump.set(false);
            mod.antiAfk().swing.set(false);
            mod.antiAfk().sneak.set(true);
        });
        activate(context);
        waitUntil(context, "the anti-AFK sneak", 160, mc -> mc.player.isShiftKeyDown());
        context.waitTicks(20);
        check(context.computeOnClient(mc -> !mc.options.keyShift.isDown() && !mc.player.isShiftKeyDown()),
                "the player kept sneaking after the anti-AFK action");
    }

    private void mining(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, new ItemStack(Items.IRON_SWORD));
        give(server, 2, new ItemStack(Items.IRON_PICKAXE));
        give(server, 3, new ItemStack(Items.IRON_SHOVEL));
        server.runCommand("setblock 0 -59 2 minecraft:stone");
        server.runCommand("setblock 0 -60 2 minecraft:stone");
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.clicker().attack.mode.set(ClickChannel.Mode.HOLD);
            mod.clicker().attackTarget.set(ClickerModule.AttackTarget.ENTITIES_AND_BLOCKS);
            mod.autoTool().enabledSetting().set(true);
        });
        context.waitTicks(3);

        activate(context);
        boolean[] usedPickaxe = {false};
        waitUntil(context, "the upper stone to break", 100, mc -> {
            usedPickaxe[0] |= mc.player.getMainHandItem().is(Items.IRON_PICKAXE);
            return mc.level.getBlockState(new BlockPos(0, -59, 2)).isAir();
        });
        check(usedPickaxe[0], "auto tool did not pick the pickaxe");
        waitUntil(context, "auto tool to switch back to the sword", 40, mc -> Slots.selected(mc.player.getInventory()) == 0);

        // Without mouse focus (game window in the background) held mining must keep working.
        context.runOnClient(mc -> mc.mouseHandler.releaseMouse());
        server.runCommand("tp @a 0.5 -60 0.5 0 30");
        waitUntil(context, "the lower stone to break without mouse focus", 100,
                mc -> mc.level.getBlockState(new BlockPos(0, -60, 2)).isAir());
    }

    private void screenPauses(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("setblock 0 -59 2 minecraft:obsidian");
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.clicker().attack.mode.set(ClickChannel.Mode.HOLD);
            mod.clicker().attackTarget.set(ClickerModule.AttackTarget.ENTITIES_AND_BLOCKS);
        });
        context.waitTicks(3);
        activate(context);
        waitUntil(context, "the attack key to be held", 20, mc -> mc.options.keyAttack.isDown());
        context.setScreen(() -> new ConfigScreen(null));
        context.waitTicks(3);
        check(context.computeOnClient(mc -> !mc.options.keyAttack.isDown()), "the attack key stayed down behind a screen");
        context.setScreen(() -> null);
        waitUntil(context, "the attack key to be held again", 20, mc -> mc.options.keyAttack.isDown());
    }

    private void safety(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.safety().enabledSetting().set(true);
            mod.safety().health.set(30);
        });
        activate(context);
        context.waitTicks(5);
        check(context.computeOnClient(mc -> MultiClicker.get().isActive()), "safety stopped the mod at full health");
        server.runOnServer(s -> player(s).setHealth(4));
        waitUntil(context, "safety to stop the mod", 20, mc -> !MultiClicker.get().isActive());
    }

    private void config(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.clicker().attack.interval.set(7);
            mod.inventoryCleaner().items.add("minecraft:dirt");
            mod.saveConfig();
            mod.config().resetAll();
            check(mod.clicker().attack.interval.get() == 1, "reset did not restore the default interval");
            mod.config().load();
            check(mod.clicker().attack.interval.get() == 7, "the saved interval was not loaded");
            check(mod.inventoryCleaner().items.get().contains("minecraft:dirt"), "the saved list was not loaded");

            check(mod.config().saveProfile("GameTest"), "saving a profile failed");
            mod.config().resetAll();
            check(mod.config().loadProfile("GameTest"), "loading the profile failed");
            check(mod.clicker().attack.interval.get() == 7, "the profile did not restore the interval");
            mod.config().deleteProfile("GameTest");
            check(!mod.config().profiles().contains("GameTest"), "the profile was not deleted");

            Preset.MINING.apply(mod);
            check(mod.clicker().attack.mode.get() == ClickChannel.Mode.HOLD && mod.autoTool().isEnabled(),
                    "the mining preset was not applied");
            mod.config().resetAll();
            mod.saveConfig();
        });
    }

    private void autoFish(ClientGameTestContext context, TestServerContext server) {
        give(server, 0, enchanted(server, Items.FISHING_ROD, Enchantments.LURE, 3));
        server.runCommand("fill -4 -63 2 4 -61 12 minecraft:water");
        server.runCommand("tp @a 0.5 -60 0.5 0 20");
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.clicker().attack.enabled.set(false);
            mod.autoFish().enabledSetting().set(true);
        });
        context.waitTicks(5);
        activate(context);
        waitUntil(context, "the rod to be cast", 60, mc -> mc.player.fishing != null);
        waitUntil(context, "a catch", 1400, mc -> countItems(mc, stack -> !stack.is(Items.FISHING_ROD)) > 0);
        waitUntil(context, "the rod to be cast again", 120, mc -> mc.player.fishing != null);
    }

    private void hudScreenshots(ClientGameTestContext context, TestServerContext server) {
        hud(context, server, "");
    }

    private void hud(ClientGameTestContext context, TestServerContext server, String prefix) {
        // Bare hands: the husk survives long enough for the screenshots.
        give(server, 1, new ItemStack(Items.TOTEM_OF_UNDYING, 2));
        server.runCommand("summon minecraft:husk 0.5 -60 3.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.autoEat().enabledSetting().set(true);
            mod.offhand().enabledSetting().set(true);
            mod.antiAfk().enabledSetting().set(true);
        });
        context.waitTicks(5);
        context.takeScreenshot(prefix + "hud_inactive");
        activate(context);
        context.waitTicks(30);
        context.takeScreenshot(prefix + "hud_active_outline");
        context.runOnClient(mc -> MultiClicker.get().highlight().style.set(HighlightModule.Style.GLOW));
        context.waitTicks(5);
        context.takeScreenshot(prefix + "hud_active_glow");
        context.runOnClient(mc -> MultiClicker.get().highlight().style.set(HighlightModule.Style.FILLED));
        context.waitTicks(5);
        context.takeScreenshot(prefix + "hud_active_filled");
    }

    private void languageScreenshots(ClientGameTestContext context, String code) {
        for (Category category : List.of(Category.CLICKER, Category.SURVIVAL, Category.AUTOMATION)) {
            context.setScreen(() -> new ConfigScreen(null));
            context.runOnClient(mc -> ((ConfigScreen) mc.screen).showCategory(category));
            parkCursor(context);
            context.takeScreenshot("lang_" + code + "_" + category.name().toLowerCase());
        }
        context.setScreen(() -> new ProfilesScreen(new ConfigScreen(null)));
        parkCursor(context);
        context.takeScreenshot("lang_" + code + "_profiles");
    }

    // --- Project page -----------------------------------------------------------------------------

    /** A small mob farm: a few kills on the HUD, the next target highlighted, then the menus over it. */
    private void readmeScreenshots(ClientGameTestContext context, TestServerContext server, String lang) {
        reset(context, server);
        server.runCommand("time set 2000");
        server.runCommand("fill -4 -61 1 4 -61 9 minecraft:polished_andesite");
        server.runCommand("fill -4 -60 9 4 -57 9 minecraft:stone_bricks");
        server.runCommand("fill -4 -60 2 -4 -57 8 minecraft:stone_bricks");
        server.runCommand("fill 4 -60 2 4 -57 8 minecraft:stone_bricks");
        server.runCommand("setblock -4 -56 2 minecraft:lantern");
        server.runCommand("setblock 4 -56 2 minecraft:lantern");
        server.runCommand("summon minecraft:creeper -1.5 -60 7.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[160f,0f]}");
        server.runCommand("summon minecraft:husk 2.0 -60 6.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[200f,0f]}");
        give(server, 0, enchanted(server, Items.DIAMOND_SWORD, Enchantments.LOOTING, 3));
        give(server, 1, new ItemStack(Items.COOKED_BEEF, 32));
        give(server, 2, enchanted(server, Items.DIAMOND_PICKAXE, Enchantments.EFFICIENCY, 5));
        give(server, 8, new ItemStack(Items.TOTEM_OF_UNDYING));
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.autoEat().enabledSetting().set(true);
            mod.offhand().enabledSetting().set(true);
            mod.antiAfk().enabledSetting().set(true);
            mod.highlight().style.set(HighlightModule.Style.GLOW);
            mod.mining().blocks.set(List.of("minecraft:stone", "minecraft:deepslate", "minecraft:cobblestone",
                    "minecraft:andesite", "minecraft:diorite", "minecraft:granite", "minecraft:tuff"));
        });
        server.runCommand("tp @a 0.5 -60 0.5 0 12");
        context.waitTicks(10);

        activate(context);
        for (int i = 1; i <= 3; i++) {
            server.runCommand("summon minecraft:husk 0.5 -60 3.0 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1f,Rotation:[180f,0f]}");
            int kills = i;
            waitUntil(context, "kill number " + kills, 100, mc -> MultiClicker.get().stats().kills() >= kills);
        }
        // The last husk cannot be hurt, so it stays the highlighted target.
        server.runCommand("summon minecraft:husk 0.5 -60 3.0 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
        server.runCommand("effect give @e[type=minecraft:husk] minecraft:resistance infinite 4 true");
        context.waitTicks(70);
        context.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        // Between two hits, so the husk does not flash red.
        waitUntil(context, "the sword to be charged again", 100, mc -> {
            float charge = mc.player.getAttackStrengthScale(0);
            return charge > 0.85F && charge < 0.98F;
        });
        context.takeScreenshot("readme_" + lang + "_hud");

        for (Category category : List.of(Category.CLICKER, Category.SURVIVAL, Category.VISUAL)) {
            context.setScreen(() -> new ConfigScreen(null));
            context.runOnClient(mc -> ((ConfigScreen) mc.screen).showCategory(category));
            parkCursor(context);
            context.takeScreenshot("readme_" + lang + "_menu_" + category.name().toLowerCase());
        }
        List<String> profiles = lang.equals("ru")
                ? List.of("Железная ферма", "Рыбалка на ночь")
                : List.of("Iron farm", "Night fishing");
        context.runOnClient(mc -> profiles.forEach(MultiClicker.get().config()::saveProfile));
        context.setScreen(() -> new ProfilesScreen(new ConfigScreen(null)));
        parkCursor(context);
        context.takeScreenshot("readme_" + lang + "_profiles");
        context.runOnClient(mc -> profiles.forEach(MultiClicker.get().config()::deleteProfile));
        context.setScreen(() -> new ListEditScreen(new ConfigScreen(null), MultiClicker.get().mining().blocks));
        parkCursor(context);
        context.takeScreenshot("readme_" + lang + "_list_editor");
        context.setScreen(() -> null);
        deactivate(context);
    }

    // --- Menus ----------------------------------------------------------------------------------

    private void menuScreenshots(ClientGameTestContext context, String prefix) {
        for (Category category : Category.values()) {
            context.setScreen(() -> new ConfigScreen(null));
            context.runOnClient(mc -> ((ConfigScreen) mc.screen).showCategory(category));
            parkCursor(context);
            context.takeScreenshot(prefix + "_menu_" + category.name().toLowerCase());
        }
        context.runOnClient(mc -> ((ConfigScreen) mc.screen).search("eat"));
        context.waitTicks(8);
        context.takeScreenshot(prefix + "_menu_search");
        context.setScreen(() -> new ProfilesScreen(new ConfigScreen(null)));
        parkCursor(context);
        context.takeScreenshot(prefix + "_profiles");
        context.setScreen(() -> new ListEditScreen(new ConfigScreen(null), MultiClicker.get().inventoryCleaner().items));
        parkCursor(context);
        context.takeScreenshot(prefix + "_list_editor");
        context.setScreen(() -> null);
        context.waitTicks(2);
    }

    /** Opening a screen centers the cursor; move it to a corner so no tooltip covers the screenshot. */
    private static void parkCursor(ClientGameTestContext context) {
        context.getInput().setCursorPos(1, 1);
        context.waitTicks(8);
    }

    // --- Helpers --------------------------------------------------------------------------------

    private static void reset(ClientGameTestContext context, TestServerContext server) {
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            MultiClicker mod = MultiClicker.get();
            mod.setActive(false, null);
            mod.config().resetAll();
            mc.options.toggleSprint().set(false);
            mc.options.toggleCrouch().set(false);
            mc.options.keySprint.setDown(false);
            mc.options.keyShift.setDown(false);
            mc.mouseHandler.grabMouse();
        });
        server.runCommand("kill @e[type=!minecraft:player]");
        server.runCommand("clear @a");
        server.runCommand("effect clear @a");
        server.runCommand("gamemode survival @a");
        server.runCommand("fill -5 -60 -3 5 -55 14 minecraft:air");
        server.runCommand("fill -5 -63 -3 5 -62 14 minecraft:dirt");
        server.runCommand("fill -5 -61 -3 5 -61 14 minecraft:grass_block");
        server.runCommand("kill @e[type=minecraft:item]");
        server.runOnServer(s -> {
            ServerPlayer player = player(s);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5);
        });
        server.runCommand("tp @a 0.5 -60 0.5 0 0");
        context.runOnClient(mc -> Slots.select(mc.player.getInventory(), 0));
        context.waitTicks(5);
    }

    private static void setLanguage(ClientGameTestContext context, String code) {
        context.runOnClient(mc -> {
            mc.options.languageCode = code;
            mc.getLanguageManager().setSelected(code);
            mc.reloadResourcePacks();
        });
        context.waitFor(mc -> mc.getOverlay() == null, 1200);
        context.waitTicks(2);
    }

    private static void activate(ClientGameTestContext context) {
        context.runOnClient(mc -> MultiClicker.get().setActive(true, null));
        check(context.computeOnClient(mc -> MultiClicker.get().isActive()), "the mod did not turn on");
    }

    private static void deactivate(ClientGameTestContext context) {
        context.runOnClient(mc -> MultiClicker.get().setActive(false, null));
    }

    private static void waitUntil(ClientGameTestContext context, String what, int maxTicks, Predicate<Minecraft> condition) {
        for (int tick = 0; tick < maxTicks; tick++) {
            if (context.computeOnClient(condition::test)) {
                return;
            }
            context.waitTick();
        }
        throw new AssertionError("timed out after " + maxTicks + " ticks waiting for " + what);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void give(TestServerContext server, int slot, ItemStack stack) {
        server.runOnServer(s -> player(s).getInventory().setItem(slot, stack.copy()));
    }

    private static ItemStack enchanted(TestServerContext server, Item item, ResourceKey<Enchantment> enchantment, int level) {
        return server.computeOnServer(s -> {
            Holder<Enchantment> holder = s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
            ItemStack stack = new ItemStack(item);
            stack.enchant(holder, level);
            return stack;
        });
    }

    private static Entity husk(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getType() == EntityType.HUSK && entity instanceof LivingEntity husk && husk.isAlive() && !husk.isDeadOrDying()) {
                return husk;
            }
        }
        return null;
    }

    private static int countAir(Minecraft mc, int x, int y, int fromZ, int toZ) {
        int air = 0;
        for (int z = fromZ; z <= toZ; z++) {
            if (mc.level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                air++;
            }
        }
        return air;
    }

    private static int countItems(Minecraft mc, Predicate<ItemStack> filter) {
        int count = 0;
        for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && filter.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

}
