
function log {
	echo "[`date +'%m-%d %H:%M:%S'`] [INFO] $@"
}

function log_error {
	echo "[`date +'%m-%d %H:%M:%S'`] [ERROR] $@"
}

function ensure_not_empty {
	for var in "$@";do
		IFS='=' read -ra KV <<< "$var"
		if [ ${#KV[*]} -eq 1 ];then
			log "${KV[0]} is required"
			exit 1
		fi
	done
}

# generate some bash code which can parse argument like "--name=marsqing" 
# 	and set the "name" variable to "marsqing"
# Usage: eval "`parse_and_set_argument arg1 arg2`", (double quote is required)
#	which will parse current $@ like "--arg1=value1 --arg2=value2"
# 	and set local variable arg1 to value1 and arg2 to value2
# Parameter: any number of variable names to parse
function parse_argument_and_set_variable {
	local long_options="dummy:"
	for param in "$@";do
		long_options=$long_options,$param:	
	done

	echo -n 'options=$(getopt -o x -l '
	echo -n $long_options
	echo ' -- "$@")'

	cat <<-'END'
		eval set -- $options
		while [ $# -gt 0 ]
		do
			case "$1" in
	END


	for param in "$@";do
		echo -n "--$param) $param="
		echo '"$2";shift;;'
	done
				
	cat <<-'END'
				(--) shift;break;;
				(-*) echo "$0: error - unrecognized option $1"1>&2;return 1;;
				(*) break;;
			esac
			shift
		done
	END
}

function kill_by_javaclass {
	local javaclass=$1	
	/usr/local/jdk/bin/jps -lvm | awk -v javaclass=$javaclass '$2==javaclass{cmd=sprintf("kill -s TERM %s; sleep 1; kill -9 %s", $1, $1);system(cmd)}'
}
