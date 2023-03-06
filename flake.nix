{
  description = "Slack shuffle bot as a Nix Flake";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix.url = "github:jlesquembre/clj-nix";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    clj-nix,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
      cljpkgs = clj-nix.packages.${system};
    in {
      packages = {
        bot-clj = cljpkgs.mkCljBin {
          projectSrc = ./.;
          name = "davidsierradz/slack-shuffle-bot";
          version = "1.0";
          main-ns = "davidsierradz.slack-shuffle-bot";
          jdkRunner = pkgs.jdk17_headless;
        };
      };
      devShell = pkgs.mkShell {
        nativeBuildInputs = [pkgs.clojure];
        buildInputs = [];
      };
    });
}
