{
  description = "Dev dependencies for esp32 development";
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    nixpkgs-esp32-idf = {
      url = "github:mirrexagon/nixpkgs-esp-dev";
      inputs = {
        nixpkgs.follows = "nixpkgs";
        flake-utils.follows = "flake-utils";
      };
    };
  };
  outputs =
    { self
    , nixpkgs
    , flake-utils
    , nixpkgs-esp32-idf
    }: flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [ nixpkgs-esp32-idf.overlays.default ];
      };
    in
    {
      devShells.default = pkgs.mkShell {
        packages = with pkgs; [ esp-idf-full ];
      };
    });
}
