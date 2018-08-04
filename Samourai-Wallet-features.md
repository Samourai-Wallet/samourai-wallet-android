# Samourai Wallet

## Features

### Wallet
- Your private keys are stored on your device and are never communicated with any server
- Sweep funds from private keys
- Standard BIP44 avoids address reuse 
- Put Samourai into stealth mode to hide it on the device
- May use separate PIN code for stealth mode launch
- Enable remote SMS commands to regain access to your funds if you lose your phone
- Obtain new phone number if SIM card swapped out following loss or theft
- Custom fiat currency prices from popular exchanges
- Segwit/UASF block explorer support
- BIP39 passphrase enforced on new wallets
- Sign messages using utxo addresses (including P2SH-P2WPKH Segwit utxos)
- Read, validate, sweep Coinkite OpenDime
- Detect private keys remaining in clipboard

### Security
- 5-to-8 digit PIN protected access
- PIN entry grid may be scrambled
- AES-256 encryption of internal metadata
- Exportable backup encrypted using BIP39 passphrase
- Route outgoing transactions via your own trusted node (JSON-RPC)
- Connect via your preferred VPN
- Connect via Tor (Socks5 proxy using Orbot)
- Samourai pushTx over Tor
- PoW (proof-of-work) check when using trusted node
- Real-time alert if your wallet is being "dusted"

### Stealth addressing/BIP47 payment channels
- BIP47 "Reusable Payment Codes" support
- PayNym.is lookup of BIP47 payment codes
- BIP47 payment codes scannable via BIP21
- Sign messages using BIP47 notification address

### Transactions
- Full Segwit support (P2SH-P2WPKH[BIP49], P2WPKH[BIP84, bech32])
- Sweep P2SH-P2WPKH, P2WPKH[bech32] private keys
- Batched spending (several outputs)
- RBF (replace-by-fee) detection for incoming transactions
- BIP69 deterministic sorting of input/outputs to prevent the wallet from leaving a discernible block chain fingerprint
- STONEWALL spending for obfuscating your outgoing transactions, based on [Boltzmann score](boltzmann)   
- Ricochet spend (spend using several hops), updated for bech32 support
- Select fee provider (21.co or bitcoind)
- Dynamic fee support guarantees fast confirm times
- Display up-to-date miners' fees
- Display UTXO list (optionally display private keys of UTXOs, redeem scripts of P2SH-P2WPKH, P2WPKH)
- Any UTXO can be flagged as "unspendable"
- Allow custom fee (in addition to proposed low-normal-high fees) when spending
- CPFP (child-pays-for-parent) for unconfirmed received transactions
- CPFP (child-pays-for-parent) for unconfirmed sent transactions
- Opt-in RBF (replace-by-fee)
- Display transaction as hex/QR code for alternative means of transmission
- Block utxo of non-broadcast transactions (avoid spending utxo used in transactions not yet broadcast)
- push any signed tx (scanned or pasted in hex format)