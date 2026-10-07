# Just Enough Guns New — プレイヤー・サーバー Wiki

**基準言語:** English · **対応リリース:** `1.8.2`

[Wiki 索引](README.md) · [English](en-US.md) · [简体中文](zh-CN.md) · [Deutsch](de-DE.md) · [Español](es-ES.md)

## この MOD について

Just Enough Guns New は、MigaMi の Forge 1.20.1 向け **Just Enough Guns** を現代の環境へ移植した非公式プロジェクトです。バニラに近いサバイバル進行を保ちながら、銃器、マガジン、アタッチメント、敵対ガンナー、派閥レイド、Walkürenritt 車両、特殊装備、航空脅威を追加します。

プロジェクトは Fabric と NeoForge の独立したモジュールに分かれています。サーバーと接続するすべてのクライアントで、Minecraft のバージョン、ローダー系列、MOD バージョンを一致させてください。

## クイックスタート

1. [互換性表](#互換性)から、Minecraft とローダーに合う行を選びます。
2. 対応するローダー、Fabric API または NeoForge、表に記載された GeckoLib をインストールします。
3. 対応する `jegn-1.8.2` JAR をインスタンスの `mods` フォルダーへ入れます。Fabric と NeoForge の JAR を混在させないでください。
4. 一度起動してテストワールドを作成またはコピーし、MOD リストに Just Enough Guns が表示されることを確認します。
5. レシピ、キー設定、サーバー設定、追加 MOD の依存関係を確認するまでは、本番ワールドでテストしないでください。

### 最初のセッションのチェックリスト

- 対応する作業台、弾薬、マガジンを作成または見つけます。
- マガジン式の銃には、正しい弾薬をマガジンへ装填してから取り付けます。
- キー設定の競合を確認し、射撃、照準、リロード、インスペクトを試します。
- 予備マガジン、修理または冷却アイテム、射撃モードに合う弾薬を持ち歩きます。
- 新しいサーバーでは通常のガンナーから始め、大規模なレイド、車両、Terror Phantom は後から有効にします。

## 互換性

| ローダー | Minecraft | Java | MOD | 必須依存関係 |
| --- | --- | --- | --- | --- |
| Fabric | 1.21.1 | 21 | 1.8.2 | Fabric API、GeckoLib 4.8.3 |
| NeoForge | 1.21.1–1.21.4 | 21 | 1.8.2 | NeoForge 21.1.x、GeckoLib 4.8.3 |
| Fabric | 26.2 | 25 | 1.8.2 | Fabric API、GeckoLib 5.5+ |
| NeoForge | 26.2 | 25 | 1.8.2 | NeoForge 26.2.x、GeckoLib 5.5.1 |
| Fabric | 26.3 | 25 | 1.8.2 | Fabric API、GeckoLib 5.5.7 |
| NeoForge | 26.3 | 25 | 1.8.2 | NeoForge 26.3.x、GeckoLib 5.5.7 |

Fabric 26.1 と NeoForge 26.1 はレガシー系列です。特別なパック要件がなければ、Java 25 の維持系列には 26.2 を使用してください。

## 操作方法

### 銃器

| 操作 | デフォルト入力 |
| --- | --- |
| 射撃 | 左クリック |
| エイム | 右クリック |
| リロード | `R` |
| アニメーション銃のインスペクト | `Y` |
| 銃剣格闘またはライト切替（対応時） | `V` |
| スニーク関連（対応時） | `Shift` |

これは 1.3.0 以降の初期設定です。古いリリースでは右クリック射撃、`F` リロード、`Shift` エイムでした。アップデート後はキー設定を確認してください。

### 車両

| 操作 | デフォルト入力 |
| --- | --- |
| 乗車またはインタラクト | 右クリック |
| 操舵と加速 | `W` / `A` / `S` / `D` |
| ブレーキまたは後退 | `S` |
| 車載武器を向ける | マウス移動 |
| 現在の武器を発射 | 左クリック |
| 照準、ロック、ズーム、副機能 | 右クリック |
| リロード（対応時） | `R` |
| 降車 | `Shift` |

座席によって使用できる操作が変わります。運転席は移動を担当し、武器席や副操縦席は砲塔、ミサイル、ターゲットロック、デコイを担当する場合があります。

## ゲームプレイガイド

### 銃器と弾薬

ピストル、リボルバー、ライフル、SMG、ショットガン、機関銃、ランチャー、弓、火炎放射器系武器、終盤の特殊銃を使用できます。多くの武器には対応する弾薬またはマガジンが必要です。

マガジン式武器は装填済みマガジンを使い、手動式・単発武器は対応する弾薬を直接使います。対応するライフル、SMG、ショットガンには拡張マガジンとドラムマガジンがあります。アタッチメント、ストック、グリップ、サイト、スキン、バッジ、特殊弾薬は通常のサバイバル進行に含まれます。

反動、移動時の拡散、オーバーヒート、動的クロスヘア、ヒットマーカー、マズルエフェクト、弾道が武器の状態を示します。発射できないときは、弾薬、マガジン、熱量、リロード状態、サーバー設定を確認してください。

### 防弾装備

防弾ヘルメットとベストは、バニラの Projectile Protection ではなく、弾薬と銃の倍率から計算した徹甲値と装甲値の比較で銃弾を軽減します。頭部への防護ヒットにはヘルメット、それ以外の防護ヒットにはベストを使います。対応する装備がなければ銃ダメージは変わりません。

装甲値を下回る弾でも一部ダメージを与えて耐久値を減らします。装甲値を上回る弾はより大きなダメージと耐久圧力を与えます。正確な数値は分岐ごとのデータで変わるため、対象バージョンのゲーム内説明とソースを優先してください。

### ガンナー、派閥、レイド

ガンナーはゾンビ、スケルトン、ピグリン、ピリジャー/ヴィンディケーター、ファントム、グール、Parched 系などに登場します。派閥イベントはパトロールと派閥の不吉な予兆から、帰還時のレイド、レイドフレア、ボスバー、設定可能なウェーブへ進みます。C4 ベスト爆撃ガンナーなどの種類はサーバー設定で制御されます。

### 車両

Walkürenritt には組み立て式の陸上車両、ボート、航空機、ヘリコプター、固定武器プラットフォームがあります。車両には座席、インベントリ、修理ツール、充電またはエネルギー、ミサイル、デコイ、専用 HUD があります。

組み立て手順を完了してからワールドへ展開します。必要な弾薬、修理ツール、充電アイテムを車両が要求するインベントリへ入れ、武器準備、リロード、ロック警告、ダメージ、デコイ状態を HUD で確認してください。

敵車両 AI は、車種に応じてパトロール、追跡、後退、不適切な地形の回避、砲塔操作を行います。維持対象の各分岐はプレイヤーから見える車両挙動を共有しますが、ローダー側の実装は別々です。

### 特殊装備

- **FPV ドローン:** モニターでカメラとペイロードを操作し、爆発ペイロードをカミカゼ降下へ移行できます。
- **C4 とクレイモア:** 正しい起爆装置またはトリガーを使い、敵の C4 を撤去するときは C4 デフューザーを持ちます。
- **C4 ベスト:** 設定可能な爆撃ガンナーが爆発ベストを装備します。
- **Javelin と Igla 9K38:** 射程内かつ視線が通った対象をロックし、スモークはミサイルロックを妨害します。
- **車両ミサイルロック HUD:** 対象が有効・可視・射程内になるとシークフレームと音声を表示します。

### Terror Phantom

Terror Phantom は、Bound Terror Phantom、ファントムガンナー召喚、設定可能な死亡爆発、End Ship Armada を含む稀な空中脅威です。1.8.0 の特殊装備系列では自然スポーンがデフォルトでソフト無効化されています。サーバー管理者は設定から再有効化や調整ができます。

## 難しすぎる、または簡単すぎる？

一度に一つの項目だけを変更し、数日プレイしてから次を調整してください。以下はこの Fabric 26.2 ブランチで実際に使えるコマンドです。設定変更には OP レベル 2 が必要です。

| プレイヤーが感じること | 最初に試す調整 | 結果 |
| --- | --- | --- |
| 序盤のガンナーが強すぎる | `/justEnoughGuns config combat naturalGunnerDynamicDifficultyEnabled false` | 周囲の最強装備に合わせる動的補正を止めます。 |
| 帰宅するたびにパトロールが来る | `/justEnoughGuns config patrol minimumDays 15` または `... spawnChance 0.15` | 出現開始日または確率を下げます。 |
| 腰だめ射撃が当たらない | `/justEnoughGuns config combat hipFireSpreadMultiplier 1.0` | 標準値 1.5 より拡散を狭めます。 |
| マガジン運用が重い | `/justEnoughGuns config combat magazineFeed false` | 対応銃を弾薬の直接給弾にします。接続中のプレイヤーに弾薬ルールが表示されます。 |
| ロケットや C4 が早すぎる | ゲーム内設定の **Mobs** で **All Gunners** を選び、`Rocket Launcher Start Day` または `Bomber Gunner Start Day` を上げる | 通常のガンナーを残したまま特殊脅威だけ遅らせます。 |
| 敵車両が基地を圧倒する | `/justEnoughGuns config vehicle enemySpawning enabled false` | 自然な敵車両変換を止め、プレイヤー車両は残します。 |
| 爆発の画面揺れが強い | `config/jeg-client.toml` の `rendering.explosionScreenShake = 0` | 自分のカメラ効果だけを無効化します。ダメージは変わりません。 |

すべてのガンナー確率を 0 にする必要はありません。パトロール、派閥レイド、自然ガンナー成長、敵車両は別々の仕組みです。成長設定の `-1` はバランス標準値または **All Gunners** の上書き値を継承します。

## ゲーム内サーバー設定画面

Fabric 26.2 では一時停止画面上部の **JEGN Configuration** ボタンから開きます。サーバー側と同じ MOD バージョンを使い、OP レベル 2 が必要です。シングルプレイではチート、専用サーバーでは `/op <player>` で権限を付与します。

1. `Esc` → **JEGN Configuration** を押します。
2. **Interface / Patrols / Mobs / Combat / Vehicles** からカテゴリを選びます。
3. トグルを押すか数値欄に入力します。**Mobs** では左右ボタンでガンナー種類を選びます。
4. ラベルにマウスを置くと、対応コマンド、範囲、`-1` 継承規則が表示されます。
5. **Apply** で検証・保存、**Reset** で現在のカテゴリだけ初期値に戻します。未保存のまま **Done** を押すと破棄確認が出ます。

サーバー設定画面で変更できる主な項目は、UI の准星・ヒットフィードバック、パトロール、ガンナー成長、弾薬マガジン方式、腰だめ拡散、地形支援、派閥レイド難易度、敵車両出現です。クライアントだけの HUD や画面揺れは `jeg-client.toml` を編集します。

## コマンドリファレンス

すべて `/justEnoughGuns` から始まります。各語の後で `Tab` を押すと候補が出ます。設定値を省略すると現在値を表示し、値を付けると保存します。以下の設定変更は OP レベル 2 が必要です。

```text
/justEnoughGuns unlockGunRecipes
/justEnoughGuns spawnPatrol <faction> <size> <pos> [forceGuns] [spawnRadius]
/justEnoughGuns simulatePatrol <faction> <size> <player> [forceGuns]
/justEnoughGuns config patrol minimumDays 15
/justEnoughGuns config patrol spawnChance 0.15
/justEnoughGuns config combat hipFireSpreadMultiplier 1.0
/justEnoughGuns config combat magazineFeed false
/justEnoughGuns config vehicle enemySpawning startDay 120
/justEnoughGuns config mob spawn all bomberStartDay 100
```

派閥名は `night_of_the_undead`、`the_rattlers`、`nosy_business`、`bad_piggies`、`hell_hogs`、`lost_souls` です。パトロールサイズは 1–20、出現半径は 0–16（省略時 10）です。`unlockGunRecipes` はテスト用に全銃レシピを与えるプレイヤー専用コマンドです。Peaceful ではパトロールを生成できません。

`config mob spawn` の種類は `all`、`skeleton`、`stray`、`zombie`、`husk`、`parched`、`drowned`、`zombieVillager`、`zombifiedPiglin`、`piglin`、`piglinBrute`、`witherSkeleton`、`pillager`、`vindicator`、`generic`。設定名は `minSpawnChance`、`maxSpawnChance`、`spawnChancePerDay`、`weaponInitialTier`、`weaponMaxTier`、`weaponTierPerDay`、`armorInitialTier`、`armorMaxTier`、`armorTierPerDay`、`rocketLauncherStartDay`、`rocketLauncherChance`、`rocketLauncherMaxChance`、`rocketLauncherChancePerDay`、`bomberStartDay`、`bomberChance`、`bomberMaxChance`、`bomberChancePerDay`、`weaponAggression` です。

## 設定ファイル

実行中のサーバーが上書きしないよう、直接編集する場合は停止してください。`config/jeg-client.toml` では `rendering.showAmmoHud`、`rendering.showTimersHud`、`rendering.crosshair`、`rendering.showHitmarker`、`rendering.dynamicCrosshairDotMode`、`rendering.explosionScreenShake`（0–100）を調整できます。`config/jeg-server.toml` では `combat.magazineFeed`、`combat.hipFireSpreadMultiplier`（0–5）、`factionPatrol.*`、`factionRaid.*`、`vehicle.enemyVehicle*` を調整できます。`magazineFeed` を変更すると弾薬ルール表示と制限レシピが更新されます。

## サーバー管理

リアルタイム変更は上の[ゲーム内サーバー設定画面](#ゲーム内サーバー設定画面)、素早い試験は[コマンドリファレンス](#コマンドリファレンス)、画面にない高度な値は `config/jeg-server.toml` を使います。TOML を直接編集するときはサーバーを停止し、ワールドをバックアップしてください。

## トラブルシューティング

### ゲームが起動しない

互換性表で Minecraft、ローダー、Java、GeckoLib のバージョンを確認します。重複 JAR や別ローダーの JAR を削除し、必要な依存関係と Just Enough Guns だけで試します。

### 専用サーバー起動時にクラッシュする

JAR がサーバーのローダーと Minecraft に一致しているか確認してください。NeoForge サーバーへクライアント専用 MOD や Fabric JAR を入れないでください。クリーンなテスト環境で再現し、最初の起動ログを保存します。

### 銃、レシピ、車両が表示されない

再起動後にレシピブック、アイテム検索、サーバーログを確認します。クライアントとサーバーが同じ MOD ファイルを使い、依存関係も同じ Minecraft 系列であることを確認してください。レシピ欠落を報告するときはアイテム ID とログ行を添えます。

### キーや HUD が表示されない

キー設定で競合を検索してデフォルトへ戻し、動的クロスヘア、ヒットマーカー、弾薬 HUD を有効にして試します。車両 HUD は座席と現在の武器にも依存します。

### ミサイルがロックできない

距離、視線、対象種類、弾薬、ランチャー状態を確認します。スモークは意図的にロックを妨害します。ロックフレームは、射程内で視認できる有効対象にだけ表示されます。

## バグ報告

リポジトリの issue に次の情報を含めてください。

1. Minecraft バージョンとローダー（Fabric または NeoForge）。
2. Just Enough Guns のバージョンと正確な JAR ファイル名。
3. Java、Fabric API、NeoForge、GeckoLib のバージョン。
4. ワールド状態と関連設定を含む再現手順。
5. 完全なクラッシュレポートまたは最新ログ。表示や音声の問題にはスクリーンショットや録画も添付します。
6. 他の追加 MOD を外したクリーン環境でも再現するかどうか。

「クラッシュする」だけの説明やランチャーのスクリーンショットだけでは調査できません。正確なバージョン表と最初の有効なスタックトレースが、原因の切り分けに役立ちます。

## 開発者・メンテナー向けリンク

- [ルート README](../../README.md) と [description](../../description.md)
- [1.8.2 リリースノート](../../CHANGELOG.md)
- [1.8.0 機能ノート](../../CHANGELOG.md)
- [アドバンスメントガイド](../../docs/ADVANCEMENT_GUIDE.md)
- [敵車両 AI ノート](../../docs/vehicle_enemy_ai.md)
- [検証ノート](../../docs/VALIDATION.md)

プレイヤー向けの挙動を変更するときは、先に英語ページを更新してから他の四言語を同期します。分岐固有の実装詳細は各モジュールの `docs/` に残してください。

## リリース、クレジット、ライセンス

公開 1.8.2 は、4 章の進行ガイド、弾薬ルール表示、ライブ設定フィードバック、車両操作調整を追加しました。1.8.1 は専用サーバー起動と設定処理を修正しました。1.8.0 では FPV ドローン、C4、クレイモア、C4 ベスト、Javelin、Igla、スモークによるロック拒否、ミサイルロック HUD、キルクレジット修正、車両/ミサイル/ロケットのバランス調整を追加しました。

Just Enough Guns New は非公式移植であり、Just Enough Guns や Superb Warfare と提携・承認関係にありません。Just Enough Guns を基にしたコードは GPL-3.0、元の JEG アセットは ARR で作者の許諾により使用しています。SBW 由来の車両・特殊装備素材には指定されたクレジットとライセンス条件が適用されます。完全な一覧はルート README を確認してください。

