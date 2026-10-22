# Per Ankh privacy policy

Effective 22 October 2026. Per Ankh is published by LOVI Tech. This policy is also served at
https://perankh.hrana.app/privacy.

## In short

Per Ankh works offline and collects nothing about you. Only two things ever go over the internet:
a problem report that you write yourself and choose to send, and, when you open About in the
settings, a question about which version of Per Ankh is the newest. Neither carries your name,
email address, account or device identifier.

## What stays on your phone

Your progress (what you have learned, your reviews, awards) and your settings are stored only on
your phone. If Android's backup is on, Android may copy them to your own Google account, under
Google's terms; we never receive that copy. Deleting the app deletes the data.

## Problem reports

A flag on lesson pages, in the Codex and in the reader lets you report a problem. A report is sent
only when you tap **Send** in the report form; if your phone is offline, it waits on your phone and
is sent the next time the app starts. A report contains:

- the category you pick and the note you type (please do not put personal details in it);
- what was on screen: the lesson, the exercise, the hieroglyphs, transliterations and options
  shown, the answer you gave and how it was marked, and which sound clip played;
- the app version and the Android version number.

It does not contain your name, email address, phone number, location, contacts, photos, any
account, advertising ID or device ID.

Reports travel over an encrypted connection (HTTPS) directly to our server at perankh.hrana.app,
hosted by Akamai Cloud (Linode). No other company sees their contents. The server keeps no record
of where a report came from: it writes no access log, and holds your network address in memory
only to limit how often reports can be sent. Server error logs, kept for up to 14 days for
security, may contain network addresses.

We use reports only to find and fix problems in the course. We never sell them, share them,
use them for advertising, or combine them with other data.

## How long we keep reports

A report is kept while the problem is open, and deleted within 12 months of being closed.
Because reports are anonymous we cannot find "your" reports; if you want one deleted, tell us
roughly when you sent it and what it said, and we will remove it.

## Checking for a new version

From version 0.3.0, the About page in the settings shows whether a newer version of Per Ankh is
out. To find out, the app asks GitHub (github.com, a Microsoft service, where Per Ankh's releases
are published) for the newest release; if GitHub cannot be reached, it asks our server at
perankh.hrana.app, which answers from its own copy of GitHub's answer. The app asks only when you
open About, and not again until it is restarted once it has an answer. The question carries the
app's name and version and nothing else.

Like any connection, it shows the server your phone's network address. GitHub handles that under
the [GitHub General Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).
Our server treats it as it does for reports: no access log, the address held in memory only to
limit how often the server can be asked, and error logs kept for up to 14 days.

## Children

Per Ankh is a general-audience learning app and is not directed at children under 13. It does
not knowingly collect personal information from anyone.

## Changes and contact

If a future version adds anything that collects more data, such as optional accounts to sync
progress between devices, this policy will be updated before that version is released, and that
feature will be optional. Questions and requests: open an issue at
https://github.com/ekoori/PerAnkh/issues.
