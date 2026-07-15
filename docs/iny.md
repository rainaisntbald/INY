# INY language reference

INY (“It’s Not YAML”) is an indentation-independent configuration language. This document describes the file format; use the [Bukkit guide](bukkit.md) or [Core guide](core.md) for Java integration.

## Documents and entries

A document is a root section containing key/value entries. Empty documents are valid.

```iny
name: "Example"
enabled: true
attempts: 3
```

Bare keys match `[A-Za-z_][A-Za-z0-9_-]*`. Quoted keys and keys containing dots are not supported.

Entries must be separated by a line boundary. A `#` starts a comment through the end of its line. Blank lines and comments do not affect structure.

```iny
# Service identity
name: "Example"

enabled: true # inline comment
```

Leading whitespace is ignored. It may improve readability, but it never chooses a parent or nesting level.

## Sections

Sections use braces. The separator before an opening brace may be omitted:

```iny
server {
  host: "localhost"
  limits: {
    players: 100
  }
}
```

These forms are equivalent:

```iny
database {
  host: "localhost"
}

cache: {
  host: "localhost"
}
```

An empty section is written as `{}`:

```iny
options: {}
```

Duplicate keys within the same section are rejected. The diagnostic identifies both declarations.

## Scalar values

INY supports:

- Quoted strings.
- Bare identifiers, represented as strings.
- Arbitrary-precision integers.
- Arbitrary-precision decimal and exponent numbers.
- `true` and `false`.
- `null`.

```iny
quoted: "hello world"
bare: production
integer: 9000000000
decimal: 12.50
exponent: 1.25e3
enabled: true
optional: null
```

Quoted strings support `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, and `\uXXXX` escapes.

Integers and decimals remain exact in the parsed model. Java typed reads decide whether a number safely fits the requested target type; the language itself does not impose Java primitive ranges.

## Lists

A list begins after a key and line boundary. Every item uses a dash as the first non-whitespace token on its logical line:

```iny
worlds:
  - "world"
  - "world_nether"
  - fallback
```

Indentation in that example is visual only. These items are supported:

- `- value` adds a scalar or factory call.
- `- { ... }` adds a braced section.
- A standalone `-` introduces a child list beginning on following lines.

```iny
servers:
  - {
    name: "lobby"
    enabled: true
  }
  - {
    name: "survival"
    enabled: false
  }
```

Entries inside a braced section still need logical line separation.

### Nested lists

Because indentation has no meaning, nested list ownership is expressed with standalone dashes. This is a list containing two child lists:

```iny
matrix:
-
- 1
- 2
-
- 3
- 4
```

A child list ends before the next standalone dash, which returns to its parent. A normal list ends before the next section entry, a closing brace, or end of input.

Once a nested child begins, a following `- scalar` belongs to that child rather than its parent. Use another standalone dash for a sibling child or end the parent list with the surrounding section structure.

There is no empty-list spelling in INY v1.

## Factory-call expressions

A namespaced identifier followed by parentheses is a factory call:

```iny
spawn: minecraft:location("world", 10, 64, -20)
```

Namespaces match `[a-z0-9][a-z0-9_.-]*`. The value after the colon is a lowercase path using letters, digits, `_`, `-`, `.`, or `/`; it cannot end in `/` or contain `//`.

Calls accept zero or more comma-separated values. Arguments may be scalars, `null`, sections, lists, or nested calls:

```iny
empty: example:empty()

region: example:region(
  example:point(0, 64, 0),
  example:point(20, 80, 20)
)

wrapped: example:wrapper({
  name: "inside"
})
```

Newlines may appear between arguments. Trailing commas are not supported.

A namespaced value is treated as a call only when a valid identifier is followed by `(`. Calls are parsed without consulting a Java factory registry and are resolved later when requested through the API.

## Dotted API paths

Java lookups navigate nested sections with dotted paths:

```iny
server {
  limits {
    players: 100
  }
}
```

The value above is addressed as `server.limits.players`. Each path segment follows the same bare-key rule as file keys. Empty segments, surrounding whitespace, quoted segments, and escaped dots are not supported.

Dotted paths are an API feature, not key syntax: dots cannot appear inside an INY key.

## Structural line rules

A scalar may appear on the same line as its colon:

```iny
name: "example"
```

If a value begins on a later line, it must be structural—a braced section or a dash list:

```iny
settings:
  {
    enabled: true
  }

labels:
  - "one"
  - "two"
```

This rule prevents a missing scalar value from accidentally consuming the next key.

Both LF and CRLF line endings are accepted.

## Compact grammar

```text
document         = newlines, { entry, newlines }, EOF ;
entry            = key, ( "{", section-body, "}"
                       | ":", inline-value
                       | ":", newlines, structural-value ) ;
inline-value     = value ;
structural-value = "{", section-body, "}" | list ;
section-body     = newlines, { entry, newlines }, [ "}" lookahead ] ;
list             = list-item, { newlines, list-item } ;
list-item        = line-dash, value
                 | line-dash, "{", section-body, "}"
                 | line-dash, newlines, child-list ;
child-list       = list, stopping before the next standalone line-dash ;
scalar           = quoted-string | number | boolean | "null" | bare-identifier ;
value            = scalar | call | "{", section-body, "}" | list ;
call             = namespaced-identifier, "(", newlines,
                   [ value, { newlines, ",", newlines, value } ],
                   newlines, ")" ;
namespaced-identifier = identifier, ":", namespaced-value ;
namespace        = /[a-z0-9][a-z0-9_.-]*/ ;
namespaced-value = lowercase letters, digits, "_", "-", ".", or "/",
                   not ending in "/" and not containing "//" ;
key              = /[A-Za-z_][A-Za-z0-9_-]*/ ;
newlines         = one or more LF or CRLF boundaries, including comment lines ;
line-dash        = "-" as the first non-whitespace token of a line ;
```
