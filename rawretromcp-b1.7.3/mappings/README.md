# Vendored mapping files for Path C (Feather → MCP remap)

These three Tiny v2 mapping files are the inputs to the eventual `feather → MCP` remap pipeline. They're committed to the repo so `./gradlew` doesn't need to fetch them on every clean build.

| File | Namespaces | Source | Size |
|---|---|---|---|
| `calamus-b1.7.3-client.tiny` | `official ↔ intermediary` | `net.ornithemc:calamus-intermediary:b1.7.3-client:v2` (Ornithe maven) | 141 KB |
| `feather-b1.7.3-client+build.26.tiny` | `intermediary ↔ named` (Feather) | `net.ornithemc:feather:b1.7.3-client+build.26:v2` (Ornithe maven) | 358 KB |
| `mcp-b1.7.3.tiny` | `named` (MCP) ↔ `client ↔ server` (official) | Copied from RetroMCP's `conf/mappings.tiny` | 273 KB |

The chain connects through **`official`** (Calamus ↔ MCP) and **`intermediary`** (Calamus ↔ Feather).

## How to refresh these if upstream updates

```bash
# Feather — bump build number if Ornithe ships a newer Feather build
curl -sL "https://maven.ornithemc.net/releases/net/ornithemc/feather/b1.7.3-client+build.26/feather-b1.7.3-client+build.26-v2.jar" -o feather.jar
unzip -p feather.jar mappings/mappings.tiny > feather-b1.7.3-client+build.26.tiny
rm feather.jar

# Calamus intermediary
curl -sL "https://maven.ornithemc.net/releases/net/ornithemc/calamus-intermediary/b1.7.3-client/calamus-intermediary-b1.7.3-client-v2.jar" -o calamus.jar
unzip -p calamus.jar mappings/mappings.tiny > calamus-b1.7.3-client.tiny
rm calamus.jar

# MCP — copy from RetroMCP's conf after updating RetroMCP-Java
cp "../../All Versions/RawRetroMCP-b1.7.3/conf/mappings.tiny" mcp-b1.7.3.tiny
```

See `docs/PATH_C_DESIGN.md` for the full plan of how these feed into the remap pipeline.
