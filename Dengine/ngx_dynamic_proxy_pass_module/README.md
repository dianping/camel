ngx_dynamic_proxy_pass_module
=============================

A nginx module which supports dynamic proxy_pass
	


降级配置选项：

	Syntax:	upstream_degrade_rate 比率
	Default: 60
	Context:	http, upstream

	Syntax: upstream_degrade_force_state 
	Default: 0
	Context: upstream
	取值：0  自动升降级
		  1  强制升级
		  -1 强制降级

	Syntax:	upstream_degrate_shm_size	共享内存大小
	Default: 10M
	Context:	http

	Syntax: upstream_degrate_interface
	Default:
	Context: location

	1、查询
		GET http://localhost:port/degrade/status
		或者
		GET http://localhost:port/degrade/status/detail



