# tsurugi-mcp-serverの使用例

tsurugi-mcp-serverは[Tsurugi](https://github.com/project-tsurugi/tsurugidb)のデータベースを操作するMCPサーバーです。

tsurugi-mcp-serverを使うことにより、[Model Context Protocol](https://github.com/modelcontextprotocol)に対応したLLM（いわゆるAI）に日本語で質問や命令をして、Tsurugiのテーブルを参照したり更新したりすることができます。

例えば[Claude Desktop](https://claude.ai/download)で以下のように使うことができます。  
（Claude Desktopの設定方法は[README](../README.md)を参照してください。tsurugi-mcp-serverではLLMから実行できるツールをいくつか用意しており、Claude Desktopがツールを実行する際はユーザーに対して使用許可が求められます）

- **Tsurugiのテーブル一覧を見せて**
  - テーブル一覧が表示されます。
    - `listTableNames` ツールが実行されます。
- **productsテーブルの定義を教えて**
  - productsというテーブルの定義（カラムの情報）が表示されます。
    - `getTableMetadata` ツールが実行されます。
- **productsのデータを見せて**
  - productsテーブルのデータが表示されます。
    - 指定されたテーブルを参照するselect文が生成され、 `query` ツールが実行されます。
      - 何の条件も指定していないので、 `SELECT * FROM products` というselect文が生成されます。
    - データ量が多い場合は複数回 `query` ツールが実行されることがあります。
- **ノートパソコンのstockを10増やして**
  - productsテーブルのノートパソコンのレコードのstockカラムに10加算されます。
    - データを更新するupdate文が生成され、 `update` ツールが実行されます。
      - 先にproductsテーブルの内容を表示したので、product_nameが「ノートパソコン」であるデータのproduct_idが1であることが分かっており、`UPDATE products SET stock = stock + 10 WHERE product_id = 1` というupdate文が生成されます。
- **サンプルのテーブルを作って**
  - 適当なテーブルが作られます。
    - `listTableNames` ツールを実行してテーブル一覧を確認し、存在しないテーブル名を使ってcreate文を生成して、 `executeDdl` ツールで実行します。さらにサンプルデータを登録するために `update` ツールでinsert文を実行することがあります。

> [!NOTE]
>
> tsurugi-mcp-serverの起動オプションを指定することで、実行できるツールを限定する（例えば更新系の `update` ツールや `executeDdl` ツールを使えないようにする）こともできます。

