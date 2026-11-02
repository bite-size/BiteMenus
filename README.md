<div align="center">
  <h1>BiteMenus</h1>
  <p>Build, customize, and manage interactive GUI menus entirely in-game.</p>
</div>

## Features
- **In-Game Editor:** Drag-and-drop UI elements directly in-game. No more messing with YAML for basic layouts!
- **PlaceholderAPI Support:** Fully parses PAPI placeholders in names, lores, and commands.
- **Dynamic Auto-Refresh:** Event-driven, per-player auto-refreshing menus that update Placeholders without global lagging tasks!
- **Modern Colors:** Built-in support for Kyori Adventure's MiniMessage format (`<#FF0368>`) with backwards compatibility for legacy Bukkit codes (`&c`).
- **Economy Integration:** Native support for Vault API to add costs to buttons.
- **Developer API:** Let your own plugins add custom button actions, draw live per-player items, and react to menu events.
- **bStats Tracking:** Secure and sandboxed bStats implementation to monitor your plugin's growth!

---

## 📥 Installation

**Target Version:** Minecraft `26.3`
**Plugin Version:** v1.3

1. Download the latest `BiteMenus-1.3.jar` file.
2. Drop the `.jar` into your server's `plugins/` directory.
3. Restart your server. 
4. *(Optional but Recommended)* Install **PlaceholderAPI** and **Vault** to unlock dynamic placeholders and button economy costs!

---

## 🛠️ Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/bm help` | *None* | Shows the help menu |
| `/bm open <menu>` | `bitemenus.use.open` | Opens a menu |
| `/bm list` | `bitemenus.use.list` | Lists all loaded menus |
| `/bm info <menu>` | `bitemenus.use.info` | View detailed stats and metadata about a menu |
| `/bm create <name> <size> [title]` | `bitemenus.admin.create` | Creates a new menu |
| `/bm edit <name>` | `bitemenus.admin.edit` | Opens the Drag-and-Drop In-Game Editor |
| `/bm resize <name> <size>` | `bitemenus.admin.resize` | Changes the row size of an existing menu |
| `/bm retitle <name> <title>` | `bitemenus.admin.retitle` | Changes the title of an existing menu |
| `/bm button <action> <menu> ...` | `bitemenus.admin.button` | Interactive tool to attach commands/links to items |
| `/bm flag <menu> <button|menu> <flag> <val>`| `bitemenus.admin.flags` | Modifies properties (like close-on-click, cooldowns, or an API item provider) |
| `/bm remove <name>` | `bitemenus.admin.remove` | Deletes a menu entirely |
| `/bm reload` | `bitemenus.admin.reload` | Reloads all configurations and menus |

*Note: The plugin also registers the aliases `/bitemenus` and `/bmenus`!*

---

## ⚙️ Configuration Files

### `config.yml`
Controls the fundamental default behaviors when creating a new menu:
- **`timezone`**: Sets the timezone used when displaying menu creation dates via the `/bm info` command (e.g., `UTC`, `America/New_York`).
- **`menu-defaults`**: What properties should automatically apply when `/bm create` is run:
  - **`permission`**: Auto-generates a required permission (e.g., `bitemenus.menu.<menu_name>`).
  - **`auto_refresh`**: Determines whether the menu should automatically update its placeholders every second.
  - **`filler_item`**: Automatically fills blank spaces with a specific material (like `GRAY_STAINED_GLASS_PANE`), complete with options to hide vanilla attributes.

### `messages.yml`
Every single piece of text sent by the plugin can be customized here!
- Utilizes the powerful **MiniMessage** format. You can use standard `<red>` tags or rich hex colors like `<#F7567C>`.
- Configurable **Prefix** and **Error-Prefix** to fit your server's branding.
- Support for standard variables like `<label>`, `<time>`, `<menu>`, `<cost>`, and `<title>` dynamically replaced at runtime.

---

## 🚀 Creating Your First Menu

1. Run `/bm create example 27 "My Awesome Menu"`
2. Run `/bm edit example`
3. Drag any items from your creative inventory directly into the menu!
4. Close your inventory to save it.
5. Run `/bm button command example true broadcast Hello World!` and click an item in the menu to assign the command!
6. Run `/bm open example` to test it!

---

## 🧩 Developer API

*Added in v1.3.*

BiteMenus exposes an API so your own plugins can add custom button actions, draw per-player items, and listen to menu events. Menus are still built in-game; your plugin supplies the behavior and data.

### Adding the dependency

The API is published through [JitPack](https://jitpack.io/#bite-size/BiteMenus):

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.bite-size.BiteMenus:bitemenus-api:v1.3")
}
```

Then declare BiteMenus as a dependency in your `plugin.yml`:

```yaml
depend: [BiteMenus]
```

### Getting the API

```kotlin
val menus = BiteMenusApi.get()
```

All API calls must be made on the main server thread.

### Custom actions

Register a tag, then use it in any button's actions as `[TAG] arguments`. Placeholders and `@p` are already replaced in the arguments.

```kotlin
menus.registerAction(this, "SHOP") { ctx ->
    shopService.open(ctx.player, ctx.arguments)
}
```

```yaml
actions:
  - '[SHOP] weapons'
```

### Item providers

An item provider draws a button separately for each viewer. Register one, then point a button at it in-game with `/bm flag <menu> button provider <id>`, or set `flags.provider` in the menu's YAML. While the menu is open, auto-refresh redraws the item every second. If the provider returns `null` or isn't loaded, the button's stored icon is shown.

```kotlin
menus.registerItemProvider(this, "myplugin:balance") { ctx ->
    ItemStack(Material.GOLD_INGOT).apply {
        editMeta { it.setDisplayName("&6Balance: &f${economy.balance(ctx.player)}") }
    }
}
```

Actions and providers are removed automatically when your plugin disables.

### Reading and editing menus

```kotlin
val shop = menus.getMenu("shop") ?: menus.createMenu("shop", "<gold>Shop", 27)
shop.setButton(13, MenuButton.builder()
    .icon(ItemStack(Material.DIAMOND))
    .addAction("[SHOP] gems")
    .cooldown(5)
    .build())
shop.save()
```

### Events

| Event | Fired | Cancellable |
|---|---|---|
| `MenuOpenEvent` | Before a menu opens (not in Edit Mode) | Yes |
| `ButtonClickEvent` | When a button is clicked, before cooldowns and costs | Yes |
| `MenusReloadedEvent` | After `/bm reload` | No |

---

## 📜 Changelog

### v1.3
- **Fixed:** Legacy color codes `&a`, `&b`, `&c`, `&e` and `&f` showed Bitesize brand colors instead of green, aqua, red, yellow and white. This affected menu titles, item names and lore in every release since v1.0.
- **Fixed:** Closing the in-game editor saved each item's displayed (color-parsed) copy back to the menu, which hard-coded the parsed colors into the file. Items you don't change in the editor now keep exactly what was stored.
- **Added:** `&k` (obfuscated) is now supported.
- **Added:** [Developer API](#-developer-api) with custom button actions, per-player item providers, menu editing, and open/click/reload events.
- **Added:** `provider` button flag, which connects a button to an API item provider: `/bm flag <menu> button provider <id>`.
- **Changed:** BiteMenus now uses the Adventure library bundled with Paper rather than shipping its own copy, so the jar is smaller.

> **Upgrading from v1.2:** If you used the editor on v1.2, some item names or lore may have been saved with the wrong colors written in as hex codes, such as `#4ECDC4` where you used `&a` or `<green>`. The color fixes can't undo those. Re-color the affected items in-game or fix their `name`/`lore` lines in `plugins/BiteMenus/menus/`.

### v1.2
- **Fixed:** Custom model data, item models, persistent data, enchantments and stack sizes on button icons are no longer lost on `/bm reload`.
- **Changed:** Menu files that fail to load are reported and left untouched. The last working version stays loaded, and saves to that menu are blocked until the file is fixed.

### v1.1
- **Added:** bStats metrics.

### v1.0
- First release: a Kotlin rewrite of LMenus for Paper, with the in-game editor, PlaceholderAPI and Vault support, MiniMessage colors, and auto-refreshing menus.
