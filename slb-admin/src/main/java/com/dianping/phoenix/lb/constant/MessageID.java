package com.dianping.phoenix.lb.constant;

/**
 * @author Leo Liang
 *
 */
public enum MessageID {
	POOL_SAVE_FAIL("pool_save_fail"), //
	POOL_NAME_EMPTY("pool_name_empty"), //

	SLBPOOL_NAME_EMPTY("slbpool_name_empty"), //

	STRATEGY_SAVE_FAIL("strategy_save_fail"), //
	STRATEGY_NAME_EMPTY("strategy_name_empty"), //
	STRATEGY_TYPE_EMPTY("strategy_type_empty"), //

	SLBPOOL_SAVE_FAIL("slbpool_save_fail"), //
	SLBPOOL_NO_MEMBER("slbpool_no_member"), //
	SLBPOOL_MEMBER_NO_IP("slbpool_member_no_ip"), //

	MONITORRULE_ID_EMPTY("monitor_rule_id_empty"), //
	MONITORRULE_POOL_EMPTY("monitor_rule_pool_empty"), //
	MONITORRULE_STATUS_CODE_EMPTY("monitor_rule_status_code_empty"), //
	MONITORRULE_MINUTE_EMPTY("monitor_rule_minute_empty"), //
	MONITORRULE_VALUE_EMPTY("monitor_rule_value_empty"), //

	USER_ACCOUNT_EMPTY("user_account_empty"), //
	USER_NAME_EMPTY("user_name_empty"), //

	NGINX_STATUS_DATE_EMPTY("nginx_status_date_empty"), //

	VARIABLE_SAVE_FAIL("variable_save_fail"), //

	COMMON_ASPECT_SAVE_FAIL("common_aspect_save_fail"), //
	COMMON_ASPECT_NOT_FOUND("common_aspect_not_found"), //
	COMMON_ASPECT_NAME_EMPTY("common_aspect_name_empty"), //
	COMMON_ASPECT_REF_NOT_EMPTY("common_aspect_ref_not_empty"), //
	COMMON_ASPECT_POINTCUT_NULL("common_aspect_pointcut_null"), //
	//
	VIRTUALSERVER_CHECK_FAILED("vs_check_failed"), //
	VIRTUALSERVER_ALREADY_EXISTS("vs_already_exists"), //
	VIRTUALSERVER_SAVE_FAIL("vs_save_fail"), //
	VIRTUALSERVER_CONCURRENT_MOD("vs_concurrent_mod"), //
	VIRTUALSERVER_NOT_EXISTS("vs_not_exists"), //
	VIRTUALSERVER_DEL_FAIL("vs_del_fail"), //
	VIRTUALSERVER_NAME_EMPTY("vs_name_empty"), //
	VIRTUALSERVER_NO_DEFAULT_POOL_NAME("vs_no_default_pool_name"), //
	VIRTUALSERVER_PORT_EMPTY("vs_port_empty"), //
	VIRTUALSERVER_TAGID_EMPTY("vs_pushid_empty"), //
	VIRTUALSERVER_DEFAULTPOOL_NOT_EXISTS("vs_defaultpool_not_exists"), //
	VIRTUALSERVER_SLBPOOL_NOT_EXISTS("vs_slbpool_not_exists"), //
	VIRTUALSERVER_DIRECTIVE_TYPE_NOT_SUPPORT("vs_directive_type_not_support"), //
	VIRTUALSERVER_LOCATION_NO_DIRECTIVE("vs_location_no_directive"), //
	VIRTUALSERVER_LOCATION_NO_PATTERN("vs_location_no_pattern"), //
	VIRTUALSERVER_LOCATION_NO_MATCHTYPE("vs_location_no_matchtype"), //
	VIRTUALSERVER_TAG_FAIL("vs_tag_fail"), //
	VIRTUALSERVER_TAG_LOAD_FAIL("vs_tag_load_fail"), //
	VIRTUALSERVER_TAG_NOT_FOUND("vs_tag_not_found"), //
	VIRTUALSERVER_TAG_LIST_FAIL("vs_tag_list_fail"), //
	VIRTUALSERVER_MULTIVS_GENERATE_NGINX_CONFIG("multi_vs_gengrate_nginx_config"), //

	POOL_STRATEGY_NOT_SUPPORT("pool_strategy_not_support"), //
	POOL_LOWER_THAN_MINAVAIL_PCT("pool_lower_than_minavail_pct"), //
	POOL_NO_MEMBER("pool_no_member"), //
	POOL_MEMBER_NO_NAME("pool_member_no_name"), //
	POOL_MEMBER_NO_IP("pool_member_no_ip"), //

	PROXY_PASS_NO_POOL("proxy_pass_no_pool"), //

	TAG_REMOVE_NOT_FOUND("tag_remove_not_found"), //

	DEPLOY_FIND_ACTIVE_DEPLOY_FAIL("deploy_find_active_deploy_fail"), //
	DEPLOY_ALREADY_RUNNING("deploy_already_running"), //
	DEPLOY_EXCEPTION("deploy_exception"), //

	DEPLOY_POOL_NOT_RELATED_VS("deploy_pool_not_related_vs"), //需要部署的pool没有关联的vs

	GIT_EXCEPTION("git_exception"), //

	UNSAFE_CONFIG("unsafe_config"), //

	GROUP_NAME_DEFAULT("group_name_default");

	private String messageId;

	private MessageID(String messageId) {
		this.messageId = messageId;
	}

	public String messageId() {
		return this.messageId;
	}

}
