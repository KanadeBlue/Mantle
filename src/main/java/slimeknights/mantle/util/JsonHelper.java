package slimeknights.mantle.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.mantle.network.packet.ISimplePacket;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utilities to help in parsing JSON
 */
@SuppressWarnings("unused")
public class JsonHelper {
  private JsonHelper() {}

  /**
   * Gets an element from JSON, throwing an exception if missing
   * @param json        Object parent
   * @param memberName  Name to get
   * @return  JsonElement found
   * @throws JsonSyntaxException if element is missing
   */
  public static JsonElement getElement(JsonObject json, String memberName) {
    if (json.has(memberName)) {
      return json.get(memberName);
    } else {
      throw new JsonSyntaxException("Missing " + memberName + "");
    }
  }

  /**
   * Parses a list from an JsonArray
   * @param array   Json array
   * @param name    Json key of the array
   * @param mapper  Mapper from the element object and name to new object
   * @param <T>     Output type
   * @return  List of output objects
   */
  public static <T> List<T> parseList(JsonArray array, String name, BiFunction<JsonElement,String,T> mapper) {
    if (array.size() == 0) {
      throw new JsonSyntaxException(name + " must have at least 1 element");
    }
    // build the list
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    for (int i = 0; i < array.size(); i++) {
      builder.add(mapper.apply(array.get(i), name + "[" + i + "]"));
    }
    return builder.build();
  }

  /**
   * Parses a list from an JsonArray
   * @param array   Json array
   * @param name    Json key of the array
   * @param mapper  Mapper from the json object to new object
   * @param <T>     Output type
   * @return  List of output objects
   */
  public static <T> List<T> parseList(JsonArray array, String name, Function<JsonObject,T> mapper) {
    return parseList(array, name, (element, s) -> mapper.apply(GsonHelper.convertToJsonObject(element, s)));
  }

  /**
   * Parses a list from an JsonArray
   * @param parent  Parent JSON object
   * @param name    Json key of the array
   * @param mapper  Mapper from raw type to new object
   * @param <T>     Output type
   * @return  List of output objects
   */
  public static <T> List<T> parseList(JsonObject parent, String name, BiFunction<JsonElement,String,T> mapper) {
    return parseList(GsonHelper.getAsJsonArray(parent, name), name, mapper);
  }

  /**
   * Parses a list from an JsonArray
   * @param parent  Parent JSON object
   * @param name    Json key of the array
   * @param mapper  Mapper from json object to new object
   * @param <T>     Output type
   * @return  List of output objects
   */
  public static <T> List<T> parseList(JsonObject parent, String name, Function<JsonObject,T> mapper) {
    return parseList(GsonHelper.getAsJsonArray(parent, name), name, mapper);
  }

  /**
   * Gets a resource location from JSON, throwing a nice exception if invalid
   * @param json  JSON object
   * @param key   Key to fetch
   * @return  Resource location parsed
   */
  public static ResourceLocation getResourceLocation(JsonObject json, String key) {
    String text = GsonHelper.getAsString(json, key);
    ResourceLocation location = ResourceLocation.tryParse(text);
    if (location == null) {
      throw new JsonSyntaxException("Expected " + key + " to be a Resource location, was '" + text + "'");
    }
    return location;
  }

  /**
   * Gets a resource location from JSON, throwing a nice exception if invalid
   * @param json  JSON object
   * @param key   Key to fetch
   * @param fallback  Fallback if key is not present
   * @return  Resource location parsed
   */
  public static ResourceLocation getResourceLocation(JsonObject json, String key, ResourceLocation fallback) {
    if (json.has(key)) {
      return getResourceLocation(json, key);
    }
    return fallback;
  }

  /**
   * Gets a resource location from JSON, throwing a nice exception if invalid
   * @param json  JSON object
   * @param key   Key to fetch
   * @return  Resource location parsed
   */
  public static ResourceLocation convertToResourceLocation(JsonElement json, String key) {
    String text = GsonHelper.convertToString(json, key);
    ResourceLocation location = ResourceLocation.tryParse(text);
    if (location == null) {
      throw new JsonSyntaxException("Expected " + key + " to be a Resource location, was '" + text + "'");
    }
    return location;
  }

  /**
   * Parses a registry entry from JSON
   * @param registry  Registry
   * @param element   Element to deserialize
   * @param key       Json key
   * @param <T>  Object type
   * @return  Registry value
   * @throws JsonSyntaxException  If something failed to parse
   */
  public static <T extends IForgeRegistryEntry<T>> T convertToEntry(IForgeRegistry<T> registry, JsonElement element, String key) {
    ResourceLocation name = JsonHelper.convertToResourceLocation(element, key);
    if (registry.containsKey(name)) {
      T value = registry.getValue(name);
      if (value != null) {
        return value;
      }
    }
    throw new JsonSyntaxException("Unknown " + registry.getRegistryName() + " " + name);
  }

  /**
   * Parses a registry entry from JSON
   * @param registry  Registry
   * @param parent    Parent JSON object
   * @param key       Json key
   * @param <T>  Object type
   * @return  Registry value
   * @throws JsonSyntaxException  If something failed to parse
   */
  public static <T extends IForgeRegistryEntry<T>> T getAsEntry(IForgeRegistry<T> registry, JsonObject parent, String key) {
    return convertToEntry(registry, JsonHelper.getElement(parent, key), key);
  }

  /** Parses an enum from its name */
  private static <T extends Enum<T>> T enumByName(String name, Class<T> enumClass) {
    for (T value : enumClass.getEnumConstants()) {
      if (value.name().toLowerCase(Locale.ROOT).equals(name)) {
        return value;
      }
    }
    throw new JsonSyntaxException("Invalid " + enumClass.getSimpleName() + " " + name);
  }

  /** Gets an enum value from its string name */
  public static <T extends Enum<T>> T convertToEnum(JsonElement element, String key, Class<T> enumClass) {
    String name = GsonHelper.convertToString(element, key);
    return enumByName(name, enumClass);
  }

  /** Gets an enum value from its string name */
  public static <T extends Enum<T>> T getAsEnum(JsonObject json, String key, Class<T> enumClass) {
    String name = GsonHelper.getAsString(json, key);
    return enumByName(name, enumClass);
  }

  /**
   * Parses a color as a string
   * @param color  Color to parse
   * @return  Parsed string
   */
  public static int parseColor(@Nullable String color) {
    if (color == null || color.isEmpty()) {
      return -1;
    }
    // two options, 6 character or 8 character, must not start with - sign
    if (color.charAt(0) != '-') {
      try {
        // length of 8 must parse as long, supports transparency
        int length = color.length();
        if (length == 8) {
          return (int)Long.parseLong(color, 16);
        }
        if (length == 6) {
          return 0xFF000000 | Integer.parseInt(color, 16);
        }
      } catch (NumberFormatException ex) {
        // NO-OP
      }
    }
    throw new JsonSyntaxException("Invalid color '" + color + "'");
  }


  /* Resource loaders */

  /**
   * Converts the resource into a JSON file
   * @param resource  Resource to read. Closed when done
   * @return  JSON object, or null if failed to parse
   */
  @Nullable
  public static JsonObject getJson(Resource resource) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      return GsonHelper.parse(reader);
    } catch (JsonParseException | IOException e) {
      Mantle.logger.error("Failed to load JSON from resource {} from pack '{}'", resource.getLocation(), resource.getSourceName(), e);
      return null;
    }
  }

  /** Gets a list of JSON objects for a single path in all domains and packs, for a language file like loader */
  public static List<JsonObject> getFileInAllDomainsAndPacks(ResourceManager manager, String path, @Nullable String preferredPath) {
    return manager
      .getNamespaces().stream()
      .filter(ResourceLocation::isValidNamespace)
      .flatMap(namespace -> {
        ResourceLocation location = new ResourceLocation(namespace, path);
        try {
          return manager.getResources(location).stream();
        } catch (FileNotFoundException e) {
          // suppress, the above method throws instead of returning empty
        } catch (IOException e) {
          Mantle.logger.error("Failed to load JSON files from {}", location, e);
        }
        return Stream.empty();
      })
      .map(preferredPath != null ? resource -> {
        ResourceLocation loaded = resource.getLocation();
        Mantle.logger.warn("Using deprecated path {} in pack {} - use {}:{} instead", loaded, resource.getSourceName(), loaded.getNamespace(), preferredPath);
        return getJson(resource);
      } : JsonHelper::getJson)
      .filter(Objects::nonNull).toList();
  }

  /** Sends the packet to the given player */
  private static void sendPackets(NetworkWrapper network, ServerPlayer player, ISimplePacket[] packets) {
    // on an integrated server, the modifier registries have a single instance on both the client and the server thread
    // this means syncing is unneeded, and has the side-effect of recreating all the modifier instances (which can lead to unexpected behavior)
    // as a result, integrated servers just mark fullyLoaded as true without syncing anything, side-effect is listeners may run twice on single player

    // on a dedicated server, the client is running a separate game instance, this is where we send packets, plus fully loaded should already be true
    // this event is not fired when connecting to a server
    if (!player.connection.getConnection().isMemoryConnection()) {
      PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
      for (ISimplePacket packet : packets) {
        network.send(target, packet);
      }
    }
  }

  /** Called when the player logs in to send packets */
  public static void syncPackets(OnDatapackSyncEvent event, NetworkWrapper network, ISimplePacket... packets) {
    // send to single player
    ServerPlayer targetedPlayer = event.getPlayer();
    if (targetedPlayer != null) {
      sendPackets(network, targetedPlayer, packets);
    } else {
      // send to all players
      for (ServerPlayer player : event.getPlayerList().getPlayers()) {
        sendPackets(network, player, packets);
      }
    }
  }


  /* Block States */

  /**
   * Converts the given JSON element into a block state
   * @param element  Element to convert
   * @param key      Element key
   * @return  Block state
   * @throws JsonSyntaxException  if a property does not parse or the element is the wrong type
   */
  public static BlockState convertToBlockState(JsonElement element, String key) {
    // primitive means its a block directly
    if (element.isJsonPrimitive()) {
      return JsonHelper.convertToEntry(ForgeRegistries.BLOCKS, element, key).defaultBlockState();
    }
    if (element.isJsonObject()) {
      return convertToBlockState(element.getAsJsonObject());
    }
    throw new JsonSyntaxException("Expected " + key + " to be a string or an object, was " + GsonHelper.getType(element));
  }

  /**
   * Converts the given JSON element into a block state
   * @param parent   Parent containing the block state
   * @param key      Element key
   * @return  Block state
   * @throws JsonSyntaxException  if a property does not parse or the element is missing or the wrong type
   */
  public static BlockState getAsBlockState(JsonObject parent, String key) {
    if (parent.has(key)) {
      return convertToBlockState(parent.get(key), key);
    }
    throw new JsonSyntaxException("Missing " + key + ", expected to find a string or an object");
  }

  /**
   * Sets the property
   * @param state     State before changes
   * @param property  Property to set
   * @param name      Value name
   * @param <T>  Type of property
   * @return  State with the property
   * @throws JsonSyntaxException  if the property has no element with the given name
   */
  private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String name) {
    Optional<T> value = property.getValue(name);
    if (value.isPresent()) {
      return state.setValue(property, value.get());
    }
    throw new JsonSyntaxException("Property " + property + " does not contain value " + name);
  }

  /**
   * Converts the given JSON object into a block state
   * @param json  Json object containing "block" and "properties"
   * @return  Block state
   * @throws JsonSyntaxException  if any property name or property value is invalid
   */
  public static BlockState convertToBlockState(JsonObject json) {
    Block block = JsonHelper.getAsEntry(ForgeRegistries.BLOCKS, json, "block");
    BlockState state = block.defaultBlockState();
    if (json.has("properties")) {
      StateDefinition<Block,BlockState> definition = block.getStateDefinition();
      for (Entry<String,JsonElement> entry : GsonHelper.getAsJsonObject(json, "properties").entrySet()) {
        String key = entry.getKey();
        Property<?> property = definition.getProperty(key);
        if (property == null) {
          throw new JsonSyntaxException("Property " + key + " does not exist in block " + block);
        }
        state = setValue(state, property, GsonHelper.convertToString(entry.getValue(), key));
      }
    }
    return state;
  }

  /**
   * Serializes the given block state to JSON, essentially writes all values that differ from the state.
   * @param state  State
   * @return  JsonPrimitive of the block name if it matches the default state, JsonObject otherwise
   */
  public static JsonElement serializeBlockState(BlockState state) {
    Block block = state.getBlock();
    if (state == block.defaultBlockState()) {
      return new JsonPrimitive(Objects.requireNonNull(block.getRegistryName()).toString());
    }
    return serializeBlockState(state, new JsonObject());
  }

  /** Serializes the property if it differs in the default state */
  private static <T extends Comparable<T>> void serializeProperty(BlockState serialize, Property<T> property, BlockState defaultState, JsonObject json) {
    T value = serialize.getValue(property);
    if (!value.equals(defaultState.getValue(property))) {
      json.addProperty(property.getName(), property.getName(value));
    }
  }

  /**
   * Serializes the given block state to JSON, essentially writes all values that differ from the state
   * @param state  State
   * @return  JsonObject containing properties that differ from the default state
   */
  public static JsonObject serializeBlockState(BlockState state, JsonObject json) {
    Block block = state.getBlock();
    json.addProperty("block", Objects.requireNonNull(block.getRegistryName()).toString());
    BlockState defaultState = block.defaultBlockState();
    JsonObject properties = new JsonObject();
    for (Property<?> property : block.getStateDefinition().getProperties()) {
      serializeProperty(state, property, defaultState, properties);
    }
    if (properties.size() > 0) {
      json.add("properties", properties);
    }
    return json;
  }
}
