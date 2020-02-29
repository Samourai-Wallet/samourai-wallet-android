# Table of Contents
- [**Getting Acquainted with Bitcoin**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Getting%20Started.md#Getting-Acquainted-with-Bitcoin)
- [**Starting your wallet**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Getting%20Started.md#Starting-your-wallet)
- [**Importing a wallet from another app**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Getting%20Started.md#Importing-a-wallet-from-another-app)
- [**Receiving Bitcoin**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Getting%20Started.md#Receiving-Bitcoin)
- [**Sweep a private key**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Getting%20Started.md#Sweep-a-private-key)
- [**Sending Bitcoin**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Getting%20Started.md#Sending-Bitcoin)
- [**Using Ricochet Send**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Getting%20Started.md#Using-Ricochet-Send)

# Getting Acquainted with Bitcoin

[bitcoin-only](https://bitcoin-only.com/) 

[6102bitcoin FAQ](https://github.com/6102bitcoin/FAQ)

[6102bitcoin bitcoin-intro](https://github.com/6102bitcoin/bitcoin-intro) 

# Starting your wallet

Samourai  Wallet uses a BIP39 seedphrase. The password is the 13th word. **It wont have been saved anywhere, their are multiple warnings when you create a wallet to remember/record it.**

Suggestions, write down everything seedphrase/passphrase/pin and put in secure location. Also make multiple copies of your encrypted backup on your device(save to seperate location/SD card/password manager) or in another secure digital location. You can also email it to yourself(its encrypted so clearnet is OK) or better yet send via something like Tutanota(save your recover seed for this too), which is also encrypted.

## New wallet

[**Video Tutorial**](https://www.youtube.com/watch?v=SXboJEaXzlA&list=PLIBmWVGQhizJ-mgDIWO5I5OcJlXc3apTF&index=2&t=0s)

Samourai Wallets private keys are generated offline on your device and are never known by anyone but yourself.  Follow this guide to create a new Samourai Wallet.

### Step 1 - Install

Install Samourai Wallet from the [Google Play store](https://play.google.com/store/apps/details?id=com.samourai.wallet&hl=en_US) or [Gitlab APK](https://code.samourai.io/wallet/samourai-wallet/blob/master/apk) onto your Android device. 

### Step 2 - Create new wallet

Once Samourai is installed onto your device launch the app and tap on the **Create Wallet** button

### Step 3 - Create a passphrase

You will now be asked to create and confirm a passphrase of your choosing. This passphrase will provide additional security to your Bitcoin wallet and will allow you to easily export your wallet onto any other compatible Bitcoin wallet software. **IMPORTANT: We do not know or store your passphrase, if you forget your passphrase we cannot help you reset it. Do not forget your passphrase!**

### Step 4 - Create a PIN code

You will now be asked to create and confirm a PIN code between 5 and 8 digits long. The PIN is used to easily access your wallet without needing to enter your passphrase. If you forget your PIN code you can always access your wallet with your passphrase.

### Step 5 - Write down your secret words

Samourai has now created a brand new Bitcoin wallet for you. You will be shown 12 random words. **It is crucial that you write down and secure these 12 secret words.**
 
These words when used **(in order!)** together with your passphrase can regenerate your entire wallet, balance, and history.  **IMPORTANT: Your secret words must be kept a secret. Anyone who knows your secret words and your passphrase will be able to steal your Bitcoin. Never keep your words saved on a computer or the cloud.**

### Step 6 - Success!

Your wallet has now been created and you are ready to begin sending and receiving Bitcoin payments!

## Importing a wallet from another app

[**Video Tutorial**](https://www.youtube.com/watch?v=MvcPud3-2ng&list=PLIBmWVGQhizJ-mgDIWO5I5OcJlXc3apTF&index=3&t=0s)

Samourai Wallet is compatible with all modern Bitcoin wallets that follow standards put forward and enforced by the Bitcoin community. Follow this guide to import an existing wallet

### Step 1 - Determine compatibility

Samourai Wallet is compatible with all Bitcoin wallets that support the standard **BIP39 & BIP44**. You may need to check with your existing wallet provider to ensure they support these modern standards. 

This is a partial list of compatible wallets:

* Mycelium Wallet
* Yallet
* Coinomi
* Blockchain.info HD
* Airbitz
* Electrum HD
* Multibit HD
* Trezor
* Ledger
* Bither

### Step 2 - Install Samourai

Install Samourai Wallet from the [Google Play store](https://play.google.com/store/apps/details?id=com.samourai.wallet&hl=en_US) or [Gitlab APK](https://code.samourai.io/wallet/samourai-wallet/blob/master/apk) onto your Android device. 

### Step 3 - Restore wallet

Once Samourai is installed onto your device launch the app and tap on the three vertical dots in the top-right hand corner of the screen. From the drop-down list that appears, tap "Import external wallet". 

### Step 4 - Enter secret words (mnemonic)

On the first line enter your wallet's secret words. Depending on the wallet you are restoring from this may be 12 or 24 words.

On the second line enter your BIP39 passphrase. *Many wallets do not have one of these, so if you are unsure leave it blank.*

### Step 5 - Create PIN code

You will now be asked to create and confirm a PIN code between 5 and 8 digits long.

If you didn't get to the PIN code creation screen then there is something wrong with your secret words. Please double check the spelling and order of the words and try again.

### Step 6 - Verify

A successful restore will display the expected balance of your wallet and a complete transaction history. 

If the restore is unsuccessful a 0 balance and empty history would be displayed. 

In the case of an unsuccessful restore try the above steps again taking care to make sure the passphrase is correct.

# Receiving Bitcoin 

## Make your first deposit

First things first. You cannot buy Bitcoin with Samourai Wallet. You can only store Bitcoins you already own. In order to take full advantage of our premium functionality that enhances your privacy you must have some Bitcoin in your wallet. We recommend an initial deposit of least $5.00 worth of BTC to unlock the full potential of Samourai.

### Step 1 - Get your latest Bitcoin address

Your Samourai Wallet contains an infinite amount of Bitcoin addresses that get used once and then are archived. To get your latest Bitcoin address press the blue '+' button on the bottom right of the main balance and transactions screen. Then press the green 'Receive' button to open the Receive screen.

### Step 2 - Scan or share your address

Samourai Wallet will display a QR code of your latest Bitcoin address. Underneath the QR code is the actual address text. You can scan this QR code with any other Bitcoin wallet or you share the QR code or address manually...

* **To copy the address to clipboard:** Tap the address text and press 'YES'
* **To share the QR code image:** Tap the share icon in the toolbar

### Step 3 - Send from your other wallet

With your other wallet or service either scan the QR code on the Receive screen or paste the address. We recommend depositing at least $5.00 worth of BTC to unlock the full potential of the wallet. 

### Step 4 - Congrats!

You just made your first deposit! Samourai will notify you once it sees the transaction on the network. This is usually nearly instant. You will see your balance update and your first transaction appear in the transaction list :)

## Get your latest Bitcoin address

Your Samourai Wallet contains an infinite amount of Bitcoin addresses that get used once and then are archived. These archived addresses are never discarded, but they are not used again to protect your privacy and security.

This means your Bitcoin address will change all the time, this is not cause for alarm, consider your addresses as *'one-time-use'* and disposable. 

### Step 1 - The receive screen

To get your latest Bitcoin address press the blue '+' button on the bottom right of the main balance and transactions screen. Then press the green 'Receive' button to open the Receive screen.

### Step 2 - Share your address

Samourai Wallet will display a QR code of your latest Bitcoin address. Underneath the QR code is the actual address text. You can show this QR code if you are in transacting in person or you can share the address or QR code manually.

* **To copy the address to clipboard:** Tap the address text and press 'YES'
* **To share the QR code image:** Tap the share icon in the toolbar

By default, your Samourai wallet will generate bech32 or "Native Segwit" addresses (beginning with 'bc1'). If you are sending to your Samourai wallet using a service that does not yet support Native Segwit addresses, you can also obtain addresses in Segwit Compatibility and Legacy formats, via the **Advanced** gearbox icon on the Receive screen. 

## Sweep a private key

Advanced users can sweep any existing valid Bitcoin private key into Samourai Wallet. This will send the balance of the single private key to an address in Samourai Wallet. 

### Step 1 - Prepare the private key

Samourai Wallet can scan a QR code or you can input the private key manually. Make sure the private key is accessible and ready to be pasted or scanned by Samourai Wallet

### Step 2 - Open the sweep camera

Tap the **three vertical dots** on the top right of the toolbar and then tap **Sweep Private Key**. Then you'll be able to choose enter the private key or launch the sweep camera. Now you can paste the private key or focus the camera onto the QR code of the private key until it scans.

*If the private key is encrypted with a password you will be prompted to enter the password before being able to continue.*

### Step 3 - Confirm the sweep

You will be prompted to confirm the sweep. The entire balance of the private key will be sent to your latest Bitcoin address minus the miner fee. 

**Note: Samourai will not save the private key on the device. You should discard the used private key after sweeping as it is no longer advisable to use again.**

# Sending Bitcoin

[**Offline Sending Video Tutorial**](https://www.youtube.com/watch?v=sl5hR_mwwYM&list=PLIBmWVGQhizJ-mgDIWO5I5OcJlXc3apTF&index=6&t=0s)

## Sending a transaction 

There comes a point when you need to make an outgoing transaction from your Samourai Wallet. This is called a send. Samourai can get pretty fancy with different types of sends but we'll keep it simple here. 

### Quick Scan

If you have a QR code of the recipients Bitcoin address the quickest way to send is to tap the Scanner icon in the toolbar on the main balance screen. This will activate the camera.  Focus the camera on the QR code until it successfully scans. 

This method is very useful when sending to online merchants as they often encode the exact BTC amount directly into the QR code making the process even easier. 

If there is no BTC amount auto-filled after scanning, simply enter the amount you wish to send manually. 

### Manual Send

To get to the Send Screen without activating the Quick Scan camera simply tap the blue '+' button on the bottom right of the main balance and transactions screen. Then press the red 'Send' button. This will open the Send Screen. 

On the send screen you can manually activate the Quick Scan camera by pressing the Scanner code icon in the toolbar. You can also manually paste any Bitcoin address into  the 'To' field. 

### Confirm the send

Provided you have enough Bitcoin to cover the send amount a green 'SEND' button will appear across the bottom of your screen. Tap it to continue. You will be asked to confirm the send one last time. Press OK to broadcast the transaction to the Bitcoin network and wait for at least one confirmation.

## Configuring your wallet for lowest transaction fees

By default, Samourai Wallet errs on the side of increasing the blockchain privacy and plausible deniability of transactions. This focus has a side effect of increasing the size of transactions created, thus increasing the overall cost of the miner fee when sending. The wallet can be fully configured for creating low fee transactions by following the steps shown below.

### Step 1 - Open settings

Tap the **three vertical dots** on the top right of the toolbar and then tap **Settings**. This will launch the wallet settings screen, where all configuration will take place. 

### Step 2 - Disable like-typed outputs

From the main Settings screen tap the **Transactions** option. Disable the **Receive change to like-typed outputs** option by removing the checkmark from the checkbox. Like-typed outputs is a Samourai feature that improves your privacy on the blockchain.

### Step 3 - Disable STONEWALL 

From the **Transactions** settings screen Disable the **STONEWALL Spend** option by removing the checkmark from the checkbox. Disabling STONEWALL will result in much smaller transactions, at the loss of blockchain privacy. 

### Step 4 - Enable RBF

From the **Transactions** settings screen Enable the **Spend using RBF** option by adding a checkmark to the checkbox. Enabling RBF will allow you to later top-up low transaction fees. 


The wallet is now properly configured for creating low fee transactions at the sacrifice of some privacy and plausible deniability gains.

# Premium transaction features 

## Using PayNym payment code connections (BIP47)

Think of a PayNym as a permanent and reusable code that you can share with anyone. It looks similar to a Bitcoin address but it is longer and starts with **'PM8T'**

Anybody who connects to your PayNym can generate private Bitcoin addresses on demand for sending Bitcoin to your wallet.

### Create your first connection to the Samourai Dev Wallet

There are certain premium features and functionality that are only available to users who have an active connection to the Samourai Dev Wallet. We highly recommend users initiate a connection to the Samourai Dev Wallet. 

#### Prerequisites:

* In order to create a connection you will need a small amount of Bitcoin in your wallet to cover the connection fee

### Step 1 - Open PayNyms

From the main screen of the wallet press the blue '+' button located on the bottom right of the screen. Tap the purple **PayNyms** button.

These are your payment code connections. Right now the list is empty, but soon it will contain your first connection to the Samourai Dev Wallet. 

### Step 2 - Create your first connection to the Samourai Dev Wallet

Tap the purple '+' button located on the bottom right of the PayNym screen. Tap the **Recommended** button

You will see a list appear with some recommended payment codes. Select **Samourai Dev Wallet** from the list

### Step 3 - Accept the connection fee

Before the connection is made you will be asked to review the connection fee. Press **Confirm** to accept the fee and broadcast the connection to the Bitcoin network. 

If you do not have enough Bitcoin in your wallet, simply deposit more Bitcoin then return to the PayNyms screen. You will see the Samourai Dev Wallet PayNym added to your list. Tap on it to resume the connection process. 

### Step 4 - Wait for confirmation

You must wait for 1 confirmation on the Bitcoin block chain before the connection is considered established. Once established you can test out the new connection by tapping on Samourai Dev Wallet in the PayNyms list and sending a small donation to help fund further development ;)

## Using Ricochet Send

A Ricochet Send is a Samourai exclusive transaction type. Ricochet defends against Bitcoin blacklists by adding additional decoy transactions between the initial send and eventual recipient. You should consider using Ricochet when sending to Bitcoin Exchanges, and companies that are known to close accounts for flimsy reasons. 

### Step 1 - Start with a normal send

Ricochet is really simple. First initiate a send like you normally would. Either by tapping the square Scanner icon in the wallet toolbar and scanning a QR code, or by tapping the blue '+' button located on the bottom right of the balance and transaction screen and then tapping **Send** button.

### Step 2 - Enable Ricochet

On the Send Screen you will see a **'Ricochet'** label with a toggle in the off position. Tap the toggle to turn it on.

**Note: You should highly consider first connecting to the Samourai Dev Wallet PayNym. It is much better for your privacy if you make the connection before performing a Ricochet.**

### Step 3 - Press Send

It's that simple. Once you confirm the Ricochet fees your Ricochet transaction will be sent to your destination but first it will hop between 4 additional Bitcoin addresses. This type of transaction may take 1-2 minutes to complete. You can also enable Ricochet to space the "hop" transactions to occur in seperete blocks, furthering the connection to the original address.




