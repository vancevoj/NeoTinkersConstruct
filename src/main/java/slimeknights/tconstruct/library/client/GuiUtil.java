package slimeknights.tconstruct.library.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuiUtil {
  /**
   * Draws the background of a container
   * @param graphics    Graphics context
   * @param screen      Parent screen
   * @param background  Background location
   */
  public static void drawBackground(GuiGraphics graphics, AbstractContainerScreen<?> screen, ResourceLocation background) {
    // reset the shader color before drawing the opaque background; the 7-arg blit uses the colorless
    // position_tex shader, so a stale RenderSystem shader color (e.g. a fluid tint alpha < 1 left over
    // from immediate-mode fluid/highlight rendering) would otherwise make the GUI background transparent,
    // letting the dimmed inventory show through. Mirrors MultiModuleScreen.drawBackground / BackgroundContainerScreen.
    graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    graphics.blit(background, screen.leftPos, screen.topPos, 0, 0, screen.imageWidth, screen.imageHeight);
    graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
  }

  /**
   * Checks if the given area is hovered
   * @param mouseX    Mouse X position
   * @param mouseY    Mouse Y position
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @return  True if the area is hovered
   */
  public static boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
  }

  /**
   * Checks if the given tank area is hovered
   * @param mouseX    Mouse X position
   * @param mouseY    Mouse Y position
   * @param amount    Current tank amount
   * @param capacity  Tank capacity
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @return  True if the tank is hovered, false otherwise
   */
  public static boolean isTankHovered(int mouseX, int mouseY, int amount, int capacity, int x, int y, int width, int height) {
    // check X position first, its easier
    if (mouseX < x || mouseX > x + width || mouseY > y + height) {
      return false;
    }
    // next, try height
    int topHeight = height - (height * amount / capacity);
    return mouseY > y + topHeight;
  }

  /**
   * Renders a fluid tank with a partial fluid level
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param capacity  Tank capacity, determines height
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @param depth     Tank depth
   */
  public static void renderFluidTank(GuiGraphics graphics, AbstractContainerScreen<?> screen, FluidStack stack, int capacity, int x, int y, int width, int height, int depth) {
    renderFluidTank(graphics, screen, stack, stack.getAmount(), capacity, x, y, width, height, depth);
  }

  /**
   * Renders a fluid tank with a partial fluid level and an amount override
   * @param graphics  Graphics context
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param capacity  Tank capacity, determines height
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @param depth     Tank depth
   */
  public static void renderFluidTank(GuiGraphics graphics, AbstractContainerScreen<?> screen, FluidStack stack, int amount, int capacity, int x, int y, int width, int height, int depth) {
    if(!stack.isEmpty() && capacity > 0) {
      int maxY = y + height;
      int fluidHeight = Math.min(height * amount / capacity, height);
      renderTiledFluid(graphics, screen, stack, x, maxY - fluidHeight, width, fluidHeight, depth);
    }
  }

  /**
   * Colors and renders a fluid sprite tiled over the given area.
   * 1.21 rewrite: draws through GuiGraphics (the old immediate-mode Tesselator/BufferUploader path
   * rendered at the wrong position/scale under 1.21's batched GUI rendering, producing a duplicated
   * fluid band and a misplaced fuel bar). x/y are relative to the screen's leftPos/topPos.
   * @param graphics  Graphics context
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param x         Fluid X
   * @param y         Fluid Y
   * @param width     Fluid width
   * @param height    Fluid height
   * @param depth     Fluid depth (blit z offset)
   */
  public static void renderTiledFluid(GuiGraphics graphics, AbstractContainerScreen<?> screen, FluidStack stack, int x, int y, int width, int height, int depth) {
    if (stack.isEmpty() || width <= 0 || height <= 0) {
      return;
    }
    IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(stack.getFluid());
    TextureAtlasSprite sprite = screen.getMinecraft().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(clientFluid.getStillTexture(stack));
    int color = clientFluid.getTintColor(stack);
    float a = ((color >> 24) & 0xFF) / 255f;
    if (a <= 0) {
      a = 1f; // many fluids only provide RGB; treat a missing alpha as opaque
    }
    float r = ((color >> 16) & 0xFF) / 255f;
    float g = ((color >> 8) & 0xFF) / 255f;
    float b = (color & 0xFF) / 255f;
    int startX = x + screen.leftPos;
    int startY = y + screen.topPos;
    int spriteW = Math.max(1, sprite.contents().width());
    int spriteH = Math.max(1, sprite.contents().height());
    // tile the still sprite over the area, clamping edge tiles to the area bounds
    for (int dx = 0; dx < width; dx += spriteW) {
      int tileW = Math.min(spriteW, width - dx);
      for (int dy = 0; dy < height; dy += spriteH) {
        int tileH = Math.min(spriteH, height - dy);
        graphics.blit(startX + dx, startY + dy, depth, tileW, tileH, sprite, r, g, b, a);
      }
    }
  }

  /**
   * Renders a texture atlas sprite tiled over the given area
   * @param matrices    Matrix instance
   * @param screen      Parent screen
   * @param sprite      Sprite to render
   * @param x           X position to render
   * @param y           Y position to render
   * @param width       Render width
   * @param height      Render height
   * @param depth       Render depth
   * @param upsideDown  If true, flips the sprite
   */
  public static void renderTiledTextureAtlas(PoseStack matrices, AbstractContainerScreen<?> screen, TextureAtlasSprite sprite, int x, int y, int width, int height, int depth, boolean upsideDown) {
    // start drawing sprites
    RenderUtils.bindTexture(sprite.atlasLocation());
    BufferBuilder builder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

    // tile vertically
    float u1 = sprite.getU0();
    float v1 = sprite.getV0();
    int spriteHeight = sprite.contents().height();
    int spriteWidth = sprite.contents().width();
    int startX = x + screen.leftPos;
    int startY = y + screen.topPos;
    do {
      int renderHeight = Math.min(spriteHeight, height);
      height -= renderHeight;
      // 1.21: getV takes a normalized 0..1 coordinate (was 0..16 in 1.20). Passing 16*h/spriteH
      // sampled far outside the sprite, smearing the whole block atlas (rainbow fluid garbage).
      float v2 = sprite.getV((float) renderHeight / spriteHeight);

      // we need to draw the quads per width too
      int x2 = startX;
      int widthLeft = width;
      Matrix4f matrix = matrices.last().pose();
      // tile horizontally
      do {
        int renderWidth = Math.min(spriteWidth, widthLeft);
        widthLeft -= renderWidth;

        float u2 = sprite.getU((float) renderWidth / spriteWidth);
        if(upsideDown) {
          // FIXME: I think this causes tiling errors, look into it
          buildSquare(matrix, builder, x2, x2 + renderWidth, startY, startY + renderHeight, depth, u1, u2, v2, v1);
        } else {
          buildSquare(matrix, builder, x2, x2 + renderWidth, startY, startY + renderHeight, depth, u1, u2, v1, v2);
        }
        x2 += renderWidth;
      } while(widthLeft > 0);

      startY += renderHeight;
    } while(height > 0);

    // finish drawing sprites
    BufferUploader.drawWithShader(builder.buildOrThrow());
    // RenderSystem.enableAlphaTest();
    RenderSystem.enableDepthTest();
  }

  /**
   * Adds a square of texture to a buffer builder
   * @param builder  Builder instance
   * @param x1       X start
   * @param x2       X end
   * @param y1       Y start
   * @param y2       Y end
   * @param z        Depth
   * @param u1       Texture U start
   * @param u2       Texture U end
   * @param v1       Texture V start
   * @param v2       Texture V end
   */
  private static void buildSquare(Matrix4f matrix, BufferBuilder builder, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2) {
    builder.addVertex(matrix, x1, y2, z).setUv(u1, v2);
    builder.addVertex(matrix, x2, y2, z).setUv(u2, v2);
    builder.addVertex(matrix, x2, y1, z).setUv(u2, v1);
    builder.addVertex(matrix, x1, y1, z).setUv(u1, v1);
  }

  /**
   * Draws an upwards progress bar. TODO: is this just {@link slimeknights.mantle.client.screen.ScalableElementScreen}?
   * @param element   Element to draw
   * @param x         X position to start
   * @param y         Y position to start
   * @param progress  Progress between 0 and 1
   */
  public static void drawProgressUp(GuiGraphics graphics, ElementScreen element, int x, int y, float progress) {
    int height;
    if (progress > 1) {
      height = element.h;
    } else if (progress < 0) {
      height = 0;
    } else {
      // add an extra 0.5 so it rounds instead of flooring
      height = (int)(progress * element.h + 0.5);
    }
    // amount to offset element by for the height
    int deltaY = element.h - height;
    graphics.blit(element.texture, x, y + deltaY, element.x, element.y + deltaY, element.w, height, element.texW, element.texH);
  }

  /**
   * Renders a highlight overlay for the given area
   * @param graphics  Graphics instance
   * @param x         Element X position
   * @param y         Element Y position
   * @param width     Element width
   * @param height    Element height
   */
  public static void renderHighlight(GuiGraphics graphics, int x, int y, int width, int height) {
      RenderSystem.disableDepthTest();
      RenderSystem.colorMask(true, true, true, false);
      graphics.fill(x, y, x + width, y + height, 100, 0x80FFFFFF);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableDepthTest();
  }

  /** Renders a pattern at the given location */
  public static void renderPattern(GuiGraphics graphics, Pattern pattern, int x, int y) {
    TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(pattern.getTexture());
    graphics.blit(x, y, 100, 16, 16, sprite);
  }
}
