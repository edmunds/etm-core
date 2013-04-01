global
        log 127.0.0.1   local0
        log 127.0.0.1   local1 notice
        #log loghost    local0 info
        maxconn 4096
        #debug
        #quiet
        user haproxy
        group haproxy

defaults
        log     global
        mode    http
        option  httplog
        option  dontlognull
        option  redispatch
        retries 3
        maxconn 2000
        contimeout      5000
        clitimeout      50000
        srvtimeout      50000

listen admin 0.0.0.0:22002
        mode http
        stats uri /

backend rewrite_append_slash
        mode http
        reqrep ^([^\ :]*)\ /(([^?]*/)?[^/\.?]+)(\?.*)?\ (.*/) \1\ /\2/\4\ \5
        redirect code 301 prefix /

backend rewrite_remove_index
        mode http
        reqrep ^([^\ :]*)\ /(.*/)?index.html(.*) \1\ /\2\3
        redirect code 301 prefix /

backend static_local
        mode http
        timeout connect 30s
        timeout server 10s
        balance roundrobin
        acl acl_host_edmunds hdr(host) www.edmunds.com
        reqrep ^([^\ :]*)\ /robots.txt(.*)$ \1\ /robots-exclude.txt\2 if !acl_host_edmunds
        server srv_static_local localhost:9000 weight 1 maxconn 512 check

<#list applications as application>
backend ${application.mavenModule.artifactId}
        mode http
        timeout connect 30s
        timeout server 10s
        balance roundrobin
<#list application.virtualServerPoolMembers as poolMember>
        server ip-${poolMember.hostAddress.host?replace(".", "-")} ${poolMember.hostAddress} weight 1 maxconn 512 check
</#list>
<#if application.httpMonitor.url??>
        option httpchk GET ${application.httpMonitor.url}
<#if application.httpMonitor.content??>
        http-check expect string ${application.httpMonitor.content?replace(" ", "\\ ")}
</#if>
</#if>

</#list>
frontend http_proxy
        bind *:80
        mode http
        option forwardfor
        option http-server-close
        option http-pretend-keepalive
<#if defaultBackend??>
        default_backend ${defaultBackend}
</#if>
        reqdeny /support-internal/
        acl acl_valid_hostname hdr(host)      www.edmunds.com  beta.edmunds.com  mobilerest.edmunds.com  origin-www.edmunds.com
        acl acl_valid_hostname hdr_end(host) -www.edmunds.com -beta.edmunds.com -mobilerest.edmunds.com .media.edmunds.com
        redirect code 301 prefix http://www.edmunds.com if !acl_valid_hostname
        use_backend static_local if { path /robots.txt /favicon.ico }
        use_backend static_local if { path_beg /server-status /cgi-bin/edw_cookie_proxy.cgi /autoobserver-archive/ }
        use_backend static_local if { path_reg ^/sitemap(.*)xml$ }
        acl acl_append_slash    path_reg /[^/\.]+$
        acl acl_no_append_slash path_reg ^/(api/|era/j_spring_security_check|era/j_spring_security_logout|mobilerest|api-nocache/|server-status)
        use_backend rewrite_append_slash if acl_append_slash !acl_no_append_slash
        use_backend rewrite_remove_index if { path_reg /index.html$ }
<#list rules as rule>
        use_backend ${rule.artifactId} if { path_reg ${rule.regEx} }
</#list>
