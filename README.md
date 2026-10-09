# Bond of the Beast

Local test build 1.1.0-beta_23 for Minecraft 1.20.1, Fabric, and Shape Shifter Curse 1.9.2. Java sources target Java 17; run Gradle with JDK 21.

Voluntary bond abilities unlock while a pet wears its collar: after 30 minutes, 2 hours, and 6 hours. The grimoire shows pet cards with a 3D preview and a button to open controls.

Current command unlock rules and the status of the legacy XP counter are documented in [BOND_RULES_RU.md](BOND_RULES_RU.md).

## Local test launch

Run these batch files from this directory. Each opens a console window with its process logs:

- `run-local-server.bat` starts the local server with a visible console.
- `run-owner-client.bat` starts the owner client.
- `run-pet-client.bat` starts the pet client.
- `run-two-players.bat` starts the server, then both clients. In each game, connect to `127.0.0.1:25565`.

The same Gradle commands can be run manually from this directory:

```powershell
.\gradlew.bat runLocalServer
.\gradlew.bat runClientOwner
.\gradlew.bat runClientPet
```

On first server launch, accept the Minecraft EULA in `run/local-server/eula.txt` and start the server again. Each process stores its world and settings under a separate directory in `run/`.

For development, use `.\gradlew.bat runClient`, `.\gradlew.bat build`, or `.\gradlew.bat test`. No automated tests are configured yet; Gradle reports `NO-SOURCE` for `test`.
