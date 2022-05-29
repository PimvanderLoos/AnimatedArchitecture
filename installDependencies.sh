#!/bin/bash

test -z "$1" && { echo "No temporary directory provided!"; exit 1; }

BUILD_DIR="$1"

function install_dependency() {
    url="$1"
    name="$2"
    groupId="$3"
    artifactId="$4"
    version="$5"
    packaging="$6" 

    tmp_name="$name-$version.jar"
    tmp_file="$BUILD_DIR/$tmp_name"
    echo "Installing $tmp_name in local repository..."

    wget -O "$tmp_file" "$url"
    mvn install:install-file -Dfile="$tmp_file" -DgroupId="$groupId" -DartifactId="$artifactId" -Dversion="$version" -Dpackaging="$packaging"
}

install_dependency "https://github.com/Rumsfield/konquest-doc/raw/main/release-jars/Konquest-0.9.1.jar" \
    "Konquest" \
    "konquest" \
    "konquest" \
    "0.9.1" \
    "jar"




