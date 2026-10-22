# 落地归根

Paper 26.3 插件。一棵树的原木被全部砍完后，树叶立刻枯萎，并在这棵树最底部的那一格原木种回原来的树苗。

## 规则

- 第一刀就记下整棵树。先砍树桩再砍树冠也可以：树苗种在**当时最底部的原木**上，不是最后砍的那一格。
- 只认自然树叶。玩家放上去的树叶不会凋零，不算树冠，所以木屋不会被误砍成树。
- 旁边另一棵树撑得住的树叶留下，不跟着枯。
- 树叶枯萎后不掉东西。树苗种在最底部那一格；那一格要是被占住了，只枯萎不种，控制台会写一句。
- 没砍完就重启，这棵树的记录会丢掉，下一刀重新认。
- 去皮原木不算树干。它仍会像原版一样撑住旁边的树叶。
- 和树连成一片的同种原木如果超过 512 块，整片当作建筑，不处理。树冠高度上、水平 8 格以内的横枝算这棵树；树桩旁边的地板不算。

暗橡、巨型云杉在原版里要四株树苗才长成大树。这里按「一颗」只种一株。

## 树种

橡木、云杉、白桦、丛林、金合欢、深色橡木、红树（胎生苗）、樱花、苍白橡木、杨树（橙、红、黄三种树叶，种回杨树苗）。

橡木原木如果树冠主要是杜鹃叶，种杜鹃或盛开的杜鹃，而不是橡树苗。

菌柄、去皮原木、竹子不处理。

## 命令

`/luodiguigen reload` 重载配置。权限 `luodiguigen.reload`，默认 OP。重载不会忘掉正在砍的树。

`plugins/LuoDiGuiGen/config.yml`：

- `enabled`：总开关
- `minimum-leaves`：至少多少片自己的自然树叶才算树，默认 6
- `effects`：枯萎时的音效和粒子
- `worlds`：世界名列表，空着表示全部世界

## 构建

需要 JDK 25，Gradle 8.10 即可。Gradle 8.10 自己要跑在 Java 17 上；用 Java 25 启动它会在解析构建脚本时报 `class file major version 69`。

这台电脑的 JDK 25 在 Adongcraft 运行时里，Gradle 默认找不到。在项目的 `gradle.properties`（不要提交）里加上它的 `Contents/Home`：

```properties
org.gradle.java.installations.paths=/Users/mac/Library/Application Support/Adongcraft/runtimes/osx-arm64-25/jdk-25.jdk/Contents/Home
```

```bash
./gradlew test build
```

产物是 `build/libs/mcpaper-luodiguigen-1.0.0.jar`，放进服务器的 `plugins` 目录后重启。
