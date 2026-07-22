# Using INY with Bukkit

The Bukkit layer is the normal entry point for server plugins. The INY plugin owns one server-wide service, one factory registry, and the startup transition that makes configuration safe to consume.

There are two roles:

- A **consumer** bundles and reads an INY configuration.
- A **producer** registers a namespaced factory that lets configuration construct one of its Java types.

A plugin can do either or both. In both cases, depend on the Bukkit artifact only; it already contains INY Core.

## Add the dependency

Use INY as a compile-only dependency because the INY plugin supplies it at runtime:

```kotlin
repositories {
    maven("https://maven.iridiummc.net/releases")
}

dependencies {
    compileOnly("net.iridiummc:iny-bukkit:<version>")
}
```

Declare the server plugin dependency too:

```yaml
depend: [INY]
```

Use `softdepend` only when your plugin has a complete fallback and never touches INY classes when INY is absent.

Do not create a private `Iny` instance in a Bukkit plugin. Obtain the active facade with `INY.getInstance()` so every plugin observes the same factories and lifecycle.

## Consume a configuration

Put a default file at `src/main/resources/config.iny` in your plugin:

```iny
messages {
  welcome: "Welcome to the server"
}

limits {
  players: 100
  worlds:
    - "world"
    - "world_nether"
}

spawn: minecraft:location("world", 0.5, 64, 0.5)
```

Load it after INY announces readiness:

```java
import net.iridiummc.iny.InyReadyEvent;
import net.iridiummc.iny.INY;
import net.iridiummc.iny.api.InyConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExamplePlugin extends JavaPlugin implements Listener {
    private InyConfig config;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onInyReady(InyReadyEvent event) {
        config = event.iny().loadConfig(this);
    }
}
```

On the first load, `loadConfig(this)` copies your bundled `config.iny` into the plugin data directory. Later loads read the editable copy. Use `loadConfig(this, "rewards.iny")` for another plugin-relative resource, or `load(path)` when your plugin already owns an explicit path.

The load methods are synchronous UTF-8 operations. Call them from an appropriate thread for your server and file size.

### Use a callback instead of an event

Callbacks are useful when configuration loading belongs in an ordinary method:

```java
@Override
public void onEnable() {
    INY.getInstance().onInyReady(() -> {
        config = INY.getInstance().loadConfig(this);
        startFeatures();
    });
}
```

`onInyReady` is late-safe: callbacks registered after readiness are scheduled onto Bukkit's main thread. The Bukkit event is fired before callbacks registered through the facade.

## Read values

`InyConfig` is the root `InySection`, so dotted paths and subsection-relative paths use the same API:

```java
String welcome = config.get("messages.welcome", String.class);
int playerLimit = config.get("limits.players", int.class);
List<String> worlds = config.getList("limits.worlds", String.class);

InySection limits = config.getSection("limits");
int sameLimit = limits.get("players", Integer.class);
```

All lists returned by INY are immutable. Sections are immutable views, and `entries()` preserves declaration order.

Use `find` for an optional value and `contains` when explicit `null` matters:

```java
Optional<String> prefix = config.find("messages.prefix", String.class);

if (config.contains("integration.token")) {
    Object token = config.get("integration.token", Object.class);
    // token may deliberately be null
}
```

An object lookup exposes ordinary Java values rather than parser nodes:

```java
Object value = config.get("messages.welcome", Object.class);
if (value instanceof String text) {
    getLogger().info(text);
}
```

Scalar object values are `String`, `Boolean`, `BigInteger`, `BigDecimal`, or `null`. Structural values are `InySection` and immutable `List<Object>`. A factory call resolves to the Java object produced by its registered factory.

Typed numeric reads are strict. For example, a fractional value cannot become an `Integer`, and an out-of-range integer does not silently overflow.

## Use the built-in Bukkit factories

The shared service installs these factory calls:

| Call                   | Result      | Arguments                                    |
|------------------------|-------------|----------------------------------------------|
| `minecraft:world`      | `World`     | world name                                   |
| `minecraft:material`   | `Material`  | material name                                |
| `minecraft:vector`     | `Vector`    | x, y, z                                      |
| `minecraft:location`   | `Location`  | world, x, y, z, optional yaw, optional pitch |
| `minecraft:block`      | `Block`     | world, integer x, integer y, integer z       |
| `minecraft:item_stack` | `ItemStack` | material name, count                         |

For example:

```iny
icon: minecraft:item_stack("diamond", 3)
offset: minecraft:vector(0, 1.5, 0)
```

```java
ItemStack icon = config.get("icon", ItemStack.class);
Vector offset = config.get("offset", Vector.class);
```

Factory calls are resolved lazily. Parsing can therefore succeed when an optional provider is absent; requesting that value later throws `InyUnknownFactoryException`.

## Produce a configuration type

Register factories synchronously from your plugin's `onEnable()`. Registration closes at the end of Bukkit startup, before INY becomes ready.

Suppose your plugin owns this type:

```java
public record Reward(String permission, int amount) {}
```

Register its constructor-shaped configuration call:

```java
@Override
public void onEnable() {
    INY.getInstance().registerFactory(
            this,
            "rewards:reward",
            Reward.class,
            context -> {
                context.arguments().requireCount(2);
                return new Reward(
                        context.arguments().get(0, String.class),
                        context.arguments().get(1, Integer.class)
                );
            }
    );
}
```

Consumers can then write and read:

```iny
daily: rewards:reward("rewards.daily", 250)
```

```java
Reward reward = config.get("daily", Reward.class);
```

The owner argument is significant. If Bukkit disables the provider, INY removes all factories owned by it from the live registry snapshot. Consumers typically depend on INY without directly depending on the provider plugin.

### Design factory arguments deliberately

Factory arguments are positional and immutable. Prefer a small, stable call shape:

```java
context.arguments().requireCountBetween(1, 2);

String name = context.arguments().get(0, String.class);
int amount = context.arguments().getOrDefault(1, Integer.class, 1);
```

`get(index, type)` resolves nested factory calls and applies registered decoders. This makes composition natural:

```iny
region: example:region(
  example:point(0, 64, 0),
  example:point(20, 80, 20)
)
```

Factories must return a non-null instance compatible with their declared result type. INY reports argument count, argument decoding, execution, and invalid-result failures with the configuration path and factory identifier.

Duplicate identifiers fail immediately. Use `replaceFactory` only when replacement is intentional and registration is still open; ownership transfers to the replacing plugin.

## Delay readiness for an integration

Use a readiness blocker when your factories depend on any plugin's asynchronous startup work:

```java
private ReadinessBlocker itemData;

@Override
public void onEnable() {
    itemData = INY.getInstance().readiness().registerBlocker(
            this,
            "item-data",
            Duration.ofSeconds(15),
            TimeoutPolicy.FAIL_READINESS
    );
}

public void onItemDataReady() {
    itemData.complete();
}

public void onItemDataFailure(Throwable cause) {
    itemData.fail(cause);
}
```

Choose `CONTINUE_WITH_WARNING` when degraded operation is valid. Choose `FAIL_READINESS` when consuming configuration without the dependency would be unsafe. Register blockers during startup; registration is rejected after the barrier closes.

## Lifecycle checklist

- Register factories and blockers synchronously during `onEnable()`.
- Consume configuration only from `InyReadyEvent` or `onInyReady`.
- `InyConfig` retains immutable parsed data, but factory calls are resolved against the registry snapshot current at the time of each read. Results are not cached.
- Keep Bukkit thread-affinity rules in mind for objects returned by Minecraft or plugin factories.
- Treat `net.iridiummc.iny.internal.*` and the Minecraft implementation package as unsupported implementation details.
- Use the [language reference](iny.md) for file syntax and the Javadocs for exhaustive method and exception details.
