#!/bin/bash

# This file will strip down the translation file to the bare minimum.
# All the translations themselves are removed.
# Only the name of the translation (with dots replaced by
# underscores to match the enum names) and their supported
# variables are important.

# 'variables' contains the list of supported variables
# For the next few lines.
# Lines supporting a variable or group of variables
# Are generally grouped together and split by newlines.
# So all strings under a '# Supported: %VARIABLE%' header
# Will support that variable / those variables until the
# next empty line.

# Example input:
: '
# Supported: %INPUT%
ERROR.INVALIDDOORNAME=&4"%INPUT% is not a valid door name!
'

# Example output:
: '
# Supported: %INPUT%
ERROR.INVALIDDOORNAME=ERROR.INVALIDDOORNAME %INPUT%
'

variables=""
cat en_US.txt | while read -r line
do
    # Handle empty lines in the file.
    # Just print a new empty line and also reset the list
    # of supported variables.
    if [[ -z "$line" ]]
    then
        echo ""
        variables=""
        continue
    fi

    # Handle comments in the file.
    # Regular comments need to just be printed as-is.
    # However, lines starting with "# Supported: " DO need to be processed
    # As the variables they support are needed later on.
    if [[ $(echo "$line" | grep -c "^#") -gt 0 ]]
    then
        if [[ $(echo "$line" | grep -c "^# Supported: ") -gt 0 ]]
        then
            variables=$(echo "$line" | sed "s/# Supported: //g")
        fi

        echo "$line"
        continue
    fi

    IFS='=' read -ra ADDR <<< "$line"
    title="${ADDR[0]}"

    echo "$title="$(echo "$title" | sed "s/\./\_/g")" $variables" | xargs

done > en_US_TEST.txt
