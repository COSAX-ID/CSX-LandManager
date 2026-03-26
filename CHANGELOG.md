# Changelog

All notable changes to CSX Land Manager will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.3] - Shop & GUI Improvements

### Added
- 🛡️ Ownership validation in shop - prevents buying own claims

### Fixed
- 📁 Added .qwen/ directory to .gitignore

## [1.0.2] - Build Fix Release

### Fixed
- 🔧 Fixed missing HikariCP dependency in shaded JAR (`NoClassDefFoundError: com.zaxxer.hikari.HikariConfig`)
- 🔧 Fixed H2 database shading compatibility issue (`ClassNotFoundException: org.h2.mvstore.db.NullValueDataType`)
- 📦 Updated Maven Shade Plugin configuration to properly bundle dependencies

### Changed
- H2 Database now bundled without relocation to maintain database file compatibility
- HikariCP relocated to `dev.cosax.cSXLandManager.libs.hikari`

---

## [1.0.0] - Initial Release

### Added
- 🏠 Land claim and management system
- 💰 Buy, rent, and extend land functionality
- 🔐 GriefPrevention integration for claim protection
- 💳 Vault API integration for economy (EssentialsX, CMI support)
- 🗄️ Database support (SQLite, MySQL, MariaDB)
- 📦 GUI-based land management interface
- 🔔 Notification system for land transactions
- ⏰ Auto-renew system for rented lands
- 👥 Ownership transfer system
- 🎫 Payment bypass permission (`landmgmt.bypass`)
- 🗃️ Database migration system with backward compatibility

---

## Version History

| Version | Release Date | Key Changes |
|---------|-------------|-------------|
| 1.0.3 | 2026 | Shop ownership validation, .qwen/ gitignore |
| 1.0.2 | 2026 | HikariCP & H2 dependency bundling fix |
| 1.0.0 | 2026 | Initial release with core land management features |

---

## Upcoming Features

- [ ] Support for multiple claim types
- [ ] Advanced claim tier system
- [ ] Claim war/contest feature
- [ ] Integration with other protection plugins

---

*For more detailed release notes, visit the [GitHub Releases](https://github.com/minggudevv/CSX-Land-Manager/releases) page.*
