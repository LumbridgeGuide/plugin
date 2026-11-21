# Lumbridge Guide

The RuneLite companion to [Lumbridge Guide](https://lumbridge.guide), a site for Old School RuneScape bingo events and gear setups.

## Features

- **Bingo**: see the boards you play in, your team and its tiles in the side panel, with the board's verification code for screenshots. Optionally show your team name before your name in chat.
- **Gear**: turn a gear config into a bank tag tab, and save what you are wearing and carrying as a new config.
- **Account**: link your RuneScape account to your Lumbridge Guide profile and keep its stats and quests up to date.

## Getting started

1. Sign in at [lumbridge.guide](https://lumbridge.guide) and create an API key under Settings, Account.
2. Paste the key into the plugin's settings, under Account.
3. Open the Lumbridge Guide panel from the RuneLite sidebar.

Without an API key the plugin makes no requests.

## How it works

The plugin talks only to the Lumbridge Guide API (`api.lumbridge.guide`), and every request carries your API key. It never contacts the game servers, and it only reads data about your own account.

| When | Request | Why |
|---|---|---|
| The plugin starts, the API key changes, and every 5 minutes while you are logged in | Fetches your boards | Shows your boards, including claims made on the website. The interval is a setting, and 0 turns it off |
| You log in | Sends your account hash | Checks whether this account is linked to you |
| You press Sync now | Sends your account details (below) | Links the account on the first sync, then updates it |
| You log in or out, with Sync on login and logout turned on | Sends your account details | Keeps a linked account up to date. Off by default, and never links a new account |
| You generate a bank tag tab | Fetches the gear config for the code or link you entered | Gets the items to tag |
| You save a gear config | Sends the gear you chose to include | Creates the config on your profile |

Board data is kept in memory only and is cleared when the API key is removed or rejected.

## What the plugin sends

- **Account hash**: the number RuneLite uses to tell accounts apart. It is not your login name or password, and it keeps the account linked when you change your display name.
- **Account details**, sent only by Sync now, or on login and logout when you turn that on:
  - display name and account type (for example ironman or group ironman)
  - the world type, so syncs from Leagues, Deadman and other temporary worlds are refused
  - level and XP for each skill
  - quest points and the state of each quest
- **Gear config**: the name you give it, and the worn items, inventory and rune pouch contents you chose to include.

## What the plugin stores

- The plugin's settings, including your API key, in your RuneLite settings. The settings panel masks the key.
- Bank tag tabs you generate, through RuneLite's Bank Tags plugin, named `lg-<config name>`. You can edit or delete them like any other tag tab.

Nothing else is written to disk.

See the [privacy policy](https://lumbridge.guide/privacy) for how Lumbridge Guide keeps this data.

## Licence

BSD 2-Clause. See [LICENSE](LICENSE).
