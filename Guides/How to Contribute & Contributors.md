# How to Contribute 

If you'd like to contribute to these guides or the Samourai Wallet ecosystem documention in general you can contact:

### Samourai Wallet

[Twitter](https://twitter.com/SamouraiWallet?s=09)

[Keybase](https://keybase.io/samourai)

[Github](https://github.com/samouraiwallet)

### Crazyk031

[Twitter](https://twitter.com/Crazyk_031?s=09)

[Keybase](https://keybase.io/crazyk031)

[Github](https://github.com/Crazyk031)

# Contribute a Translation 

Contribute a translation to Samourai Wallet
This document's purpose is to describe step by step how to contribute to the localization of Samourai Wallet. For any questions or suggestions please refer to the official [Samourai Telegram channel](https://t.me/SamouraiWallet). Currently, localization contributions are handled only with Pull Requests on our open source Github repository, so in order to contribute and receive credit for your work, you'll need to register for a free Github account.

**Step 1 - Fork Samourai Wallet**

Once you signed up, the first thing to do is to fork the [Samourai wallet repository](https://github.com/Samourai-Wallet/samourai-wallet-android). You can do this using the "**Fork**" button:

![Translation 1](Images/Translation%201.png)

**Step 2- Locate your copy of Samourai Wallet**

Once you forked (copied) the repository, then you can work on your own copy. To do this, go to Your Repositories in your profile and open the link to the repository you just forked, and it should look like this:

![Translation 2](Images/Translation%202.png)

**Step 3 - Navigate to the resources folder**

Now, you'll need to navigate to the folder where the translation are saved: to do this, you just need to click on the folders in the project, navigating down to the resources folder in the file structure

`app/src/main/res`

Once you're there, you should see a list of folders like this one:

![Translation 3](Images/Translation%203.png)

The folders used for translations are the "values" folders, and as you can see, there's one folder for each language, identified by a 2 letter code (e.g. values-it for Italian, values-ru for Russian, etc.)

**Step 4 - Find the English strings**

To find the english strings to be localized, open: 

`values/strings.xml`

A raw text copy of the file is available at this URL: https://raw.githubusercontent.com/Samourai-Wallet/samourai-wallet-android/develop/app/src/main/res/values/strings.xml

**Step 5 - Start Translating**

If a subfolder with your language code is already in the Resources folder it means that someone already posted a translation of this language: you can navigate inside it and make any changes needed to the translation.

If there is no subfolder with your language code press the "Create new file" button and name your new file **EXACTLY** like this, substituting the XX with your localization code:

`values-XX/strings.xml`

For example, a french translator would create 

`values-fr/strings.xml`

Now that you have an empty file, you can paste the original content of the english strings.xml file in here and start translating every line. Keep in mind that you must not translate the text between "", but only the text inside the HTML tags.

 For example:

`<string name="cancel">Cancel</string>`

Becomes (in Brazilian):

`<string name="cancel">Cancelar</string>`

**Step 6 - Create a pull request**

Once you've translated everything, save your work and create a pull request. To do this, go back to the [Samourai Wallet repository](https://github.com/Samourai-Wallet/samourai-wallet-android) and click **New pull request**.

 In the new window, click on "**compare across forks**" to submit a pull request from your own fork that you created. Once you've activated comparing across forks, pick your repository's **development** branch to pull on Samourai wallet's **development** branch, like this:

![Translation 4](Images/Translation%204.png)

 Now, leave a brief comment to explain you've added a new translation and press the "**Create pull request**" button.

**Step 7 - Wait for approval**

Once the Samourai team reviews your translation and approves it, your pull request will be merged in the main project and you will receive credit as a contributor to Samourai Wallet. We really appreciate you taking the time to help us improve Samourai Wallet by adding your translation. 



**Special thanks to [LifeIsPizza](https://github.com/LifeIsPizza) for writing this localization guide.**

# Those Who've Contributed 

_This is an incomplete list, many more have contributed their time and effort to further the documention & ecosystem surrounding Samourai Wallet_

If your a contributor and aren't on the list, but would like to be added, DM @crazyk031 on Twitter or Telegram. 

**PuraVida** 
Overall wealth of knowledge, creator of his own [FAQ](https://github.com/PuraVlda/samourai-wallet-android/wiki) & operator of [Lightning Hood](https://lightninghood.com/) where you can get all your Samourai swag! 

**BTCxZelko** 
Co creator of [Ronin](https://github.com/RoninDojo/RoninDojo) & wealth of knowledge about Dojo on Pi4

**6102bitcoin**
Writer of an amazing [FAQ](https://github.com/6102bitcoin/FAQ) & [bitcoin-intro](https://github.com/6102bitcoin/bitcoin-intro) guide! 

**GuerraMoneta**
Co creator of [Ronin](https://github.com/RoninDojo/RoninDojo) & wealth of knowledge about Dojo on Odroid

**LaurentMT** 
Overall wealth of knowledge & Dojo troubleshooting wizard! 

**Furkan**
Admin on Samourai Wallet Telegram chat & devoloper of his own FAQ





