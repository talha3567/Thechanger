# MCTier Mod

MCTiers rank checker and automated queue mod for Fabric 1.21.5.

## Features
- `/rank <tier>`: Set the target rank (e.g., `ht3`, `lt1`). Starts the search.
- `/rank stop`: Stops the automated searching.
- Automated re-queueing on MCPVP if the opponent doesn't match the target rank.
- Explosion sounds and "GELDİ" overlay when a match is found.

## How to Build

1. Ensure you have **Java 21** installed.
2. Open a terminal in the project root.
3. Run the following command:
   ```bash
   ./gradlew build
   ```
4. The compiled mod JAR will be located in `build/libs/`.

## Requirements
- Fabric Loader 0.16.10 or newer.
- Minecraft 1.21.5.
- Fabric API.
