#!/bin/bash

cat en_US.txt | while read -r line
do
    # Ignore empty lines.
    if [[ -z "$line" ]]; then echo ""; continue; fi

    # Ignore comments in the file.
    if [[ $(echo "$line" | grep -c "^#") -gt 0 ]]; then echo "$line"; continue; fi;

    IFS='=' read -ra ADDR <<< "$line"
    title="${ADDR[0]}"

    echo "$title="$(echo "$title" | sed "s/\./\_/g")

done > en_US_TEST.txt
