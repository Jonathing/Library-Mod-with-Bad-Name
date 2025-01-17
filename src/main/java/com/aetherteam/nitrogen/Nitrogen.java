package com.aetherteam.nitrogen;

import com.aetherteam.nitrogen.api.menu.Menu;
import com.aetherteam.nitrogen.api.menu.Menus;
import com.aetherteam.nitrogen.api.users.UserData;
import com.aetherteam.nitrogen.api.users.User;
import com.aetherteam.nitrogen.data.generators.NitrogenLanguageData;
import com.aetherteam.nitrogen.network.PacketRelay;
import com.aetherteam.nitrogen.network.NitrogenPacketHandler;
import com.aetherteam.nitrogen.network.packet.clientbound.UpdateUserInfoPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Mod(Nitrogen.MODID)
@Mod.EventBusSubscriber(modid = Nitrogen.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Nitrogen {
    public static final String MODID = "aether_nitrogen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceKey<Registry<Menu>> MENU_REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Nitrogen.MODID, "menu"));

    /**
     * This is a test JavaDoc, please ignore.
     * @author bconlon
     */
    public Nitrogen() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::dataSetup);

        DeferredRegister<?>[] registers = {
                Menus.MENUS
        };

        for (DeferredRegister<?> register : registers) {
            register.register(modEventBus);
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NitrogenConfig.CLIENT_SPEC);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        NitrogenPacketHandler.register();
    }

    public void dataSetup(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        // Client Data
        generator.addProvider(event.includeClient(), new NitrogenLanguageData(packOutput));
    }

    @SubscribeEvent
    public static void serverAboutToStart(ServerStartingEvent event) {
        UserData.Server.initializeFromCache(event.getServer());
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            UUID uuid = serverPlayer.getGameProfile().getId();
            Map<UUID, User> userData = UserData.Server.getStoredUsers();
            User user;
            if (userData.containsKey(uuid)) {
                user = userData.get(uuid);
                if (user != null && user.getRenewalDate() != null && isAfterRenewalTime(user)) {
                    UserData.Server.sendUserRequest(serverPlayer.getServer(), serverPlayer, uuid);
                } else {
                    PacketRelay.sendToPlayer(NitrogenPacketHandler.INSTANCE, new UpdateUserInfoPacket(user), serverPlayer);
                }
            } else {
                UserData.Server.sendUserRequest(serverPlayer.getServer(), serverPlayer, uuid);
            }
        }
    }

    private static boolean isAfterRenewalTime(User user) {
        ZonedDateTime renewalDateTime = LocalDateTime.parse(user.getRenewalDate(), User.DATE_FORMAT).atZone(ZoneId.of("UTC"));
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        return currentDateTime.isAfter(renewalDateTime);
    }
}
