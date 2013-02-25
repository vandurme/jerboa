#!/bin/bash
if [[ -z $1 ]] || [[ -z $2 ]]; then
    echo "usage: jar-models.sh output-file-name /path/to/be/jarred"
    exit 1;
fi

jar cf $1 $2