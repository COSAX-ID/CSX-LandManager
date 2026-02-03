# CSX Land Manager

<div align="center">

![CSX LandManager](https://img.shields.io/badge/CSX-LandManager-1.2.8-brightgreen?style=for-the-badge)
![Version](https://img.shields.io/badge/Minecraft-1.21.x-orange?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-21-red?style=for-the-badge&logo=openjdk)
![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)

**A comprehensive GriefPrevention addon plugin for Minecraft 1.21.x that provides GUI-based land management with rent, buy, sell functionality and automatic ownership transfer during rental periods.**

[Features](#-features) • [Installation](#-installation) • [Usage](#-usage) • [Configuration](#-configuration) • [Permissions](#-permissions) • [Changelog](#-changelog) • [Building](#-building) • [Contributing](#-contributing) • [License](#-license)

</div>

---

## 🌟 Features

<details>
<summary><strong>Expand features list</strong></summary>

### 🏠 Land Management
- **Rent Claims** - Allow players to rent claims for flexible durations (minutes, hours, days, weeks, months)
- **Sell Claims** - Permanent ownership transfer of claims
- **Buy Claims** - Purchase claims listed for sale through GUI
- **Ownership Transfer** - During rental, ownership transfers to renter and returns when rent expires

### 💰 Economy System
- **Vault Integration** - Full economy support with EssentialsX, CMI, and other Vault-compatible plugins
- **Atomic Transactions** - Payment processing with automatic refund on failure
- **Payment Bypass** - VIP/Donator system with `landmgmt.bypass` permission
- **Detailed Logging** - Complete transaction logging for debugging

### 🔄 Rental System
- **Auto-Renewal** - Optional automatic rent renewal with balance checking
- **Flexible Duration** - Rent by minutes, hours, days, weeks, or months
- **Ownership Transfer** - Claim ownership transfers to renter during rental period
- **Auto-Return** - Ownership returns to original owner when rent expires

### 🖥️ User Interface
- **GUI Interface** - Beautiful chest inventory-based GUI for easy management
- **Duration Selector** - Visual duration selection interface
- **Real-time Updates** - GUI refreshes to show current status
- **Sound Effects** - Configurable click and success sounds

### 💾 Storage & Database
- **Multiple Storage** - YAML, SQLite, H2, MySQL, MariaDB support
- **Auto Migration** - Automatic database schema migration for smooth upgrades
- **Async Operations** - Non-blocking database operations for performance

### 🔧 Admin Features
- **Admin Commands** - Cancel rents, manage claims
- **Debug Mode** - Detailed logging for troubleshooting
- **Permission System** - Granular permissions including payment bypass

### 🎨 Customization
- **Configurable Messages** - Customize all plugin messages
- **GUI Settings** - Configure GUI size, titles, and sounds
- **Claim Limits** - Set min/max prices and durations

</details>

---

## 📋 Requirements

| Requirement | Version/Link |
|-------------|---------------|
| **Minecraft** | 1.21.x |
| **Server Software** | [Paper](https://papermc.io/) (recommended) or [Spigot](https://www.spigotmc.org/) |
| **Java** | 21 or higher |
| **Required Plugins** | |
| &nbsp;&nbsp;• [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) | Claim management |
| &nbsp;&nbsp;• [Vault](https://www.spigotmc.org/resources/vault.631/) | Economy API |
| **Economy Plugin** | [EssentialsX](https://essentialsx.net/) (recommended), CMI, or any Vault-compatible plugin |

---

## 📥 Installation

### Quick Start Guide

1. **Install Required Plugins:**
   - Install [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/)
   - Install [Vault](https://www.spigotmc.org/resources/vault.631/)
   - Install an economy plugin (EssentialsX recommended)

2. **Install CSX Land Manager:**
   - Download the latest `CSXLandManager-1.2.8.jar` from [Releases](https://github.com/minggudevv/CSX-Land-Manager/releases)
   - Place the JAR file in your server's `plugins/` folder
   - Restart the server

3. **Configuration (Optional):**
   - Edit `plugins/CSXLandManager/config.yml` for settings
   - Edit `plugins/CSXLandManager/messages.yml` for custom messages
   - Set up database (if not using default YAML)

4. **Permissions Setup:**
   - Configure permissions using LuckPerms or your permission plugin
   - See [Permissions](#-permissions) section for details

### From Source

See [Building](#-building-from-source) section for compilation instructions.

---

## 🚀 Usage

### For Players

#### Renting a Claim
1. Stand inside a claim available for rent
2. Use `/lm` to open the Land Management GUI
3. Click **Set Duration** (clock) to choose rental period:
   - Select: Minutes, Hours, Days, Weeks, or Months
   - Enter amount (e.g., "7" for 7 days)
4. Click **Rent Claim** to start renting
5. **IMPORTANT**: During rental, claim ownership transfers to you!

#### Extending Rent
- Open GUI and click **Extend Rent** to add more time
- Payment will be processed to extend your rental

#### Auto-Renewal
- Toggle auto-renew in the GUI
- System automatically charges your account and extends rent
- Requires sufficient balance in your account

#### Buying a Claim
1. Stand inside a claim for sale
2. Open GUI with `/lm`
3. Check the sell price displayed
4. Click **Buy Claim** to purchase permanently
5. Payment is processed atomically (refund if transfer fails)

### For Claim Owners

#### Setting Up Your Claim for Rent
1. Stand in your claim
2. Open GUI with `/lm`
3. Click **Set Rent Price** (gold block)
4. Enter price in chat (e.g., "1000")
5. Click **Set Duration** to configure rental period options
6. Claim is now available for rent!

#### Selling Your Claim
1. Stand in your claim
2. Open GUI and click **Set Sell Price** (emerald block)
3. Enter your desired price
4. Claim is now for sale!

#### Managing Your Claim
- **Toggle Renting** - Enable/disable renting
- **Toggle Selling** - Enable/disable selling
- **Cancel Rent** - End current rent early
- **Remove From Sale** - Take claim off market
- **Set Duration** - Configure available rental durations

### For Administrators

#### Admin Commands
```bash
/lm cancelrent        # Cancel any active rent
/lm reload            # Reload configuration
```

#### Admin Features
- Full access to all claim management features
- Cancel any player's rent
- Set prices on any claim (if permissions allow)
- View detailed transaction logs (with debug enabled)

---

## ⚙️ Configuration

### Storage Backend

Configure your preferred storage in `plugins/CSXLandManager/config.yml`:

```yaml
storage:
  type: YAML  # Options: YAML, SQLite, H2, MySQL, MariaDB
```

<details>
<summary><strong>Database Configuration Examples</strong></summary>

**MySQL Configuration:**
```yaml
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

**MariaDB Configuration:**
```yaml
storage:
  type: MariaDB
  mariadb:
    host: localhost
    port: 3306
    database: minecraft
    username: minecraft
    password: your_password
    pool-size: 10
```

**H2/SQLite Configuration:**
```yaml
storage:
  type: H2  # or SQLite
  # No additional configuration needed
```

</details>

### Rent Settings

```yaml
rent:
  auto-renew-enabled: true              # Enable auto-renewal feature
  default-duration: 604800000           # 1 week in milliseconds
  expiration-check-interval: 1200       # Check interval in ticks (60 seconds)
  min-rent-duration: 60000              # 1 minute minimum
  max-rent-duration: 0                  # 0 = unlimited
```

### Price Limits

```yaml
claim:
  min-rent-price: 0.0                   # Minimum rent price
  max-rent-price: 0.0                   # 0 = unlimited
  min-sell-price: 0.0                   # Minimum sell price
  max-sell-price: 0.0                   # 0 = unlimited
```

### GUI Customization

```yaml
gui:
  size: 54                               # GUI size in slots
  sounds:
    enabled: true                        # Enable sound effects
  titles:
    main: "&6Land Management"           # Main GUI title
    confirm: "&cConfirm Action"          # Confirmation GUI title
    admin: "&4Admin Panel"               # Admin GUI title
```

### Debug Mode

```yaml
debug: false                            # Enable detailed logging
```

---

## 🔐 Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| **Basic Permissions** |
| `landmgmt.use` | `true` | Open land management GUI |
| **Transaction Permissions** |
| `landmgmt.rent` | `true` | Rent claims |
| `landmgmt.buy` | `true` | Buy claims |
| `landmgmt.sell` | `true` | Sell own claims |
| `landmgmt.autorenew` | `true` | Use auto-renew feature |
| **Special Permissions** |
| `landmgmt.bypass` | `false` | **Bypass all payment requirements** (VIP/donator feature) |
| **Administration** |
| `landmgmt.admin` | `op` | Administrative commands (cancel rents, reload config) |

### Permission Examples (LuckPerms)

#### Default Player
```bash
lp default permission set landmgmt.use true
lp default permission set landmgmt.rent true
lp default permission set landmgmt.buy true
lp default permission set landmgmt.sell true
```

#### VIP / Donator (Free Transactions)
```bash
lp vip permission set landmgmt.bypass true
```

#### Admin (Pays for transactions)
```bash
lp admin permission set landmgmt.admin true
# Admin will pay for buy/rent unless also given bypass permission
```

#### Super Admin (Full Admin + Free)
```bash
lp superadmin permission set landmgmt.admin true
lp superadmin permission set landmgmt.bypass true
```

---

## 📜 Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/lm` | `/landmanagement`, `/landmgmt`, `/landm` | Open land management GUI | `landmgmt.use` |
| `/lm setprice <price>` | - | Set rent price for your claim | `landmgmt.sell` |
| `/lm sell <price>` | - | List your claim for sale | `landmgmt.sell` |
| `/lm unsell` | - | Remove claim from sale | `landmgmt.sell` |
| `/lm cancelrent` | - | Cancel current rent (admin/owner) | `landmgmt.admin` |
| `/lm reload` | - | Reload configuration | `landmgmt.admin` |

---

## 📅 Changelog

### v1.2.8 - Current Release
**Payment System Overhaul**
- ✨ **NEW**: `landmgmt.bypass` permission for payment bypass
- 🔄 Admins now pay for transactions unless they have bypass permission
- 💰 Better economy flow and server balance
- 📝 Updated all payment checks (buy, rent, extend)

### v1.2.7
**GUI & Inventory Fixes**
- 🐛 Fixed `InventoryCloseEvent` synchronous errors
- ✅ Improved GUI reliability and consistency
- 🔧 Better async handling for GUI operations
- 📊 Enhanced error handling

### v1.2.6
**Database Migration**
- 🗄️ Automatic database schema migration
- 🔄 Backward compatibility with older databases
- ✅ Dynamic INSERT statements for old/new schemas

### v1.2.5
**Vault API Fixes**
- 💰 Fixed auto-renew money disappearing bug
- 📊 Improved transaction logging
- 🔍 Better economy provider detection (EssentialsX, CMI)
- ⚠️ Comprehensive payment debugging

### v1.2.4
**Rent System Enhancements**
- 🔄 Ownership transfer during rental period
- 👤 Original owner saved and restored on expiration
- ⏱️ Added minutes as rent duration option
- 📦 Updated storage layer for original owner field

### v1.2.3
**GriefPrevention Compatibility**
- 🔧 Multiple fallback methods for ownership transfer
- 🛠️ Better compatibility with different GriefPrevention versions
- 📝 Improved error diagnostics

---

## 🏗️ Building from Source

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+** or higher

### Build Steps

```bash
# Clone repository
git clone https://github.com/minggudevv/CSX-Land-Manager.git
cd CSX-Land-Manager

# Build plugin
mvn clean package

# Output: target/CSXLandManager-1.2.8.jar
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

---

## 📁 Project Structure

```
CSXLandManager/
├── src/main/java/dev/cosax/cSXLandManager/
│   ├── CSXLandManager.java          # Main plugin class
│   ├── command/                      # Command handlers
│   │   └── LandManagementCommand.java
│   ├── config/                       # Configuration classes
│   │   ├── Config.java
│   │   └── Messages.java
│   ├── gui/                          # GUI classes
│   │   ├── ClaimGUI.java             # Main claim management GUI
│   │   ├── DurationSelectorGUI.java  # Duration selection GUI
│   │   └── GUIAction.java            # GUI action types
│   ├── listener/                     # Event listeners
│   │   ├── GUIListener.java          # GUI interaction handling
│   │   ├── PlayerListener.java       # Player events
│   │   └── ChatListener.java         # Chat input handling
│   ├── manager/                      # Business logic managers
│   │   ├── ClaimManager.java         # Claim management & GP integration
│   │   ├── EconomyManager.java       # Economy operations
│   │   ├── GUIManager.java           # GUI creation & management
│   │   ├── RentManager.java          # Rent operations
│   │   └── StorageManager.java       # Data storage coordination
│   ├── model/                        # Data models
│   │   ├── ClaimData.java            # Claim data structure
│   │   └── RentStatus.java           # Rental status enum
│   ├── storage/                      # Storage implementations
│   │   ├── StorageProvider.java      # Storage interface
│   │   ├── StorageManager.java       # Storage coordinator
│   │   └── impl/                     # Various storage backends
│   └── task/                         # Background tasks
│       └── RentExpirationTask.java   # Rent expiration checking
└── src/main/resources/
    ├── plugin.yml                    # Plugin metadata
    ├── config.yml                    # Configuration
    └── messages.yml                  # Messages
```

---

## 🐛 Troubleshooting

<details>
<summary><strong>Common Issues and Solutions</strong></summary>

### Plugin not loading
**Symptoms:** Plugin doesn't appear when server starts

**Solutions:**
- ✅ Ensure GriefPrevention and Vault are installed and loaded
- ✅ Check console for error messages
- ✅ Verify you're using Java 21 or higher
- ✅ Check for plugin conflicts

### GUI not opening
**Symptoms:** `/lm` command doesn't open GUI

**Solutions:**
- ✅ Check you have the `landmgmt.use` permission
- ✅ Ensure you're standing inside a GriefPrevention claim
- ✅ Check for conflicts with other GUI plugins
- ✅ Try closing other inventories first

### Payment not working
**Symptoms:** Can't buy/rent, money not transferring

**Solutions:**
- ✅ Verify Vault is working: `/vault balance`
- ✅ Check player has sufficient money
- ✅ Ensure economy plugin is loaded (EssentialsX, CMI, etc.)
- ✅ Check `landmgmt.bypass` permission isn't granted incorrectly
- ✅ Enable debug mode in config.yml and check console logs

### Rent expiration issues
**Symptoms:** Rent not expiring or ownership not returning

**Solutions:**
- ✅ Check `rent.expiration-check-interval` in config.yml
- ✅ Verify database is saving claim data
- ✅ Check console for errors in rent expiration task
- ✅ Ensure original owner is saved in database

### Database errors
**Symptoms:** SQL errors or data not saving

**Solutions:**
- ✅ Check database connection settings in config.yml
- ✅ Verify database server is running
- ✅ Ensure database user has proper permissions
- ✅ Try switching to YAML storage for testing

### Ownership transfer not working
**Symptoms:** "Could not access claim owner field" error

**Solutions:**
- ✅ Check GriefPrevention version compatibility
- ✅ Enable debug mode for detailed error logging
- ✅ Verify claim exists and is valid
- ✅ Check for GriefPrevention claim corruption

</details>

---

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

---

## 👨‍💻 Credits

### Development
- **Author:** [cosaxid](https://cosax.alfikz.my.id)
- **Version:** 1.2.8
- **Repository:** [minggudevv/CSX-Land-Manager](https://github.com/minggudevv/CSX-Land-Manager)

### Built With
- [Spigot/Paper API](https://www.spigotmc.org/) - Minecraft Server API
- [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) - Claim management plugin
- [Vault API](https://www.spigotmc.org/resources/vault.631/) - Economy API
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Database connection pooling
- [Maven](https://maven.apache.org/) - Build tool

### Special Thanks
- **BigScry** for GriefPrevention
- **MilkBowl** for Vault
- **EssentialsX Team** for the economy plugin
- **PaperMC Team** for the optimized server software

---

## ⚠️ Important Notes

### Key Features to Remember
1. 📋 This plugin **manages existing GriefPrevention claims** - it does NOT create new claims
2. 🔨 Players must use GriefPrevention's golden shovel to create claims first
3. 💾 **Always backup your data** before updating to a new version
4. 🔄 **During rental**, claim ownership transfers to the renter automatically
5. ⏰ **When rent expires**, ownership returns to the original owner
6. 💰 **Admins pay for transactions** unless they have `landmgmt.bypass` permission

### Data Migration
- Plugin includes automatic database migration system
- When upgrading from older versions, database schema updates automatically
- YAML storage is used as fallback for migration

### Performance Tips
- Use H2 or SQLite for small servers (< 50 players)
- Use MySQL or MariaDB for large servers (100+ players)
- Adjust `rent.expiration-check-interval` based on server load

---

## 📞 Support

- 🐛 **Report Issues:** [GitHub Issues](https://github.com/minggudevv/CSX-Land-Manager/issues)
- 💬 **Discussions:** [GitHub Discussions](https://github.com/minggudevv/CSX-Land-Manager/discussions)
- 📖 **Documentation:** [Project Wiki](https://github.com/minggudevv/CSX-Land-Manager/wiki)
- 📧 **Contact:** [cosax.alfikz.my.id](https://cosax.alfikz.my.id)

---

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

### Pull Request Guidelines
- Write clear commit messages
- Describe the changes and why they're needed
- Include screenshots for GUI changes
- Document any breaking changes

---

## 💖 Donation

If you enjoy this plugin, consider supporting the development:

- **GitHub Sponsors:** [Sponsor page](https://github.com/sponsors/minggudevv)
- **Trakteer:** [Link here if available]

Your support helps keep this project updated and maintained! ❤️

---

<div align="center">

### ⭐ Star this plugin on GitHub if you find it helpful!

**Made with ❤️ for the Minecraft community**

[⬆ Back to Top](#csx-land-manager)

![GitHub stars](https://img.shields.io/github/stars/minggudevv/CSX-Land-Manager?style=social)
![GitHub forks](https://img.shields.io/github/forks/minggudevv/CSX-Land-Manager?style=social)
![GitHub issues](https://img.shields.io/github/issues/minggudevv/CSX-Land-Manager)

</div>
