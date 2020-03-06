# Table of Contents
- [**Restoring your wallet from auto backup**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Restore%20%26%20Recovery.md#Restoring-your-wallet-from-auto-backup)
- [**Restoring your wallet with your secret words**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Restore%20%26%20Recovery.md#Restoring-your-wallet-with-your-secret-words)
- [**Testing your current wallet passphrase and backup**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Restore%20%26%20Recovery.md#Testing-your-current-wallet-passphrase-and-backup)
- [**Exporting Wallet**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Restore%20%26%20Recovery.md#Exporting-Wallet)
- [**Forgotten Details**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Restore%20%26%20Recovery.md#Forgotten-Details)
- [**Private Keys**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Restore%20%26%20Recovery.md#Private-Keys)

# Restoring your wallet

**Derivation Paths:**

**Deposit:** `m/44'|49'|84'|47'/0'/0'`

**Bad Bank:** `m/84'/0'/2147483644'`

**Pre Mix:** `m/84'/0'/2147483645'`

**Post Mix:** `m/84'/0'/2147483646'`

**Ricochet:** `m/44'|49'|84'/0'/2147483647'`

For more information on wallet derivation paths check [WalletsRecovery.org](https://walletsrecovery.org/)

## Restoring your wallet from auto backup

[Video Tutorial](https://www.youtube.com/watch?v=Fqpix_h1oEU&list=PLIBmWVGQhizJ-mgDIWO5I5OcJlXc3apTF&index=4&t=0s)

By default Samourai Wallet saves an encrypted backup of your entire wallet on your device. This allows you to easily restore your wallet from the latest auto backup in case you accidentally uninstall Samourai wallet.

### Step 1 - Install

If Samourai was previously installed but now it isn't, simply install Samourai Wallet from the [Google Play store](https://play.google.com/store/apps/details?id=com.samourai.wallet) or [Gitlab APK](https://code.samourai.io/wallet/samourai-wallet/tree/master/apk) onto your Android device. 

### Step 2 - Restore Wallet

Once Samourai is installed onto your device launch the app. If there is an existing backup on the device it will automatically be detected and an option to restore it will be displayed on the bottom of the screen. Simply tap **RESTORE** then input your wallet passphrase that you chose when you created the wallet.

If there is no existing backup on the device, you can still restore using the secret words.

If the backup is stored in another area of the device, or externally, tap the three vertical dots in the top right corner and select **Import Samourai backup**. There will then be the option to *"CHOOSE FILE"* or *"PASTE"* depending on the location of the backup.

### Step 3 - Enter passphrase

Enter the passphrase of the wallet you want to restore. 

### Step 4 - Enter PIN

If the wallet decrypted successfully you will be asked to enter the existing PIN code of the wallet. This is the PIN code you previously created. 

If you received an error instead of the PIN screen, please check your passphrase and try again. The passphrase must be exact and may include white space and special characters. 

### Step 5 - Wallet Restored

Your wallet should now be fully restored with the expected balance, transaction history, payment codes, and settings all retained. 

**Note: If the auto backup process fails or you return unexpected results. Please try restoring your wallet with your secret words.**

## Restoring your wallet with your secret words

[Video Tutorial](https://www.youtube.com/watch?v=MvcPud3-2ng&list=PLIBmWVGQhizJ-mgDIWO5I5OcJlXc3apTF&index=3&t=0s)

Your secret words are extremely important. When you combine your passphrase with your 12 secret words you get the *"backup of last resort"*. This combination is an industry standard and can be considered a valid backup for almost all modern bitcoin wallets. 

**NOTE: We highly recommend first trying to restore by automatic backup before following this guide.**

### Step 1 - Install Samourai

Install Samourai Wallet from the [Google Play store](https://play.google.com/store/apps/details?id=com.samourai.wallet) or [Gitlab APK](https://code.samourai.io/wallet/samourai-wallet/tree/master/apk) onto your Android device. 

### Step 2 - Restore wallet

Once Samourai is installed onto your device launch the app and tap the three vertical dots in the top right corner then select **Import external wallet** button. 

### Step 3 - Enter secret words (mnemonic)

In the box under *"Mnemonic"* enter your wallet's secret words. This will be 12 words.

Below the box tap *"ADD BIP39 PASSWORD"* to enter your wallet passphrase that you chose when you first created the wallet.

### Step 4 - Create PIN code

You will now be asked to create and confirm a PIN code between 5 and 8 digits long.

If you didn't get to the PIN code creation screen then there is something wrong with your secret words. Please double check the spelling and order of the words and try again.

### Step 5 - Verify

A successful restore will display the expected balance of your wallet and a complete transaction history. 

If the restore is unsuccessful a 0 balance and empty history would be displayed. 

In the case of an unsuccessful restore try the above steps again taking care to make sure the passphrase is correct.

# Testing

## Testing your current wallet passphrase and backup

Samourai Wallet has included some debugging tools in the Settings menu. You can use these tools to test your passphrase and the status of your current backup file. It is recommended to run this diagnostic periodically. 

### Step 1 - Open the Troubleshoot menu

Tap the **three vertical dots** on the top right of the toolbar and then tap **Settings**. This will launch the main settings screen. Tap on **Troubleshoot** in the list.

### Step 2 - Start the passphrase test

Tap the **Passphrase/backup test** list item to begin the test. Samourai will check the passphrase you supplied to make sure it corresponds to the wallet you are currently using. This is risk free and will not lock you out if you get the passphrase wrong. 

### Step 3 - Enter your passphrase

You will be asked to enter your passphrase. Upon success you will be notified and prompted to proceed to the next test. 

If the test fails you should try again taking care to make sure the passphrase is entered exactly as when you first created the wallet including any special characters or white space. 

### Step 4 - Start the backup test

Press **OK** to start the backup test. This will simply double check that the latest auto backup on the device is valid with the passphrase you provided. 

If the test fails please contact support. It should not fail if the previous passphrase test passed. 

# Exporting Wallet

## Manually export wallet backup

In addition to automatically backing up your wallet you can choose to manually export an encrypted backup of your wallet at any time.

### Step 1 - Initiate export

Tap the **three dots** on the top right of the Samourai toolbar to access the extra menu and tap **Export wallet backup**. 

### Step 2 - Choose Export Options

You will be asked to choose where you would like to export the wallet backup. 

- Choose **Export to clipboard** if you would like to copy the backup and paste it elsewhere. 

- Choose **Export to email** if you would like Samourai to open your default mail application and paste the backup into a new message for you. 

# Forgotten Details

[Video Tutorial](https://www.youtube.com/watch?v=XDMYIFavUiw&list=PLIBmWVGQhizJ-mgDIWO5I5OcJlXc3apTF&index=5&t=0s)

## Forgotten Passphrase

A forgotten passphrase is very serious as your passphrase is directly tied to your secret private keys that allow access to your funds. 

### If you have access to your wallet:

If you currently have access to your wallet via PIN code you should immediately send your coins out of the wallet into another wallet you control. **It is not safe to keep your bitcoin in Samourai if you do not know your passphrase.**

Once your bitcoins are in another wallet that you control, you may erase Samourai wallet and create a new wallet. When you create a new wallet you will need to define a new passphrase and write down new secret words. 

### If you do not have access to your wallet:

Samourai does not store your passphrase for you, as such it cannot reset lost or forgotten passphrases.  If you can no longer authenticate into your Samourai wallet and you do not have your passphrase follow the directions below to help yourself regain access to your wallet:

**1. Don't create a new wallet**

Samourai Wallet creates an auto backup of the last wallet on the device. If you create a new wallet you will erase the existing auto backup file of your wallet.

**2. Locate your auto backup file**

Samourai Wallet stores your encrypted auto backup file on the device file-system. Follow the directions below to locate your backup. Once located, open and copy the entire contents of the file to your clipboard.
 * Open Android Settings
 * Tap **Storage**
 * Scroll down and tap **Explore**
 * Locate and tap **Download**
 * Locate and open **Samourai.txt**

**3. Install the Samourai Backup Reader app**

We have created a Troubleshooting app that you can download from Github called [Samourai Backup Reader](https://github.com/Samourai-Wallet/Backup-Reader/releases/tag/1.1) - Install the APK on your device with Samourai

**4. Run Samourai Backup Reader**

Launch the Samourai Backup Reader APK. You will be asked to enter a passphrase. Enter the passphrase you think is  correct for your wallet and press **OK**. Next, paste the contents of samourai.txt into the text box and press **OK**

**5. Keep trying**

The Backup Reader app will attempt to decrypt the backup with the passphrase you specified. If the passphrase is incorrect you will see an error. We advise you to keep trying common variations of the passwords/passphrases that you frequently use until you no longer get an error.  Be aware that white-space and special characters are considered valid.

## Forgotten Secret Words

If you forget your 12 secret words you can easily review them again from within the Samourai Wallet.

### Step 1 - Open Settings

Navigate to the settings by tapping the three dots on the top right of the Samourai toolbar to access the extra menu and then tapping **Settings**. 

Once in the settings screen tap on **Wallet**

### Step 2 - Show mnemonic

Your secret words are also known as a mnemonic. Tap on **Show Mnemonic**

Your secret words will be displayed to you. Write them down and keep them safe and secure.

***

If you have forgotten your secret words and you no longer have access to your wallet you may be able to regain access using the Samourai Backup feature.

## Forgotten PIN Code

After three unsuccessful PIN code attempts you will be asked to enter your wallet passphrase. If the passphrase is correct you will be presented with a PIN reminder. You may then use your PIN code to enter your wallet as normal. 

# Private Keys

## Reveal all individual private keys in the wallet

The purpose of this article is to explain how to derive individual private keys for every bitcoin address in your Samourai Wallet, using a Mnemonic Code Converter tool. This tool, when properly configured, will reveal the private keys for every single address in your Samourai Wallet.

When you first create a wallet you are required to write down your secret words, which is a list of 12 random words in a specific order. To reveal your private keys you need your 12 secret words, and your passphrase.

### Step 1

Download the Mnemonic Code Converter tool linked at the bottom of this article to your computer. You may want to disconnect your internet connection while working with your list of private keys.

* Download the Mnemonic Code Converter.
* Double-click *“bip39-standalone.html”*  to open the converter tool in your default web browser. This tool runs locally and does not interface with the internet, so it is safe to completely disconnect from the internet before continuing.

### Step 2

* Find the text box labeled *“Wallet Secret Words”* and enter each of your 12 secret words separated by a single space.

* In the second text box labeled *“Wallet Passphrase”*, enter your Samourai Wallet passphrase. You created this passphrase when you first created your wallet and it is **required** to successfully generate your private keys. Make sure you enter the passphrase exactly as when you created it, including any whitespace and special characters.
 
### Step 3

* In the next section labeled *“Derivation Path”*, select the type of private keys you would like to reveal. Select BIP44 (Legacy) for addresses beginning with a **‘1’**, BIP49 (Segwit Compatible) for your segwit compatible addresses beginning with a **‘3’** or BIP84 (Segwit Native) for your segwit native adresses beginning with **'bc1'**.

* If you would like to reveal your private keys for addresses on the *internal chain* (also known as change addresses) then input **‘1’** into the text box labeled *“External/Internal”*.

* If you would like to reveal your private keys for addresses on the *external chain* (addresses for receiving payments) then input **‘0’** into the text box labeled *“External/Internal”*.

### Step 4

Scroll down to *“Derived Addresses”*, to reveal the private keys for every address in your wallet that corresponds to the Derivation Path you set in Step 3.

The *“Derived Addresses”* table contains the first 20 Addresses together with their public and private keys. You can reveal additional rows of addresses by adding to the text input labeled *‘Show More Rows’* at the bottom of the table. For example, if you wanted to view 200 results you would enter 200 in this text box.

Additionally, you may use these instructions to access private keys from within your Samourai Wallet [UTXO Management](https://samourai.kayako.com/section/18-utxo-management) if this mnemonic address generation tool did not suit you.

**bip39-standalone.zip** 1200 KB [Download](https://samourai.kayako.com/api/v1/articles/46/attachments/601/download)

