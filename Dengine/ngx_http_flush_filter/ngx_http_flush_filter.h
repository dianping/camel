#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>
#include <ngx_http_upstream.h>
#include <ngx_regex.h>




typedef struct {

	size_t  flush_buff_size;

}ngx_http_flush_filter_loc_conf_t;

typedef struct {


	size_t  buff_size;

}ngx_http_flush_filter_ctx_t;



ngx_int_t   ngx_http_flush_filter_postconfiguration(ngx_conf_t *cf);
void *ngx_http_flush_filter_create_loc_conf(ngx_conf_t *cf);
char *ngx_http_flush_filter_merge_loc_conf(ngx_conf_t *cf, void *prev, void *conf);
