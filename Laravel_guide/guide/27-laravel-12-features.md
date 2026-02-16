# 27 - Laravel 12 Features

Laravel 12 is a maintenance‑focused release with minimal breaking changes. It upgrades the starter kits, updates the baseline PHP support, and continues the yearly release cadence.

## Goals

- Understand what changed in Laravel 12
- Know the supported PHP versions
- Plan upgrades safely

## 1. Release Highlights

Laravel 12 focuses on stability and tooling rather than large framework changes.

Key points:

- Minimal breaking changes
- Updated starter kits (React, Vue, Livewire)
- Optional WorkOS AuthKit integration

## 2. Updated Starter Kits

The new starter kits provide modern front‑end stacks:

- **React** and **Vue** starter kits use Inertia 2 and TypeScript
- **Livewire** starter kit includes Livewire + Flux UI
- All kits include modern Tailwind styling and improved structure

These starter kits are available when creating a new Laravel app.

## 3. WorkOS AuthKit (Optional)

Laravel 12 starter kits can integrate WorkOS AuthKit for:

- Social login
- Passkeys
- Enterprise SSO
- Organization management

## 4. PHP Support

Laravel 12 supports PHP 8.2, 8.3, 8.4, and 8.5.

## 5. Support Policy

Laravel major releases receive:

- 1 year of bug fixes
- 2 years of security fixes

## 6. Upgrade Checklist

- Update `composer.json` to `laravel/framework:^12.0`.
- Run `composer update`.
- Run test suite and fix any deprecations.
- Rebuild caches in production.

## Tips

- Use a staging environment for upgrades.
- Review release notes before upgrading.
- Keep `APP_DEBUG=false` in production.

---

[Previous: Full‑Text Search with Scout](./26-full-text-search-scout.md) | [Back to Index](./README.md)
