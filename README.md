# Cream

A fabric mod that adds a minimalist, 16-bit computer to Minecraft, running a custom instruction set!

Compiled versions, user documentation and screenshots can be found on **[Modrinth](https://modrinth.com/mod/cream-elydre)**.

The compiler, basilisc operating system and instruction set documentation can be found on this **[GitHub repository](https://github.com/elydre/ancolie)**.

## Building

```sh
# build the mod and run the data generator
./gradlew runDatagen
./gradlew build

# test the mod in a client instance
./gradlew runClient

# find the compiled mod in build/libs
```

## Peripheral cables

Place one or more `Peripheral Cable` blocks between a computer and a peripheral
block. Cables can form branches and are searched up to 256 cable blocks.

Read port `0x1FFF` to index the connected peripherals and get their count.
The result is cached until the next index operation. Peripherals are numbered
in cable traversal order, starting at index `0`; access them with the two
arguments `portXIn(dev, port)` or `portXOut(dev, port, value)`. Port `0` of
each peripheral returns its type, so multiple peripherals of the same type can
coexist without a fixed device id.

The `Weather Peripheral` has type `1` and exposes:

- port `1`: rain (`0` or `1`)
- port `2`: thunder (`0` or `1`)
- port `3`: biome temperature multiplied by `1000`

For example, a program can read the first peripheral type with
`portXIn(0, 0)`, rain with `portXIn(0, 1)`, thunder with `portXIn(0, 2)`, and
temperature with `portXIn(0, 3)`. The second peripheral uses index `1`.
Writes are forwarded to peripherals as well, so future writable peripherals
can use the same interface.

## Future Plans

- Writable disks
- Interaction with storage blocks (chests, barrels, etc.)
- Communication between computers
