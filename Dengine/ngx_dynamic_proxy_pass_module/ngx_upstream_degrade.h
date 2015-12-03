#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>

#define UPSTREA_DEGRATE_BACKUP_NAME "BACKUP"
#define UPSTREA_DEGRATE_BACKUP_NAME_LENGTH 6

#define UPSTREA_DEGRATE_DEFAULT_RATE 60

#define UPSTREAM_DEGRADE_FORCE_UP 1
#define UPSTREAM_DEGRADE_FORCE_DOWN -1
#define UPSTREAM_DEGRADE_FORCE_AUTO 0

#define UPSTREAM_DEGRADE_STATE_ON 1
#define UPSTREAM_DEGRADE_STATE_OFF 0

#define UPSTREAM_DEGRADE_SHM_NAME_LENGTH 256
#define UPSTREAM_DEGRADE_SHM_MAX_DYNAMIC_UPSTREAM_SIZE 4096

#define UPSTREAM_DEGRADE_INTERVAL 5000

void * ngx_http_dypp_create_main_conf(ngx_conf_t *cf);
char * ngx_http_dypp_init_main_conf(ngx_conf_t *cf, void *conf);
void * ngx_http_dypp_create_srv_conf(ngx_conf_t *cf);
char * ngx_http_dypp_set_degrade_rate(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
char *ngx_http_dypp_set_degrade_force_state(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
char * ngx_http_upstream_degrade_interface(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);

char * ngx_http_dypp_degrade_shm_size(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);

ngx_int_t ngx_http_dypp_add_timers(ngx_cycle_t *cycle);

void ngx_http_upstream_degrade_timer(ngx_event_t *ev);

//降级用，比如有两个pool：
//proxy_pass pool{}
//proxy_pass pool@BACKUP
//如果pool可用比例降到一定比例，则切换到backup

typedef struct {
	
    ngx_rbtree_node_t         node;
    ngx_str_t                 str;//upstream name，作为rbtree的节点

    u_char	deleted;

    /**
     * 支持强制升级、降级、自动三种模式，默认自动
     */
    ngx_int_t force_state;

	ngx_str_t  upstream_name;
	/**
	 * 如果比例大于等于rate，则正常 1
	 * 否则，异常 0
	 */
	ngx_uint_t  degrate_state;

	ngx_uint_t 	degrade_rate;

	ngx_uint_t  degrate_up_count;

	ngx_uint_t	server_count;

    //此upstream是否配置健康监测
    u_char				  		upstream_checked;
} ngx_http_upstream_degrade_shm_t;

typedef struct {

	ngx_uint_t  		generation;
    ngx_shmtx_t         mutex;
    ngx_shmtx_sh_t      lock;
    ngx_uint_t 			upstream_count;

    ngx_rbtree_t 		tree;
    ngx_rbtree_node_t	sentinel;
	ngx_http_upstream_degrade_shm_t uds[1];
} ngx_http_upstream_degrades_shm_t;

typedef struct {

    ngx_slab_pool_t					*shpool;
	/**
	 * 计算degrade的时间间隔
	 */
	ngx_msec_t	degrade_interval;

	ngx_uint_t degrade_shm_size;	

	ngx_uint_t static_upstream_size;

	ngx_http_upstream_degrades_shm_t *udshm;

	ngx_http_conf_ctx_t  *ctx;

} ngx_http_dypp_main_conf_t;

typedef struct {

	ngx_uint_t  degrade_rate;
	ngx_uint_t  degrade_force_state;

} ngx_http_dypp_srv_conf_t;




//upstream_check里面的数据结构
typedef struct {
    ngx_shmtx_t                              mutex;
    ngx_shmtx_sh_t                           lock;

    ngx_pid_t                                owner;

    ngx_msec_t                               access_time;

    ngx_uint_t                               fall_count;
    ngx_uint_t                               rise_count;

    ngx_uint_t                               busyness;
    ngx_uint_t                               access_count;

    struct sockaddr                         *sockaddr;
    socklen_t                                socklen;

    ngx_int_t                                ref;
    ngx_uint_t                               delete;

    ngx_atomic_t                             down;

    u_char                                   padding[64];
} ngx_http_upstream_check_peer_shm_t;


typedef struct {
    ngx_uint_t                               generation;
    ngx_uint_t                               checksum;
    ngx_uint_t                               number;
    ngx_uint_t                               max_number;

    /* ngx_http_upstream_check_status_peer_t */
    ngx_http_upstream_check_peer_shm_t       peers[1];
} ngx_http_upstream_check_peers_shm_t;

typedef struct {
    ngx_str_t                                check_shm_name;
    ngx_uint_t                               checksum;
    ngx_array_t                              peers;
    ngx_slab_pool_t                         *shpool;

    ngx_http_upstream_check_peers_shm_t     *peers_shm;
} ngx_http_upstream_check_peers_t;

typedef struct ngx_http_upstream_check_srv_conf_s
    ngx_http_upstream_check_srv_conf_t;
typedef struct ngx_http_upstream_check_peer_s ngx_http_upstream_check_peer_t;

typedef ngx_int_t (*ngx_http_upstream_check_packet_init_pt)
    (ngx_http_upstream_check_peer_t *peer);
typedef ngx_int_t (*ngx_http_upstream_check_packet_parse_pt)
    (ngx_http_upstream_check_peer_t *peer);
typedef void (*ngx_http_upstream_check_packet_clean_pt)
    (ngx_http_upstream_check_peer_t *peer);

typedef struct {
    ngx_uint_t                               type;

    ngx_str_t                                name;

    ngx_str_t                                default_send;

    /* HTTP */
    ngx_uint_t                               default_status_alive;

    ngx_event_handler_pt                     send_handler;
    ngx_event_handler_pt                     recv_handler;

    ngx_http_upstream_check_packet_init_pt   init;
    ngx_http_upstream_check_packet_parse_pt  parse;
    ngx_http_upstream_check_packet_clean_pt  reinit;

    unsigned need_pool;
    unsigned need_keepalive;
} ngx_check_conf_t;

struct ngx_http_upstream_check_srv_conf_s {
    ngx_uint_t                               port;
    ngx_uint_t                               fall_count;
    ngx_uint_t                               rise_count;
    ngx_msec_t                               check_interval;
    ngx_msec_t                               check_timeout;
    ngx_uint_t                               check_keepalive_requests;

    ngx_check_conf_t                        *check_type_conf;
    ngx_str_t                                send;

    union {
        ngx_uint_t                           return_code;
        ngx_uint_t                           status_alive;
    } code;

    ngx_uint_t                               default_down;
	ngx_str_t								 html_pattern;
    ngx_uint_t                               unique;
};


struct ngx_http_upstream_check_peer_s {
    ngx_flag_t                               state;
    ngx_pool_t                              *pool;
    ngx_uint_t                               index;
    ngx_uint_t                               max_busy;
    ngx_str_t                               *upstream_name;
    ngx_addr_t                              *check_peer_addr;
    ngx_addr_t                              *peer_addr;
    ngx_event_t                              check_ev;
    ngx_event_t                              check_timeout_ev;
    ngx_peer_connection_t                    pc;

    void                                    *check_data;
    ngx_event_handler_pt                     send_handler;
    ngx_event_handler_pt                     recv_handler;

    ngx_http_upstream_check_packet_init_pt   init;
    ngx_http_upstream_check_packet_parse_pt  parse;
    ngx_http_upstream_check_packet_clean_pt  reinit;

    ngx_http_upstream_check_peer_shm_t      *shm;
    ngx_http_upstream_check_srv_conf_t      *conf;

    ngx_http_upstream_srv_conf_t			*uscf;

	ngx_regex_compile_t                     *html_pattern_ngx_regex;

    unsigned                                 delete;
};
//upstream_check
