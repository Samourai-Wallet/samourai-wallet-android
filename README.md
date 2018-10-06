# Masanari Wallet

### Origin

Masanari was forked from [Samourai-wallet-android](https://github.com/Samourai-Wallet/samourai-wallet-android/commit/87f67e74632e1326685e9a2ec81e395f7e73f18e) (version 0.98.86) on september 30, 2018.

This version still contains the "FIAT" source-code,
which was removed in the [original code](https://blog.samouraiwallet.com/post/178536644472/09887-welcome-new-international-users-and)
and [wont](https://old.reddit.com/r/Bitcoin/comments/9jtp75/users_will_never_be_ready_samourai_wallet_drops/e6v7vzg/) be coming back.

### Main changes and plans for Masanari

* Bring back fiat currency (done)
* Keep merging bugfix patches from orininal project (done. Up-to-date with v0.98.89)
* Implement **bits** denomination: 1 bit = 100 satoshis ([bip-176](https://github.com/bitcoin/bips/blob/master/bip-0176.mediawiki) by Jimmy Song)
* Add Dutch language (done)
* (Option) Remove "premium" functions if they require Samourai developers fees (Both for Ricochet transactions (0.002 BTC) and intitial PaymentNym payment code (0.00015 BTC))
* Make wallet function without the need for backend API's of samouraiwallet.com
* Refactor code (make classes smaller, cleanup) and add unit tests (currently none)
* Work towards a stable version instead of adding more features

### Why the name Masanari?

Masanari is an alternative name of [Hattori Hanzō](https://en.wikipedia.org/wiki/Hattori_Hanz%C5%8D), a famous samurai of the Sengoku era,
credited with saving the life of [Tokugawa Ieyasu](https://en.wikipedia.org/wiki/Tokugawa_Ieyasu) and then helping him to become the ruler of united Japan.

### Features:

[Masanari Wallet features list](Masanari-Wallet-features.md)

### Build:

Import as Android Studio project. Should build "as is".

### BIP44:

Masanari implementation based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki),
extends [bitcoinj](https://bitcoinj.github.io/).

### BIP47:

Masanari implementation based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0047.mediawiki) by Justus Ranvier.
Extends BIP44 implementation (above).

[Generic source code for BIP47.](https://github.com/SamouraiDev/BIP47_RPC)

[BIP47 test vectors](https://gist.github.com/SamouraiDev/6aad669604c5930864bd)

### BIP49 (Segwit):

Masanari P2SH-P2WPKH implementation based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0049.mediawiki)
by Daniel Weigl and includes support for BIP49-specific XPUBs: [YPUB](https://github.com/Samourai-Wallet/sentinel-android/issues/16).

### BIP69:

Masanari implementation based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0069.mediawiki) by Kristov Atlas.

### BIP84 (Segwit):

Masanari implementation based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki) by Pavol Rusnak.

### BIP125 (Replace-by-fee, RBF):

Masanari implementation based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0125.mediawiki) by David A.
Harding and Peter Todd.

### BIP141 (Segwit):

Masanari spends to bech32 addresses P2WPKH based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0141.mediawiki) by Eric Lombrozo,
Johnson Lau and Pieter Wuille.

### BIP173 (Segwit):

Masanari implementation based on [original BIP](https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki) by Pieter Wuille and Greg Maxwell.

### Spending:

Masanari spends include the possibility of including custom fees as well as the use of batch spending
(build up a list of batched outputs for grouped spend and fee savings).

### Ricochet:

Masanari implementation of multi-hop spend designed to outrun the baying pack of #KYCRunningDogs.

### STONEWALL:

STONEWALL spend is designed to increase the number of combinations between inputs and outputs (transaction entropy).
It replaces the previously used BIP126. The objective is to obtain a positive entropy score
using [Boltzmann](https://github.com/Samourai-Wallet/boltzmann) evaluation of the transaction.

### Tor:

Masanari indicates whether or not connections are being routed via Tor Socks5 proxy (uses Orbot).

### TestNet3:

MainNet/TestNet selection is displayed when sideloading a new installation. To switch networks, make a backup of your current wallet,
uninstall/reinstall (sideload) and select desired network.

### OpenDime:

Plug in your OpenDime using the appropriate OTG (On-The-Go) USB cable and Masanari can be used to view address and balance,
validate the private key, and sweep balance to your wallet.

### License:

[Unlicense](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/master/LICENSE)

### Contributing:

All development goes in 'develop' branch - do not submit pull requests to 'master'.
