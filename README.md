# INY - INY's Not YAML

INY's Not YAML (INY) is a typed, extensible configuration library for Java and Bukkit plugins.

It was built as an alternative to YAML for projects that need more than basic key-value configuration. INY supports nested sections, lists, strict typed access, custom decoders, namespaced factory calls, runtime providers and actions, and a shared Bukkit registry that other plugins can extend.

```yaml
messages {
  welcome: "Welcome to the server"
}

limits {
  players: 100
  worlds:
    - minecraft:world("world")
    - minecraft:world("the_nether")
}

spawn: minecraft:location("world", 0.5, 64, 0.5)
```

```java
String welcome = config.get("messages.welcome", String.class);
Integer playerLimit = config.get("limits.players", Integer.class);
List<World> worlds = config.getList("limits.worlds", World.class);
Location spawn = config.get("spawn", Location.class);
```

## Why INY exists

The most widely used configuration format in the Minecraft server ecosystem, YAML, has many shortcomings when it comes to custom types.

For example, in the case of creating an ItemStack from a configuration file, a plugin may do this

```yaml
item:
  material: "DIAMOND"
  count: 5
```

```java
String materialName = config.getString("item.material");
Integer count = config.getInt("item.count");
Material material = Material.get(materialName);
// Validation...
ItemStack itemStack = new ItemStack(material, count);
```

While this is functional for simple items, it fails to allow for more complex items

Spigot does in fact allow you to serialize items, though this quickly creates an unwieldy configuration, often with plenty of unncessary information

INY aims to fix this for allowing for factories to abstract out the construction of custom data types

```yaml
item: minecraft:item_stack("DIAMOND, 5")
```

```java
ItemStack itemStack = config.get("item", ItemStack.class);
```

This allows for more complex and descriptive representations of custom types, as well as inter-plugin communication without dependencies.

## Features

* A white-space independent syntax
* Nested sections, lists, etc.
* Dotted paths and section-relative navigation
* Built-in decoders for ordinary scalar/section values
* Namespaced factories for constructing Java objects
* Typed runtime contexts, deferred providers, and deferred runnables
* Contextual diagnostics with source positions and configuration paths
* A standalone Java core with no Bukkit dependency for custom implementation
* A server-wide Bukkit registry for inter-plugin communication
* Factory ownership with automatic removal of factories on plugin disable
* A readiness barrier for server start-up
* Built-in factories and runnables for common Bukkit/Minecraft use cases

## Modules

### `iny-core`

The standalone parser, configuration model, decoder system, and factory registry.

Use this for normal Java applications or libraries that do not depend on Bukkit.

```kotlin
repositories {
    maven("https://maven.iridiummc.net/releases")
}

dependencies {
    implementation("net.iridiummc:iny-core:<version>")
}
```

```java
Iny iny = Iny.builder().build();
InyConfig config = iny.load(Path.of("config.iny"));
```

### `iny-bukkit`

The Bukkit integration and normal entry point for server plugins.

It provides one shared INY service, lifecycle-aware factory registration, configuration loading from plugin data folders, readiness handling, and built-in Minecraft factories.

```kotlin
repositories {
    maven("https://maven.iridiummc.net/releases")
}

dependencies {
    compileOnly("net.iridiummc:iny-bukkit:<version>")
}
```

```yaml
depend: [INY]
```

```java
@Override
public void onEnable() {
    INY.getInstance().onInyReady(() -> {
        InyConfig config = INY.getInstance().loadConfig(this);
        startPlugin(config);
    });
}
```

Do not create a private `Iny` service inside a Bukkit plugin. Use `INY.getInstance()` so every plugin observes the same registry and lifecycle.


## Documentation

Documentation can be found in [the wiki]("https://github.com/rainaisntbald/INY/wiki")

## Requirements

* Java 25
* A compatible Bukkit or Paper server for `iny-bukkit`

## Building

```bash
./gradlew build
```