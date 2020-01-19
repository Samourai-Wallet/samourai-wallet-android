# Importing Existing Wallet to Dojo

As of Dojo v1.4.1 importing of previous wallets into your Dojo is supported. To import a wallet you should start by ensuring you know your pin then testing the backup password, to ensure you remember. Then test the encrypted backup decryption. You can look at your wallet and find your earliest transaction to see what block it was mined in, keep this in mind for when you do rescan.

**Keep in mind if your importing a wallet that has been on SW servers, your Xpubs have been exposed to those servers**

1. Tap the **three vertical dots** in the upper right corner of main wallet screen 

2. Settings

3. Troubleshooting

4. Passphrase/backup test

5. Check your passphrase and test wallet backup decryption 

Now that you've ensured the backup decryption is good and you remember the passphrase. You must secure erase the wallet.

1. Tap the **three vertical dots** in the upper right corner of main wallet screen 

2. Settings

3. Wallet

4. Secure erase wallet

**Your encrypted backup will not be erased!**

Then you will be taken back to the wallet creation screen, on the bottom it will show the wallet has found a backup.

1. Enable Tor 

2. Tap the **three vertical dots** in the upper right corner

3. Connect to existing Dojo 

4. Restore wallet from backup

For the restore you will need to input passphrase and pin. Your wallet will show a balance of 0 BTC, **do not be alarmed**. Now that your wallet is recovered and connected to your Dojo you must do a rescan of the blocks via maintence tool

1. Access Dojo maintence tool

2. Go to block rescan tab
 * Preferably find a block that you know is just before the wallet was created, to cut down rescan time

3. Input from chosen block to tip of chain

4. Begin rescan

**Tracker logs should look like this:**

<img src="https://github.com/Crazyk031/samourai-wallet-android/blob/develop/Guides/Images/Rescan%20Tracker%20logs.jpg" width="500" height="700" /> 



If you get an error on rescan don't be alarmed, check the tracker logs on Dojo to ensure its rescanning. **Do not start another rescan** this will slow the rescan down. If you do, Stop/Start Dojo and pickup the rescan from where earliest rescan left off. On Rpi 4 rescan averages a block every 3 seconds, it will take time for the rescan to complete. Once its reached the tip of the chain your wallet will be ready to use, all behind your own Dojo! Keep in mind though, if it was created via SW servers your Xpubs have been exposed to the SW servers.

