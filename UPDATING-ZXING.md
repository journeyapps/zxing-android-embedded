# Updating ZXing

This outlines the process to bring this library up to date with the latest changes from the official
ZXing project.

## 1. zxing-android-complete

This is a "complete" version of the BarcodeScanner project, converted to a library. The first step
is to update this.

1. Clone the zxing project from Github. The scripts assume that zxing-android-minimal and zxing
   are in the same folder.
2. In `zxing-android-complete`, run `ruby update.rb`.
3. Inspect the changes with `git diff`, and check that the changes make sense.
4. Test that the project can compile and run. A simple way to test is to replace
   `compile(project(':zxing-android'))` with `compile(project(':zxing-android-complete'))` in
   [sample/build.gradle](sample/build.gradle), and test the sample project.
5. Commit the changes.

See [zxing-android-complete/Readme.md](zxing-android-complete/Readme.md) for more details on the
conversion process.

## 2. zxing-android

This is a stripped-down version of zxing-android-complete. Some of the work to produce the
stripped-down version is automated, but some is manual.

The project contain two source folders and two resource folders. `src-orig` and `res-orig` contain
sources and resources from zxing-android-complete that are either unmodified, or adapted
automatically by the update script. `src` and `res` contains sources and resources that are manually
adapted for the library.

1. Make sure zxing-android-complete is updated (see above).
2. In `zxing-android`, run `ruby update.rb`.
3. Inspect the changes with `git diff`, and check that the changes make sense.
4. Inspect `zxing-android/res.patch` and `zxing-android/sources.patch`. This contains the diff
   between the files in our `res` and `src` folders, and the ones from zxing-android-complete.
   Use this, along with the changes between that were made in the zxing-android-complete project
   to these files, to manually update them.
5. Manually update AndroidManifest.xml if required.
6. Test!
7. Commit and make a pull request.

Notes:

* Assets (used for the help section) are not covered by this conversion process at all. We need a
  better overall plan here on how to handle help files (Use the stock Barcode Scanner help files?
  Completely remove the help section?).
