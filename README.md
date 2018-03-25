Overview
========
Bilal is a free software that displays Muslim Prayer times on Android phones and tablets. It uses a Java version of the PrayerTime package that is part of Islamic Tools Library from the Arab Eyes project.

Bilal is an unpolished code that was developed in a rush for academic purposes to demonstrate some Android features (Alarm, Notification, Wear support etc.). So it is quite incomplete. The initial release only shows prayer times for one hard coded city, following one hard coded calculation method. Both parameters can be changed easily in the code for test purposes.

It was open sourced under GPLv3 in the hope that volunteers can contribute changes to improve it. See the TODO file for that.

Screenshots
===========

![bilal-screenshot-phone](https://cloud.githubusercontent.com/assets/5300525/8514511/fb1d6576-2388-11e5-8939-be2b9c22b761.png)
![bilal-screenshot-watch](https://cloud.githubusercontent.com/assets/5300525/8423273/a64e318c-1ede-11e5-97e9-132cf7c15d3c.png)


Testing
=======
- For SDK 26 and x86 images emulators.

- Use a phone emulator with Google API support and, if possible, hardware acceleration as described here:
    - http://developer.android.com/tools/devices/emulator.html
    - https://software.intel.com/en-us/android/articles/speeding-up-the-android-emulator-on-intel-architecture

- Use a smart watch emulator to see Prayer Time wear notification.

- For example, to start the emulators from the command line: 
  $ /opt/android-sdk-linux/tools/emulator -avd phone -netspeed full -netdelay none -no-boot-anim -gpu on -qemu -m 512 -enable-kvm

  $ /opt/android-sdk-linux/tools/emulator -avd watch -netspeed full -netdelay none -no-boot-anim -gpu on -qemu -m 512 -enable-kvm

- Google and download the old 'Android Wear' app and install it on the phone emulator:
  $ /opt/android-sdk-linux/platform-tools/adb -s emulator-5556 install ~/Downloads/com.google.android.wearable.app-2.apk 

- Pair the emulators as follows  (this is not supported by the new app 'Wear OS by Google', you will need a real phone):
  - find out the port used by the phone emulator (from its window title) or using:
    $ /opt/android-sdk-linux/platform-tools/adb devices

  - log into the phone emulator using telnet:
    $ telnet localhost 5556

  - in the telnet session add a tcp port redirection like this:
    redir add tcp:5601:5601 

  - run the wearable app on the phone, accept the licence and choose 'Pair with a new device'. Upon success the message 'connected' will appear in the action bar.

- Start Bilal then play with the time settings to trigger a notification.
 

Djalel Chefrour
cdjalel@gmail.com
