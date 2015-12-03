设置flush指令
===================================  

配置flush数据大小，强制数据在特定大小时进行flush
用于http chunked，控制数据块大小

配置指令
-----------------------------------  
Syntax: flush_buffer_size  size
Default: 0
Context: http, server, location

例如：flush_buffer_size 10k;
在数据大小为10k时，强制flush
