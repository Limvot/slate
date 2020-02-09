let
  moz_overlay = import (builtins.fetchTarball https://github.com/mozilla/nixpkgs-mozilla/archive/master.tar.gz);
  nixpkgs = import <nixpkgs> {
    config = {
      allowUnfree = true;
      android_sdk.accept_license = true;
    };
    overlays = [ moz_overlay ];
  };
  androidsdk = (nixpkgs.androidenv.composeAndroidPackages {
    toolsVersion = "25.2.5";
    platformToolsVersion = "28.0.1";
    buildToolsVersions = [ "28.0.3" ];
    includeEmulator = false;
    emulatorVersion = "28.0.14";
    platformVersions = [ "26" ];
    includeSources = false;
    includeDocs = false;
    includeSystemImages = false;
    systemImageTypes = [ "default" ];
    #abiVersions = [ "armeabi-v7a" ];
    #lldbVersions = [ "2.0.2558144" ];
    lldbVersions = [ ];
    cmakeVersions = [ "3.6.4111459" ];
    includeNDK = true;
    ndkVersion = "18.1.5063045";
    useGoogleAPIs = false;
    useGoogleTVAddOns = false;
    #includeExtras = [ "extras;google;gcm" ];
    includeExtras = [ ];
  }).androidsdk;
in
  with nixpkgs;
  stdenv.mkDerivation {
    name = "moz_overlay_shell";
    src = ./.;
    ANDROID_HOME = "${androidsdk}/libexec/android-sdk";
    KOTLIN_LIB_HOME = "${kotlin}/lib";
    buildInputs = [
      file
      tree
      openssl
      pkg-config
      ant
      jdk
      gnumake
      gawk
      ((nixpkgs.rustChannelOf { channel = "stable"; }).rust.override { targets = [
        "aarch64-linux-android"
        "armv7-linux-androideabi"
      ]; })
      androidsdk
      kotlin
    ];
  buildPhase = ''
    export ANDROID_SDK_HOME=`pwd` # Key files cannot be stored in the user's home directory. This overrides it.

    export GNUMAKE=${gnumake}/bin/make
    export NDK_HOST_AWK=${gawk}/bin/gawk
    export CARGO_HOME="$(pwd)/.cargo"
    # first for debug
    ant debug
    mkdir -p .cargo
    (echo "[target.aarch64-linux-android]"
     echo "linker = \"${androidsdk}/libexec/android-sdk/ndk-bundle/toolchains/aarch64-linux-android-4.9/prebuilt/linux-x86_64/bin/aarch64-linux-android-ld\""
     echo "rustflags = [\"-Clink-args=-L${androidsdk}/libexec/android-sdk/ndk-bundle/platforms/android-26/arch-arm64/usr/lib -L${androidsdk}/libexec/android-sdk/ndk-bundle/toolchains/aarch64-linux-android-4.9/prebuilt/linux-x86_64/lib/gcc/aarch64-linux-android/4.9.x\"]"
     echo
     echo "[target.armv7-linux-androideabi]"
     echo "linker = \"${androidsdk}/libexec/android-sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin/arm-linux-androideabi-ld\""
     echo "rustflags = [\"-Clink-args=-L${androidsdk}/libexec/android-sdk/ndk-bundle/platforms/android-26/arch-arm/usr/lib -L${androidsdk}/libexec/android-sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/lib/gcc/arm-linux-androideabi/4.9.x\"]"

    ) >> .cargo/config
    pushd rust

     PATH="${androidsdk}/libexec/android-sdk/ndk-bundle/toolchains/aarch64-linux-android-4.9/prebuilt/linux-x86_64/bin:$PATH" cargo build --target aarch64-linux-android --release
    mkdir -p ../jni/arm64-v8a
    cp target/aarch64-linux-android/release/librust.so ../jni/arm64-v8a/
    PATH="${androidsdk}/libexec/android-sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin:$PATH" cargo build --target armv7-linux-androideabi --release
    mkdir -p ../jni/armeabi-v7a
    cp target/armv7-linux-androideabi/release/librust.so ../jni/armeabi-v7a/
    (echo ""
     echo 'LOCAL_PATH := $(call my-dir)'
     echo ""
     echo 'include $(CLEAR_VARS)'
     echo ""
     echo 'LOCAL_MODULE    := rust'
     echo 'LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/librust.so'
     echo ""
     echo 'include $(BUILD_SHARED_LIBRARY)'
    ) >> ../jni/Android.mk
    popd
    nm -g ./jni/arm64-v8a/librust.so
    ${androidsdk}/libexec/android-sdk/ndk-bundle/ndk-build
    nm -g ./libs/arm64-v8a/librust.so
    cp ./jni/arm64-v8a/librust.so ./libs/arm64-v8a/librust.so
    cp ./jni/armeabi-v7a/librust.so ./libs/armeabi-v7a/librust.so
    nm -g ./libs/arm64-v8a/librust.so
    ant debug
  '';
  installPhase = ''
    mkdir -p $out
    mv bin/*-debug.apk $out
  '';
  }
