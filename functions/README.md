# Google AdMob Server-Side Verification (SSV) Setup Guide

This serverless Cloud Function protects your Android app against **Mod APKs**, **Lucky Patcher**, and **Ad-Bypass hacks**.

## How It Works
1. When a user watches a Rewarded Ad in **FREE PYQ**, Google's server directly pings your Cloud Function with a cryptographic ECDSA signature.
2. The Cloud Function verifies Google's signature using official AdMob public keys.
3. Upon validation, the test is permanently unlocked in **Firestore** (`users/{userId}/unlocked_tests/{testKey}`).
4. The Android app syncs the unlock directly from Firestore — so even if someone modifies the client APK, they cannot fake the unlock!

## Deployment Options

### Option 1: Deploy with Firebase CLI (Recommended)
If you have the Firebase CLI installed on your computer:
```bash
cd functions
firebase login
firebase use free-pyq
firebase deploy --only functions
```

Your Callback URL will be:
`https://asia-southeast1-free-pyq.cloudfunctions.net/admobSsvCallback`
*(or `https://us-central1-free-pyq.cloudfunctions.net/admobSsvCallback` depending on your Firebase region)*

### Option 2: Add into AdMob Console
1. Go to **Google AdMob Console** (`admob.google.com`).
2. Go to **Apps** -> **FREE PYQ** -> **Ad Units**.
3. Click on your **Rewarded Ad Unit**.
4. Expand **Advanced settings** -> **Server-side verification (SSV)**.
5. Check the box to enable SSV.
6. In **Callback URL**, paste your deployed Function URL.
7. Click **Save**.
