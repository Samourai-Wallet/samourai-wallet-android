# Table of Contents
- [**Trusted Node**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Advanced%20Features.md#Trusted-Node)
- [**Dojo**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Advanced%20Features.md#Dojo)
- [**Pairing your wallet to your Dojo**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Advanced%20Features.md#Pairing-your-wallet-to-your-Dojo)
- [**Displaying unspent outputs**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Advanced%20Features.md#Displaying-unspent-outputs)
- [**View a UTXO private key**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Advanced%20Features.md#View-a-UTXO-private-key)
- [**Sign a message with a UTXO**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Advanced%20Features.md#Sign-a-message-with-a-UTXO)
- [**Like-type change outputs**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Advanced%20Features.md#Like-type-change-outputs)



## Trusted Node

**_Trusted node has been phased out, you must now use Dojo_**

If you would like to know more about trusted node & how it was implemented [Samourai.kayako](https://samourai.kayako.com/section/15-Trusted-node)

## Dojo

**What is Dojo?**

Samourai Dojo is the backend server for your Samourai Wallet. By default your wallet is connected to the Samourai servers Dojo, but to provide privacy & financial self sovereignty you can implement your own Dojo! This provides HD account, loose addresses (BIP47) balances, transactions lists & unspent output lists to the wallet. As well as PushTX endpoint broadcasts transactions through the backing bitcoind node. All through Tor! 

[Dojo Github](https://github.com/Samourai-Wallet/samourai-dojo)

[Dojo FAQ](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Dojo.md)

[Dojo Telegram Group](https://t.me/samourai_dojo)

For a very easy & slick implementation of Dojo check out [Ronin](https://github.com/RoninDojo/RoninDojo)

### Pairing your wallet to your Dojo

Once the database(IBD) has finished syncing, you can pair your Samourai Wallet with your Dojo in 2 steps:

- Open the maintenance tool in a Tor browser (Tor v3 onion address) and sign in with your admin key.

- Get your smartphone and launch the Samourai Wallet app. **Currently only new wallets should be paired.** When you first open the app hit the [⋮] in the upper right corner and choose  "Connect to existing Dojo". Scan the QRCode displayed in the "Pairing" tab of the maintenance tool.

If you experience any problems when pairing, try re-installing the app and select "Connect to existing Dojo" from the [⋮] menu.

## Utxo Management 

### Displaying unspent outputs

Unspent outputs are simply put, Bitcoin in your wallet that can be spent as an input in a new transaction. The combined amount of unspent outputs is equal to your total spendable balance. 

**How to display unspent outputs**

Tap the **three vertical dots** on the top right of the toolbar and then tap **Show unspent outputs**. This will launch a list of current unspent outputs in your wallet.

### View a UTXO private key

Samourai Wallet is an HD wallet. This means that when you combine your 12 secret words with your passphrase you can generate every private key for every address your wallet will ever create. However there are circumstances where you may quickly require an individual private key for an active address in your wallet. This guide will walk you through viewing a private key for an active UTXO in your wallet.

**Step 1 - Show unspent outputs**

Tap the **three vertical dots** on the top right of the toolbar and then tap **Show unspent outputs**. This will launch a list of current unspent outputs in your wallet.

**Step 2 - Select the address**

Tap the unspent output that corresponds with the bitcoin address that you want to view the private key of. Tap on **show private key**

**Step 3 - View the private key**

The private key will be displayed to you in both text and QR code form. **Please be aware that the private key grants access to the bitcoin on that address and should never be shared with anyone!**

### Sign a message with a UTXO

Samourai Wallet has built-in signature generation to prove control of a bitcoin address private key without revealing the private key to anyone. The list of current unspent outputs contains a list of addresses that can sign messages in your wallet. Currently you can only sign messages with addresses that contain an unspent balance of Bitcoin. 

**Step 1 - Show unspent outputs**

Tap the **three vertical dots** on the top right of the toolbar and then tap **Show unspent outputs**. This will launch a list of current unspent outputs in your wallet.

**Step 2 - Select the address to sign from**

Tap the unspent output that corresponds with the Bitcoin address that you want to sign the message from. Tap on **sign message**

**Step 3 - Enter your message**

Enter whatever message you want to sign in the textbox and then press **Yes** when finished.

**Step 4 - Copy and share**

Copy the signature and share it.

### Like-type change outputs

When a Bitcoin transaction is created, the Bitcoin wallet searches for available unspent outputs in the wallet and uses them to send the specified amount to the specified destination. 

The entire unspent output must be spent, so if the amount on the output is larger than the specified amount to send, the remainder is returned to your wallet, in an output referred to as "_change_".

By default, Samourai Wallet will either create segwit enabled or standard change outputs depending on the type of address that is being **sent to**. For example, if you are sending to a segwit enabled address, the change output returned to your wallet will also be segwit enabled. Conversely, if you're sending to a legacy Bitcoin address, the change output will be a legacy address. 

**Privacy benefit**

Like-type change outputs are a privacy feature designed to obfuscate the change output and the destination output when viewed on the blockchain. 

**Disable like-type change outputs**

Users who prefer to keep all change outputs segwit enabled can disable like-type change outputs in the settings.

1. Tap the three vertical dots on the top right of the toolbar and then tap Settings.

2. Tap the Transactions option.

3. Tap the Receive change to like-typed outputs option.
