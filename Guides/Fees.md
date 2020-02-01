# Table of Contents
- [**Miner Fee's**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Fees.md#Miner-Fee's)
- [**Increasing the Miner Fee/RBF**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Fees.md#Increasing-the-miner-fee/RBF)
- [**Premium Transaction Fee's**](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/develop/Guides/Fees.md#Premium-Transaction-Fee's)

## Miner Fee's

Miner fees are paid by the sender to incentivize the Bitcoin miners to include transactions in the blockchain

### Choosing the right miner fee profile

Samourai Wallet provides users with dynamic fee profiles based on current network conditions. From the send screen you are able to modify the fee used on a per-transaction basis using the fee slider. The fee slider can be adjusted based on current mempool activity to select how quickly you need the transaction to be added to a block. In Bitcoin a block is usually produced roughly every 10 minutes.

**Low Fee Profile**

This profile aims to have your transaction confirmed within the next 1-24 blocks

**Normal Fee Profile**

This profile aims to have your transaction confirmed within the next 1-3 blocks

**Urgent Fee Profile**

This profile aims to have your transaction confirmed in the next 1-2 blocks.

**Slide to choose your preferred fee profile.**
 
By default Samourai Wallet transactions are set to follow a Normal fee policy.  On the Send Screen, you can fine-tune and adjust your fee using the fee slider. 

You will be prompted to review the miner fee once you press the green send button. 

### Increasing the miner fee/RBF

**RBF=Replace-by-Fee**

Samourai is one of the only Bitcoin wallets that allows you to top-up the miner fee of transactions even after you have already sent/received them. This can be useful if a transaction was made with a low custom fee resulting in a very long confirmation time. 

**Step 1 - Tap the unconfirmed transaction**

To top up the transaction fee of the sent/received transaction simply **tap the unconfirmed transaction** in the Main screen in Samourai. You will be taken to the Transaction Details screen. 

**Step 2 - Increase Fee**

On the bottom of the Transaction Details screen, tap the button labeled  **Boost Transaction Fee** to begin the process. You will be asked to confirm the new miner fee before the boost is attempted. 

**Step 3 - Wait for confirmation**

Once you have sent the boost you will see a small transaction leave your wallet, this is the boost transaction. Both this boost transaction and your original stuck transaction should confirm within the next block.

## Premium Transaction Fee's 

### How much does a payment code connection cost?

Samourai Wallet charges a one time fee of 0.00015 BTC + normal miner fee paid at time of connection. Once a connection is active, there are no further connection fees with that payment code. 

The fee is automatically paid by your wallet as part of the Connection transaction. You will be asked to review and accept the total fee before confirming the connection. 

### How much does Ricochet cost?

Samourai Wallet charges 0.002 BTC + normal miner fees for every 4 hop Ricochet transaction.

The fee is automatically paid by your wallet as part of the Ricochet transaction. You will be asked to review and accept the total fee before confirming the send. 


