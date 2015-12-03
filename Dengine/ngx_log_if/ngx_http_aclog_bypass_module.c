#include <nginx.h>
#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>


typedef struct {
    ngx_array_t             *logs;
    ngx_open_file_cache_t   *open_file_cache;
    time_t                   open_file_cache_valid;
    ngx_uint_t               open_file_cache_min_uses;
#if (tengine_version >= 1002004)
    ngx_uint_t               escape;
#endif
#if (tengine_version >= 1004000)
    ngx_flag_t               log_empty_request;
#endif
    ngx_uint_t               off;
} ngx_http_log_loc_conf_t;


typedef struct {
    ngx_array_t             *codes;
    ngx_flag_t               log;
    ngx_flag_t               is_and;
} ngx_http_aclog_bypass_condition_t;


typedef struct {
    ngx_uint_t               log_off;
    ngx_array_t             *conditions;
    ngx_flag_t               complete;
} ngx_http_aclog_bypass_conf_t;


extern ngx_module_t          ngx_http_log_module;


static char *ngx_http_aclog_bypass(ngx_conf_t *cf, ngx_command_t *cmd,
    void *conf);
static ngx_int_t ngx_http_aclog_bypass_handler(ngx_http_request_t *r);
static ngx_int_t ngx_http_aclog_bypass_init(ngx_conf_t *cf);
static void *ngx_http_aclog_bypass_create_loc_conf(ngx_conf_t *cf);
static char *ngx_http_aclog_bypass_merge_loc_conf(ngx_conf_t *cf,
    void *parent, void *child);
static char *ngx_http_aclog_bypass_condition_value(ngx_conf_t *cf,
    ngx_http_aclog_bypass_condition_t *abc, ngx_str_t *value);
static char *ngx_http_aclog_bypass_condition(ngx_conf_t *cf,
    ngx_http_aclog_bypass_condition_t *abc);


static ngx_command_t ngx_http_aclog_bypass_commands[] = {

    { ngx_string("access_log_bypass_if"),
      NGX_HTTP_MAIN_CONF|NGX_HTTP_SRV_CONF|NGX_HTTP_LOC_CONF|NGX_CONF_1MORE,
      ngx_http_aclog_bypass,
      NGX_HTTP_LOC_CONF_OFFSET,
      0,
      NULL },

      ngx_null_command
};


static ngx_http_module_t ngx_http_aclog_bypass_module_ctx = {
    NULL,                                      /* preconfiguration */
    ngx_http_aclog_bypass_init,                /* postconfiguration */

    NULL,                                      /* create main configuration */
    NULL,                                      /* init main configuration */

    NULL,                                      /* create server configuration */
    NULL,                                      /* merge server configuration */

    ngx_http_aclog_bypass_create_loc_conf,     /* create location configuration */
    ngx_http_aclog_bypass_merge_loc_conf       /* merge location configuration */
};


ngx_module_t ngx_http_aclog_bypass_module = {
    NGX_MODULE_V1,
    &ngx_http_aclog_bypass_module_ctx,         /* module context */
    ngx_http_aclog_bypass_commands,            /* module directives */
    NGX_HTTP_MODULE,                           /* module type */
    NULL,                                      /* init master */
    NULL,                                      /* init module */
    NULL,                                      /* init process */
    NULL,                                      /* init thread */
    NULL,                                      /* exit thread */
    NULL,                                      /* exit process */
    NULL,                                      /* exit master */
    NGX_MODULE_V1_PADDING
};


static void *
ngx_http_aclog_bypass_create_loc_conf(ngx_conf_t *cf)
{
    return ngx_pcalloc(cf->pool, sizeof(ngx_http_aclog_bypass_conf_t));
}


static char *
ngx_http_aclog_bypass_merge_loc_conf(ngx_conf_t *cf, void *parent, void *child)
{
    uintptr_t                         *code;
    ngx_uint_t                         i;
    ngx_http_log_loc_conf_t           *lcf;
    ngx_http_aclog_bypass_conf_t      *conf = child;
    ngx_http_aclog_bypass_conf_t      *prev = parent;
    ngx_http_aclog_bypass_condition_t *condition;

    lcf = ngx_http_conf_get_module_loc_conf(cf, ngx_http_log_module);

    conf->log_off = lcf->off;

    if (prev->conditions) {
        condition = prev->conditions->elts;
        if (condition[prev->conditions->nelts - 1].is_and) {
            ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                               "can not use \"and\" flag on the last condition");
            return NGX_CONF_ERROR;
        }
    }

    if (lcf->off || lcf->logs == NULL) {
        return NGX_CONF_OK;
    }

    if (conf->conditions == NULL) {
        conf->conditions = prev->conditions;
    }

    if (conf->conditions == prev->conditions) {
        if (prev->conditions != NULL && !prev->complete) {
            prev->complete = 1;
            conf->complete = 1;
            condition = prev->conditions->elts;
            for (i = 0; i < prev->conditions->nelts; i++) {
                code = ngx_array_push_n(condition[i].codes, sizeof(uintptr_t));
                if (code == NULL) {
                    return NGX_CONF_ERROR;
                }

                *code = (uintptr_t) NULL;
            }
        }

        return NGX_CONF_OK;
    }

    conf->complete = 1;
    condition = conf->conditions->elts;
    for (i = 0; i < conf->conditions->nelts; i++) {
        code = ngx_array_push_n(condition[i].codes, sizeof(uintptr_t));
        if (code == NULL) {
            return NGX_CONF_ERROR;
        }

        *code = (uintptr_t) NULL;
    }

    if (condition[conf->conditions->nelts - 1].is_and) {
        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                           "can not use \"and\" flag on the last condition");
        return NGX_CONF_ERROR;
    }

    return NGX_CONF_OK;
}


static ngx_int_t
ngx_http_aclog_bypass_init(ngx_conf_t *cf)
{
    ngx_uint_t                    i;
    ngx_http_handler_pt          *h;
    ngx_http_core_main_conf_t    *cmcf;

    cmcf = ngx_http_conf_get_module_main_conf(cf, ngx_http_core_module);

    h = ngx_array_push(&cmcf->phases[NGX_HTTP_LOG_PHASE].handlers);
    if (h == NULL) {
        return NGX_ERROR;
    }

    for (i = 1; i < cmcf->phases[NGX_HTTP_LOG_PHASE].handlers.nelts; i++) {
        *h = *(h - 1);
        h--;
    }

    *h = ngx_http_aclog_bypass_handler;

    return NGX_OK;
}


static ngx_int_t
ngx_http_aclog_bypass_handler(ngx_http_request_t *r)
{
    ngx_uint_t                         i;
    ngx_http_log_loc_conf_t           *lcf;
    ngx_http_script_code_pt            code;
    ngx_http_script_engine_t           e;
    ngx_http_variable_value_t          stack[10];
    ngx_http_aclog_bypass_conf_t      *alcf;
    ngx_http_aclog_bypass_condition_t *condition;

    lcf = ngx_http_get_module_loc_conf(r, ngx_http_log_module);
    alcf = ngx_http_get_module_loc_conf(r, ngx_http_aclog_bypass_module);

    if (alcf->log_off || lcf->logs == NULL) {
        return NGX_DECLINED;
    }

    if (alcf->conditions == NULL) {
        lcf->off = 0;
        return NGX_OK;
    }

    condition = alcf->conditions->elts;
    for (i = 0; i < alcf->conditions->nelts; i++) {
        ngx_memzero(&e, sizeof(ngx_http_script_engine_t));
        ngx_memzero(&stack, sizeof(stack));
        e.ip = condition[i].codes->elts;
        e.request = r;
        e.quote = 1;
        e.log = condition[i].log;
        e.status = NGX_DECLINED;
        e.sp = stack;

        while (*(uintptr_t *) e.ip) {
            code = *(ngx_http_script_code_pt *) e.ip;
            code(&e);
        }

        e.sp--;
        if (e.sp->len && (e.sp->len != 1 || e.sp->data[0] != '0')) {
            if (!condition[i].is_and) {
                lcf->off = 1;
                return NGX_OK;
            }
        } else {
            while (condition[i].is_and) {
                i++;
            }
        }
    }

    lcf->off = 0;
    return NGX_OK;
}


static char *
ngx_http_aclog_bypass(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    ngx_str_t                             *value;
    ngx_http_aclog_bypass_conf_t          *alcf = conf;
    ngx_http_aclog_bypass_condition_t     *abc;

    if (alcf->conditions == NULL) {
        alcf->conditions = ngx_array_create(cf->pool, 7,
                                     sizeof(ngx_http_aclog_bypass_condition_t));
        if (alcf->conditions == NULL) {
            return NGX_CONF_ERROR;
        }
    }

    abc = ngx_array_push(alcf->conditions);
    if (abc == NULL) {
        return NGX_CONF_ERROR;
    }
    ngx_memzero(abc, sizeof(ngx_http_aclog_bypass_condition_t));

    value = cf->args->elts;
    if (ngx_strcmp(value[cf->args->nelts - 1].data, "and") == 0) {
        cf->args->nelts--;
        abc->is_and = 1;
    }

    if (ngx_http_aclog_bypass_condition(cf, abc) != NGX_CONF_OK) {
        return NGX_CONF_ERROR;
    }

    return NGX_CONF_OK;
}


static char *
ngx_http_aclog_bypass_condition_value(ngx_conf_t *cf,
    ngx_http_aclog_bypass_condition_t *abc, ngx_str_t *value)
{
    ngx_int_t                              n;
    ngx_http_script_compile_t              sc;
    ngx_http_script_value_code_t          *val;
    ngx_http_script_complex_value_code_t  *complex;

    n = ngx_http_script_variables_count(value);

    if (n == 0) {
        val = ngx_http_script_start_code(cf->pool, &abc->codes,
                                         sizeof(ngx_http_script_value_code_t));
        if (val == NULL) {
            return NGX_CONF_ERROR;
        }

        n = ngx_atoi(value->data, value->len);

        if (n == NGX_ERROR) {
            n = 0;
        }

        val->code = ngx_http_script_value_code;
        val->value = (uintptr_t) n;
        val->text_len = (uintptr_t) value->len;
        val->text_data = (uintptr_t) value->data;

        return NGX_CONF_OK;
    }

    complex = ngx_http_script_start_code(cf->pool, &abc->codes,
                                 sizeof(ngx_http_script_complex_value_code_t));
    if (complex == NULL) {
        return NGX_CONF_ERROR;
    }

    complex->code = ngx_http_script_complex_value_code;
    complex->lengths = NULL;

    ngx_memzero(&sc, sizeof(ngx_http_script_compile_t));

    sc.cf = cf;
    sc.source = value;
    sc.lengths = &complex->lengths;
    sc.values = &abc->codes;
    sc.variables = n;
    sc.complete_lengths = 1;

    if (ngx_http_script_compile(&sc) != NGX_OK) {
        return NGX_CONF_ERROR;
    }

    return NGX_CONF_OK;
}


static char *
ngx_http_aclog_bypass_condition(ngx_conf_t *cf,
    ngx_http_aclog_bypass_condition_t *abc)
{
    u_char                        *p;
    size_t                         len;
    ngx_str_t                     *value;
    ngx_uint_t                     cur, last;
    ngx_http_script_code_pt       *code;
    ngx_http_script_file_code_t   *fop;
#if (NGX_PCRE)
    ngx_regex_compile_t            rc;
    ngx_http_script_regex_code_t  *regex;
    u_char                         errstr[NGX_MAX_CONF_ERRSTR];
#endif

    value = cf->args->elts;
    last = cf->args->nelts - 1;

    if (value[1].len < 1 || value[1].data[0] != '(') {
        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                           "invalid condition \"%V\"", &value[1]);
        return NGX_CONF_ERROR;
    }

    if (value[1].len == 1) {
        cur = 2;

    } else {
        cur = 1;
        value[1].len--;
        value[1].data++;
    }

    if (value[last].len < 1 || value[last].data[value[last].len - 1] != ')') {
        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                           "invalid condition \"%V\"", &value[last]);
        return NGX_CONF_ERROR;
    }

    if (value[last].len == 1) {
        last--;

    } else {
        value[last].len--;
        value[last].data[value[last].len] = '\0';
    }

    len = value[cur].len;
    p = value[cur].data;

    if (len > 1 && p[0] == '$') {

        if (cur != last && cur + 2 != last) {
            ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                               "invalid condition \"%V\"", &value[cur]);
            return NGX_CONF_ERROR;
        }

        if (ngx_http_aclog_bypass_condition_value(cf, abc, &value[cur])
            != NGX_CONF_OK)
        {
            return NGX_CONF_ERROR;
        }

        if (cur == last) {
            return NGX_CONF_OK;
        }

        cur++;

        len = value[cur].len;
        p = value[cur].data;

        if (len == 1 && p[0] == '=') {

            if (ngx_http_aclog_bypass_condition_value(cf, abc, &value[last])
                != NGX_CONF_OK)
            {
                return NGX_CONF_ERROR;
            }

            code = ngx_http_script_start_code(cf->pool, &abc->codes,
                                              sizeof(uintptr_t));
            if (code == NULL) {
                return NGX_CONF_ERROR;
            }

            *code = ngx_http_script_equal_code;

            return NGX_CONF_OK;
        }

        if (len == 2 && p[0] == '!' && p[1] == '=') {

            if (ngx_http_aclog_bypass_condition_value(cf, abc, &value[last])
                != NGX_CONF_OK)
            {
                return NGX_CONF_ERROR;
            }

            code = ngx_http_script_start_code(cf->pool, &abc->codes,
                                              sizeof(uintptr_t));
            if (code == NULL) {
                return NGX_CONF_ERROR;
            }

            *code = ngx_http_script_not_equal_code;
            return NGX_CONF_OK;
        }

        if ((len == 1 && p[0] == '~')
            || (len == 2 && p[0] == '~' && p[1] == '*')
            || (len == 2 && p[0] == '!' && p[1] == '~')
            || (len == 3 && p[0] == '!' && p[1] == '~' && p[2] == '*'))
        {
#if (NGX_PCRE)
            regex = ngx_http_script_start_code(cf->pool, &abc->codes,
                                         sizeof(ngx_http_script_regex_code_t));
            if (regex == NULL) {
                return NGX_CONF_ERROR;
            }

            ngx_memzero(regex, sizeof(ngx_http_script_regex_code_t));

            ngx_memzero(&rc, sizeof(ngx_regex_compile_t));

            rc.pattern = value[last];
            rc.options = (p[len - 1] == '*') ? NGX_REGEX_CASELESS : 0;
            rc.err.len = NGX_MAX_CONF_ERRSTR;
            rc.err.data = errstr;

            regex->regex = ngx_http_regex_compile(cf, &rc);
            if (regex->regex == NULL) {
                return NGX_CONF_ERROR;
            }

            regex->code = ngx_http_script_regex_start_code;
            regex->next = sizeof(ngx_http_script_regex_code_t);
            regex->test = 1;
            if (p[0] == '!') {
                regex->negative_test = 1;
            }
            regex->name = value[last];

            return NGX_CONF_OK;
#else
            ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                               "using regex \"%V\" requires PCRE library",
                               &value[last]);
            return NGX_CONF_ERROR;
#endif
        }

        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                           "unexpected \"%V\" in condition", &value[cur]);
        return NGX_CONF_ERROR;

    } else if ((len == 2 && p[0] == '-')
               || (len == 3 && p[0] == '!' && p[1] == '-'))
    {
        if (cur + 1 != last) {
            ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                               "invalid condition \"%V\"", &value[cur]);
            return NGX_CONF_ERROR;
        }

        value[last].data[value[last].len] = '\0';
        value[last].len++;

        if (ngx_http_aclog_bypass_condition_value(cf, abc, &value[last])
            != NGX_CONF_OK)
        {
            return NGX_CONF_ERROR;
        }

        fop = ngx_http_script_start_code(cf->pool, &abc->codes,
                                          sizeof(ngx_http_script_file_code_t));
        if (fop == NULL) {
            return NGX_CONF_ERROR;
        }

        fop->code = ngx_http_script_file_code;

        if (p[1] == 'f') {
            fop->op = ngx_http_script_file_plain;
            return NGX_CONF_OK;
        }

        if (p[1] == 'd') {
            fop->op = ngx_http_script_file_dir;
            return NGX_CONF_OK;
        }

        if (p[1] == 'e') {
            fop->op = ngx_http_script_file_exists;
            return NGX_CONF_OK;
        }

        if (p[1] == 'x') {
            fop->op = ngx_http_script_file_exec;
            return NGX_CONF_OK;
        }

        if (p[0] == '!') {
            if (p[2] == 'f') {
                fop->op = ngx_http_script_file_not_plain;
                return NGX_CONF_OK;
            }

            if (p[2] == 'd') {
                fop->op = ngx_http_script_file_not_dir;
                return NGX_CONF_OK;
            }

            if (p[2] == 'e') {
                fop->op = ngx_http_script_file_not_exists;
                return NGX_CONF_OK;
            }

            if (p[2] == 'x') {
                fop->op = ngx_http_script_file_not_exec;
                return NGX_CONF_OK;
            }
        }

        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                           "invalid condition \"%V\"", &value[cur]);
        return NGX_CONF_ERROR;
    }

    ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
                       "invalid condition \"%V\"", &value[cur]);

    return NGX_CONF_ERROR;
}

