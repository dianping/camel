#ifndef _NGX_HTTP_DYUPS_H_INCLUDE_
#define _NGX_HTTP_DYUPS_H_INCLUDE_


#include <ngx_config.h>
#include <ngx_core.h>


ngx_int_t ngx_dyups_update_upstream(ngx_str_t *name, ngx_buf_t *buf,
    ngx_str_t *rv);

ngx_int_t ngx_dyups_delete_upstream(ngx_str_t *name, ngx_str_t *rv);


extern ngx_flag_t ngx_http_dyups_api_enable;

typedef struct {
    ngx_uint_t                     idx;
    ngx_uint_t                    *ref;
    ngx_uint_t                     deleted;
    ngx_flag_t                     dynamic;
    ngx_pool_t                    *pool;
    ngx_http_conf_ctx_t           *ctx;
    ngx_http_upstream_srv_conf_t  *upstream;
} ngx_http_dyups_srv_conf_t;


typedef struct {
    ngx_flag_t                     enable;
    ngx_flag_t                     trylock;
    ngx_array_t                    dy_upstreams;/* ngx_http_dyups_srv_conf_t */
    ngx_str_t                      conf_path;
    ngx_str_t                      shm_name;
    ngx_uint_t                     shm_size;
    ngx_msec_t                     read_msg_timeout;
} ngx_http_dyups_main_conf_t;


typedef struct {
    ngx_uint_t                           ref;
    ngx_http_upstream_init_peer_pt       init;
} ngx_http_dyups_upstream_srv_conf_t;


typedef struct {
    void                                *data;
    ngx_http_dyups_upstream_srv_conf_t  *scf;
    ngx_event_get_peer_pt                get;
    ngx_event_free_peer_pt               free;
} ngx_http_dyups_ctx_t;


typedef struct ngx_dyups_status_s {
    ngx_pid_t                            pid;
    ngx_msec_t                           time;
} ngx_dyups_status_t;


typedef struct ngx_dyups_shctx_s {
    ngx_queue_t                          msg_queue;
    ngx_uint_t                           version;
    ngx_dyups_status_t                  *status;
} ngx_dyups_shctx_t;


typedef struct ngx_dyups_global_ctx_s {
    ngx_event_t                          msg_timer;
    ngx_slab_pool_t                     *shpool;
    ngx_dyups_shctx_t                   *sh;
} ngx_dyups_global_ctx_t;


typedef struct ngx_dyups_msg_s {
    ngx_queue_t                          queue;
    ngx_str_t                            name;
    ngx_str_t                            content;
    ngx_int_t                            count;
    ngx_uint_t                           flag;
    ngx_pid_t                           *pid;
} ngx_dyups_msg_t;


#endif
