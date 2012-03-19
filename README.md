Unison
======

Unison is a group music recommender for parties. It picks out the best music on
your Android device given the people around you.

For more informations, [contact me][2].

Want to test it ?
-----------------

You are welcome to test the app. Requirements:

- The Android SDK (level > 10). If you have a Mac & [Homebrew][3], try 'brew
  install android'
- A [USB debugging enabled][1] android device (or you can just fire up an
  emulator).

Then:

    git clone git://github.com/lucasmaystre/unison-android.git
    android update project --path unison-android/
    cd unison-android/
    # Connect your android device to your computer
    ant debug install

That's it!

[1]: http://developer.android.com/guide/developing/device.html
[2]: mailto:lucas@maystre.ch
[3]: http://mxcl.github.com/homebrew/
