# Embedding INY Core

Use INY Core when an application or library needs the parser and typed configuration model without Bukkit. The core is intentionally small: construct one immutable `Iny` service, parse configurations through it, and retain the resulting immutable `InyConfig` values.

## Add the core

INY requires Java 25.

```kotlin
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
