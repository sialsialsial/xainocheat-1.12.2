package com.github.sialsialsial.xainocheat;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.Items;
import net.minecraft.item.ItemElytra;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends GuiScreen {

    // Module
    public static class Module {
        public final String name;
        public final Category category;
        private boolean enabled;

        public Module(String name, Category category) {
            this.name = name;
            this.category = category;
            this.enabled = false;
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean e) { this.enabled = e; }
        public void toggle() { this.enabled = !this.enabled; }
    }

    // Category
    public enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        RENDER("Render"),
        WORLD("World"); // Added: new category (panel), specialy for scaffold

        public final String title;
        Category(String title) { this.title = title; }
    }

    // ModuleManager
    public static class ModuleManager {
        private static final List<Module> modules = new ArrayList<>();

        public static void init() {
            if (!modules.isEmpty()) return;
            add(new Module("KillAura", Category.COMBAT));
            add(new Module("AutoTotem", Category.COMBAT));

            add(new Module("JumpHack", Category.MOVEMENT));
            add(new Module("Fly", Category.MOVEMENT));
            add(new Module("NoFall", Category.MOVEMENT));
            add(new Module("ElytraFly", Category.MOVEMENT));

            add(new Module("ESP", Category.RENDER));
            add(new Module("FullBright", Category.RENDER));

            add(new Module("Scaffold", Category.WORLD));
        }

        public static void add(Module m) { modules.add(m); }
        public static List<Module> getAll() { return modules; }

        public static List<Module> getByCategory(Category c) {
            List<Module> r = new ArrayList<>();
            for (Module m : modules) if (m.category == c) r.add(m);
            return r;
        }

        public static Module getByName(String name) {
            for (Module m : modules) if (m.name.equals(name)) return m;
            return null;
        }
    }

    // Panel
    private static class Panel {
        final Category category;
        int x, y;
        boolean dragging;
        int dragOffsetX, dragOffsetY;
        boolean expanded = true;

        Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }
    }

    private final List<Panel> panels = new ArrayList<>();

    private static final int PANEL_WIDTH = 110;
    private static final int TITLE_HEIGHT = 16;
    private static final int ROW_HEIGHT = 14;

    @Override
    public void initGui() {
        panels.clear();
        int x = 20;
        for (Category c : Category.values()) {
            panels.add(new Panel(c, x, 40));
            x += PANEL_WIDTH + 10;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x66000000);
        for (Panel p : panels) drawPanel(p, mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(Panel panel, int mouseX, int mouseY) {
        boolean hoverTitle = isHovered(mouseX, mouseY, panel.x, panel.y, PANEL_WIDTH, TITLE_HEIGHT);
        int titleColor = panel.dragging ? 0xFF3A3A3A : (hoverTitle ? 0xFF2E2E2E : 0xFF1F1F1F);
        drawRect(panel.x, panel.y, panel.x + PANEL_WIDTH, panel.y + TITLE_HEIGHT, titleColor);

        fontRenderer.drawStringWithShadow(panel.category.title, panel.x + 5, panel.y + 4, 0xFFFFFFFF);
        fontRenderer.drawString(panel.expanded ? "-" : "+",
                panel.x + PANEL_WIDTH - 12, panel.y + 4, 0xFFAAAAAA);

        int y = panel.y + TITLE_HEIGHT;

        if (panel.expanded) {
            for (Module m : ModuleManager.getByCategory(panel.category)) {
                boolean hover = isHovered(mouseX, mouseY, panel.x, y, PANEL_WIDTH, ROW_HEIGHT);
                drawRect(panel.x, y, panel.x + PANEL_WIDTH, y + ROW_HEIGHT,
                        hover ? 0xFF2A2A2A : 0xFF1A1A1A);
                drawRect(panel.x, y, panel.x + 2, y + ROW_HEIGHT,
                        m.isEnabled() ? 0xFF2ECC71 : 0xFF555555);
                fontRenderer.drawString(m.name, panel.x + 7, y + 3,
                        m.isEnabled() ? 0xFFFFFFFF : 0xFFAAAAAA);
                y += ROW_HEIGHT;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        for (Panel p : panels) {
            if (isHovered(mouseX, mouseY, p.x, p.y, PANEL_WIDTH, TITLE_HEIGHT)) {
                if (isHovered(mouseX, mouseY, p.x + PANEL_WIDTH - 14, p.y, 14, TITLE_HEIGHT)) {
                    p.expanded = !p.expanded;
                } else {
                    p.dragging = true;
                    p.dragOffsetX = mouseX - p.x;
                    p.dragOffsetY = mouseY - p.y;
                }
                return;
            }

            if (p.expanded) {
                int y = p.y + TITLE_HEIGHT;
                for (Module m : ModuleManager.getByCategory(p.category)) {
                    if (isHovered(mouseX, mouseY, p.x, y, PANEL_WIDTH, ROW_HEIGHT)) {
                        m.toggle();
                        return;
                    }
                    y += ROW_HEIGHT;
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (Panel p : panels) p.dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (Panel p : panels) {
            if (p.dragging) {
                p.x = mouseX - p.dragOffsetX;
                p.y = mouseY - p.dragOffsetY;
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class TickHandler {
        private final Minecraft mc = Minecraft.getMinecraft();
        private float normalGamma = -1.0F;
        private long lastAttackTime = 0;
        private int lastSlot = -1; // Added for Scaffold
        // Added for ElytraFly
        private boolean isWearingElytra(EntityPlayer player) {
            return player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() instanceof ItemElytra;
        }
        public static void register() {
            MinecraftForge.EVENT_BUS.register(new TickHandler());
        }

        private int findBlockSlot() {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack != null && stack.getItem() instanceof ItemBlock) {
                    return i;
                }
            }
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack != null && stack.getItem() instanceof ItemBlock) {
                    return i;
                }
            }
            return -1;
        }
        // Added for AutoTotem
        private int findTotemSlot() {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    return i;
                }
            }
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    return i + 36;
                }
            }
            return -1;
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (mc.player == null || mc.world == null) return;

            Module fullbright = ModuleManager.getByName("FullBright");
            Module jumphack   = ModuleManager.getByName("JumpHack");
            Module fly   = ModuleManager.getByName("Fly");
            Module killaura = ModuleManager.getByName("KillAura");
            Module scaffold = ModuleManager.getByName("Scaffold"); // Added for Scaffold
            Module elytrafly = ModuleManager.getByName("ElytraFly"); // Added for ElytraFly
            Module autototem = ModuleManager.getByName("AutoTotem"); // Added for AutoTotem

            // FullBright
            if (fullbright != null && fullbright.isEnabled()) {
                if (normalGamma < 0.0F) {
                    normalGamma = mc.gameSettings.gammaSetting;
                }
                mc.gameSettings.gammaSetting = 150.0F;
            } else if (normalGamma >= 0.0F) {
                mc.gameSettings.gammaSetting = normalGamma;
                normalGamma = -1.0F;
            }
            // JumpHack
            if (jumphack != null && jumphack.isEnabled()) {
                if (mc.gameSettings.keyBindJump.isKeyDown())  mc.player.motionY = 0.5;
                if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.player.motionY = -0.5;
            }
            // Fly
            if (fly != null && fly.isEnabled()) {
                float yaw = (float) Math.toRadians(mc.player.rotationYaw);
                double speed = 0.8;
                double mx = 0.0, mz = 0.0, my = 0.0;

                if (mc.gameSettings.keyBindJump.isKeyDown())  my =  0.5;
                if (mc.gameSettings.keyBindSneak.isKeyDown()) my = -0.5;

                if (mc.gameSettings.keyBindForward.isKeyDown()) {
                    mx -= Math.sin(yaw) * speed;
                    mz += Math.cos(yaw) * speed;
                }
                if (mc.gameSettings.keyBindBack.isKeyDown()) {
                    mx += Math.sin(yaw) * speed;
                    mz -= Math.cos(yaw) * speed;
                }
                // Fixed: strafe movement directions
                // Стрейфыы
                if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                    mx += Math.cos(yaw) * speed;
                    mz += Math.sin(yaw) * speed;
                }
                if (mc.gameSettings.keyBindRight.isKeyDown()) {
                    mx -= Math.cos(yaw) * speed;
                    mz -= Math.sin(yaw) * speed;
                }

                mc.player.motionX = mx;
                mc.player.motionY = my;
                mc.player.motionZ = mz;
            }
            // KillAura
            if (killaura != null && killaura.isEnabled()) {
                long now = System.currentTimeMillis();
                if (now - lastAttackTime >= 100) {
                    EntityPlayer target = null;
                    double closestDist = 4.0;

                    for (EntityPlayer p : mc.world.playerEntities) {
                        if (p == mc.player || p.isDead || p.getHealth() <= 0) continue;

                        double dist = mc.player.getDistance(p);
                        if (dist < closestDist) {
                            closestDist = dist;
                            target = p;
                        }
                    }
                    if (target != null) {
                        double dx = target.posX - mc.player.posX;
                        double dz = target.posZ - mc.player.posZ;
                        double dy = (target.posY + target.getEyeHeight()) - (mc.player.posY + mc.player.getEyeHeight());
                        double dist = MathHelper.sqrt(dx * dx + dz * dz);

                        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                        float pitch = (float) (-(Math.atan2(dy, dist) * 180.0 / Math.PI));

                        mc.player.rotationYaw = yaw;
                        mc.player.rotationPitch = pitch;
                        mc.playerController.attackEntity(mc.player, target);
                        mc.player.swingArm(EnumHand.MAIN_HAND);

                        lastAttackTime = now;
                    }
                }
            }

        // Added: Scaffold
        if (scaffold != null && scaffold.isEnabled()) {
            int bx = MathHelper.floor(mc.player.posX);
            int by = MathHelper.floor(mc.player.posY) - 1;
            int bz = MathHelper.floor(mc.player.posZ);
            BlockPos below = new BlockPos(bx, by, bz);

            if (mc.world.isAirBlock(below) || mc.world.getBlockState(below).getBlock().isReplaceable(mc.world, below)) {
                int blockSlot = findBlockSlot();
                if (blockSlot != -1) {
                    int prevSlot = mc.player.inventory.currentItem;
                    float prevPitch = mc.player.rotationPitch;

                    if (blockSlot < 9) {
                        mc.player.inventory.currentItem = blockSlot;
                    } else {
                        mc.playerController.windowClick(0, blockSlot, prevSlot, ClickType.QUICK_MOVE, mc.player);
                    }

                    mc.player.rotationPitch = 90.0F;
                    Vec3d hitVec = new Vec3d(bx + 0.5, by + 1.0, bz + 0.5);
                    mc.playerController.processRightClickBlock(
                        mc.player,
                        mc.world,
                        below,
                        EnumFacing.UP,
                        hitVec,
                        EnumHand.MAIN_HAND
                    );

                    if (blockSlot < 9) {
                        mc.player.inventory.currentItem = prevSlot;
                    } else {
                        mc.playerController.windowClick(0, blockSlot, prevSlot, ClickType.QUICK_MOVE, mc.player);
                    }
                    mc.player.rotationPitch = prevPitch;
                }
            }
        }
        // Added: ElytraFly
        if (elytrafly != null && elytrafly.isEnabled()) {
            ItemStack chest = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            if (chest.getItem() instanceof ItemElytra && ItemElytra.isUsable(chest)) {
                if (!mc.player.isElytraFlying() && !mc.player.onGround && mc.player.motionY < 0.0) {
                    mc.player.connection.sendPacket(
                        new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING)
                    );
                }

                if (mc.player.isElytraFlying()) {
                    float yaw = (float) Math.toRadians(mc.player.rotationYaw);
                    double speed = 1.2;
                    double mx = 0.0;
                    double my = 0.0;
                    double mz = 0.0;

                    if (mc.gameSettings.keyBindJump.isKeyDown()) my = 0.6;
                    if (mc.gameSettings.keyBindSneak.isKeyDown()) my = -0.6;

                    if (mc.gameSettings.keyBindForward.isKeyDown()) {
                        mx -= Math.sin(yaw) * speed;
                        mz += Math.cos(yaw) * speed;
                    }
                    if (mc.gameSettings.keyBindBack.isKeyDown()) {
                        mx += Math.sin(yaw) * speed;
                        mz -= Math.cos(yaw) * speed;
                    }
                    if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                        mx += Math.cos(yaw) * speed;
                        mz += Math.sin(yaw) * speed;
                    }
                    if (mc.gameSettings.keyBindRight.isKeyDown()) {
                        mx -= Math.cos(yaw) * speed;
                        mz -= Math.sin(yaw) * speed;
                    }

                    mc.player.motionX = mx;
                    mc.player.motionY = my;
                    mc.player.motionZ = mz;
                }
            }
        }
        // AutoTotem
        if (autototem != null && autototem.isEnabled()) {
            if (mc.player.openContainer == mc.player.inventoryContainer) {
                ItemStack offhand = mc.player.getHeldItemOffhand();
                ItemStack cursor = mc.player.inventory.getItemStack();
                if (!cursor.isEmpty() && cursor.getItem() == Items.TOTEM_OF_UNDYING) {
                    mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, mc.player);
                    mc.playerController.updateController();
                    return;
                }
                
                if (offhand.isEmpty() || offhand.getItem() != Items.TOTEM_OF_UNDYING) {
                    if (cursor.isEmpty()) {
                        int totemSlot = findTotemSlot();
                        if (totemSlot != -1) {
                            mc.playerController.windowClick(0, totemSlot, 0, ClickType.PICKUP, mc.player);
                            mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, mc.player);
                            if (!mc.player.inventory.getItemStack().isEmpty()) {
                                mc.playerController.windowClick(0, totemSlot, 0, ClickType.PICKUP, mc.player);
                            }
                            mc.playerController.updateController();
                        }
                    }
                }
            }
        }
    }

        // ESP
        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event) {
            Module esp = ModuleManager.getByName("ESP");
            if (esp == null || !esp.isEnabled()) return;
            if (mc.player == null || mc.world == null) return;

            float partialTicks = event.getPartialTicks();
            EntityPlayer player = mc.player;

            double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
            double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

            // saving OpenGL status
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(2.0F);
            GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);

            GL11.glPushMatrix();
            GL11.glTranslated(-px, -py, -pz);

            for (EntityPlayer target : mc.world.playerEntities) {
                if (target == player) continue;

                double tx = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
                double ty = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks;
                double tz = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;

                AxisAlignedBB box = target.getEntityBoundingBox()
                        .offset(tx - target.posX, ty - target.posY, tz - target.posZ)
                        .grow(0.1, 0.1, 0.1);

                GL11.glBegin(GL11.GL_LINES);

                GL11.glVertex3d(box.minX, box.minY, box.minZ); GL11.glVertex3d(box.maxX, box.minY, box.minZ);
                GL11.glVertex3d(box.maxX, box.minY, box.minZ); GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
                GL11.glVertex3d(box.maxX, box.minY, box.maxZ); GL11.glVertex3d(box.minX, box.minY, box.maxZ);
                GL11.glVertex3d(box.minX, box.minY, box.maxZ); GL11.glVertex3d(box.minX, box.minY, box.minZ);

                GL11.glVertex3d(box.minX, box.maxY, box.minZ); GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
                GL11.glVertex3d(box.maxX, box.maxY, box.minZ); GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
                GL11.glVertex3d(box.maxX, box.maxY, box.maxZ); GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
                GL11.glVertex3d(box.minX, box.maxY, box.maxZ); GL11.glVertex3d(box.minX, box.maxY, box.minZ);

                GL11.glVertex3d(box.minX, box.minY, box.minZ); GL11.glVertex3d(box.minX, box.maxY, box.minZ);
                GL11.glVertex3d(box.maxX, box.minY, box.minZ); GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
                GL11.glVertex3d(box.maxX, box.minY, box.maxZ); GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
                GL11.glVertex3d(box.minX, box.minY, box.maxZ); GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
                GL11.glEnd();
            }

            GL11.glPopMatrix();

            // Recovery OpenGL
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        // NoFall
        @SubscribeEvent
        public void onFall(LivingFallEvent event) {
            Module nofall = ModuleManager.getByName("NoFall");
            if (nofall != null && nofall.isEnabled()) {
                event.setCanceled(true);
            }
        }
    }
}
