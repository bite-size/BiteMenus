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
- **bStats Tracking:** Secure and sandboxed bStats implementation to monitor your plugin's growth!

---

## 📥 Installation

**Target Version:** Minecraft `26.3`
**Plugin Version:** v1.1

1. Download the latest `BiteMenus-1.1.jar` file.
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
| `/bm flag <menu> <button|menu> <flag> <val>`| `bitemenus.admin.flags` | Modifies properties (like close-on-click or cooldowns) |
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
