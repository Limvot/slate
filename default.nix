{ pkgs ? import <nixpkgs> {
  config.android_sdk.accept_license = true;
  config.allowUnfree = true;
} }:
with pkgs;
let
	composition = androidenv.composeAndroidPackages {
		 platformVersions = [ "29" ];
		 buildToolsVersions = [ "29.0.2" ];

		 #platformVersions = [ "28" ];
		 #buildToolsVersions = [ "28.0.3" ];
		 abiVersions = [ "x86" "x86_64"];
	 };
	 sdk = composition.androidsdk;
   fhs = pkgs.buildFHSUserEnv {
      name = "android-env";
      targetPkgs = pkgs: (with pkgs;
        [
          openjdk8
          #needed for adb
          sdk
          glibc
        ]);
      runScript = "bash";
      profile = ''
      export ANDROID_JAVA_HOME=${pkgs.openjdk8.home}
      '';
    };
in androidenv.buildApp {
  name = "Slate";
  src = ./.;
  platformVersions = [ "29" ];
  buildToolsVersions = [ "29.0.2" ];
  #platformVersions = [ "28" ];
  #buildToolsVersions = [ "28.0.3" ];
  nativeBuildInputs = [
    openjdk8
    fhs
  ];
  shellHook = "exec android-env";
}


