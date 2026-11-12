package dev.mecharsenal.client;

import dev.letitwalk.client.PilotInput;
import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.network.PilotStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = MechArsenal.MOD_ID, value = Dist.CLIENT)
public final class PilotHudOverlay {
    private static final int CYAN = 0xFF75F5C4;
    private static final int AMBER = 0xFFFFC268;
    private static final int DIM = 0xFF9AB6AC;
    private static final int PANEL = 0xD0081517;
    private static PilotStatusPayload status;
    private static float recoil;
    private static boolean hudEnabled = true;
    private static boolean piloting;

    private PilotHudOverlay() {
    }

    public static void updateStatus(PilotStatusPayload current) {
        status = current;
    }

    public static void applyRecoil(float strength) {
        recoil = Math.min(7.0F, recoil + strength);
    }

    public static void toggleHud() {
        hudEnabled = !hudEnabled;
    }

    public static void setPilotActive(boolean active) {
        if (active != piloting) {
            status = null;
        }
        piloting = active;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        recoil *= 0.72F;
        if (recoil < 0.05F) {
            recoil = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (PilotInput.isActive() && recoil > 0.0F) {
            event.setPitch(event.getPitch() - recoil);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (PilotInput.isActive() && hudEnabled && VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PilotInput.isActive() || minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (hasWeapons()) {
            graphics.fill(0, 0, width, height, 0x2200D88B);
        }
        if (!hudEnabled) {
            return;
        }

        drawFrame(graphics, width, height);
        drawStatusPanel(graphics, minecraft, height);
        drawReticle(graphics, width / 2, height / 2 - Math.round(recoil * 2.0F));
        graphics.drawString(minecraft.font, Component.translatable("hud.mech_arsenal.toggle_hint"),
                12, height - 14, DIM, false);
    }

    private static boolean hasWeapons() {
        return status != null && status.autocannons() + status.heavyCannons() > 0;
    }

    private static void drawStatusPanel(GuiGraphics graphics, Minecraft minecraft, int height) {
        int x = 12;
        int y = 12;
        graphics.fill(x, y, x + 172, y + 52, PANEL);
        graphics.fill(x, y + 4, x + 2, y + 47, CYAN);
        graphics.drawString(minecraft.font, Component.translatable("hud.mech_arsenal.title"),
                x + 9, y + 5, CYAN, false);
        graphics.fill(x + 9, y + 16, x + 164, y + 17, 0x8059A991);

        int autocannons = status == null ? 0 : status.autocannons();
        int heavyCannons = status == null ? 0 : status.heavyCannons();
        int autocannonRounds = status == null ? 0 : status.autocannonRounds();
        int heavyShells = status == null ? 0 : status.heavyShells();

        drawAutocannonIcon(graphics, x + 10, y + 23, CYAN);
        graphics.drawString(minecraft.font, "AC", x + 27, y + 22, DIM, false);
        graphics.drawString(minecraft.font, Integer.toString(autocannons), x + 45, y + 22, 0xFFFFFFFF, false);
        graphics.fill(x + 67, y + 21, x + 68, y + 33, 0x704D7468);
        drawHeavyCannonIcon(graphics, x + 74, y + 23, AMBER);
        graphics.drawString(minecraft.font, "120", x + 92, y + 22, DIM, false);
        graphics.drawString(minecraft.font, Integer.toString(heavyCannons), x + 116, y + 22, 0xFFFFFFFF, false);

        drawShellIcon(graphics, x + 11, y + 37, CYAN);
        graphics.drawString(minecraft.font, Integer.toString(autocannonRounds), x + 23, y + 35, 0xFFD8E8DF, false);
        drawShellIcon(graphics, x + 75, y + 37, AMBER);
        graphics.drawString(minecraft.font, Integer.toString(heavyShells), x + 87, y + 35, 0xFFD8E8DF, false);
        if (status == null) {
            graphics.drawString(minecraft.font, Component.translatable("hud.mech_arsenal.syncing"),
                    12, height - 26, DIM, false);
        }
    }

    private static void drawAutocannonIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y + 4, x + 10, y + 8, color);
        graphics.fill(x + 4, y + 1, x + 8, y + 4, color);
        graphics.fill(x + 9, y + 5, x + 14, y + 7, color);
        graphics.fill(x + 3, y + 8, x + 9, y + 10, DIM);
    }

    private static void drawHeavyCannonIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y + 3, x + 9, y + 9, color);
        graphics.fill(x + 3, y + 1, x + 8, y + 3, color);
        graphics.fill(x + 8, y + 4, x + 15, y + 8, color);
        graphics.fill(x + 3, y + 9, x + 9, y + 11, DIM);
    }

    private static void drawShellIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 2, y + 1, x + 5, y + 2, color);
        graphics.fill(x + 1, y + 3, x + 6, y + 7, color);
        graphics.fill(x + 2, y + 8, x + 5, y + 9, color);
        graphics.fill(x + 3, y + 3, x + 4, y + 7, 0xFF293B38);
    }

    private static void drawFrame(GuiGraphics graphics, int width, int height) {
        int x = width / 2;
        int y = height / 2;
        for (int[] point : new int[][]{
                {x - 12, y - 12}, {x + 9, y - 12}, {x - 12, y + 9}, {x + 9, y + 9}
        }) {
            boolean right = point[0] > x;
            boolean bottom = point[1] > y;
            int xEnd = point[0] + (right ? -4 : 4);
            int yEnd = point[1] + (bottom ? -4 : 4);
            graphics.fill(Math.min(point[0], xEnd), point[1], Math.max(point[0], xEnd) + 1,
                    point[1] + 1, CYAN);
            graphics.fill(point[0], Math.min(point[1], yEnd), point[0] + 1,
                    Math.max(point[1], yEnd) + 1, CYAN);
        }
        graphics.fill(x - 1, y - 1, x + 2, y + 2, AMBER);
        graphics.fill(x - 1, y - 17, x + 2, y - 14, DIM);
        graphics.fill(x - 1, y + 14, x + 2, y + 17, DIM);
    }

    private static void drawReticle(GuiGraphics graphics, int centerX, int centerY) {
        graphics.fill(centerX - 1, centerY - 5, centerX + 2, centerY - 3, CYAN);
        graphics.fill(centerX - 1, centerY + 3, centerX + 2, centerY + 5, CYAN);
        graphics.fill(centerX - 5, centerY - 1, centerX - 3, centerY + 2, CYAN);
        graphics.fill(centerX + 3, centerY - 1, centerX + 5, centerY + 2, CYAN);
    }
}
