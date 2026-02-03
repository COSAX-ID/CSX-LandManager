# CSX LandManager

<div align="center">

![CSX LandManager](https://img.shields.io/badge/CSX-LandManager-0.1.5--brightgreen?style=for-the-badge)
![Version](https://img.shields.io/badge/Minecraft-1.21.x-orange?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-21-red?style=for-the-badge&logo=openjdk)
![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)

**A comprehensive GriefPrevention addon plugin for Minecraft 1.21.x that provides GUI-based land management with Vault economy integration.**

[Features](#features) • [Installation](#installation) • [Usage](#usage) • [Configuration](#configuration) • [Building](#building) • [Contributing](#contributing) • [License](#license)

</div>

## 🌟 Features

<details>
<summary><strong>Expand features list</strong></summary>

- 🏠 **Rent Claims** - Allow players to rent claims for flexible durations (hours, days, weeks, months)
- 💰 **Sell Claims** - Permanent ownership transfer of claims
- 🛒 **Buy Claims** - Purchase claims listed for sale through GUI
- 🔄 **Auto-Renewal** - Optional automatic rent renewal with balance checking
- 🖥️ **GUI Interface** - Beautiful chest inventory-based GUI for easy management
- 💾 **Multiple Storage** - YAML, SQLite, H2, MySQL, MariaDB support
- 🔗 **GriefPrevention Integration** - Works seamlessly with existing claims
- 💵 **Vault Economy** - Full economy support with real-time balance checks
- ⚡ **Async Operations** - Non-blocking database operations
- 🎛️ **Admin Tools** - Administrative commands for server management
- 🎨 **Customizable** - Configurable messages, colors, and GUI settings

</details>

## 📋 Requirements

| Requirement | Version/Link |
|-------------|---------------|
| **Minecraft** | 1.21.x |
| **Server Software** | [Spigot](https://www.spigotmc.org/) or [Paper](https://papermc.io/) |
| **Java** | 21 or higher |
| **Required Plugins** | |
| &nbsp;&nbsp;• [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) | Claim management |
| &nbsp;&nbsp;• [Vault](https://www.spigotmc.org/resources/vault.631/) | Economy API |
| **Economy Plugin** | EssentialsX, CMI, or any Vault-compatible economy plugin |

## 📥 Installation

### Quick Start

1. Download the latest `CSXLandManager-VERSION.jar` from [Releases](../../releases)
2. Place the JAR file in your server's `plugins/` folder
3. Restart the server or load the plugin using a plugin manager
4. Configure the plugin settings in `plugins/CSXLandManager/config.yml`
5. Customize messages in `plugins/CSXLandManager/messages.yml`

### From Source

See [Building](#building) section for compilation instructions.

## 🚀 Usage

### Quick Guide

#### For Players

**Renting a Claim:**
1. Stand inside a claim available for rent
2. Use `/landmanagement` to open the GUI
3. Click **Set Duration** (compass) and choose your rental period
4. Click **Rent Claim** to start renting

**Extending Rent:**
- Open GUI and click **Extend Rent** to add more time

**Buying a Claim:**
- Stand inside a claim for sale
- Open GUI and click **Buy Claim** to purchase

**Auto-Renewal:**
- Toggle auto-renew in the GUI to automatically extend your rent

#### For Claim Owners

**Setting Rent Price:**
1. Stand in your claim
2. Open GUI and click **Set Rent Price** (gold block)
3. Enter price in chat
4. Claim is now available for rent!

**Selling a Claim:**
1. Stand in your claim
2. Open GUI and click **Set Sell Price** (emerald block)
3. Enter price in chat
4. Claim is now for sale!

**Managing Claims:**
- **Toggle Rent/Sale** - Enable or disable renting and selling
- **Cancel Rent** - End current rent agreement
- **Remove From Sale** - Take claim off the market

#### For Administrators

**Admin Commands:**
- `/landmanagement cancelrent` - Cancel any rent
- `/landmanagement reload` - Reload configuration
- Full access to all claim management features

## ⚙️ Configuration

### Storage Backend

Configure your preferred storage in `config.yml`:

```yaml
storage:
  type: YAML  # Options: YAML, SQLite, H2, MySQL, MariaDB
```

<details>
<summary><strong>Database Configuration</strong></summary>

```yaml
# MySQL/MariaDB
storage:
  type: MySQL
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: minecraft
    password: your_password
    pool-size: 10
    connection-timeout: 30000
    max-lifetime: 1800000
```

</details>

### Rent Settings

```yaml
rent:
  auto-renew-enabled: true
  default-duration: 604800000  # 1 week
  expiration-check-interval: 1200  # 1 minute
```

### GUI Customization

```yaml
gui:
  size: 54
  sounds:
    enabled: true
  titles:
    main: "&6Land Management"
```

### Claim Settings

```yaml
claim:
  min-rent-price: 0.0
  max-rent-price: 0.0  # 0 = unlimited
  min-sell-price: 0.0
  max-sell-price: 0.0
  min-rent-duration: 3600000  # 1 hour
  max-rent-duration: 0  # 0 = unlimited
```

## 📜 Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/landmanagement` | `/landmgmt`, `/lm`, `/landm` | Open land management GUI | `landmgmt.use` |
| `/landmanagement setprice <price>` | - | Set rent price for your claim | `landmgmt.sell` |
| `/landmanagement sell <price>` | - | List your claim for sale | `landmgmt.sell` |
| `/landmanagement unsell` | - | Remove claim from sale | `landmgmt.sell` |
| `/landmanagement cancelrent` | - | Cancel current rent | `landmgmt.admin` |
| `/landmanagement reload` | - | Reload configuration | `landmgmt.admin` |

## 🔐 Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `landmgmt.use` | `true` | Open land management GUI |
| `landmgmt.rent` | `true` | Rent claims |
| `landmgmt.buy` | `true` | Buy claims |
| `landmgmt.sell` | `true` | Sell own claims |
| `landmgmt.autorenew` | `true` | Use auto-renew feature |
| `landmgmt.admin` | `op` | Administrative commands |

## 🏗️ Building from Source

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+** or higher

### Build Steps

```bash
# Clone repository
git clone https://github.com/cosaxid/CSX-LandManager.git
cd CSX-LandManager

# Build plugin
mvn clean package

# Output: target/CSXLandManager-{VERSION}.jar
```

### Development Setup

```bash
# Install dependencies
mvn clean install

# Run tests (if available)
mvn test

# Build without running tests
mvn clean package -DskipTests
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Coding Standards

- Follow Java code conventions
- Use meaningful variable and method names
- Add Javadoc comments for public methods
- Test your changes thoroughly
- Update documentation as needed

## 📁 Project Structure

```
CSXLandManager/
├── src/main/java/dev/cosax/cSXLandManager/
│   ├── CSXLandManager.java          # Main plugin class
│   ├── config/                       # Configuration handlers
│   ├── manager/                      # Core managers
│   ├── storage/                      # Data persistence layer
│   │   └── impl/                     # Storage implementations
│   ├── model/                        # Data models
│   ├── gui/                          # User interface
│   ├── command/                      # Plugin commands
│   ├── listener/                     # Event handlers
│   └── task/                         # Scheduled tasks
└── src/main/resources/
    ├── plugin.yml                    # Plugin metadata
    ├── config.yml                    # Configuration
    └── messages.yml                  # Messages
```

## 🐛 Troubleshooting

<details>
<summary><strong>Common Issues</strong></summary>

**Plugin not loading:**
- Ensure GriefPrevention and Vault are installed
- Check console for error messages
- Verify API version compatibility

**GUI not opening:**
- Check you have the `landmgmt.use` permission
- Ensure you're standing inside a claim
- Check for conflicts with other plugins

**Rent not working:**
- Verify Vault is working (`/vault balance`)
- Check player has enough money
- Ensure claim is available for rent

**Database errors:**
- Check database connection settings in config.yml
- Verify database server is running
- Check console for specific error messages

</details>

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

```
Copyright 2025 cosaxid

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 👨‍💻 Credits

- **Author**: [cosaxid](https://cosax.alfikz.my.id)
- **Built With**: Spigot/Paper API, GriefPrevention API, Vault API
- **Support**: [Report Issues](../../issues)

## ⚠️ Important Notes

- This plugin only manages **existing GriefPrevention claims**
- It does **NOT** create new claims
- Players must use GriefPrevention's golden shovel to create claims first
- Always backup your data before updating

## 📞 Support

- 📧 Issues: [GitHub Issues](../../issues)
- 📖 Documentation: [Wiki](../../wiki)
- 💬 Discussions: [GitHub Discussions](../../discussions)

---

<div align="center">

**Made with ❤️ for the Minecraft community**

[⬆ Back to Top](#csx-landmanager)

</div>
