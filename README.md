# Lumbridge Guide

The RuneLite companion to [Lumbridge Guide](https://lumbridge.guide), a site for Old School RuneScape bingo events and gear setups.

## Features

- **Bingo**: the boards you play in, your team, its tiles and the board code, in the side panel. Optionally your team name before your name in chat.
- **Tile proof**: when a drop, kill count or XP target matches an open tile, the plugin takes a screenshot of the game view stamped with the board code and the time, and offers to send it as proof. You can also take proof for any tile from its details. Nothing is sent until you press Send proof.
- **Tile progress**: optionally counts your kills and XP towards kill count and XP tiles, and shows your team's progress.
- **Chat messages**: claims, decisions on your proof and boards starting or ending appear in your chat box.
- **Gear**: turn a setup into a bank tag tab, and save what you are wearing and carrying as a new setup.
- **Account**: link your RuneScape account to your Lumbridge Guide profile and keep its stats and quests up to date.

## Getting started

1. Sign in at [lumbridge.guide](https://lumbridge.guide) and create a plugin key under Settings, Account.
2. Paste the key into the plugin's settings, under Account.
3. Open the Lumbridge Guide panel from the RuneLite sidebar.

Without a plugin key the plugin makes no requests.

## How it works

The plugin talks only to the Lumbridge Guide API (`api.lumbridge.guide`), and every request carries your plugin key. It never contacts the game servers, and it only reads data about your own account.

| When | Request | Why |
|---|---|---|
| The plugin starts, the plugin key changes, you press Refresh, and every few minutes while you are logged in | Asks for changes to your boards, sending the board versions it already has | Shows your boards, new claims, decisions on your proof and boards starting or ending. The website sets the interval |
| You press Send proof | Sends the screenshot, the tile, the board code, whether the plugin detected it or you took it by hand, what was detected (item, monster, kill count and time) and a fingerprint of the image. For XP tiles it also sends your XP total and the XP gained, plus your account hash when Sync on login and logout is on | Proof for the board's hosts |
| Once a minute, with Track tile progress on, when your progress changed | Sends your kill count or XP progress on running tiles | Shows your team your progress. Off by default |
| You press Refresh under the account, or log in with Sync on login and logout on | Sends your account hash | Checks whether this account is linked |
| You press Sync now | Sends your account details (below) | Links the account on the first sync, then updates it |
| You log in or out, with Sync on login and logout on | Sends your account details | Keeps a linked account up to date. Off by default, and never links a new account |
| You make a bank tag tab | Fetches the setup for the code or link you entered | Gets the items to tag |
| You save a setup | Sends the gear you chose to include | Creates the setup on your profile |

Board data is kept in memory only and is cleared when the plugin key is removed or rejected.

## What the plugin sends

- **Account hash**: the number RuneLite uses to tell accounts apart. It is not your login name or password, and it keeps the account linked when you change your display name. It is sent only by Sync now, Refresh under the account, and, with Sync on login and logout on, at login and with XP proof.
- **Account details**, sent only by Sync now, or on login and logout when you turn that on:
  - display name and account type (for example ironman or group ironman)
  - the world type, so syncs from Leagues, Deadman and other temporary worlds are refused
  - level and XP for each skill
  - quest points and the state of each quest
- **Proof**: the screenshot of the game view, chat box included, and the details listed in the table above. Only when you press Send proof.
- **Tile progress**: your kill count or XP gained on running tiles, only with Track tile progress on.
- **Setup**: the name you give it, and the worn items, inventory and rune pouch contents you chose to include.

## Chat messages

The plugin adds messages to your own chat box. Other players never see them. Each starts with the board name, for example `[Clan Bingo] Team Red claimed Barrows chest`, in the accent colour.

- Tile claims, for all teams, your team only or none (the Claim messages setting). Boards that hide claims during play stay hidden.
- Decisions on your proof: accepted, not accepted or reversed, including ones made while you were logged out.
- A board starting or ending.
- A line when detected proof is ready to send, next to a "Proof ready to send" popup.

## What the plugin stores

- The plugin's settings, including your plugin key and the board you last viewed, in your RuneLite settings. The settings panel masks the key.
- Proof waiting to be sent: the screenshot and its details under `.runelite/lumbridge-guide/proof/<account hash>/`, at most 10 per account. Each one is deleted once it is sent, you press Not now, or its board ends or the tile no longer needs proof.
- Tile progress starting points: your kill count and XP when tracking began on each tile, in your RuneLite profile settings.
- Bank tag tabs you make, through RuneLite's Bank Tags plugin, named `lg-<setup name>`. You can edit or delete them like any other tag tab.

See the [privacy policy](https://lumbridge.guide/privacy) for how Lumbridge Guide keeps this data.

## Licence

BSD 2-Clause. See [LICENSE](LICENSE).
