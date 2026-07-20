# INY - INY's Not YAML

INY's Not YAML (INY) is a typed, extensible configuration library for Java and Bukkit plugins.

It was built as an alternative to YAML for projects that need more than basic key-value configuration. INY supports nested sections, lists, strict typed access, custom decoders, namespaced factory calls, and a shared Bukkit registry that other plugins can extend.

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

```java
String welcome = config.get("messages.welcome", String.class);
int playerLimit = config.get("limits.players", int.class);
List<String> worlds = config.getList("limits.worlds", String.class);
Location spawn = config.get("spawn", Location.class);
```

## Why INY exists

Most configuration formats work well until a project needs custom types, reliable validation, or integrations between plugins.

INY keeps the configuration syntax small while allowing Java code to define how domain-specific values are constructed.

For example, a plugin can register:

```java
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
```

Other plugins can then use that type directly in configuration:

```iny
daily: rewards:reward("rewards.daily", 250)
```

```java
Reward reward = config.get("daily", Reward.class);
```

The consumer does not need to parse the value manually or depend on the provider's internal configuration format.

## Features

* A whitespace-independent configuration syntax
* Nested sections and immutable lists
* Dotted and section-relative navigation
* Strict numeric decoding with range and precision checks
* Custom decoders for ordinary scalar or section representations
* Namespaced factory calls for constructing Java objects
* Lazy factory resolution
* Immutable configuration values
* Contextual diagnostics with source positions and configuration paths
* A standalone Java core with no Bukkit dependency
* A server-wide Bukkit registry shared between dependent plugins
* Factory ownership and automatic removal when provider plugins disable
* A readiness barrier for integrations with asynchronous startup work
* Built-in factories for common Bukkit and Minecraft types
* Java module boundaries that expose only supported API packages

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

## Language overview

Sections use braces:

```iny
database {
  host: "localhost"
  port: 5432
}
```

Lists use dashes:

```iny
worlds:
  - "world"
  - "world_nether"
  - "world_the_end"
```

Factory calls use namespaced identifiers:

```iny
icon: minecraft:item_stack("diamond", 3)
offset: minecraft:vector(0, 1.5, 0)
```

Indentation is only for readability and has no semantic meaning.

See the [language reference](docs/iny.md) for the complete syntax.

## Reading values

`InyConfig` is also its root section:

```java
String host = config.get("database.host", String.class);
int port = config.get("database.port", int.class);

InySection database = config.getSection("database");
String sameHost = database.get("host", String.class);
```

Use `find` for optional non-null values:

```java
Optional<String> prefix = config.find("messages.prefix", String.class);
```

Use `contains` when missing and explicit `null` must be distinguished:

```java
if (config.contains("integration.token")) {
    Object token = config.get("integration.token", Object.class);
}
```

Returned sections, entry maps, and lists are immutable.

## Custom decoders

A decoder converts a normal parsed value into one requested Java type.

```java
public record Port(int value) {}

public final class PortDecoder implements InyDecoder<Port> {
    @Override
    public Class<Port> targetType() {
        return Port.class;
    }

    @Override
    public Port decode(Object value, InyDecodeContext context) {
        int port = context.decode(value, Integer.class);

        if (port < 1 || port > 65_535) {
            throw context.failure("port must be between 1 and 65535");
        }

        return new Port(port);
    }
}
```

```java
Iny iny = Iny.builder()
        .registerDecoder(new PortDecoder())
        .build();
```

Use a decoder when a type has an ordinary representation such as:

```iny
port: 8080
```

Use a factory when the configuration should explicitly select a constructor-like operation:

```iny
origin: geometry:point(0, 64, 0)
```

## Bukkit readiness

Factories normally register synchronously during `onEnable()`.

A provider whose data becomes available later can register a readiness blocker:

```java
ReadinessBlocker blocker = INY.getInstance()
        .readiness()
        .registerBlocker(
                this,
                "item-data",
                Duration.ofSeconds(15),
                TimeoutPolicy.FAIL_READINESS
        );
```

Complete or fail it when the provider finishes loading:

```java
blocker.complete();
```

```java
blocker.fail(cause);
```

INY becomes ready only after Bukkit startup registration has closed and all active blockers have reached a terminal state.

## Built-in Bukkit factories

`iny-bukkit` includes factories for:

* `minecraft:world`
* `minecraft:material`
* `minecraft:vector`
* `minecraft:location`
* `minecraft:block`
* `minecraft:item_stack`

(This list is expected to be expanded in the near future)

Example:

```iny
spawn: minecraft:location("world", 0.5, 64, 0.5, 90, 0)
icon: minecraft:item_stack("diamond", 3)
```

## Error handling

INY errors retain context such as:

* source file and position;
* dotted configuration path;
* requested and actual Java types;
* factory identifier;
* factory argument index;
* nested causes.

Configuration errors are intended to be handled at the loading boundary:

```java
try {
    InyConfig config = iny.load(path);
} catch (InyException exception) {
    logger.error("Could not load configuration", exception);
}
```

Factories and decoders should use the supplied context helpers rather than discarding failures or throwing low-level exceptions directly.

## Documentation

* [Bukkit guide](docs/bukkit.md)
* [Embedding INY Core](docs/core.md)
* [Language reference](docs/iny.md)
* [Javadocs](#)

## Requirements

* Java 25
* A compatible Bukkit or Paper server for `iny-bukkit`

## Project status

INY 1.x provides the stable parser, typed configuration API, decoder system, factory model, and Bukkit lifecycle integration.

New functionality may be added in minor releases without breaking existing configuration or API usage. Breaking public API changes are reserved for major versions.

## Building

```bash
./gradlew build
```

The test suite covers parsing, diagnostics, numeric safety, typed navigation, custom decoding, factory resolution, registry ownership, concurrent reads, Bukkit startup readiness, plugin disable behaviour, filesystem containment, and exported API boundaries.
