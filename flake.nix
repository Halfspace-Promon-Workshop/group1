{
  description = "Java Dependency Graph Analyzer - A tool to analyze and visualize Java class dependencies in 3D";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        
        java-dependency-analyzer = pkgs.stdenv.mkDerivation {
          pname = "java-dependency-analyzer";
          version = "1.0-SNAPSHOT";

          src = ./java-dependency-analyzer;

          nativeBuildInputs = with pkgs; [
            maven
            jdk17
            makeWrapper
          ];

          buildPhase = ''
            # Set up Maven home and local repository
            export MAVEN_OPTS="-Dmaven.repo.local=$TMPDIR/.m2/repository"
            
            # Build the project
            mvn clean package -DskipTests
          '';

          installPhase = ''
            # Create bin directory
            mkdir -p $out/bin
            mkdir -p $out/share/java-dependency-analyzer
            
            # Copy the JAR with dependencies
            cp target/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar \
              $out/share/java-dependency-analyzer/
            
            # Create wrapper script
            makeWrapper ${pkgs.jdk17}/bin/java $out/bin/java-dependency-analyzer \
              --add-flags "-jar $out/share/java-dependency-analyzer/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar"
          '';

          meta = with pkgs.lib; {
            description = "A tool to analyze and visualize Java class dependencies in 3D";
            homepage = "https://github.com/yourusername/java-dependency-analyzer";
            license = licenses.mit;
            platforms = platforms.unix;
            mainProgram = "java-dependency-analyzer";
          };
        };

      in
      {
        packages = {
          default = java-dependency-analyzer;
          java-dependency-analyzer = java-dependency-analyzer;
        };

        apps = {
          default = {
            type = "app";
            program = "${java-dependency-analyzer}/bin/java-dependency-analyzer";
          };
        };

        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk17
            maven
          ];

          shellHook = ''
            echo "Java Dependency Analyzer Development Environment"
            echo "================================================"
            echo ""
            echo "Available commands:"
            echo "  mvn clean package    - Build the project"
            echo "  mvn clean test       - Run tests"
            echo ""
            echo "Java version: $(java -version 2>&1 | head -n 1)"
            echo "Maven version: $(mvn -version | head -n 1)"
            echo ""
            echo "Note: You need to start JDT Language Server separately."
            echo "See README.md for instructions."
          '';
        };
      }
    );
}
