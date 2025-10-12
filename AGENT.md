# Agent Context for "Make Your Phone Yours"

This document provides instructions and context to AI coding agents to help them understand and work on this project.

## Project Overview

This is an Android application that helps users monitor their mobile data usage. The app allows users to set a data limit and a time period, and it will then monitor their data usage in the background. When the data limit is exceeded, the app will play a warning sound and display a notification.

## Project Structure

*   `app/src/main/java/com/praki/makeyourphoneyours/`: The main source code for the application.
    *   `MainActivity.kt`: The main UI of the app.
    *   `DataUsageService.kt`: The background service that monitors data usage.
    *   `NetworkChangeReceiver.kt`: The broadcast receiver that starts and stops the service based on network connectivity.
*   `app/src/main/res/`: The resource files for the application.
*   `build.gradle.kts`: The main build file for the project.

## Build and Test Instructions

To build the project, run the following command:

```bash
./gradlew build
```

To run the tests, run the following command:

```bash
./gradlew test
```

## Coding Conventions and Style Guidelines

This project follows the standard Kotlin coding conventions. Please refer to the [official Kotlin documentation](https://kotlinlang.org/docs/coding-conventions.html) for more information.

## Commit and PR Guidelines

*   Commit messages should be concise and descriptive.
*   Pull requests should be small and focused on a single feature or bug fix.

## Architecture and Design Patterns

This project follows a simple architecture with a single activity, a background service, and a broadcast receiver. The code is written in a clean and modular way.