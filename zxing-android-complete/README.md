# zxing-android-complete

This is a complete copy of the ZXing Android application, converted to an Android library.

This is not intended to be used directly, but rather as a starting point to create a stripped-down
version.

`ruby update.rb` script does roughly the following:

1. Copy the Java source code, resources and assets from the ZXing Android application.
2. Prefix all resource files and names with `zxing_`.
3. Replace switch statements on resource ids with if statements.
4. Place assets in a `zxing` subfolder.
5. Update references in the Java code to all the renamed resources and assets.
6. Remove unwanted sections from AndroidManifest.xml.
7. Set minSdkVersion to 7.