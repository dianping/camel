function nginx_check {
	sudo -u root /usr/local/nginx/sbin/nginx -t -c $config
}

