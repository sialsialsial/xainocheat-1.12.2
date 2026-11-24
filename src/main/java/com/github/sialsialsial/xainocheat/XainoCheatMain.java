package com.github.sialsialsial.xainocheat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Mod(modid = "xainocheat", useMetadata = true)
public class XainoCheatMain {
    public static boolean creativeFlyEnabled = false;
    public static boolean flyEnabled = false;
    public static boolean noFallEnabled = false;
    public static boolean fullbrightEnabled = false;
    public static boolean espEnabled = false;
    public static boolean jumpHackEnabled = false;
    public static boolean legacyUIEnabled = false;
    public static boolean killAuraEnabled = false;

    // License
    private static boolean licenseAccepted = false;
    private static final String LICENSE_VERSION = "1";

    private static final String LICENSE_TEXT =
        "By clicking \"Принять и продолжить\", you acknowledge that you have read and agree to the following terms.\n\n" +
        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, " +
        "INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE " +
        "AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, " +
        "DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
        "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";

    // Added: statics for modmenu`s icon

    private static final ResourceLocation MOD_ICON = new ResourceLocation("xainocheat:modmenuicon.png");
    private static int iconX = 10;
    private static int iconY = 10;
    private static boolean iconDragging = false;
    private static int iconDragOffsetX = 0;
    private static int iconDragOffsetY = 0;

    private static int iconPressMouseX = 0;
    private static int iconPressMouseY = 0;
    private static long iconPressTime = 0;
    private static final int CLICK_DRAG_THRESHOLD = 5;   // пикселей — порог клик/драг
    private static final long CLICK_TIME_THRESHOLD = 300; // мс — макс. время клика

    private static final int ICON_SIZE = 32;
    private static int iconSpeedX = 1;
    private static int iconSpeedY = 1;

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static JFrame frame;

    // Work with data.txt
    private static File getDataFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "xainocheat");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "data.txt");
    }

    private static boolean loadLicenseState() {
        File file = getDataFile();
        if (!file.exists()) return false;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean accepted = false;
            boolean versionOk = false;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("accepted=")) {
                    accepted = Boolean.parseBoolean(line.substring("accepted=".length()).trim());
                } else if (line.startsWith("version=")) {
                    versionOk = line.substring("version=".length()).trim().equals(LICENSE_VERSION);
                }
            }
            return accepted && versionOk;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void saveLicenseState(boolean accepted) {
        File file = getDataFile();
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("accepted=" + accepted);
            w.newLine();
            w.write("timestamp=" + System.currentTimeMillis());
            w.newLine();
            w.write("version=" + LICENSE_VERSION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // License`s JFrame
    private static boolean showLicenseWindow() {
        final boolean[] accepted = {false};
        final Object lock = new Object();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Условия");
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.setSize(560, 460);
            f.setLayout(new BorderLayout(8, 8));
            f.setAlwaysOnTop(true);

            JTextArea textArea = new JTextArea(LICENSE_TEXT);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
            JScrollPane scroll = new JScrollPane(textArea);

            JCheckBox agreeBox = new JCheckBox("Я ознакомился(-ась) и принимаю настоящее лицензионное соглашение");
            agreeBox.setSelected(false);

            JButton acceptBtn = new JButton("Принять и продолжить");
            acceptBtn.setEnabled(false);
            JButton declineBtn = new JButton("Отклонить и выйти");

            agreeBox.addActionListener(e -> acceptBtn.setEnabled(agreeBox.isSelected()));

            acceptBtn.addActionListener(e -> {
                saveLicenseState(true);
                accepted[0] = true;
                synchronized (lock) { lock.notifyAll(); }
                f.dispose();
            });

            declineBtn.addActionListener(e -> {
                saveLicenseState(false);
                accepted[0] = false;
                synchronized (lock) { lock.notifyAll(); }
                f.dispose();
            });

            JPanel south = new JPanel(new BorderLayout(8, 8));
            south.add(agreeBox, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.add(declineBtn);
            buttons.add(acceptBtn);
            south.add(buttons, BorderLayout.SOUTH);

            f.add(scroll, BorderLayout.CENTER);
            f.add(south, BorderLayout.SOUTH);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });

        synchronized (lock) {
            try { lock.wait(); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return accepted[0];
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!loadLicenseState()) {
            boolean ok = showLicenseWindow();
            if (!ok) {
                return;
            }
        }
        licenseAccepted = true;
        
        MinecraftForge.EVENT_BUS.register(this);
        ClickGui.ModuleManager.init();
        FMLCommonHandler.instance().bus().register(new ClickGui.TickHandler());

        // Swing GUI
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("ClickGUI");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setSize(300, 250);
            frame.setLayout(new FlowLayout());

            JLabel MovementLabel = new JLabel("Movement");

            //Fly button
            JToggleButton flyBtn = new JToggleButton("Fly: OFF");
            flyBtn.addActionListener(e -> {
                flyEnabled = flyBtn.isSelected();
                flyBtn.setText("Fly: " + (flyEnabled ? "ON" : "OFF"));
            });

            //CreativeFly button
            JToggleButton creativeFlyBtn = new JToggleButton("CreativeFly: OFF");
            creativeFlyBtn.addActionListener(e -> {
                creativeFlyEnabled = creativeFlyBtn.isSelected();
                creativeFlyBtn.setText("CreativeFly: " + (creativeFlyEnabled ? "ON" : "OFF"));
            });

            //JumpHack button
            JToggleButton jumpHackBtn = new JToggleButton("JumpHack: OFF");
            jumpHackBtn.setSelected(true);
            jumpHackBtn.addActionListener(e -> {
                jumpHackEnabled = jumpHackBtn.isSelected();
                jumpHackBtn.setText("JumpHack: " + (jumpHackEnabled ? "ON" : "OFF"));
            });

            //NoFall button
            JToggleButton noFallBtn = new JToggleButton("NoFall: OFF");
            noFallBtn.setSelected(true);
            noFallBtn.addActionListener(e -> {
                noFallEnabled = noFallBtn.isSelected();
                noFallBtn.setText("NoFall: " + (noFallEnabled ? "ON" : "OFF"));
            });

            JLabel RenderLabel = new JLabel("Render");

            //FullBright button
            JToggleButton fbBtn = new JToggleButton("Fullbright: OFF");
            fbBtn.setSelected(true);
            fbBtn.addActionListener(e -> {
                fullbrightEnabled = fbBtn.isSelected();
                fbBtn.setText("Fullbright: " + (fullbrightEnabled ? "ON" : "OFF"));
            });

            //ESP button
            JToggleButton espBtn = new JToggleButton("ESP: OFF");
            espBtn.setSelected(true);
            espBtn.addActionListener(e -> {
                espEnabled = espBtn.isSelected();
                espBtn.setText("ESP: " + (espEnabled ? "ON" : "OFF"));
            });
            frame.add(MovementLabel);

            frame.add(flyBtn);
            frame.add(creativeFlyBtn);
            frame.add(jumpHackBtn);
            frame.add(noFallBtn);

            frame.add(RenderLabel);

            frame.add(fbBtn);
            frame.add(espBtn);
            //frame.setVisible(true);
        });
    }

    // Added: The New ClickGui`s menu bind
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!licenseAccepted) return;
        if (Minecraft.getMinecraft().player == null) return;
        if (!Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(new ClickGui());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!licenseAccepted) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.player == null) return;

        // Fly & CreativeFly & JumpHack
        // Now swing click-gui are legacy
        if (flyEnabled) {
            mc.player.motionY = 0.0;
            if (mc.gameSettings.keyBindJump.isKeyDown()) mc.player.motionY = 0.5;
            if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.player.motionY = -0.5;
            mc.player.capabilities.isFlying = true;
        } else if (creativeFlyEnabled) {
            mc.player.motionY = 0.0;
            if (mc.gameSettings.keyBindJump.isKeyDown()) mc.player.motionY = 0.5;
            if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.player.motionY = -0.5;
        } else if (jumpHackEnabled) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) mc.player.motionY = 0.5;
            if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.player.motionY = -0.5;
        } if (legacyUIEnabled == false) {
            frame.setVisible(false);
        } else if (legacyUIEnabled) {
            frame.setVisible(true);
        }

        // Fullbright
        if (fullbrightEnabled) {
            mc.gameSettings.gammaSetting = 150.0F;
        }

        // Added: KillAura
        //if (killAuraEnabled) {
        //EntityPlayer target = null;
        //double closestDist = 4.0;

        //for (EntityPlayer p : mc.world.playerEntities) {
            //if (p == mc.player || p.isDead || p.getHealth() <= 0) continue;

            //double dist = mc.player.getDistanceToEntity(p);
            //if (dist < closestDist) {
                //closestDist = dist;
                //target = p;
            //}
        //}

        //if (target != null) {
            //double dx = target.posX - mc.player.posX;
            //double dz = target.posZ - mc.player.posZ;
            //double dy = (target.posY + target.getEyeHeight()) - (mc.player.posY + mc.player.getEyeHeight());
            //double dist = MathHelper.sqrt(dx * dx + dz * dz);

            //float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            //float pitch = (float) (-(Math.atan2(dy, dist) * 180.0 / Math.PI));

            //mc.player.rotationYaw = yaw;
            //mc.player.rotationPitch = pitch;
            //mc.playerController.attackEntity(mc.player, target);
            //mc.player.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        //}
    }

    // NoFall
    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (!licenseAccepted) return;
        if (noFallEnabled && event.getEntityLiving() == mc.player) {
            event.setCanceled(true);
        }
    }

    // ESP
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!licenseAccepted) return;
        if (!espEnabled || mc.world == null || mc.player == null) return;

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

        for (EntityPlayer target : mc.world.playerEntities) {
            if (target == player) continue;

            double tx = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
            double ty = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks;
            double tz = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;


            AxisAlignedBB box = target.getEntityBoundingBox()
            .offset(-px, -py, -pz)
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

        // Recovery OpenGL
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }


    // Added: drawing of modmenuicon, overlay
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!licenseAccepted) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.player == null) return;

        mc.getTextureManager().bindTexture(MOD_ICON);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(
            iconX, iconY,
            0, 0,
            ICON_SIZE, ICON_SIZE,
            ICON_SIZE, ICON_SIZE
        );
        GlStateManager.disableBlend();
    }
    // Added: обработка клика для modmenuicon, subscribe event, перетаск.
    @SubscribeEvent
    public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!licenseAccepted) return;
        if (mc.player == null) return;
        if (Mouse.getEventButton() != 0) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;

        boolean overIcon = mouseX >= iconX && mouseX <= iconX + ICON_SIZE
                    && mouseY >= iconY && mouseY <= iconY + ICON_SIZE;

        if (Mouse.getEventButtonState() && overIcon) {
            iconDragging = true;
            iconDragOffsetX = mouseX - iconX;
            iconDragOffsetY = mouseY - iconY;
            iconPressMouseX = mouseX;
            iconPressMouseY = mouseY;
            iconPressTime = System.currentTimeMillis();
            event.setCanceled(true);
            return;
        }

        if (!Mouse.getEventButtonState() && iconDragging) {
            iconDragging = false;
            event.setCanceled(true);

            long elapsed = System.currentTimeMillis() - iconPressTime;
            int dx = Math.abs(mouseX - iconPressMouseX);
            int dy = Math.abs(mouseY - iconPressMouseY);

            if (dx <= CLICK_DRAG_THRESHOLD && dy <= CLICK_DRAG_THRESHOLD && elapsed <= CLICK_TIME_THRESHOLD) {
                mc.displayGuiScreen(new ClickGui());
            }
        }
    }

    @SubscribeEvent
    public void onMouseMove(MouseEvent event) {
        if (!licenseAccepted) return;
        if (!iconDragging) return;
        if (mc.player == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

        iconX = mouseX - iconDragOffsetX;
        iconY = mouseY - iconDragOffsetY;

        iconX = Math.max(0, Math.min(iconX, sr.getScaledWidth() - ICON_SIZE));
        iconY = Math.max(0, Math.min(iconY, sr.getScaledHeight() - ICON_SIZE));
    }
}
