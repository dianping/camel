#!/bin/bash
set -e
set -u

#trap "echo 'INT signal received'" INT
#trap "echo 'TERM signal received'" TERM
cd `dirname $0`
source ./util.sh
source ./git_func.sh

log "PID is $$"
log "CMD is $0 $@"

eval "`parse_argument_and_set_variable git_url git_host tag comment target_dir func`"

ensure_not_empty func="$func" target_dir="$target_dir"

if [ "$func"x = "commit_all_changes"x ]; then
	ensure_not_empty comment="$comment"
fi

if [ "$func"x = "tag_and_push"x ]; then
	ensure_not_empty comment="$comment" tag="$tag" git_url="$git_url"
fi

if [ "$func"x = "clone"x ]; then
	ensure_not_empty git_url="$git_url"
fi

if [ "$func"x = "push"x ]; then
	ensure_not_empty git_url="$git_url"
fi

$func $@
