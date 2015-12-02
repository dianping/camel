#!/bin/bash
set -e
set -u

#trap "echo 'INT signal received'" INT
#trap "echo 'TERM signal received'" TERM
cd `dirname $0`
source ./util.sh
source ./nginx_func.sh

log "PID is $$"
log "CMD is $0 $@"

eval "`parse_argument_and_set_variable func config`"

ensure_not_empty func="$func" config="$config"

$func $@
