"""源码分层检查：防止新增代码绕过 Service 或把 SQL 放回 Controller。"""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/dev/labdesk'
checks = []

def check(name, condition):
    assert condition, name
    checks.append(name)

sources = {path.relative_to(JAVA).as_posix(): path.read_text(encoding='utf-8')
           for path in JAVA.rglob('*.java')}
for path, source in sources.items():
    imports = re.findall(r'^import\s+([^;]+);', source, re.MULTILINE)
    layer = path.split('/')[0]
    # 持久化访问统一封装在 mapper。拦截器、初始化器也必须通过 Service。
    if layer != 'mapper':
        check(path + ': no direct persistence API', not any(
            item.startswith(('java.sql.', 'javax.sql.', 'org.springframework.jdbc.',
                             'org.springframework.data.redis.', 'org.apache.ibatis.')) for item in imports))
        literals = re.findall(r'"(?:\\.|[^"\\])*"', source)
        check(path + ': no SQL literals', not any(
            re.search(r'\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER)\s+\w', item, re.I) for item in literals))
    if layer != 'service':
        check(path + ': mapper only consumed by service', not any(
            item.startswith('dev.labdesk.mapper.') for item in imports))
    if layer in {'controller', 'config', 'security'}:
        check(path + ': depend on service interfaces', not any(
            item.startswith('dev.labdesk.service.impl.') for item in imports))
    if layer == 'mapper':
        check(path + ': no reverse dependency', not any(
            item.startswith(('dev.labdesk.service.', 'dev.labdesk.controller.')) for item in imports))
    if '@Transactional' in source and layer != 'common':
        check(path + ': transaction belongs to service', path.startswith('service/impl/'))

for xml in (ROOT / 'src/main/resources/mapper').glob('*.xml'):
    mapper = ET.parse(xml).getroot()
    expected = 'dev.labdesk.mapper.' + xml.stem
    check(xml.name + ': namespace', mapper.attrib['namespace'] == expected)
    interface = sources['mapper/' + xml.stem + '.java']
    for statement in mapper:
        if statement.tag in {'select', 'insert', 'update', 'delete'}:
            check(xml.name + ': ' + statement.attrib['id'] + ' has interface method',
                  bool(re.search(r'\b' + statement.attrib['id'] + r'\s*\(', interface)))
    check(xml.name + ': parameter binding', '${' not in xml.read_text(encoding='utf-8'))

for service in (JAVA / 'service').glob('*.java'):
    check(service.name + ': interface exists', 'public interface ' + service.stem in service.read_text(encoding='utf-8'))
    impl = JAVA / 'service/impl' / (service.stem + 'Impl.java')
    check(service.name + ': implementation exists', impl.exists())
    check(service.name + ': implementation contract',
          'implements ' + service.stem in impl.read_text(encoding='utf-8'))

report = {'passed': len(checks), 'checks': checks,
          'note': '源码依赖和 SQL 位置检查；业务行为由 smoke.py 验证。'}
Path(__file__).with_name('architecture-result.json').write_text(
    json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'PASS: {len(checks)} architecture checks across {len(sources)} Java files.')
