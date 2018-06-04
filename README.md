Overview
========
Bilal is a free software that displays Muslim Prayer times on Android phones and tablets. It uses a Java version of the PrayerTime package that is part of Islamic Tools Library from the Arab Eyes project.

Bilal was initially developed for academic purposes to demonstrate some Android features (Alarm, Notification, Wear support etc.). It was open sourced under GPLv3 in the hope that volunteers can contribute changes to improve it. See the TODO file for that.

Currently it contains the coordinates of 23784 cities in 250 countries around the world, extracted from the geoname.org project.

It is deployed in Google Play Store as "Athan Bilal": 
<a href='https://play.google.com/store/apps/details?id=com.djalel.android.bilal&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>

Screenshots
===========
![prayer_times](https://user-images.githubusercontent.com/5300525/40919891-d560ccda-6802-11e8-9ecc-9baffd79f93f.png)
![city_search](https://user-images.githubusercontent.com/5300525/40919921-ede93c74-6802-11e8-8a70-69267f194440.png)
![calculation_method](https://user-images.githubusercontent.com/5300525/40919894-d86819ec-6802-11e8-8749-5f81d0308bb6.png)
![muezzin_selection](https://user-images.githubusercontent.com/5300525/40919934-f90e9180-6802-11e8-95a9-d0c9a9fe12c9.png)

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
  4.1. find out the port used by the phone emulator (from its window title) or using:
    $ /opt/android-sdk-linux/platform-tools/adb devices

  4.2. log into the phone emulator using telnet:
    $ telnet localhost 5556

  4.3. in the telnet session add a tcp port redirection like this:
    redir add tcp:5601:5601 

  4.4. run the wearable app on the phone, accept the licence and choose 'Pair with a new device'. Upon success the message 'connected' will appear in the action bar.

5. Start Bilal then play with the time settings to trigger a notification.
 

Djalel Chefrour
cdjalel@gmail.com
