# Table of Contents
- [**What is Whirlpool?**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#What-is-Whirlpool?)
- [**Setting up Whirlpool**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Setting-up-Whirlpool)
- [**Understanding Whirlpool Desktop Configuration Options**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Understanding-Whirlpool-Desktop-Configuration-Options)
- [**Understanding Deposit, Premix, and Postmix accounts**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Understanding-Deposit,-Premix,-and-Postmix-accounts)
- [**Fund your deposit account on Whirlpool Desktop**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Fund-your-deposit-account-on-Whirlpool-Desktop)
- [**Understanding pools and pool fees**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Understanding-pools-and-pool-fees)
- [**Adding your UTXOs to a pool with Whirlpool Desktop**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Adding-your-UTXOs-to-a-pool-with-Whirlpool-Desktop)
- [**Add a discount code SCODE to Whirlpool**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Add-a-discount-code-SCODE-to-Whirlpool)
- [**Privacy considerations when Spending cycled UTXOs**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Whirlpool.md#Privacy-considerations-when-Spending-cycled-UTXOs)

# What is Whirlpool? 

Whirlpool is Samourai Wallet's implementation of CoinJoin, a true CoinJoin that breaks determenalistic links. 

[Whirlpool Github](https://github.com/Samourai-Wallet/Whirlpool/tree/master)

[Whirlpool Telegram Group](https://t.me/whirlpool_trollbox)

**You are always in control of your UTXO's as they never leave your wallet during the whole mixing process!**

Whirlpool uses a few key steps to break determenalistic links

1. To be put into a Whirlpool mix you must Tx0
* This takes your deposit and breaks it into like size UTXOs depending on your selected pool(**.5**/**.05**/**.01**)
* **This will produce toxic change**, we will handle this later
* This is critical when it comes to mixing, if all the UTXO sizes don't match it is possible to distinguish inputs & outputs of the user's in the CoinJoin
* This prevents sybil attacks, making it expensive to conduct such an attack. An attacker would also have to run multiple clients, as only one client per mix is allowed.
2. During Tx0 is when you pay the mixing fee
* It is critical this fee not be included in the mix as it can provide a determenalistic link
3.  Whirlpool incentivizes remixing
* Premixers are the ones who pay for the miner fees to make the mix
* Once you've mixed you can freeride to provide liquidity as well as increase the anonset/entropy for yourself & participents in mixes you've taken part in

# Setting up Whirlpool

Whirlpool can be set up in 3 configurations:

1. **Standalone GUI**
* This requires OpenJDK 8+ to be installed on the machine
* Your only mixing when the GUI is open and active, if your computer sleeps your not mixing 

2. **Standalone CLI**
* 24/7 mixing as long as the device is on (ex. Pi4 or Odroid N2) and Whirlpool is active
* Not user friendly for those that arn't comfortable with command line

3. **CLI backed GUI**
* 24/7 mixing
* User friendly 
* GUI machine doesn't require OpenJDK

### A CLI backed GUI is by far the best implementation and provides the easiest user experience

- First you must setup your CLI backend([Whirlpool CLI Github](https://github.com/Samourai-Wallet/whirlpool-client-cli)), this can be done manually on Command Line or via a user friendly implementation like:

- [RoninDojo Wiki](https://github.com/RoninDojo/RoninDojo/wiki)

[RoninDojo Telegram Group](https://t.me/RoninDojoUI)

- [MyNode Github](https://github.com/mynodebtc/mynode)

[MyNode Telegram Group](https://t.me/mynode_btc) 

###  How to install CLI & pair to GUI

For our example we will use RoninDojo

_This guide was written by [BTCxZelko](https://twitter.com/BTCxZelko?s=09)_

1. Install Whirlpool via Ronin UI 

2. Paste Pairing Payload from your Mobile Device

   a. Hit the 3 dots top right

   b. Settings -> Transactions

   c. Select Pair to Whirlpool GUI and copy the payload and send to your main computer

   d. Paste this payload into the command line when prompted. _Use Ctrl+Shift+V_ 

3. Copy the APIkey when prompted. You will need it. 

4. Switch to your Main Device 

5. Head to Samourai Wallet Whirlpool Repo [Release page](https://github.com/Samourai-Wallet/whirlpool-gui/releases/)
6. Download the appropriate download file 
   
   a. For Linux based machines you can do the following from terminal:

   b. ```wget https://github.com/Samourai-Wallet/whirlpool-gui/releases/download/0.10.0/whirlpool-gui.0.10.0.AppImage```
   
   c. ```chmod +x whirlpool-gui.0.10.0.AppImage```
   
   d. ```./whirlpool-gui.0.10.0.AppImage```

7. Select 'Connect to remote CLI' 
8. In the first block insert the following with your Ronin Device's IP:
 
   a. ```https://192.168.X.XXX```

9. Keep the Port 8899 and paste your API pairing key from your RoninDojo (use Ctl+V)
10. Click 'Connect'
11. Enter your Samourai Wallet passphrase
12. You all set and ready to mix!

## Understanding Whirlpool Desktop Configuration Options

When you have paired your wallet to whirlpool you should stop the client (by pressing **STOP** in the top right) and familiarise yourself with the available configuration options. While the default configuration is ready for use immediately, it is worth understanding the client as you use Whirlpool.

**General Configuration**

Config Name | Description | Notes
----|----|----
 `Default Mix Target` | The default number of cycles to complete for new UTXOs added to the pool. | It is safe to leave this as 1, as you can always manually choose to cycle UTXOs again manually.
`Auto Mix`|When enabled this will automatically queue your UTXOs for cycling.| It is safe to turn this off if you prefer to micro manage which UTXOs to cycle. If you have a lot of UTXO's it can be faster to disable this.
`Tor`| When enabled all traffic is routed through the built in Tor layer.|This is enabled by default & should always been enabled for privacy protections.
`Proxy`  |Connect through SOCKS/HTTP proxy.   |This is intended for advanced users who wish their own Tor or VPN layer for networking. 


### Developers Settings

Toggle view of these settings by pressing **Toggle developers settings**

Config Name  |  Description | Notes
----  |  ---- | ----
`Server`  |  Select the Whirlpool server you wish to interact with. | MainNet is default, but you may also wish to use TestNet
 `Client Delay` |  Delay in seconds between each client connection. | Leaving this as default is good to help prevent flooding the server.
  `TX0 Max Outputs`|   This determines the number of new mixed UTXOs that will be created.| Leave this at 0 to mix the entire UTXO
 `SCODE` | A discount code for reduced cost mixing.   | This should be blank by default

## Understanding Deposit, Premix, and Postmix accounts

Whirlpool is completely non custodial. The term "account" refers to an address space covered by your recovery words and passphrase. Within Whirlpool there are two areas of this address space that are segregated from the rest of the wallet to preserve privacy. 

### Deposit

This account consists of all the bech32 UTXOs within your standard Samourai Wallet. UTXOs in this account have not been cycled through Whirlpool yet. You can receive deposits directly into this account or select UTXOs from this account to cycle.

### Premix

This account consists of all the UTXOs that are prepared to cycle but are still pending. UTXOs will remain in Premix until they have one confirmation and they are selected in a Whirlpool cycle. These UTXOs cannot be spent yet but will have priority in any Whirlpool cycle over UTXOs in the Postmix Account.

### Postmix

This account consists of all the UTXOs that have completed at least one cycle. These UTXOs are available for spending from within Samourai Wallet or available to cycle again for greater privacy.

## Fund your deposit account on Whirlpool Desktop

Now that you have configured the desktop client to your liking the next step is to choose or add coins to begin cycling in Whirlpool. The desktop client will detect bech32 addresses within your Samourai Wallet, these UTXOs can be selected or you can generate a new address to deposit coins from an external wallet.

### Deposit from an external wallet:

If you do not have any UTXOs in your Samourai Wallet, you can generate a deposit address for your wallet by pressing the red `+ Deposit` button within the desktop client. Once the deposited funds have received one confirmation they will be ready to be cycled.


### Choose UTXO(s) from your Samourai Wallet:

Within the Desktop Client navigate to the **Deposit** tab directly under Last Activity on the left hand side of the screen. The Deposit account contains all the bech32 UTXOs in your paired Samourai Wallet. Once the deposit tab has been selected a list of your bech32 UTXOs in Samourai Wallet will be displayed. 

## Understanding pools and pool fees

Whirlpool is different than other CoinJoin services, in that you do not pay a volume based fee per anonymity set, but instead you pay a one time flat fee for an unlimited anonymity set. You do pay a miner fee for each UTXO created after Tx0.

You can message [Whirlbot](https://t.me/SW_whirlpool_bot) on Telegram to calculate fees and see pool stats

To put it simply, it costs the same in Pool Fees to cycle 1 BTC or 1000 BTC. Once the pool fee is paid, it costs nothing to continue cycling. With each cycle you gain a greater privacy advantage with a deeper anonymity set.

Current Pools


Pool Denomination  |  Pool Fee | Current Status
----  | ----  | ----
  0.01 BTC|  0.0005 BTC | OPEN
0.05 BTC  |0.0025 BTC   | OPEN
0.5 BTC  |0.025 BTC   | OPEN

### Choosing the right pool

The denomination of the pool determines the minimum amount you can cycle through that pool and the resulting denomination of the newly cycled UTXOs in your wallet. Generally the larger the UTXO you are cycling, the larger the pool you want to choose. 
For example: 

- Cycling 1 BTC in the 0.01 Pool would create 100 outputs of 0.01 BTC

- Cycling 1 BTC in the 0.05 Pool would create 20 outputs of 0.05 BTC

- Cycling 1 BTC in the 0.5 Pool would create 2 outputs of 0.5 BTC

## Adding your UTXOs to a pool with Whirlpool Desktop

Samourai offers multiple liquidity pools depending on the size of the UTXO you wish to cycle through Whirlpool. Once you have funded your deposit account and the transaction has confirmed, it is time to add your UTXO to a pool.

### Add your UTXOs to a pool:

With the **Deposit** tab selected, you will see a list of bech32 UTXOs available on the right hand side of the screen

- Step 1

Press the red `Tx0` button on the far right side of a confirmed UTXO to open the pool selection screen

- Step 2

Select a miner fee profile, the higher you pay in miner fees the quicker your UTXOs will confirm, and the quicker your cycle will complete.

- Step 3

Select a pool, you generally want to choose the largest pool you can based on the amount of the UTXO you are cycling.

- Step 4

Select a target number of cycle, this can be left as 1 as you can later cycle these UTXOs manually. If you set a number higher than 1 the UTXOs will be cycled automatically until they reach the target.

- Step 5

Press the red `Tx0` button on the bottom of the pop up window to begin the process. 

## Add a discount code SCODE to Whirlpool

Occasionally we will announce promotional SCODE's - or Samourai Discount Codes - that you can apply to your Whirlpool client for reduced price or even free mixes. Keep an eye on our Twitter account and our Telegram rooms to be the first to hear about new SCODE's

### How to add a SCODE

Once you have a valid SCODE you can add it to your Whirlpool client

* Open your Whirlpool client and enter your passphrase to authenticate.

* Navigate to **Configuration**

* Press **Toggle Developer Settings**

* Enter your SCODE in the text field that says "SCODE"

* Press **Save**

* Restart Whirlpool


Whirlpool will now apply the SCODE discount when you make new Whirlpool Tx0 transactions. 

### How to remove a SCODE

Once an SCODE expires you should remove it from your configuration. This currently must be done manually, and it is important you do not forget to disable the SCODE once it expires. 

* Open your Whirlpool client and enter your passphrase to authenticate.

* Navigate to **Configuration**

* Press **Toggle Developer Settings**

* Remove the text field that says "SCODE"

* Press **Save**

* Restart Whirlpool


## Privacy considerations when Spending cycled UTXOs

**Toxic Change**

Toxic change is just as its name implies, it is toxic to your privacy! The change created during a Tx0 or from post mix spends will be toxic, using this change can link back to those previous UTXOs. Many people have different ways to handle this change, but since it is such a nuanced topic **DYOR!**

Whirlpool has been designed with extensive privacy enhancing strategies that help make sure you do not undo the privacy gains of Whirlpool when you go to spend your UTXOs.

It is advised that you keep your funds within Whirlpool and directly spend using your Samourai Wallet on an as needed basis. This will allow you to take advantage of our superior coin selection and post mix spending tools, making it very hard to accidentally undo the privacy gains of Whirlpool.

### Common Scenario:

I want to send my cycled UTXOs to another wallet/cold storage device

If you must transfer your UTXOs to another wallet/device then please follow the guidelines below:

**DO NOT:**

* Spend the entire balance in Whirlpool Postmix to a single address on your wallet/cold storage device. This is known as merging inputs and completely undoes the privacy gains provided by Whirlpool. 

**DO:**

* Send less than half of your Whirlpool Postmix balance to your wallet/cold storage device. This is usually enough to trigger a STONEWALL which will provide a strong amount of entropy within the transaction.

* If you need to transfer the entire balance then you should do so on a UTXO by UTXO basis. Using the Address Calculator in your Samourai Wallet generate the private keys for each of the PostMix UTXOs that you want to transfer and sweep them with your other wallet/device or using Sentinel. You should ideally sweep each UTXO spaced apart by a random interval of time to avoid time correlation de-anonymization. 

### Common Scenario:

I want to spend my coins at a third party service. Navigate to the PostMix Send Screen in your Samourai Wallet and create a transaction as you normally would. If it is possible your wallet will enable STONEWALL automatically - this will provide you with additional entropy and is considered the minimum requirement for sending from your PostMix balance with minimal privacy loss. 

If a STONEWALL cannot be created your wallet will warn you before sending. Please follow the guidelines below if you are unable to activate STONEWALL.

**DO NOT:**

* Ignore the warning. If STONEWALL cannot be activated, it means you are going to degrade your privacy and the privacy of your counterparts involved in the cycle. 

**DO:**

* Spend a smaller amount. STONEWALL activates when spending less than half of your available balance.

* Add more UTXOs to your Whirlpool wallet so that the amount you need to spend is less than half of your available balance.

* Enlist the help of a friend to create a STONEWALLx2 transaction. This is very much like a normal STONEWALL but you are using a trusted friends UTXOs in addition to your own. This has the added benefit of additional mixing within the spend transaction and you can spend more than half of your available balance. 

### Common Scenario:

I want to spend my coins to another Samourai Wallet user

Sending to another Samourai Wallet user is the same as sending to any bitcoin address and the same privacy rules apply. However, some additional Post Mix spending tools are available only between Samourai Wallet users. An example of this is Stowaway, which is a CoinJoin with a trusted friend that doesn't look like a CoinJoin and masks the true amount spent on the blockchain. 
