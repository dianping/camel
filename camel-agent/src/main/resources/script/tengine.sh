#!/bin/bash
set -e
set -u

#trap "echo 'INT signal received'" INT
#trap "echo 'TERM signal received'" TERM
cd `dirname $0`
source ./util.sh
source ./tengine_func.sh

log "PID is $$"
log "CMD is $0 $@"


eval "`parse_argument_and_set_variable config_file virtual_server_names versions tengine_reload dynamic_refresh_post_data dynamic_refresh_url refresh_method func`"

ensure_not_empty func="$func"

if [ "$func"x != "dynamic_refresh_config"x ];then
	ensure_not_empty config_file="$config_file" virtual_server_names="$virtual_server_names"
	ensure_not_empty versions="$versions" func="$func" tengine_reload="$tengine_reload"
else
	ensure_not_empty refresh_method="$refresh_method" dynamic_refresh_post_data="$dynamic_refresh_post_data" dynamic_refresh_url="$dynamic_refresh_url"
fi

$func $@
