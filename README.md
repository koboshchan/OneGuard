# OneGuard

## Deprecated

This project is deprecated.

Use Velocity built-in modern forwarding/auth instead.

OneGuard is a Paper plugin for 1.21.10.

## What It Does

OneGuard checks a transfer cookie when a player joins, sent by [OneMCServer](https://github.com/koboshchan/OneMcServerVelocity).

It does these steps:

- Reads the cookie from the player.
- Checks the sign on the cookie.
- Lets the player stay if the sign is good.
- Kicks the player if the cookie is gone or bad.

This helps make sure the join came from your own transfer flow.

## Need

- Java 21
- A Minecraft client

## Build

Run:

```sh
./gradlew build
```

or build in your IDE.

## Config

Main config file:

`plugins/OneGuard/config.yml`

Set these values:

- `publicKey`: your Ed25519 public key
- `signature.algorithm`: keep this as `Ed25519`
- `signature.keyAlgorithm`: keep this as `Ed25519`
- `transferCookie.namespace`: the cookie namespace
- `transferCookie.key`: the cookie key name

Default cookie key:

`onemcserver:auth`

## Setup

1. Open `plugins/OneGuard/config.yml`.
2. Put your Ed25519 public key in `publicKey`.
3. Make sure the cookie name matches your proxy or send code.
4. Start the server.
5. If you change the file later, use `/reload`.

Example:

```yml
publicKey: "your-ed25519-public-key"

signature:
	algorithm: "Ed25519"
	keyAlgorithm: "Ed25519"

transferCookie:
	namespace: "onemcserver"
	key: "auth"
```

## Reload config

Use this in game (recommended to restart the server instead):

```text
/reload
```