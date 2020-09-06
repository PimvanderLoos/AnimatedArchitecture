#!/bin/bash

# Copy the latest real en_US.txt if the current copy is out of date and then
# generate the test translations.
realFile="../../../../bigdoors-spigot/spigot-core/src/main/resources/en_US.txt"
copiedFile="en_US.txt"
testFile="en_US_TEST.txt"

if ! cmp --silent "$realFile" "$copiedFile"
then
    echo "Files are out of sync, updating test file now!"
    cp "$realFile" "$copiedFile"
    bash generateTestText.sh
fi

# If the en_US.txt was somehow up-to-date, but the test translations
# files doesn't exist, generate it anyway.

if [ ! -f "$testFile" ]; then
    bash generateTestText.sh
fi

