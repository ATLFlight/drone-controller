
# Table of contents
1. [Installation and Build Instructions](#installation-and-build-instructions)
  * [NOTICE](#notice)
  * [Prerequisities](#prerequisties)
    * [Install NDK](#install-ndk)
    * [Install Android SDK](#install-android-sdk)
    * [Install java](#install-java)
    * [Install GStreamer SDK](#install-gstreamer-sdk)
  * [Command line build instructions](#command-line-build-instructions)
  * [Installation](#installation)
1. [Usage](#usage)

# Installation and Build Instructions

## NOTICE

This code uses the GStreamer SDK and links with the stlport_static library. Please review the respective licenses
for these tools as all users of this code are responsible for ensuring that their usage of the project is 
compatible with the various project licenses.

## Prerequisites 

### Install NDK
  
  Install the Android NDK version r10e ie **android-ndk-r10e**
  1. Download the ndk android-ndk-r10e from [here](https://developer.android.com/ndk/downloads/older_releases.html)
    1. Download URL: https://dl.google.com/android/repository/android-ndk-r10e-linux-x86_64.zip
  1. unzip the file to a folder.

### Install Android SDK.

  ```
    Note:  This is only needed for command line builds.
  ```
  1. Download the Android SDK command line tools from [here](https://developer.android.com/studio/index.html#downloads)
    1. For linux variant, download the following file: tools_r25.2.3-linux.zip
  1. Unzip the file to a folder.
  1. Run the "android" application from the unzipped folder.
    1. **NOTE**: Running the "android" application needs GUI.  Running natively on the linux host is recommended or have X11 forwarding.
    1. **NOTE**: When running the "android" app keep note of the SDK path.  This will be needed later.
  1. Install the following packages/tools:
    1. Android SDK tools( 25.2.3)
    1. Android SDK Platform Tools( 25.0.3)
    1. Android SDK Build Tools( 25.0.2 )
    1. Android SDK Build Tools( 21.1.2 )
    1. Android 5.0.1( API 21 )
      1. SDK Platform

### Install Java

  Install the latest version of java, if not already installed.

### Install Gstreamer SDK 
  
  Install Gstreamer SDK from http://gstreamer.freedesktop.org/download/

  Download URL: https://gstreamer.freedesktop.org/data/pkg/android/1.6.0/gstreamer-1.0-android-armv7-1.6.0.tar.bz2

  ```
  gstreamer-1.0-android-armv7-1.6.0.tar.bz2
  ```

  1. Once downloaded unzip the file to a folder.

  ```
  %>tar xvfj gstreamer-1.0-android-armv7-1.6.0.tar.bz2
  ```

## Command line build instructions

  1. Clone repository

  ```
  %>git clone https://github.com/ATLFlight/drone-controller.git
  ```
  1. Set up the environment, and install gradle if necessary:
  
  ```
  $ source env-setup.sh
  ```
  1. Create the **local.properties** file at the top-level of the dronecontroller git root.  This allows gradle to know where the Android NDK, Android SDK
     and Gstreamer SDK are located.
     Example:

     ```
      gst.dir=<path_to_gstreamer_1.6.0_unpacked_path>
      ndk.dir=<path_to_ndk_r10e_unpacked_path>
      sdk.dir=<path_to_installed_android_sdk_path>
    ```
    **NOTE**: For the "sdk.dir" use the "SDK path" as displayed when running the "android" app as part of the "Install Android SDK" section.

  1. Tell gradle to build the app
  
  ```
  $ gradle assemble
  ```
  
  1. The generated apk file will be located at:

  ```
  <drone_controller_git_root>/app/build/outputs/apk/app-debug.apk
  ```

## Installation

  Use the adb install command to install the apk on the Android Tablet

  ```
  %>adb install <path_to_dronecontroller_apk>/app-debug.apk
  ```

# Usage

Refer to [UserGuide](UserGuide.md) for DroneController usage information