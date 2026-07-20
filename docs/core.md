# Embedding INY Core

Use INY Core when an application or library needs the parser and typed configuration model without Bukkit. The core is intentionally small: construct one immutable `Iny` service, parse configurations through it, and retain the resulting immutable `InyConfig` values.

## Add the core

INY requires Java 25.

```kotlin
repositories {
    maven("https://maven.iridiummc.net/releases")
}

dependencies {
    implementation("net.iridiummc:iny-core:<version>")
}
```

For a modular application:

```java
module example.application {
    requires net.iridiummc.iny.core;
}
```

Only exported packages are supported API. Do not import `net.iridiummc.iny.internal.*`.

## Build one service

The default service includes strict decoders for strings, booleans, Java numeric primitives and wrappers, `BigInteger`, `BigDecimal`, lists, sections, and `Object`:

```java
Iny iny = Iny.builder().build();
```

Build the service once for an application or component rather than once per file. Its decoder and factory registries are immutable and safe to share for concurrent reads.

Parse text, a named reader, or a UTF-8 file:

```java
InyConfig fromText = iny.parse("example.iny", sourceText);
InyConfig fromReader = iny.parse("embedded-default.iny", reader);
InyConfig fromFile = iny.load(Path.of("config.iny"));
```

Reader overloads consume but do not close caller-owned readers.

## Navigate the configuration

Given:

```iny
service {
  host: "localhost"
  port: 8080
  tags:
    - "public"
    - "http"
}
```

Read it with dotted or subsection-relative paths:

```java
InyConfig config = iny.parse(source);

String host = config.get("service.host", String.class);
int port = config.get("service.port", int.class);
List<String> tags = config.getList("service.tags", String.class);

InySection service = config.getSection("service");
String sameHost = service.get("host", String.class);
```

`InyConfig` is its root section: `config.root() == config`. Returned sections, entry maps, and lists are immutable.

Use `Object.class` when the configuration deliberately permits multiple types:

```java
Object endpoint = config.get("service.endpoint", Object.class);

if (endpoint instanceof String text) {
    // string form
} else if (endpoint instanceof InySection section) {
    // structured form
}
```

Object reads expose Java values, never internal syntax-tree nodes. Integers are `BigInteger` and decimals are `BigDecimal` until decoded to a requested numeric type.

Use `find(path, type)` for optional non-null values. Because `Optional` cannot represent an explicit null, use `contains(path)` when missing and null must be distinguished.

## Add a custom decoder

A decoder converts an ordinary parsed Java value into one exact requested type. It is a good fit when configuration uses a normal scalar or section representation rather than an explicit constructor-like call.

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

Register it before building:

```java
Iny iny = Iny.builder()
        .registerDecoder(new PortDecoder())
        .build();

Port port = iny.parse("port: 8080").get("port", Port.class);
```

Decoder lookup uses the exact target class. Duplicate registrations are rejected. `replaceDecoder` is the explicit operation for intentionally overriding an existing decoder.

For a section-shaped value, check for `InySection` and use `context.decodeChild(...)` when decoding a child so failures retain a useful path.

## Add a factory

A factory handles an explicit namespaced call and can construct any non-null Java reference type:

```java
public record Point(double x, double y, double z) {}

Iny iny = Iny.builder()
        .registerFactory("geometry:point", Point.class, context -> {
            context.arguments().requireCount(3);
            return new Point(
                    context.arguments().get(0, Double.class),
                    context.arguments().get(1, Double.class),
                    context.arguments().get(2, Double.class)
            );
        })
        .build();
```

```iny
origin: geometry:point(0, 0, 0)
```

```java
Point origin = iny.parse(source).get("origin", Point.class);
```

Parsing stores calls unevaluated. Resolution occurs when a value is requested, and results are not cached. Factories may execute more than once for repeated reads and should normally be deterministic and free of externally visible side effects, unless intended. A missing factory therefore does not prevent parsing but fails when that call is resolved.

Use a decoder for an ordinary representation such as `port: 8080`. Use a factory when the syntax should explicitly select construction, such as `origin: geometry:point(0, 0, 0)`.

## Evaluate values at runtime

Runtime providers and runnables separate configuration construction from execution:

- `InyProvider<T>` resolves a value from an `InyRuntimeContext`.
- `InyRunnable` performs an action with an `InyRuntimeContext`.
- `InyProvider<T>` is also an `InyRunnable`; running it discards the result.

Context keys are typed, namespaced declarations. Registering a key makes the built-in `context:value("namespace:key")` provider available. The context registry defines the keys and their Java types; each immutable runtime context supplies values for any required subset.

This complete example captures a different message on each execution:

```java
InyContextKey<String> messageKey =
        InyContextKey.of("example:message", String.class);

AtomicReference<String> received = new AtomicReference<>();

Iny iny = Iny.builder()
        .registerContextKey(messageKey)
        .registerRunnable("example:capture", factoryContext -> {
            factoryContext.arguments().requireCount(1);

            InyProvider<String> message =
                    factoryContext.arguments().getProvider(0, String.class);

            return runtime -> received.set(message.resolve(runtime));
        })
        .build();

InyConfig config = iny.parse("""
        action: example:capture(
          context:value("example:message")
        )
        """);

InyRunnable action = config.getRunnable("action");

action.run(InyRuntimeContext.builder()
        .put(messageKey, "first")
        .build());

action.run(InyRuntimeContext.builder()
        .put(messageKey, "second")
        .build());
```

During the configuration phase, retrieving `action` invokes `example:capture` and constructs an `InyRunnable`. It does not resolve the message or run the action. During each runtime phase, the runnable passes its caller-supplied context into the captured provider. Provider results are not cached, so the two calls above observe `first` and `second` respectively.

`getProvider(index, type)` on factory arguments and `getProvider(path, type)` on sections use the same lifting rules:

- A provider remains deferred and its result is checked on every resolution.
- A static value is decoded once and becomes a constant provider.
- An ordinary factory result is constructed and decoded once, then becomes a constant provider.
- A runnable-only value is rejected because it cannot produce a result.

Consequently, this configuration also works without any context value:

```iny
action: example:capture("constant")
```

Ordinary `get(...)` remains a configuration-time operation. It never calls `InyProvider.resolve(...)` or `InyRunnable.run(...)`, and it does not implicitly convert a provider to its result type. Use `getProvider(...)` or `getRunnable(...)` to cross the runtime boundary explicitly.

Runtime contexts and provider results are non-null in 1.1. A missing required value throws `InyMissingContextValueException` when the provider executes. A null or incompatible provider result throws `InyInvalidProviderResultException` at the path whose requested result type is known. Contexts are immutable; `with(key, value)` derives a new context without changing its parent.

INY is thread-neutral. Contexts are safe to share, but providers and runnables are only as thread-safe as the services and objects they capture. INY does not schedule their execution.

### Run actions in sequence

INY Core always registers `core:sequence`, which constructs one runnable from zero or more runnable arguments:

```iny
on_use: core:sequence(
  example:remove_cost(),
  example:grant_reward(),
  example:notify_player()
)
```

Each action runs in declaration order with the exact same `InyRuntimeContext`. Sequences may be empty or nested. Providers are valid sequence steps because `InyProvider<T>` extends `InyRunnable`; their results are discarded. Execution stops normally if a step throws. The `core` namespace is reserved for factories supplied by INY Core.

## Group an integration as a module

An `InyModule` is a lightweight way to install related decoders and factories:

```java
public final class ApplicationConfigModule implements InyModule {
    @Override
    public void configure(Iny.Builder builder) {
        builder.registerFactory("geometry:point", Point.class, this::point);
        builder.registerDecoder(new PortDecoder());
    }

    private Point point(InyFactoryContext context) {
        context.arguments().requireCount(3);
        return new Point(
                context.arguments().get(0, Double.class),
                context.arguments().get(1, Double.class),
                context.arguments().get(2, Double.class)
        );
    }
}
```

```java
Iny iny = Iny.builder()
        .install(new ApplicationConfigModule())
        .build();
```

Modules have no lifecycle or discovery mechanism; they are ordinary code called while configuring a builder.

## Use a live factory registry only for adapters

Ordinary applications should build a fixed service. A lifecycle-owning adapter may instead keep the decoders fixed while supplying immutable factory snapshots:

```java
Iny base = Iny.builder().build();
AtomicReference<InyFactoryRegistry> factories =
        new AtomicReference<>(base.factories());

Iny live = base.withFactoryRegistry(factories::get);
```

Publish a newly constructed `InyFactoryRegistry` whenever registrations change; never mutate a shared map behind a snapshot. The supplier must always return a non-null registry. The Bukkit layer uses this pattern to remove factories when provider plugins disable.

## Handle failures at the boundary

INY exceptions retain useful boundary information such as source position, dotted path, requested Java type, actual Java-side type, factory identifier, and argument index. In application code, catch the narrow exception relevant to the operation or catch `InyException` at the configuration-loading boundary to present one user-facing diagnostic.

Avoid catching and discarding failures inside decoders or factories. Use `context.failure(...)` in decoders and argument helpers in factories so INY can retain the path and cause.

See the [language reference](iny.md) for accepted syntax and the Javadocs for the complete API and exception accessors.
