# Starting your wallet

## New wallet

Samourai Wallets private keys are generated offline on your device and are never known by anyone but yourself.  Follow this guide to create a new Samourai Wallet.

### Step 1 - Install

Install Samourai Wallet from the [Google Play store](https://play.google.com/store/apps/details?id=com.samourai.wallet&hl=en_US) onto your Android device. 

### Step 2 - Create new wallet

Once Samourai is installed onto your device launch the app and tap on the **Create Wallet** button

### Step 3 - Create a passphrase

You will now be asked to create and confirm a passphrase of your choosing. This passphrase will provide additional security to your bitcoin wallet and will allow you to easily export your wallet onto any other compatible bitcoin wallet software. **IMPORTANT: We do not know or store your passphrase, if you forget your passphrase we cannot help you reset it. Do not forget your passphrase.**

### Step 4 - Create a PIN code

You will now be asked to create and confirm a PIN code between 5 and 8 digits long. The PIN is used to easily access your wallet without needing to enter your passphrase. If you forget your PIN code you can always access your wallet with your passphrase.

### Step 5 - Write down your secret words

Samourai has now created a brand new Bitcoin wallet for you. You will be shown 12 random words. **It is crucial that you write down and secure these 12 secret words.**
 
These words when used together with your passphrase can regenerate your entire wallet, balance, and history.  **IMPORTANT: Your secret words must be kept a secret. Anyone who knows your secret words and your passphrase will be able to steal your bitcoin. Never keep your words saved on a computer or the cloud.**

### Step 6 - Success!

Your wallet has now been created and you are ready to begin sending and receiving bitcoin payments!

## Importing a wallet from another app

Samourai Wallet is compatible with all modern bitcoin wallets that follow standards put forward and enforced by the bitcoin community. Follow this guide to import an existing wallet

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

Install Samourai Wallet from the [Google Play store](https://play.google.com/store/apps/details?id=com.samourai.wallet&hl=en_US) onto your Android device. 

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

First things first. You cannot buy bitcoin with Samourai Wallet. You can only store bitcoins you already own. In order to take full advantage of our premium functionality that enhances your privacy you must have some bitcoin in your wallet. We recommend an initial deposit of least $5.00 worth of BTC to unlock the full potential of Samourai.

### Step 1 - Get your latest bitcoin address

Your Samourai Wallet contains an infinite amount of bitcoin addresses that get used once and then are archived. To get your latest bitcoin address press the blue '+' button on the bottom right of the main balance and transactions screen. Then press the green 'Receive' button to open the Receive screen.

### Step 2 - Scan or share your address

Samourai Wallet will display a QR code of your latest bitcoin address. Underneath the QR code is the actual address text. You can scan this QR code with any other bitcoin wallet or you share the QR code or address manually...

* **To copy the address to clipboard:** Tap the address text and press 'YES'
* **To share the QR code image:** Tap the share icon in the toolbar

### Step 3 - Send from your other wallet

With your other wallet or service either scan the QR code on the Receive screen or paste the address. We recommend depositing at least $5.00 worth of BTC to unlock the full potential of the wallet. 

### Step 4 - Congrats!

You just made your first deposit. Samourai will notify you once it sees the transaction on the network. This is usually nearly instant. You will see your balance update and your first transaction appear in the transaction list!

## Get your latest bitcoin address

Your Samourai Wallet contains an infinite amount of bitcoin addresses that get used once and then are archived. These archived addresses are never discarded, but they are not used again to protect your privacy and security.

This means your bitcoin address will change all the time, this is not cause for alarm, consider your addresses as *'one-time-use'* and disposable. 

### Step 1 - The receive screen

To get your latest bitcoin address press the blue '+' button on the bottom right of the main balance and transactions screen. Then press the green 'Receive' button to open the Receive screen.

### Step 2 - Share your address

Samourai Wallet will display a QR code of your latest bitcoin address. Underneath the QR code is the actual address text. You can show this QR code if you are in transacting in person or you can share the address and QR code manually.

* **To copy the address to clipboard:** Tap the address text and press 'YES'
* **To share the QR code image:** Tap the share icon in the toolbar

By default, your Samourai wallet will generate bech32 or "Native Segwit" addresses (beginning with 'bc1'). If you are sending to your Samourai wallet using a service that does not yet support Native Segwit addresses, you can also obtain addresses in Segwit Compatibility and Legacy formats, via the **Advanced** gearbox icon on the Receive screen. 

## Sweep a private key

Advanced users can sweep any existing valid bitcoin private key into Samourai Wallet. This will send the balance of the single private key to an address in Samourai Wallet. 

### Step 1 - Prepare the private key

Samourai Wallet will scan a QR code representing the private key. Make sure the private key is accessible and ready to be scanned by Samourai Wallet

### Step 2 - Open the sweep camera

Tap the **three vertical dots** on the top right of the toolbar and then tap **Sweep Private Key**. This will launch the sweep camera. Focus the camera onto the QR code of the private key until it scans.

*If the private key is encrypted with a password you will be prompted to enter the password before being able to continue.*

### Step 3 - Confirm the sweep

You will be prompted to confirm the sweep. The entire balance of the private key will be sent to your latest bitcoin address minus the miner fee. 

**Note: Samourai will not save the private key on the device. You should discard the used private key after sweeping as it is no longer advisable to use again.**



