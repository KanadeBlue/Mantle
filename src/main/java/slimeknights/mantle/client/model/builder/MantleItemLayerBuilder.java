package slimeknights.mantle.client.model.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.model.util.MantleItemLayerModel.LayerData;

import java.util.ArrayList;
import java.util.List;

/** Builder for {@link slimeknights.mantle.client.model.util.MantleItemLayerModel} */
public class MantleItemLayerBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {
  private final List<LayerData> layers = new ArrayList<>();
  protected MantleItemLayerBuilder(ResourceLocation loaderId, T parent, ExistingFileHelper existingFileHelper) {
    super(loaderId, parent, existingFileHelper);
  }

  public MantleItemLayerBuilder(T parent, ExistingFileHelper existingFileHelper) {
    this(Mantle.getResource("item_layer"), parent, existingFileHelper);
  }

  /** Adds data for the next element */
  public MantleItemLayerBuilder<T> addLayer(LayerData data) {
    this.layers.add(data);
    return this;
  }

  /** Sets the color for the next element */
  public MantleItemLayerBuilder<T> color(int color) {
    return addLayer(new LayerData(color, 0, false, null));
  }

  /** Sets the luminosity for the next element */
  public MantleItemLayerBuilder<T> luminosity(int luminosity) {
    return addLayer(new LayerData(-1, luminosity, false, null));
  }

  @Override
  public JsonObject toJson(JsonObject json) {
    json = super.toJson(json);
    if (!layers.isEmpty()) {
      JsonArray array = new JsonArray();
      for (LayerData data : layers) {
        array.add(data.toJson());
      }
      json.add("layers", array);
    }
    return json;
  }
}
