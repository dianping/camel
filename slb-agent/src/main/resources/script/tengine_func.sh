function reload_config {
	if [ `pgrep nginx | wc -l` != 0 ]; then
		log "reload tengine config"
		sudo -u root /etc/init.d/nginx reload || { log_error "fail to reload tengine, exit code is $?"; exit 1; }
	else
		log "start tengine"
		sudo -u root /etc/init.d/nginx start || { log_error "fail to start tengine, exit code is $?"; exit 1; }
	fi
}

function dynamic_refresh_config {
	if [ `pgrep nginx | wc -l` != 0 ]; then
	    log "curling $dynamic_refresh_url with request method $refresh_method(post data: $dynamic_refresh_post_data)"
	    local command=`echo "curl -X$refresh_method -d$dynamic_refresh_post_data $dynamic_refresh_url"`
	    log "Curl CMD is $command"
		local response=`eval $command`
		if [ "$response"x != "success"x ] && [ "$response"x != "not found uptream"x ];then
			log "fail to curl"
			exit 1
		fi
        log "refresh result:"
        curl $dynamic_refresh_url
	else
		log "start tengine"
		sudo -u root /etc/init.d/nginx start || { log_error "fail to start tengine, exit code is $?"; exit 1; }
	fi
}