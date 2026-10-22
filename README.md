# Cream

A fabric mod that adds a minimalist, 16-bits computer to Minecraft, running a custom instruction set!

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

## Future Plans

- Peripheral blocks
- Writable disks
- Interaction with storage blocks (chests, barrels, etc.)
- Communication between computers
