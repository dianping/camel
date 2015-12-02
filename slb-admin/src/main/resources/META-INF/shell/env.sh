#!/bin/sh
# 步骤1：
# 创建code.dianpingoa.com的仓库
# http://code.dianpingoa.com/arch/phoenix-slb-model-<env> 
# http://code.dianpingoa.com/arch/phoenix-slb-tengine-config-<env> 
# 确保 http://code.dianpingoa.com/arch/phoenix-slb-model-<env> 有 slb_base.xml文件

# 步骤2：
# 确保有以下配置文件
# cat /data/appdatas/phoenix/slb/
# config.xml             jdbc-mysql.properties

# 步骤3：
# 确保phoenix用户可以在/usr/local/nginx/conf/下创建文件

# 其他条件：
# 需要phoenix用户启动jboss，因为nobody没有用户目录，无法创建.ssh文件。
# 需要有git命令
# 保证admin机器可以访问code.dianpingoa.com的ip（注意config.xml配置的是ip）
# phoenix用户要有sudo权限
# /etc/sudoer 添加
#    phoenix  ALL=(root,nobody)       NOPASSWD: ALL
#  或&rd  ALL=(root,nobody)       NOPASSWD: ALL

chmod a+rw /usr/local/nginx/conf/ -R