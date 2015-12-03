#include "ngx_http_flush_filter.h"

static ngx_command_t ngx_http_flush_filter_commands[] = {
	{
        ngx_string("flush_buffer_size"), // The command name
        NGX_HTTP_MAIN_CONF  | NGX_HTTP_SRV_CONF | NGX_HTTP_LOC_CONF| NGX_CONF_TAKE1,
        ngx_conf_set_size_slot, // The command handler
        NGX_HTTP_LOC_CONF_OFFSET,
        offsetof(ngx_http_flush_filter_loc_conf_t, flush_buff_size),
        NULL
    }
};

static ngx_http_module_t ngx_http_flush_filter_module_ctx = {
    NULL,
    ngx_http_flush_filter_postconfiguration,
    NULL,
    NULL,
    NULL,
    NULL,
    ngx_http_flush_filter_create_loc_conf,
    ngx_http_flush_filter_merge_loc_conf
};

ngx_module_t ngx_http_flush_filter_module = {
    NGX_MODULE_V1,
    &ngx_http_flush_filter_module_ctx,
    ngx_http_flush_filter_commands,
    NGX_HTTP_MODULE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NGX_MODULE_V1_PADDING
};

static ngx_http_output_header_filter_pt  ngx_http_next_header_filter;
static ngx_http_output_body_filter_pt    ngx_http_next_body_filter;


void *ngx_http_flush_filter_create_loc_conf(ngx_conf_t *cf){

	ngx_http_flush_filter_loc_conf_t  *fflc;

	fflc = ngx_palloc(cf->pool, sizeof(ngx_http_flush_filter_loc_conf_t));
	if(fflc == NULL){
		return NULL;
	}

	fflc->flush_buff_size = NGX_CONF_UNSET;

	return fflc;
}

char *ngx_http_flush_filter_merge_loc_conf(ngx_conf_t *cf, void *prev, void *conf){

	ngx_http_flush_filter_loc_conf_t  *parent = prev, *child = conf;

	ngx_conf_merge_size_value(child->flush_buff_size, parent->flush_buff_size, 0);

	return NGX_CONF_OK;
}

static ngx_int_t
ngx_http_flush_header_filter(ngx_http_request_t *r)
{

	ngx_http_flush_filter_ctx_t *ctx = ngx_pcalloc(r->pool, sizeof(ngx_http_flush_filter_ctx_t));
    if (ctx == NULL) {
        return NGX_ERROR;
    }

    ctx->buff_size = 0;
    ngx_http_set_ctx(r, ctx, ngx_http_flush_filter_module);


    return ngx_http_next_header_filter(r);
}

static ngx_int_t
ngx_http_flush_body_filter(ngx_http_request_t *r, ngx_chain_t *in)
{
	ngx_buf_t *buf;
	ngx_chain_t *chain_in = in;
	ngx_http_flush_filter_loc_conf_t *fflc;
	ngx_http_flush_filter_ctx_t *ffctx;

	fflc  = ngx_http_get_module_loc_conf(r, ngx_http_flush_filter_module);
	ffctx = ngx_http_get_module_ctx(r, ngx_http_flush_filter_module);

	if(fflc->flush_buff_size <= 0){
		return ngx_http_next_body_filter(r, in);
	}


	while(chain_in){

		buf = chain_in->buf;
		ffctx->buff_size += buf->last - buf->pos;
		ngx_log_error(NGX_LOG_INFO, r->connection->log, 0, "buff_size: %d", ffctx->buff_size);

		if( ffctx->buff_size >= fflc->flush_buff_size){

			ngx_log_error(NGX_LOG_INFO, r->connection->log, 0, "set flush, %d", ffctx->buff_size);

			ffctx->buff_size = 0;
			ffctx->buff_size = 0;
			//set flush
			buf->flush = 1;
		}

		chain_in = chain_in->next;
	}

	return ngx_http_next_body_filter(r, in);

}


ngx_int_t   ngx_http_flush_filter_postconfiguration(ngx_conf_t *cf){


	ngx_http_next_header_filter = ngx_http_top_header_filter;
	ngx_http_top_header_filter = ngx_http_flush_header_filter;

	ngx_http_next_body_filter = ngx_http_top_body_filter;
	ngx_http_top_body_filter = ngx_http_flush_body_filter;

	return NGX_OK;
}


