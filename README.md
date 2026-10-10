# Lumbridge Guide

The RuneLite companion to [Lumbridge Guide](https://lumbridge.guide), a site for Old School RuneScape bingo events and gear setups.

## Features

- **Bingo**: see your running boards, your team's tiles and standings in the side panel. When a drop, kill count or XP target matches an open tile, the plugin can take a screenshot with the board's verification code and offer to send it as proof. Nothing is sent until you choose to send it.
- **Gear**: turn your gear configs into bank tag tabs, and save the gear you are wearing as a new config.
- **Account**: link your RuneScape account to your Lumbridge Guide profile with Sync now.
- **Notifications**: your Lumbridge Guide inbox, such as team invites and proof results.

## Getting started

1. Sign in at [lumbridge.guide](https://lumbridge.guide) and create an API key under Settings, Account.
2. Paste the key into the plugin's settings, under Account.
3. Open the Lumbridge Guide panel from the RuneLite sidebar.

## What the plugin sends

The plugin only talks to Lumbridge Guide, and only with your API key. It only ever sends your own data:

- **On login**, with an API key set, the plugin sends RuneLite's account hash (an anonymous number for the account) to check whether it is linked to you.
- **Sync now** sends the logged-in account's name, account type, skill levels, XP and quest progress. The first sync links the account to you. Syncing on login and logout is off by default.
- **Track tile progress** (off by default) sends your kill count and XP progress on kill count and XP tiles of boards you play in, at most once a minute, so your team can see it.
- **Proof** screenshots are sent only when you press send.
- **Gear configs** are sent only when you save one.

See the [privacy policy](https://lumbridge.guide/privacy) for how this data is kept.

## Licence

BSD 2-Clause. See [LICENSE](LICENSE).
