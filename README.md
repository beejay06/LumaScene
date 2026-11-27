# LumaScene

A photo frame for NVIDIA Shield that plays alongside your music.

## In this test build (0.2)
- Photos and videos from Google Photos (pick on your phone) or a USB stick; nothing is saved on the Shield
- True shuffle: no repeats until everything has been shown, remembered between launches
- Dissolve or fade, muted videos, blurred background for vertical pictures
- Clock, date taken, song title
- Spotify / Tidal: open the app, then previous, play/pause and next from the slideshow

## Not built yet
Guest upload, free trial and purchase. Google Photos has not been proven on a Shield yet.

## Install
GitHub builds `lumascene.apk` and puts it under Releases > "LumaScene test build". Sideload it on the Shield.

## Google Photos setup (once)
In Google Cloud: enable the Google Photos Picker API, add yourself as a test user, and create an
OAuth client of type Android with package `com.beejay.lumascene` and the SHA-1 shown in the build log.
