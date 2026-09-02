import re

path = '/etc/nginx/sites-available/default'
with open(path, 'r') as f:
    content = f.read()

new_block = '''\t# /oa 采购系统 (部署于 ~/jvkit/oa, 2026-08-27)
\tlocation = /oa { return 301 /oa/; }
\tlocation /oa/ {
\t\talias /home/liyang/jvkit/oa/dist/;
\t\tindex index.html;
\t\ttry_files $uri $uri/ /oa/index.html;
\t}
\tlocation /oa/api/ {
\t\tproxy_pass http://127.0.0.1:8092/;
\t\tproxy_set_header Host $host;
\t\tproxy_set_header X-Real-IP $remote_addr;
\t\tproxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
\t\tproxy_set_header X-Forwarded-Proto $scheme;
\t}
'''

# 替换 /oa 旧块
pattern = r'\t# /oa 采购系统.*?\tlocation /oa/api/ \{[^}]+\}\n'
content = re.sub(pattern, new_block, content, flags=re.DOTALL)

# 删除 snail-ai 块
pattern2 = r'\t# Snail AI 控制台反代.*?\t\}\n'
content = re.sub(pattern2, '', content, flags=re.DOTALL)

with open(path, 'w') as f:
    f.write(content)

print('nginx default fixed')
