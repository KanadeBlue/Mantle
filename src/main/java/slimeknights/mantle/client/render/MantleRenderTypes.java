package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;
import slimeknights.mantle.Mantle;

/** Class for render types defined by Mantle */
public class MantleRenderTypes extends RenderType {
  private MantleRenderTypes(String name, VertexFormat format, Mode mode, int bufferSize, boolean useDelegate, boolean needsSorting, Runnable setupTaskIn, Runnable clearTaskIn) {
    super(name, format, mode, bufferSize, useDelegate, needsSorting, setupTaskIn, clearTaskIn);
  }

  /** Render type used for the fluid renderer */
  public static final RenderType FLUID = create(
    Mantle.modId + ":block_render_type",
    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, true, true,
    RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                             .setLightmapState(LIGHTMAP)
                             .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                             .setLightmapState(LIGHTMAP)
                             .setTextureState(BLOCK_SHEET_MIPPED)
                             .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                             .setOutputState(TRANSLUCENT_TARGET)
                             .createCompositeState(false));
}