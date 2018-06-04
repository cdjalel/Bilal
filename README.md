Overview
========
Bilal is a free software that displays Muslim Prayer times on Android phones and tablets. It uses a Java version of the PrayerTime package that is part of Islamic Tools Library from the Arab Eyes project. This library provides also the calculation methods shown in the screenshots below.

Bilal was initially developed for academic purposes to demonstrate some Android features (Alarm, Notification, Wear support etc.). It was open sourced under __GPLv3__ in the hope that volunteers can contribute changes to improve it. See the TODO file for that.

Currently it contains the coordinates of __23784__ cities in 250 countries around the world, extracted from the geoname.org project. Adding a new city or adjusting the coordinates of an existing one can de done in the sqlite ___locations.db3___ database in [app/src/main/asset/databases](https://github.com/cdjalel/Bilal/blob/master/app/src/main/assets/databases/locations.db3).

It is possible to add/replace a Muezzin by adding/modifying its mp3 files in [app/src/main/res/raw](https://github.com/cdjalel/Bilal/blob/master/app/src/main/res/raw) and the corresponding XML resources.

Bilal is deployed in Google Play Store as "Athan Bilal": 
<a href='https://play.google.com/store/apps/details?id=com.djalel.android.bilal&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>

Screenshots
===========
![screenshots](https://user-images.githubusercontent.com/5300525/40922034-fa169194-6808-11e8-8213-d02041eb3cec.png)

Testing
=======
- Testing with two emulators (phone + watch) is not possible any more due to changes in recent Android SDKs that prevent pairing them together. Instead, you can use a watch emulator with a real phone where you need to install  the app 'Wear OS by Google'.

- To test with two emulators (phone + watch) using SDK 26 and x86 emulator images follow these steps:

1. Use a phone emulator with Google API support.

$ /opt/android-sdk-linux/tools/emulator -avd phone -netspeed full -netdelay none -no-boot-anim -gpu on -qemu -m 512 -enable-kvm

2. Use a smart watch emulator to see Prayer Time wear notification.

$ /opt/android-sdk-linux/tools/emulator -avd watch -netspeed full -netdelay none -no-boot-anim -gpu on -qemu -m 512 -enable-kvm

3. Google and download the old 'Android Wear' app and install it on the phone emulator:
  
  $ /opt/android-sdk-linux/platform-tools/adb -s emulator-5556 install ~/Downloads/com.google.android.wearable.app-2.apk 

4. Pair the emulators as follows:
  a. find out the port used by the phone emulator (from its window title) or using:
    
    $ /opt/android-sdk-linux/platform-tools/adb devices

  b. log into the phone emulator using telnet:
    
    $ telnet localhost 5556

  c. in the telnet session add a tcp port redirection like this:
    
    redir add tcp:5601:5601 

  d. run the wearable app on the phone, accept the licence and choose 'Pair with a new device'. Upon success the message 'connected' will appear in the action bar.

5. Start Bilal then play with the time settings to trigger a notification.
 

Djalel Chefrour
cdjalel@gmail.com
