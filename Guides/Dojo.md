# Table of Contents 
- [**What is Dojo?**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#What-is-Dojo?)
- [**My Dojo Installation**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#MyDojo-installation-with-Docker-and-Docker-Compose)
- [**Theory of Operation**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#Theory-of-Operation)
- [**Architecture**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#Architecture)
- [**Implementation Notes**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#Implementation-Notes)
- [**Pairing your wallet to your Dojo**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#Pairing-your-wallet-to-your-Dojo)
- [**Commands for Interacting with your Dojo**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#Commands-for-Interacting-with-your-Dojo)
- [**Log Examples**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#Log-Examples)
- [**Common Dojo issues**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md#Common-Dojo-issues)


## What is Dojo?

Samourai Dojo is the backend server/node for your Samourai Wallet. By default your wallet is connected to the Samourai servers Dojo, but to provide privacy & financial self sovereignty you can implement your own Dojo! This provides HD account, loose addresses (BIP47) balances, transactions lists & unspent output lists to the wallet. As well as a PushTX endpoint that broadcasts transactions to the network through the backing bitcoind node. All through Tor! 

[Dojo Github](https://github.com/Samourai-Wallet/samourai-dojo)

[Dojo Telegram Group](https://t.me/samourai_dojo)

For a very easy & slick implementation of Dojo check out [RoninDojo Wiki](https://code.samourai.io/ronindojo/RoninDojo/-/wikis/home)

[RoninDojo Telegram Group](https://t.me/RoninDojoUI)

[View API documentation](https://github.com/Samourai-Wallet/samourai-dojo/blob/master/doc/README.md)


## Installation 

### MyDojo installation with Docker and Docker Compose

This setup is recommended to Samourai users who feel comfortable with a few command lines.

It provides in a single command the setup of a full Samourai backend composed of:

* a bitcoin full node only accessible as an ephemeral Tor hidden service,
* the backend database,
* the backend modules with an API accessible as a static Tor hidden service,
* a maintenance tool accessible through a Tor web browser,
* a block explorer ([BTC RPC Explorer](https://github.com/janoside/btc-rpc-explorer)) accessible through a Tor web browser.

See [the documentation](https://github.com/Samourai-Wallet/samourai-dojo/blob/develop/doc/DOCKER_setup.md) for detailed setup instructions.

## Theory of Operation

Tracking wallet balances via `xpub` requires conforming to [BIP44](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki), [BIP49](https://github.com/bitcoin/bips/blob/master/bip-0049.mediawiki) or [BIP84](https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki) address derivation scheme. Public keys received by Dojo correspond to single accounts and derive all addresses in the account and change chains. These addresses are at `M/0/x` and `M/1/y`, respectively.

Dojo relies on the backing bitcoind node to maintain privacy.

### Architecture

<a name="architecture"/>



                -------------------    -------------------      --------------------
               |  Samourai Wallet  |  |     Sentinel      |    | Bitcoin full nodes |
                -------------------    -------------------      --------------------
                        |_______________________|_______________________|
                                                |
                                          ------------

                                          Tor network

                                          ------------
                                                |
                  Host machine                  | (Tor hidden services)
                 ______________________________ | _____________________________
                |                               |                              |
                |                      -------------------           dmznet    |
                |                     |   Tor Container   |                    |
                |                      -------------------                     |
                |                             |        |                       |
                |             -------------------      |                       |
                |            |  Nginx Container  |     |                       |
                |             -------------------      |                       |
                |- - - - - - - - - - - | - - -|- - - - | - - - - - - - - - - - |
                |     --------------------    |     --------------------       |
                |    |  Nodejs Container  | ------ | Bitcoind Container |      |
                |     --------------------    |     --------------------       |
                |               |             |               |                |
                |     --------------------    |     --------------------       |
                |    |  MySQL Container   |   ---- |  BTC RPC Explorer  |      |
                |     --------------------          --------------------       |
                |                                                              |
                |                                                  dojonet     |
                |______________________________________________________________|


<a name="requirements"/>

Dojo is composed of 3 modules:
* API (/account): web server providing a REST API and web sockets used by Samourai Wallet and Sentinel.
* PushTx (/pushtx): web server providing a REST API used to push transactions on the Bitcoin P2P network.
* Tracker (/tracker): process listening to the bitcoind node and indexing transactions of interest.

API and PushTx modules are able to operate behind a web server (e.g. nginx) or as frontend http servers (not recommended). Both support HTTP or HTTPS (if SSL has been properly configured in /keys/index.js). These modules can also operate as a Tor hidden service (recommended).

Authentication is enforced by an API key and Json Web Tokens.

### Implementation Notes

**Tracker**

* ZMQ notifications send raw transactions and block hashes. Keep track of txids with timestamps, clearing out old txids after a timeout
* On realtime transaction:
  * Query database with all output addresses to see if an account has received a transaction. Notify client via WebSocket.
  * Query database with all input txids to see if an account has sent coins. Make proper database entries and notify via WebSocket.
* On a block notification, query database for txids included and update confirmed height
* On a blockchain reorg (orphan block), previous block hash will not match last known block hash in the app. Need to mark transactions as unconfirmed and rescan blocks from new chain tip to last known hash. Note that many of the transactions from the orphaned block may be included in the new chain.
* When an input spending a known output is confirmed in a block, delete any other inputs referencing that output, since this would be a double-spend.


**Import of HD Accounts and data sources**

* First import of an unknown HD account relies on a data source (local bitcoind or OXT). After that, the tracker will keep everything current.

* Default option relies on the local bitcoind and makes you 100% independent of Samourai Wallet's infrastructure. This option is recommended for better privacy.

* Activation of bitcoind as the data source:
  * Edit /keys/index.js and set "indexer.active" to "local_bitcoind". OXT API will be ignored.

* Activation of OXT as the data source (through socks5):
  * Edit /keys/index.js and set "indexer.active" to "third_party_explorer".

* Main drawbacks of using your local bitcoind for these imports:
  * This option is considered as experimental. 
  * It doesn't return the full transactional history associated to an HD account or to an address but only transactions having an unspent output controlled by the HD account or the address.
  * It's slightly slower than using the option relying on the OXT API.
  * It may fail to correctly import an existing wallet if this wallet had a large activity.
  * If you use bitcoind and if the import seems to return an invalid balance, you can use the "XPUB rescan" function provided by the maintenance tool. This function allows you to force the minimum number of addresses to be derived and the start index for the derivation.
  * As a rule of thumb, we recommend to use bitcoind as the source of imports and to setup your Dojo with a new clean wallet. It increases your privacy and it removes all potential issues with the import of a large wallet.
  
### Pairing your wallet to your Dojo

Once the database(IBD) has finished syncing, you can pair your Samourai Wallet with your Dojo in 2 steps:

- Open the maintenance tool in a Tor browser (Tor v3 onion address) and sign in with your admin key.

- Get your smartphone and launch the Samourai Wallet app. **Currently only new wallets should be paired.** When you first open the app hit the [⋮] in the upper right corner and choose  "Connect to existing Dojo". Scan the QR Code displayed in the "Pairing" tab of the maintenance tool.

If you experience any problems when pairing, try re-installing the app and select "Connect to existing Dojo" from the [⋮] menu.

## Commands for Interacting with your Dojo

### Usage: `./dojo.sh  "command"  "module" "options"`

**To use these commands you must be in the correct directory**

So first:

`cd dojo/docker/my-dojo`

**Available commands:**

Display the help message: `help` 

Launch a bitcoin-cli console for interacting with bitcoind RPC API: `bitcoin-cli` 

Free disk space by deleting docker dangling images and images of previous versions: `clean` 

Install your Dojo: `install`

**Logs**

----

`sudo logs "module" "options"`

**Examples**

For logs:

`sudo ./dojo.sh logs api`

For error logs:

`sudo ./dojo.sh logs api -d error -n 500`

This shows the Api errors for up to 500 lines

Display the logs of your Dojo. Use `CTRL+C` to stop the logs. 

**Available modules:**

Display the logs of all containers: `dojo.sh logs` 

Display the logs of bitcoind: `dojo.sh logs bitcoind`

Display the logs of the MySQL database: `dojo.sh logs db` 

Display the logs of tor: `dojo.sh logs tor`

Display the logs of the REST API (nodejs): `dojo.sh logs api`

Display the logs of the Tracker (nodejs): `dojo.sh logs tracker`

Display the logs of the pushTx API (nodejs): `dojo.sh logs pushtx`

Display the logs of the Orchestrator (nodejs): `dojo.sh logs pushtx-orchest`

**Available options:** _(for api, tracker, pushtx and pushtx-orchest modules):_

Select the type of log to be displayed. VALUE can be output (default) or error: `-d [VALUE]`

Display the last VALUE lines: ` -n [VALUE]`

----
**Other information**

Show current blockchain info: `sudo ./dojo.sh bitcoin-cli getblockchaininfo`

Show Dojo network info: `sudo ./dojo.sh bitcoin-cli getnetworkinfo`

----

Display the Tor onion address allowing your wallet to access your Dojo: `onion` 

Restart your Dojo: `restart` 

Start your Dojo: `start` 

Stop your Dojo: `stop` 

Delete your Dojo. **Be careful! This command will also remove all data**: `uninstall` 

Upgrade your Dojo: `upgrade` 

Display the version of dojo: `version` 

## Log Examples

If your new to CL you might be thinking what the hell am I looking at or looking for?! Let's have a look at what your looking for, below is examples of logs showing everything is running smoothly:

### bitcoind

<img src="https://github.com/Crazyk031/Images/blob/master/Bitcoind%20logs.jpg" width="467" height="428" />

This shows the blocks mined.

 As well as tor connections to peers, which rotate so you can disregard `failed: general failure` & `failed: connection refused`. 

`warning= 'xx of last 100 blocks have unexpected version` Just means the miner that mined the block is running a different version of core than yourself, you can disregard. 

### db 

<img src="https://github.com/Crazyk031/Images/blob/master/DB%20logs.jpg" width="458" height="815" />

`This connection closed normally without authentication` can be disregard as this is normal. 

### tor

<img src="https://github.com/Crazyk031/Images/blob/master/Tor%20logs.jpg" width="471" height="560" />

Tor has been 100% bootstrapped and ready to go, `Have tried resolving  or connecting to address '[scrubbed]' at 3 different places. Giving up.` is totally normal and just means tor is looking for a connection

### api

From Dojo documentation:

> API (/account): web server providing a REST API and web sockets used by Samourai Wallet and Sentinel.

api will display the pubkeys that are being tracked via `multiaddr` & `unspent`, it will also display when a new block is mined. No screenshot as the pubkeys displayed are those of your HD public addresses. **If someone has access to these they can see your entire HD wallet balance & transactions!**

### tracker 

From Dojo documentation:

> Tracker (/tracker): process listening to the bitcoind node and indexing transactions of interest.

<img src="https://github.com/Crazyk031/Images/blob/master/Tracker%20logs.jpg" width="469" height="430" />


This will show your Dojo processing active mempool transactions & validating blocks mined

### pushtx

From Dojo documentation:

> A simple server that relays transactions from the wallet to the full node.
> 
> PushTx (/pushtx): web server providing a REST API used to push transactions on the Bitcoin P2P network.

This will display the transactions pushed from your wallet & their amount in BTC

### pushtx-orchest

This will show blocks mined

## Common Dojo issues

1. After the first run of `dojo.sh install` the MySQL configs **can not** be modified. This will result in permission issues & the only solution is to completely remove and reinstall Dojo. 
- A sign that this has happened is getting a "Bad Request" when checking the Api & pushtx tabs in the Dojo maintenance tool. 

2. You need to know what are the different types of `.config` files for Dojo to know which one needs to be modified to make changes. 
- `.conf` is the config file your Dojo is working off of
- `.conf.tpl` is the temporary config file that's in Dojo that gets overwritten during initial install. 
- `.conf.save` is the config that is stored after an update to Dojo has been done to make a copy of the previous settings, just in case! 

3. With Dojo installed to use an external `bitcoind` you can run into network issues. Usual causes are:
-  A firewall blocking a port used by `bitcoind`
- An incorrect configuration (in `bitcoin.conf` or in `docker-nodejs.conf`)
- Running into difficulty to determine the correct IP addresses to be used in `bitcoin.conf`
