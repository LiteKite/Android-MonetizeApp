# Android-MonetizeApp

<img src="https://github.com/LiteKite/Android-MonetizeApp/blob/assets/assets/app_icon.png" alt="App Icon" />

##

### A sample which uses Google's Play Billing Library and it does InApp Purchases and Subscriptions.

<a href="https://codeclimate.com/github/LiteKite/Android-MonetizeApp/maintainability"><img src="https://api.codeclimate.com/v1/badges/9086ce3ec1082cb455fa/maintainability" /></a> ![build](https://github.com/LiteKite/Android-MonetizeApp/workflows/build/badge.svg?branch=main)

##

<div align="center">
<img src="https://github.com/LiteKite/Android-MonetizeApp/blob/assets/assets/screen_one.png" alt="App Screenshot One"/>
</div>

##

<div align="center">
<img src="https://github.com/LiteKite/Android-MonetizeApp/blob/assets/assets/screen_two.png" alt="App Screenshot Two"/>
</div>

##

<div align="center">
<img src="https://github.com/LiteKite/Android-MonetizeApp/blob/assets/assets/screen_three.png" alt="App Screenshot Three"/>
</div>

##

<div align="center">
<img src="https://github.com/LiteKite/Android-MonetizeApp/blob/assets/assets/screen_four.png" alt="App Screenshot Four"/>
</div>

##

<div align="center">
<img src="https://github.com/LiteKite/Android-MonetizeApp/blob/assets/assets/screen_five.png" alt="App Screenshot Five"/>
</div>

##

<div align="center">
<img src="https://github.com/LiteKite/Android-MonetizeApp/blob/assets/assets/screen_six.png" alt="App Screenshot Six"/>
</div>

## Getting Started

1. Add Play Billing Library dependency in your Android Studio Project.

2. Use the Application ID that's been used in your Google Play Developer Console.

3) Make In-app purchases by starting `BillingClient` connection, make querying purchases locally or from Google Play Store Cache from `BillingClient`

4) In-app product or "Managed Products" can be bought multiple times by consuming purchase before requesting another purchase.

5) Subscription based products cannot be consumed, It'll be based on some time periods that you choose and will expire after the time ends. These are all handled by Google Play Remote Server.

6) Add Tester Accounts in Google Play Developer Console -> App Releases for making test purchases and upload the initial version of your project APK including Google Play Billing Library dependency.

## Libraries Used

`Play Billing Library` -> for making Google Play In-app purchases.</br>

`Data Binding Library` -> for updating, handling views.</br>

`Lifecycle Components` -> LiveData for observing changes and ViewModel for MVVM Architecture.</br>

`Room Persistence Storage` -> An ORM for SQLite Database.</br>

`Hilt DI` -> Dependency Injection Library for Android.</br>

`App Startup` -> Initializes dependencies during app startup.</br>

`Spotless` -> A Code Formatter that uses Google AOSP Java format.</br>

## References

[Google's Play Billing Sample](https://github.com/android/play-billing-samples) -> A Sample provided by Google for implementing In-app purchases.

[Google Play Library Training](https://developer.android.com/training/play-billing-library/index.html) -> A Google Play Billing Library Usage Documentation.

[Google Play Library Guide](https://developer.android.com/google/play/billing/billing_library.html) -> A Google Play Billing Library Guide for the steps to make In-app purchases.

[Google Play Library Testing](https://developer.android.com/google/play/billing/billing_testing.html) -> For making test purchases without any real transactions.

[Android Architecture Components](https://developer.android.com/topic/libraries/architecture/index.html) -> For LiveData, ViewModel, Room Persistence Library and Data Binding Library.

## Support

If you've found an error in this sample, please file an issue:
https://github.com/LiteKite/Android-MonetizeApp/issues

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.

##

<h3 align="center">Like this project?, Always support...</h3>

<p align="center">
    <a href="https://www.buymeacoffee.com/svignesh93"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" height="50" /></a>
</p>

## License

~~~

Copyright 2021 LiteKite Startup.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

~~~
