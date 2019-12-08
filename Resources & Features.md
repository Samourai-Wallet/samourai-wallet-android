# Samourai Resources 

**Here are a few great resources to information about Samourai Wallet**

[Samourai Wallet Website](https://samouraiwallet.com/) 

[Samourai Wallet Telegram](https://t.me/SamouraiWallet)

[Samourai Wallet Blog](https://blog.samouraiwallet.com/) 

[Samourai Wallet Twitter](https://twitter.com/SamouraiWallet?s=09) 

[Samourai.kayako Documents](https://samourai.kayako.com/) 

# Features 

## STONEWALL 

### What is Boltzmann?

Boltzmann is a script that returns the entropy of a given transaction. This script measures the linkability of inputs to outputs of a given transaction, by determining the number of individual mappings of the inputs to outputs used in the transaction.


### What is a Boltzmann Score?

The higher the entropy of a transaction, the higher the Boltzmann score. The higher the Boltzmann score, the more resistant your transaction is to address/identity clustering techniques used by Blockchain analysis companies. This is because if the element of doubt connecting ownership of addresses by any entity is too great, it can pollute the entire 'cluster'.


To ensure higher entropy for our users transactions, Samourai have introduced STONEWALL as a send type that constructs your transactions in a way that consistently obtains a good Boltzmann score, wherever possible.

STONEWALL will activate by default for transactions in Samourai whenever the estimated Boltzmann score is high enough. If the estimated Boltzmann score for a given transaction is not high enough,  a STONEWALL will be unavailable for that transaction.

### What is STONEWALL? 

STONEWALL is a unique way of building transactions that increases the deniability of the link between sender and recipient of a transaction. A STONEWALL is designed to improve your privacy on the blockchain by introducing a large element of doubt and uncertainty into the datasets of blockchain analysis platforms.



STONEWALL is enabled by default and there is no extra charge for using it. It can be disabled in the transaction settings. 

## PayNym

### What are PayNyms?

PayNyms are a secure and private way of sending and receiving bitcoin using BIP47 Reusable Payment Codes. PayNyms allow you to add your friends to your wallet contact list for regular sending and receiving, without revealing your balance or transaction history. PayNyms are currently supported natively by Samourai Wallet.

BIP47  compatible wallets produce a special 'payment code' (beginning with **'PM8T'**) that never changes. This code can be shared publicly, and when scanned or added by a compatible wallet will generate unique unused bitcoin addreses between one another without revealing prior transaction or balance history.

A PayNym is created by taking a valid BIP47 code and applying a special hashing algorithm that produces a unique fingerprint used to create the PayNym Bot image and ID. A **PayNym Bot** is a visual representation of a valid BIP47 Reusable Payment Code.  

![Wei Dai](Wei%20Dai.png)

_Every PayNym Bot is unique._

### Your PayNym is stealthy.

Gone are the days of revealing your balance and transaction history to the entire world. Transactions are always valid bitcoin transaction that are sent and confirmed on the blockchain. A unique bitcoin address is automatically generated behind the scenes in your wallet, for every new transaction your PayNym handles. Addresses are never reused and are only known by the sender and receiver of the transaction.

### Your PayNym is yours.

PayNyms are controlled and managed using client side wallet software. Only the provable owner of the private keys can interact with their PayNym. Your PayNym isn't an intermediary, or third party service. Only your wallet posseses the private keys needed to control them.

### Understanding PayNym fees

 **How much does a PayNym connection cost?**

Samourai Wallet charges a one time fee of 0.00015 BTC + normal miner fee paid at time of connection. Once a PayNym connection is active, there are no further connection fees with that PayNym

The fee is automatically paid by your wallet as part of the connection transaction. You will be asked to review and accept the total fee before confirming the connection.

### Connecting to a PayNym contact

**Prerequisites:**

In order to create a connection you will need a small amount of bitcoin in your wallet to cover the connection fee.

You also need the Payment code you would like to connect to in either text or QR code form. This code always begins with **'PM8T'**. 

#### Step 1 - Open the PayNyms screen

From the main screen of the wallet press the blue '+' button located on the bottom right of the screen. Tap the purple PayNyms button.

#### Step 2 - New connection

On the PayNyms screen, tap the blue '+' button located on the bottom right of the screen. You will be taken to the "Add New PayNym" screen. Icons at the bottom of this screen allow you to select whether you want to scan the payment code from a QR code, or paste from the clipboard.

#### Step 3 - Add details

If the scan/paste was successful you will see a screen with the PayNym bot profile for the PayNym you have scanned. To connect to this contact, simply tap 'FOLLOW'.

A pop-up box will appear where you can edit/add a label for your PayNym. The PayNym's PayNymID will be used as the default label if no label is added. When you've confirmed these details, tap 'FOLLOW' in the pop-up box to proceed. 

#### Step 4 - Confirm connection

Before the connection is made you will be asked to review the connection fee via a pop-up message. Press "OK, FOLLOW" to accept the fee and broadcast the connection to the bitcoin network.

If you do not have enough bitcoin in your wallet, simply deposit more bitcoin, then return to the PayNym screen. You will see the payment code added to your list. Tap on it to resume the connection process.

#### Step 5 - Wait for blockchain confirmation

You must wait for 1 confirmation on the bitcoin blockchain before the connection is considered established. Once established you can easily send bitcoin to your new connection without asking for a bitcoin address.

### Claiming your PayNym Bot

Wallet users can claim their unique PayNym bot from within the latest version of Samourai Wallet. A PayNym bot is a unique graphical representation of your PayNym code.

Navigate to the PayNym screen from the main wallet screen 

1. (Tap the "+" Symbol>Tap "PayNyms")
2. Tap the NEW option in the PayNym toolbar. 
3. Tap the Claim My PayNymID button.


### Meet your new PayNym Bot

Once claimed, your uniquely generated PayNym Bot will be automatically named and displayed in your wallet and will listed on the public directory. Compatible wallet users will be able to add your Bot to their contact list to generate unique bitcoin addresses that are only known between your wallet and theirs.

## Batch Spending 

### What is a Batch Spend?

The Batch Spend feature allows users  to combine (or batch) multiple unrelated spends into one single transaction that gets broadcasted to the network. The benefit to creating these combined transactions is much lower overall fees and more efficient use of blockchain space. It has been estimated that users can **save up to 80%** bitcoin miner fees by using the new Batch Spend feature. 

**Is it for me?**

The batch spend feature is useful for anyone who needs to send bitcoin to more than one other party in a given time frame. For example, if I want to pay both Alice and Bob, it would make more sense for me to use Batch Spend and pay them both at the same time for 1 transaction fee instead of initiating two separate transactions and paying both transaction fees.

### How to make a Batch Spend

Making a Batch Spend is straightforward and much like making a normal send. The difference is instead of simply choosing one destination address and amount for a transaction, you are allowed to add as many additional destination addresses and amounts to your transaction before sending it to the bitcoin network. 

#### Navigate to the Send Screen

Simply tap the blue '+' button on the bottom right of the main screen. Then press the red **Send** button. This will open the Send Screen. 

#### Activate the Batch Spend screen

Tap the three vertical dots in the toolbar to open up the toolbar menu. Select **Batch Spend** from the list

#### Add your transactions

Scan an address using the QR code scanner in the toolbar and enter an amount to send to this address. When a valid amount and address is entered press the Add icon in the toolbar. This will add your transaction to the batch queue. Repeat this step as many times as needed. There is no limit to the amount of transactions you can add to a single batch. 

#### Send the batch transaction

When you are ready to broadcast the batch transaction to the network press the envelope icon in the toolbar. You will be asked to choose a fee profile for this send and then asked to confirm the send one last time. Once you press Yes the transaction will be sent to the network.


## STOWAWAY/CAHOOTS 

### Creating a Stowaway Transaction

**NOTE: These are temporary steps for power users who wish to help test the bleeding edge of privacy transactions within Samourai Wallet. This functionality is provided without a completed UI.**

Use Stowaway to create a transaction that looks like a "typical" bitcoin transaction but actually is a mini CoinJoin with an obfuscated amount sent on the blockchain. **You can only send a Stowaway transaction to the person you are collaborating with**, so this is a good transaction to use when sending to your privacy conscious friends who use Samourai Wallet.

-  **Step 1:** Open the Send Screen

-  **Step 2:** Type "stowaway" in the "To" text field

-  **Step 3:** Enter the amount you want to send
(in satoshi)

-  **Step 4:** A dialogue box will appear with a text blob. Press "Show QR"

- 	**Step 5:** Show the QR code to the person you are sending to. Have them scan it with their Samourai Wallet

- **Step 6:** Scan the QR code that the person you are sending to shows you with your Samourai Wallet

- **Step 7:** Repeat scanning QR codes between each other until you receive a dialog with a signed transaction hex. Press **Broadcast Transaction** to send the transaction.

- 	**Step 8:** The amount you specified will now be in the wallet of your collaborator.

- 	**Step 9:** If you review the transaction on the blockchain you will notice that the amount sent doesn't appear on the transaction. (0.00123456 BTC)

**Current Limitations**

1. Only zpub-derived bech32 utxos can be signed (until further notice)

2. Can only spend to segwit bech32 addresses (until further notice)

3. Enter spend amount in sats (available amount will be displayed at prompt)

4. Valid #Cahoots JSON blobs will be recognized by scan. Text entry (paste) can be done via Settings->Transactions->#Cahoots

**Alert: For maximum privacy you should only create Stowaway transactions with people you trust. You will be sharing details of some of your UTXOs during the creation of the Stowaway transaction.**

## STONEWALLx2

### How to create a STONEWALLx2 Transaction

**NOTE: These are temporary steps for power users who wish to help test the bleeding edge of privacy transactions within Samourai Wallet. This functionality is provided without a completed UI.**

Use STONEWALLx2 to create high entropy mini CoinJoin transaction with the help of a privacy conscious friend who allows you to mix some of their UTXOs with your own transaction. **You can send a STONEWALLx2  transaction to any third party**, so this is good to use when sending to any person to service regardless of what wallet they use.
 

- **Step 1:** Open the Send Screen

- **Step 2:** Type "STONEWALLx2" in the "To" text field

- **Step 3:** Enter the amount you want to send
(in satoshi)

- **Step 4:** Enter the address you want to send to
(bech32 addresses only)

- **Step 5:** A dialogue box will appear with a text blob. Press "Show QR"

- **Step 6:** Show the QR code to the person participating in the STONEWALLx2. Have them scan it with their Samourai Wallet

- **Step 7:** Scan the QR code that the person you are participating with shows you with your Samourai Wallet

- **Step 8:** Repeat scanning QR codes between each other until you receive a dialog with a signed transaction hex. Press Broadcast Transaction to send the transaction.

**Current Limitations**

1. Only zpub-derived bech32 utxos can be signed (until further notice)

2. Can only spend to segwit bech32 addresses (until further notice)

3. Enter spend amount in sats (available amount will be displayed at prompt)

4. Valid #Cahoots JSON blobs will be recognized by scan. Text entry (paste) can be done via Settings->Transactions->#Cahoots

**Alert: For maximum privacy you should only create Stowaway transactions with people you trust. You will be sharing details of some of your UTXOs during the creation of the Stowaway transaction.**

## Dojo

### What is Dojo?

Dojo is the backend that your wallet connects to, this is the link between your mobile wallet and the blockchain. Samourai Wallet always connects to the Dojo backend, default to the Samourai servers. With Dojo being open source it allows you to run your own Dojo, to experience maximum privacy & financial self sovereignty! 

### Dojo Resources 

[Dojo Telegram Group](https://t.me/samourai_dojo) 

[Dojo Github](https://github.com/Samourai-Wallet/samourai-dojo) 

[Dojo Blog Article](https://blog.samouraiwallet.com/post/185312260292/introducing-samourai-dojo-10-open-source-and) 

For a very user friendly implimitation of Dojo you can check out Ronin

[Ronin Github](https://github.com/RoninDojo/RoninDojo) 

## Whirlpool 

### What is Whirlpool? 

Whirlpool is Samourai Wallets implimitation of a CoinJoin. 

### Whirlpool Resources 

[Whirlpool Telegram Group](https://t.me/whirlpool_trollbox)

[Whirlpool Medium Article](https://medium.com/samourai-wallet/diving-head-first-into-whirlpool-anonymity-sets-4156a54b0bc7) 

[Whirlpool Github](https://github.com/Samourai-Wallet/Whirlpool) 

[Whirlpool Blog Article](https://blog.samouraiwallet.com/post/186458671552/a-holistic-approach-to-coinjoin?is_related_post=1) 

## Sentinel 

### What is Sentinel? 

Sentinal is a watch only wallet that allows you to watch or deposit to wallets and Bitcoin adresses. Sentinel allows you to watch or deposit to a single Bitcoin address or using the XPUB(BIP44), YPUB(BIP49) or ZPUB(BIP84) of a wallet to watch and deposit to a new address every time!

Sentinel App [Google Play store](https://play.google.com/store/apps/details?id=com.samourai.sentinel) 

[Sentinel Github](https://github.com/Samourai-Wallet/sentinel-android) 

You can also try a forked version of Sentinal called Sentinel x

[Sentinel x Github](https://github.com/InvertedX/sentinelx) 

**_The below features have been removed from the current playstore download due to restrictive new Google Play store policies_**

[Blog Article about Removal](https://blog.samouraiwallet.com/post/181821635197/temporarily-disabling-stealth-mode-remote-sms) 

[Remote SMS Commands](https://samourai.kayako.com/section/34-remote-sms-commands) 

[Stealth Mode](https://samourai.kayako.com/section/33-stealth-mode) 





