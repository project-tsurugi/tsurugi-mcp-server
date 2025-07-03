# tsurugi-mcp-server

[Model Context Protocol](https://github.com/modelcontextprotocol) server to  access [Tsurugi](https://github.com/project-tsurugi/tsurugidb).

## Limitations

- Tsurugi 1.5.0 or later.
- Java21 or later.

## Components

### Tools

- `listTableNames`
  - list table names.
- `getTableMetadata`
  - get table metadata (table schema).
  - parameter
    - `tableName` - table name (string, required)
- `query`
  - execute SQL (select).
  - parameter
    - `sql` - SQL (string, required)
    - `transaction_type` - `OCC`, `LTX`, `RTX`. (string, default: `RTX`)
    - `cursor` - To continue the previous query. (string)
  - If there is a continuation, `nextCursor` is returned.
- `update`
  - execute SQL (insert, update, delete).
  - parameter
    - `sql` - SQL (string, required)
    - `transaction_type` - `OCC`, `LTX`. (string, default: `OCC`)
    - `write_preserve` - write preserve table names. (string, required when `transaction_type` is `LTX`)
- `executeDdl`
  - execute DDL (create, drop)
  - parameter
    - `sql` - SQL (string, required)
    - `transaction_type` - `OCC`, `LTX`. (string, default: `OCC`)

### Resources

- table metadata

### Prompts

- `tableList-prompt`
  - Prompt to display the list of tables.
- `tableMetadata-prompt`
  - Prompt to display the table metadata.
- `query-prompt`
  - Prompt to display the table data.

## Configuration

### Usage with Claude Desktop

First, download the tsurugi-mcp-server jar file from [release page](https://github.com/project-tsurugi/tsurugi-mcp-server/releases).

To use this server with the [Claude Desktop](https://claude.ai/download), add the following configuration to the "mcpServers" section of your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "tsurugidb": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/tsurugi-mcp-server-all.jar",
        "-c", "tcp://localhost:12345"
      ]
    }
  }
}
```

- `command`
  - If the `java` command is not included in the PATH, specify the full path. (e.g., `"C:/Program Files/Java/jdk-21/bin/java"`)
- `args`
  - Specify the jar file by full path.
  - `-c` or `--connection` - the endpoint URL to connect Tsurugi. (required)
  - If you want to limit the tools used, add `--enable-tools`. (e.g., for read-only access: `"--enable-tools", "listTableNames, getTableMetadata, query"`)
  - If resources is not used, add `"--resource", "false"`.
  - If prompts is not used, add `"--prompt", "false"`.

## How to build

```bash
cd tsurugi-mcp-server
./gradlew shadowJar
ls build/libs/
```

## How to test

```bash
cd tsurugi-mcp-server
./gradlew test -Pdbtest.endpoint=tcp://localhost:12345
```

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
