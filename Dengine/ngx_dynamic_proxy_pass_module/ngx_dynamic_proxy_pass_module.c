#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>
#include "ngx_upstream_degrade.h"
#include "lua.h"
#include "lauxlib.h"
#include "lualib.h"
#include "ngx_http_dyups.h"

#define DEFAULT_LB_LUA_FILE "/usr/local/nginx/conf/phoenix-slb/rule.lua"
#define DEFAULT_DYPP_KEY "dypp_key"
#define DEFAULT_MAX_SERVER 200
#define LUA_VM_MAX_NUM 100


typedef struct {
	ngx_str_t dp_domain;
	ngx_str_t dypp_lua_file;
	ngx_str_t dypp_key;
	lua_State *L;

} ngx_http_dypp_loc_conf_t;

typedef struct {
	ngx_uint_t dypp_key_generate;	
} ngx_http_dypp_filter_loc_conf_t;

typedef struct {
	ngx_str_t name;
	lua_State *L;
}lua_vm;

ngx_http_request_t *cur_r;
ngx_str_t *cur_dp_domain;
unsigned long uid = 0;
lua_vm l_vm[LUA_VM_MAX_NUM];

static ngx_http_output_header_filter_pt  ngx_http_next_header_filter;

static ngx_int_t ngx_http_dypp_init_process(ngx_cycle_t* cycle);

static void* ngx_http_dypp_create_loc_conf(ngx_conf_t* cf);

static char* ngx_http_dypp_merge_loc_conf(ngx_conf_t *cf, void *parent, void *child);

static ngx_int_t ngx_http_dypp_preconfig(ngx_conf_t *cf);

ngx_int_t ngx_http_dypp_get_variable (ngx_http_request_t *r, ngx_http_variable_value_t *v, uintptr_t data);

static u_char* call_lua(ngx_http_request_t *r, lua_State *L);

static int get_cookie(lua_State *L) ;

static int get_upstream_list(lua_State *L) ;

static int get_ngx_http_variable(lua_State *L);

static ngx_int_t ngx_dynamic_proxy_pass_filter_init(ngx_conf_t *cf);

static ngx_int_t ngx_dynamic_proxy_pass_header_filter(ngx_http_request_t *r);

static void *ngx_dynamic_proxy_pass_filter_create_conf(ngx_conf_t *cf);

static char *ngx_dynamic_proxy_pass_filter_merge_conf(ngx_conf_t *cf, void *parent, void *child);

static ngx_int_t set_header(ngx_http_request_t* r, ngx_str_t* key, ngx_str_t* value);

static unsigned long generate_uid();

static u_char* has_generate_uid(ngx_http_request_t* r);



static ngx_command_t ngx_dynamic_proxy_pass_module_commands[] = {
	{
		ngx_string("dp_domain"), // The command name
		NGX_HTTP_MAIN_CONF | NGX_HTTP_SRV_CONF | NGX_HTTP_LIF_CONF | NGX_HTTP_LOC_CONF | NGX_CONF_TAKE1,
		ngx_conf_set_str_slot, // The command handler
		NGX_HTTP_LOC_CONF_OFFSET,
		offsetof(ngx_http_dypp_loc_conf_t, dp_domain),
		NULL
	},

	{ ngx_string("dypp_lua_file"),
		NGX_HTTP_MAIN_CONF | NGX_HTTP_SRV_CONF | NGX_HTTP_LOC_CONF | NGX_CONF_TAKE1,
		ngx_conf_set_str_slot,
		NGX_HTTP_LOC_CONF_OFFSET,
		offsetof(ngx_http_dypp_loc_conf_t, dypp_lua_file),
		NULL },

	{
		ngx_string("dypp_key"), // The command name
		NGX_HTTP_MAIN_CONF | NGX_HTTP_SRV_CONF | NGX_HTTP_LOC_CONF | NGX_CONF_TAKE1,
		ngx_conf_set_str_slot, // The command handler
		NGX_HTTP_LOC_CONF_OFFSET,
		offsetof(ngx_http_dypp_loc_conf_t, dypp_key),
		NULL
	},
	{
		ngx_string("upstream_degrade_rate"), // The command name
		NGX_HTTP_MAIN_CONF | NGX_HTTP_UPS_CONF | NGX_CONF_TAKE1,
		ngx_http_dypp_set_degrade_rate, // The command handler
		NGX_HTTP_SRV_CONF_OFFSET,
		offsetof(ngx_http_dypp_srv_conf_t, degrade_rate),
		NULL
	},
	{
		ngx_string("upstream_degrade_force_state"), // The command name
		NGX_HTTP_UPS_CONF | NGX_CONF_TAKE1,
		ngx_http_dypp_set_degrade_force_state, // The command handler
		NGX_HTTP_SRV_CONF_OFFSET,
		offsetof(ngx_http_dypp_srv_conf_t, degrade_force_state),
		NULL
	},

    { ngx_string("upstream_degrade_shm_size"),
      NGX_HTTP_MAIN_CONF|NGX_CONF_TAKE1,
      ngx_http_dypp_degrade_shm_size,
      0,
      0,
      NULL },

      { ngx_string("upstream_degrade_interface"),
    	NGX_HTTP_LOC_CONF|NGX_CONF_NOARGS,
    	ngx_http_upstream_degrade_interface,
        0,
        0,
        NULL },

	ngx_null_command
};

static ngx_command_t  ngx_dynamic_proxy_pass_filter_commands[] = {

	{ 
		ngx_string("dypp_key_generate"),
		NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_HTTP_LIF_CONF|NGX_CONF_TAKE1,
		ngx_conf_set_flag_slot,
		NGX_HTTP_LOC_CONF_OFFSET,
		offsetof(ngx_http_dypp_filter_loc_conf_t, dypp_key_generate),
		NULL 
	},

	ngx_null_command
};

static ngx_http_module_t ngx_dynamic_proxy_pass_module_ctx = {
	ngx_http_dypp_preconfig,
	NULL,
	ngx_http_dypp_create_main_conf,
	ngx_http_dypp_init_main_conf,
	ngx_http_dypp_create_srv_conf,
	NULL,
	ngx_http_dypp_create_loc_conf,
	ngx_http_dypp_merge_loc_conf
};

static ngx_http_module_t  ngx_dynamic_proxy_pass_filter_module_ctx = {
	NULL,           /* preconfiguration */
	ngx_dynamic_proxy_pass_filter_init,             /* postconfiguration */

	NULL,                                  /* create main configuration */
	NULL,                                  /* init main configuration */

	NULL,                                  /* create server configuration */
	NULL,                                  /* merge server configuration */

	ngx_dynamic_proxy_pass_filter_create_conf,             /* create location configuration */
	ngx_dynamic_proxy_pass_filter_merge_conf               /* merge location configuration */
};

ngx_module_t ngx_dynamic_proxy_pass_module = {
	NGX_MODULE_V1,
	&ngx_dynamic_proxy_pass_module_ctx,
	ngx_dynamic_proxy_pass_module_commands,
	NGX_HTTP_MODULE,
	NULL,
	NULL,
	&ngx_http_dypp_init_process,
	NULL,
	NULL,
	NULL,
	NULL,
	NGX_MODULE_V1_PADDING
};

ngx_module_t  ngx_dynamic_proxy_pass_filter_module = {
	NGX_MODULE_V1,
	&ngx_dynamic_proxy_pass_filter_module_ctx,      /* module context */
	ngx_dynamic_proxy_pass_filter_commands,         /* module directives */
	NGX_HTTP_MODULE,                       /* module type */
	NULL,                                  /* init master */
	NULL,                                  /* init module */
	NULL,                                  /* init process */
	NULL,                                  /* init thread */
	NULL,                                  /* exit thread */
	NULL,                                  /* exit process */
	NULL,                                  /* exit master */
	NGX_MODULE_V1_PADDING
};

static ngx_int_t ngx_http_dypp_init_process(ngx_cycle_t* cycle) {

	l_vm[0].L = luaL_newstate();
	if (l_vm[0].L == NULL) {
		ngx_log_error(NGX_LOG_ERR, cycle->log, 0, "Can not init lua");
		return NGX_ERROR;
	}
	luaL_openlibs(l_vm[0].L);
	lua_register(l_vm[0].L, "get_cookie", get_cookie);
	lua_register(l_vm[0].L, "get_upstream_list", get_upstream_list);
	lua_register(l_vm[0].L, "get_ngx_http_variable", get_ngx_http_variable);
	if (luaL_loadfile(l_vm[0].L, DEFAULT_LB_LUA_FILE)
			|| lua_pcall(l_vm[0].L, 0, 0, 0)) {
		ngx_log_error(NGX_LOG_ERR, cycle->log, 0, "Can not init lua file:%s",
				DEFAULT_LB_LUA_FILE);
		return NGX_ERROR;
	}
	l_vm[0].name.len = ngx_strlen(DEFAULT_LB_LUA_FILE);
	l_vm[0].name.data = ngx_pcalloc(cycle->pool, l_vm[0].name.len + 1);
	if (!l_vm[0].name.data) {
		return NGX_ERROR;
	}
	ngx_memcpy(l_vm[0].name.data, DEFAULT_LB_LUA_FILE, l_vm[0].name.len);


	ngx_http_dypp_add_timers(cycle);
	return NGX_OK;
}

static void* ngx_http_dypp_create_loc_conf(ngx_conf_t* cf) {
	ngx_http_dypp_loc_conf_t* conf;

	conf = ngx_pcalloc(cf->pool, sizeof(ngx_http_dypp_loc_conf_t));
	if (conf == NULL) {
		return NGX_CONF_ERROR;
	}
	conf->dp_domain.len = 0;
	conf->dp_domain.data = NULL;
	conf->dypp_lua_file.data = NULL;
	conf->dypp_lua_file.len = 0;
	conf->dypp_key.data = NULL;
	conf->dypp_key.len = 0;

	return conf;
}

static char* ngx_http_dypp_merge_loc_conf(ngx_conf_t *cf, void *parent, void *child){
	ngx_http_dypp_loc_conf_t *prev = parent;
	ngx_http_dypp_loc_conf_t *conf = child;

	ngx_conf_merge_str_value(conf->dp_domain, prev->dp_domain, "");
	ngx_conf_merge_str_value(conf->dypp_lua_file, prev->dypp_lua_file, "");
	ngx_conf_merge_str_value(conf->dypp_key, prev->dypp_key, "");

	ngx_log_error(NGX_LOG_NOTICE, cf->log, 0, "dp_domain:%V, dypp_lua_file:%V, dypp_key:%V",
			&conf->dp_domain, &conf->dypp_lua_file, &conf->dypp_key);

	return NGX_CONF_OK;
}

static ngx_int_t
ngx_http_dypp_preconfig(ngx_conf_t *cf){
	ngx_http_variable_t           *var;
	ngx_str_t name;

	char* char_name = "dp_upstream";
	name.len = strlen(char_name);
	name.data = ngx_pcalloc(cf->pool, name.len);
	ngx_memcpy(name.data, char_name, name.len);

	var = ngx_http_add_variable(cf, &name, NGX_HTTP_VAR_CHANGEABLE);
	if (var == NULL) {
		return NGX_ERROR;
	}
	//设置回调
	var->get_handler = ngx_http_dypp_get_variable;
	//var->data = (uintptr_t) ctx;
	return NGX_OK;
}

static int get_cookie(lua_State *L) {
	ngx_str_t cookie_name;
	cookie_name.data = (u_char*)lua_tolstring(L, 1, &cookie_name.len);
	ngx_str_t *value = ngx_pnalloc(cur_r->pool, sizeof(ngx_str_t));
	ngx_http_parse_multi_header_lines(&cur_r->headers_in.cookies, &cookie_name, value);
	lua_pushlstring(L, (char*)value->data, value->len);
	ngx_log_debug1(NGX_LOG_DEBUG, cur_r->connection->log, 0, "parameter from lua %V", &cookie_name);
	return 1;	
}

static int get_ngx_http_variable(lua_State *L) {
	u_char *lowcase;
	ngx_uint_t hash;
	ngx_str_t name;
	ngx_http_variable_value_t *vv;
	ngx_http_dypp_loc_conf_t *hdlc;
	ngx_str_t dypp_key;
	//	p = (u_char*)lua_tolstring(L, 1, &len);
	
	hdlc = ngx_http_get_module_loc_conf(cur_r, ngx_dynamic_proxy_pass_module);
	if(hdlc->dypp_key.len == 0 || hdlc->dypp_key.data == NULL){
		dypp_key.len = ngx_strlen("cookie_") + ngx_strlen(DEFAULT_DYPP_KEY);
		dypp_key.data = ngx_pcalloc(cur_r->pool, dypp_key.len + 1);
		ngx_sprintf(dypp_key.data, "%s%s", "cookie_", DEFAULT_DYPP_KEY);
	}
	else{
		dypp_key.len = ngx_strlen("cookie_") + hdlc->dypp_key.len;
		dypp_key.data = ngx_pcalloc(cur_r->pool, dypp_key.len + 1);
		ngx_sprintf(dypp_key.data, "%s%s", "cookie_", hdlc->dypp_key.data);
	}
	lowcase = ngx_pnalloc(cur_r->pool, dypp_key.len);
	hash = ngx_hash_strlow(lowcase, dypp_key.data, dypp_key.len);

	name.len = dypp_key.len;
	name.data = lowcase;
	vv = ngx_http_get_variable(cur_r, &name, hash);

	if(!vv || vv->len == 0 || !vv->data || vv->not_found == 1){
		//if none cookie 
		//generate id
		char buf[200];
		memset(buf, 0, 200);
		sprintf(buf, "%lu", generate_uid());
		lua_pushlstring(L, buf, ngx_strlen(buf));//TODO
		//one arg for lua
		return 1;
	}

	lua_pushlstring(L, (char*)vv->data, vv->len);
	return 1;
}

extern ngx_http_dypp_main_conf_t *dmcf_global;

typedef struct {
	ngx_str_t 	upstream_name;
	ngx_uint_t 	weight;
	ngx_uint_t  degrate_state;
    ngx_uint_t 	degrate_up_count;
    ngx_int_t 	force_state;
} ngx_http_dypp_upstream_helper;

static int get_upstream_list(lua_State *L) {

	ngx_http_upstream_degrades_shm_t	*udshm = dmcf_global->udshm;
	ngx_uint_t  i;
	ngx_int_t 	backup = -1, exact = -1, count = 0;
	u_char		use_backup = 0, is_upstream_add;
	ngx_http_upstream_degrade_shm_t * degrade = udshm->uds;
	ngx_str_t *upstream_name;
	ngx_array_t	*upstreams;
	ngx_http_dypp_upstream_helper *temp;
	ngx_log_t *log = cur_r->pool->log;

	upstreams = ngx_array_create(cur_r->pool, udshm->upstream_count, sizeof(ngx_http_dypp_upstream_helper));

	if(upstreams == NULL){
		goto fail;
	}

	for( i=0 ; i<udshm->upstream_count; i++){

		if(degrade[i].deleted){
			continue;
		}

		upstream_name = &degrade[i].upstream_name;

		if(ngx_strncmp(upstream_name->data, cur_dp_domain->data, cur_dp_domain->len) == 0) {

			is_upstream_add = 0;
			if(ngx_strncmp(upstream_name->data, cur_dp_domain->data, upstream_name->len) == 0) {
				exact = count;
				is_upstream_add = 1;
				ngx_log_error(NGX_LOG_INFO, log, 0, "[get_upstream_list]found exact: %ui", exact);
			}else if(upstream_name->data[cur_dp_domain->len] == '#') {
				//ab test
				if(upstream_name->len == cur_dp_domain->len + UPSTREA_DEGRATE_BACKUP_NAME_LENGTH + 1){
					if(ngx_strncmp(&upstream_name->data[cur_dp_domain->len + 1], UPSTREA_DEGRATE_BACKUP_NAME, UPSTREA_DEGRATE_BACKUP_NAME_LENGTH) == 0){
						//backup
						backup = count;
						is_upstream_add = 1;
						ngx_log_error(NGX_LOG_INFO, log, 0, "[get_upstream_list]found backup: %ui", backup);
					}
				}
			}else if(upstream_name->data[cur_dp_domain->len] == '@'){
				//ab test
				is_upstream_add = 1;
			}

			if(!is_upstream_add){
				//名字前缀相同，但不代表特殊含义
				continue;
			}
			temp = ngx_array_push(upstreams);
			temp->upstream_name.data = ngx_palloc(cur_r->pool, upstream_name->len);
			if(temp->upstream_name.data == NULL){
				goto fail;
			}
			temp->upstream_name.len = upstream_name->len;
			ngx_memcpy(temp->upstream_name.data, upstream_name->data, upstream_name->len);
			temp->weight = degrade[i].server_count;
			temp->degrate_up_count = degrade[i].degrate_up_count;
			temp->degrate_state =degrade[i].degrate_state;
			temp->force_state = degrade[i].force_state;
			count++;
		}
	}

	if(count == 0){
		ngx_log_error(NGX_LOG_ERR, log, 0, "[get_upstream_list]find none upstreams for %V!!", cur_dp_domain);
		goto fail;
	}

    temp = upstreams->elts;


    if(temp[exact].force_state == UPSTREAM_DEGRADE_FORCE_DOWN){
    	//强制降级
    	if(backup != -1){
    		use_backup = 1;
    	}
    }else if(temp[exact].force_state == UPSTREAM_DEGRADE_FORCE_UP){
    	//强制升级
    	use_backup = 0;
    }else{
    	//自动
        if(backup != -1 && exact != -1 && !temp[exact].degrate_state && temp[backup].degrate_up_count > 0){
        	//降级
    		ngx_log_error(NGX_LOG_DEBUG, log, 0, "[get_upstream_list]degrade to: %V", &temp[backup].upstream_name);
        	use_backup = 1;
        }
    }

    count = 0;
    for(i=0 ; i<upstreams->nelts ;i++){

    	if( (ngx_int_t)i == exact && use_backup){
    		continue;
    	}
    	if( (ngx_int_t)i == backup && !use_backup){
    		continue;
    	}
    	lua_pushlstring(L, (char*)temp[i].upstream_name.data, temp[i].upstream_name.len);
    	lua_pushinteger(L, temp[i].weight);
    	count++;
    }

    return 2*count;


fail:
	lua_pushlstring(L, (char*)cur_dp_domain->data, cur_dp_domain->len);
	lua_pushinteger(L, 1);
	return 2;
}

static u_char* call_lua(ngx_http_request_t *r, lua_State *L) {
	cur_r = r;
	lua_getglobal(L, "choose_upstream");
	lua_pcall(L, 0, 0, 0);
	// TODO can we use lua_tostring(L, -1) and is it faster?
	lua_getglobal(L, "upstream");
	const char *lua_result = lua_tostring(L, -1);
	if(!lua_result){
		return NULL;
	}
	char* chosen_upstream = ngx_pcalloc(r->pool, strlen(lua_result) + 1);
	strcpy(chosen_upstream, lua_result);
	ngx_log_debug1(NGX_LOG_DEBUG, r->connection->log, 0, "[dypp] lua result %s", chosen_upstream);
	//lua_close(L);
	lua_pop(L, 1);
	return (u_char*)chosen_upstream;
}

ngx_int_t ngx_http_dypp_get_variable (ngx_http_request_t *r, ngx_http_variable_value_t *v, uintptr_t data){
	ngx_http_dypp_loc_conf_t *hdlc;
	hdlc = ngx_http_get_module_loc_conf(r, ngx_dynamic_proxy_pass_module);
	if(hdlc->L == NULL){
		if ((char*)hdlc->dypp_lua_file.data == NULL || hdlc->dypp_lua_file.len == 0){
			hdlc->L = l_vm[0].L;
		}
		else{
			int i = 0;
			while(l_vm[i].name.data != NULL && l_vm[i].name.len != 0){
				if(l_vm[i].name.len ==  hdlc->dypp_lua_file.len && ngx_strncmp(l_vm[i].name.data, hdlc->dypp_lua_file.data, l_vm[i].name.len) == 0){
					hdlc->L = l_vm[i].L;
				}
				i++;
			}
			if(!hdlc->L){
				hdlc->L = luaL_newstate();
				if(hdlc->L == NULL) {
					ngx_log_error(NGX_LOG_ERR, r->connection->log, 0, "Can not init lua");
					return NGX_ERROR;
				}
				luaL_openlibs(hdlc->L);
				lua_register(hdlc->L, "get_cookie", get_cookie);
				lua_register(hdlc->L, "get_upstream_list", get_upstream_list);
				lua_register(hdlc->L, "get_ngx_http_variable", get_ngx_http_variable);

				if (luaL_loadfile(hdlc->L, (char*)hdlc->dypp_lua_file.data) || lua_pcall(hdlc->L,0,0,0)) {
					return NGX_ERROR;
				}
				l_vm[i].L = hdlc->L;
				l_vm[i].name.data = hdlc->dypp_lua_file.data;
				l_vm[i].name.len = hdlc->dypp_lua_file.len;
			}
		}
	}
	cur_dp_domain = &hdlc->dp_domain;

	u_char *chosen_upstream = call_lua(r, hdlc->L);
	if(!chosen_upstream){
		return NGX_ERROR;
	}

	v->len = strlen((char*)chosen_upstream);
	v->data = (u_char*)chosen_upstream;
	v->valid = 1;

	return NGX_OK;
}

static ngx_int_t
ngx_dynamic_proxy_pass_filter_init(ngx_conf_t *cf)
{

	ngx_http_next_header_filter = ngx_http_top_header_filter;
	ngx_http_top_header_filter = ngx_dynamic_proxy_pass_header_filter;

	return NGX_OK;
}

static ngx_int_t ngx_dynamic_proxy_pass_header_filter(ngx_http_request_t *r){
	ngx_http_dypp_filter_loc_conf_t  *conf;
	u_char* data;
	ngx_http_dypp_loc_conf_t *hdlc;
	
	hdlc = ngx_http_get_module_loc_conf(r, ngx_dynamic_proxy_pass_module);
	conf = ngx_http_get_module_loc_conf(r, ngx_dynamic_proxy_pass_filter_module);

	if(conf->dypp_key_generate == 1){
		data = has_generate_uid(r);
		if(data != NULL){
			ngx_str_t key, value;
			key.data = ngx_pcalloc(r->pool, ngx_strlen("Set-Cookie") + 1);
			if(key.data == NULL){
				return ngx_http_next_header_filter(r);
			}
			ngx_memcpy(key.data, "Set-Cookie", ngx_strlen("Set-Cookie"));
			key.len = ngx_strlen("Set-Cookie");

			value.data = ngx_pcalloc(r->pool, 1000 + 1);
			if(value.data == NULL){
				return ngx_http_next_header_filter(r);
			}
			ngx_sprintf(value.data, "%s%s%s", hdlc->dypp_key.data, " = ", data);
			value.len = ngx_strlen(value.data);
			set_header(r, &key, &value);
		}

		return ngx_http_next_header_filter(r);

	}

	return ngx_http_next_header_filter(r);
}

	static void *
ngx_dynamic_proxy_pass_filter_create_conf(ngx_conf_t *cf)
{
	ngx_http_dypp_filter_loc_conf_t  *conf;

	conf = ngx_pcalloc(cf->pool, sizeof(ngx_http_dypp_filter_loc_conf_t));
	if (conf == NULL) {
		return NULL;
	}

	conf->dypp_key_generate = NGX_CONF_UNSET_UINT;
	return conf;
}


	static char *
ngx_dynamic_proxy_pass_filter_merge_conf(ngx_conf_t *cf, void *parent, void *child)
{
	ngx_http_dypp_filter_loc_conf_t *prev = parent;
	ngx_http_dypp_filter_loc_conf_t *conf = child;

	ngx_conf_merge_uint_value(conf->dypp_key_generate, prev->dypp_key_generate, 1);
	return NGX_CONF_OK;
}

static ngx_int_t set_header(ngx_http_request_t* r, ngx_str_t* key, ngx_str_t* value){
	ngx_table_elt_t             *h;
	ngx_list_part_t             *part;
	unsigned int i;
	int matched = 0;


	part = &r->headers_out.headers.part;
	h = part->elts;

	for (i = 0; /* void */; i++) {

		if (i >= part->nelts) {
			if (part->next == NULL) {
				break;
			}

			part = part->next;
			h = part->elts;
			i = 0;
		}

		if (h[i].hash == 0) {
			continue;
		}

		if (h[i].key.len == key->len && ngx_strncasecmp(h[i].key.data, key->data, h[i].key.len) == 0)
		{
			goto matched;
		}

		/* not matched */
		continue;

matched:
		if (value->len == 0 || matched) {
			h[i].value.len = 0;
			h[i].hash = 0;

		} else {
			h[i].value = *value;
			h[i].hash = ngx_hash_key_lc(key->data, key->len);
		}
		matched = 1;
	}

	if (matched || value->len == 0){
		return NGX_OK;
	}

	h = ngx_list_push(&r->headers_out.headers);
	if (h == NULL) {
		return NGX_ERROR;
	}

	if (value->len == 0) {
		h->hash = 0;
	} else {
		h->hash = ngx_hash_key_lc(key->data, key->len);
	}

	h->key = *key;
	h->value = *value;

	h->lowcase_key = ngx_pnalloc(r->pool, h->key.len);
	if (h->lowcase_key == NULL) {
		return NGX_ERROR;
	}

	ngx_strlow(h->lowcase_key, h->key.data, h->key.len);

	return NGX_OK;

}

static unsigned long generate_uid(){
	return ++uid;
}

static u_char* has_generate_uid(ngx_http_request_t* r){
	u_char *lowcase;
	u_char *data;
	ngx_uint_t hash;
	ngx_str_t name;
	ngx_http_variable_value_t *vv;
	ngx_http_dypp_loc_conf_t *hdlc;
	
	hdlc = ngx_http_get_module_loc_conf(r, ngx_dynamic_proxy_pass_module);

	lowcase = ngx_pcalloc(r->pool, hdlc->dypp_key.len + ngx_strlen("http_"));
	if(!lowcase){
		return NULL;
	}
	data = ngx_pcalloc(r->pool, hdlc->dypp_key.len + ngx_strlen("http_") + 1);
	if(!data){
		return NULL;
	}
	ngx_sprintf(data, "%s%s", "http_", hdlc->dypp_key.data);
	hash = ngx_hash_strlow(lowcase, data, hdlc->dypp_key.len + ngx_strlen("http_"));

	name.len = hdlc->dypp_key.len + ngx_strlen("http_");
	name.data = lowcase;
	vv = ngx_http_get_variable(r, &name, hash);
	if(vv->valid == 1 && vv->not_found == 0 && vv->data){
		return vv->data;
	}
	return NULL;
}
