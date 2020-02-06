# Table of Contents
- [**Furkan's FAQ**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/General%20Help%20%26%20Support.md#Furkan's-FAQ)
- [**Samourai Wallet Basics**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/General%20Help%20%26%20Support.md#Is-Bitcoin-Cash/bcash-supported?)
- [**Bitcoin Basics**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/General%20Help%20%26%20Support.md#Bitcoin-Basics)

## FAQs

### Furkan's FAQ

The following FAQ was put together by [Samourai Telegram](https://t.me/SamouraiWallet) admin Furkan. To help answer common questions in the telegram group. 

**Q: WTF is Dojo?**

A: Dojo is the name of the backend that Samourai uses, also the name of the "red" Nodl box

**Q: Well where can I download the wallet for IOS?**

A: You cant, IOS is not available and wont anytime soon

**Q: Oh that sucks, can you tell me what Whirlpool is?**

A: Whirlpool is the Samourai implementation of CoinJoin

**Q: What is CoinJoin?** 

A: It's a transaction where people contribute inputs and outputs, it helps to break chain analysis tools, gives you privacy and makes the Bitcoin ecosystem more fungible 

**Q: You talk about post-mix and #cahoots what are those?**

A: After doing CoinJoin transactions its important to not merge non-CoinJoined Bitcoins with CoinJoined ones, that's where #cahoots will help, #cahoots is the general name of the available post-mix methods that Samourai has

**Q: What are those post-mix methods?**

A: There are 3 methods:

- **Stonewall**: It's a basic transaction that looks like a mini-CoinJoin on the blockchain

- **Stonewallx2**: It's a real mini-CoinJoin where your friend provides you inputs 

- **Stowaway**: It's a special transaction which looks like a normal transaction but actually is a mini-CoinJoin between you and your friend where the real transacted amount is obfuscated on the blockchain 

**Q: Does the ricochet feature also improves my privacy?**

A: Not exactly, exchanges can sometimes blacklist or "freeze" your Bitcoins when they see a CoinJoin in your transactions history, ricochet aims to solve these problems by putting distance between you and your exchange. There is also staggered ricochet which leaves a better fingerprint and looks more realistic in the blockchain (it takes longer than the normal ricochet) it's advised to use ricochet when you send Bitcoin to your exchange

**Q: Thank you, last question is what the hell PayNym's are?**

A: PayNym is what Samourai call it, in reality they are payment codes (BIP47) which are awesome for privacy. imagine you share an address with public and nobody can lookup the balance of it from the blockchain, you dont have to imagine you can start to use them today! 

### Is Bitcoin Cash/bcash supported?

**No..**

### Is segwit supported?

**Yes**

Samourai fully supports segwit. The wallet by default has segwit enabled with native segwit addresses (BIP84/bech32) displayed.

You can optionally enable Segwit Compatibility (BIP49) addresses, or Legacy (BIP44) addresses on an on-demand basis. 

### Can I buy BTC with Samourai Wallet?

**No.**

Samourai is software designed to store and secure the BTC you already have. 

### How are miner fees calculated?

Bitcoin transactions are sent to the network with an extra fee attached. This fee is paid directly to the miner who adds the transaction to a block. Miner fees are based on the size of the transaction being sent in bytes. Miner fees are not impacted by the amount of Bitcoin being spent.

The fee rate is generally measured in Satoshi/byte. A Satoshi is the smallest unit of bitcoin (0.00000001 BTC). A standard bitcoin transaction is around 250 bytes. So, a standard transaction with a fee rate of 100 sat/b  would be calculated as: 

`250 byte transaction * 100 satoshis = 25,000 satoshis = 0.00025000 BTC`

Simply put, the more unspent outputs that are used to form a transaction, the larger the transaction will be in bytes. By default, Samourai creates larger transactions than average for privacy protections. You can configure Samourai to create the smallest possible transactions at the cost of privacy protections. 

### What are the Android permissions Samourai makes use of?
You may be wondering why the Samourai Wallet app available from Google Play needs a certain permission. This article will try to explain what each permission is needed for and what exactly Samourai is doing once you give permission. 

**SMS**

**_Currently disabled in the current Google Play store version_**

This permission is required if you make use of our remote commands functionality. This permission allows Samourai to scan incoming SMS messages for your remote command. Remote commands are turned off by default, and this permissions will not be used unless you turn remote commands on. 

**Phone**

**_Currently disabled in the current Google Play store version_**

This permission is required if you make use of the Stealth Mode feature. When Stealth Mode is enabled by the user, Samourai is hidden from the phone and the user must use the default phone dialer to launch Samourai. Stealth Mode is disabled by default, and this permission will not be used unless you turn on Stealth Mode.

**Photos/Media/Files/Storage**

This permission is to save an encrypted backup of your wallet on your device. This encrypted backup is used to restore your wallet and all associated metadata. Samourai saves this encrypted backup to your device filesystem by default. You may disable this functionality in the settings if you wish to prevent Samourai from using this permission, though it is highly discouraged. 

**Camera**

This permission is required to use the device camera for scanning QR codes. This is a required permission


### Can I reuse a Bitcoin address from my wallet?

Yes. The addresses that your wallet generates can be reused as many times as you want, **but it is considered a privacy risk**. You should try to avoid this behavior whenever possible to maximize your privacy. 

### My wallet / balance / transactions won't load

If your balance is incorrect and/or transactions aren't displaying the most common reason is a network failure.

**Check Tor:**

If you previously enabled Tor on your wallet, make sure that Tor is connected and is not being blocked by your network. You may also wish to try getting a new identity via the "New Identity" button in Notifications Bar at the top of your screen, and/or by toggling Tor OFF/ON via the Networking screen in Samourai. 

**Check VPN:**

If you are using a VPN, make sure your VPN is correctly configured and not blocking traffic.

**Check Data Connection:**

Try toggling your Data Connection OFF and then back ON via the Networking screen in Samourai, and then refreshing your balance. 

**Back out of wallet or force close & relaunch**

Try backing out of the wallet using the back button on the nav bar or go to settings and forse close the app & relaunch. 

**Contact Support:**

If after checking all of the above you are still unable to load your balance & transaction history, please contact us for additional support. You can contact support by email at support@samouraiwallet.com.

### When I scan the QR for my receive address in another app, I see an error message.

If you attempt to scan the QR code for one of your Samourai wallet's receive addresses via a third-party service and see an error message, then the service you are using may not yet support bech32/Native Segwit addresses yet. 


As of update 0.99.81 to Samourai,  bech32 addresses are now the default address type used by the wallet. If you require another address type, they are still available via the advanced options on the Receive screen.

To change your receive address type to Segwit Compatibility or Legacy, simply tap the gearbox/settings icon at the bottom of the Receive screen in Samourai, and adjust your address type accordingly.

## Bitcoin Basics

Some great resources for getting aquinted with Bitcoin

[bitcoin-only](https://bitcoin-only.com/) 

[6102bitcoin FAQ](https://github.com/6102bitcoin/FAQ)

[6102bitcoin bitcoin-intro](https://github.com/6102bitcoin/bitcoin-intro) 

### BIP 44, BIP 49, and BIP84

**BIP44** refers to the accepted common standard to derive non segwit addresses. These addresses always begin with a _1_.

**BIP49** refers to the accepted common standard of deriving segwit "compatibility" addresses. These addresses begin with a _3_.

**BIP84** refers to the accepted common standard of deriving native segwit addresses. These addresses always begin with _bc1_ - and are referred to bech32 addresses. 

Samourai Wallet automatically manages and is fully compliant with all three of the address derivation standards noted above.

<img src="https://github.com/Crazyk031/samourai-wallet-android/blob/develop/Guides/Images/Addressmindblown.jpeg" width="300" height="300" /> 

### Segwit

Segwit is short for Segregated Witnesses which is a soft fork upgrade to the Bitcoin network that was activated on August 24, 2017.  The actual technical workings are fairly complicated but well documented for the curious. What it means for average users is lower transaction fees when spending bitcoin, and paving the way for future layer two applications like Lightning network. 

**Samourai supports Segwit.**

Samourai wallet was the second mobile wallet to implement the Segwit upgrade and it is enabled by default in Samourai Wallet. 

### XPUB's, YPUB's, ZPUB's...

**What is a XPUB?**

An _Extended Public Key_ - also known as an XPUB -  is a part of a Bitcoin standard (BIP32) that can be thought of as a 'read only' view into a wallet. An XPUB allows full view to all transactions, addresses, and balances in a specific wallet, but doesn't allow spending of any kind. For spending, private keys are required, and the XPUB doesn't contain any private keys. 

**What is a YPUB?**

A YPUB is exactly like an XPUB except the 'Y' denotes with certainty that this particular extended public key belongs to a wallet that is also following the BIP49 Bitcoin standard - which details a backwards compatible addressing scheme for segregated witnesses - the latest upgrade to the Bitcoin network . 

**What is a ZPUB?**

A ZPUB is exactly like a YPUB in that it denotes an extended public key for a segregated witness enabled wallet following BIP49, however it also denotes that this particular wallet does not follow the backwards compatible addressing scheme, instead it follows a new scheme that is beginning to receive widespread adoption.






