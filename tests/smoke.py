"""对运行中的真实 MySQL/Redis 服务进行验收；仅创建带标记的演示数据。"""
import json
import uuid
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError

BASE = 'http://127.0.0.1:18080/api'
checks = []

def call(path, body=None, token=None, key=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if key:
        headers['Idempotency-Key'] = key
    request = Request(BASE + path, data=None if body is None else json.dumps(body).encode(), headers=headers)
    try:
        with urlopen(request, timeout=30) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        return error.code, json.load(error)

def check(name, actual, expected):
    assert actual == expected, f'{name}: expected {expected}, got {actual}'
    checks.append(name)
    print('PASS', name)

def login(name):
    status, data = call('/login', {'username': name, 'password': name + '123'})
    check(name + ' login', status, 200)
    return data['token']

check('unauthenticated request', call('/bookings')[0], 401)
check('wrong password', call('/login', {'username': 'student', 'password': 'wrong'})[0], 401)
student, admin = login('student'), login('admin')
start = (datetime.now() + timedelta(days=30, seconds=uuid.uuid4().int % 500000)).replace(microsecond=0)
end = start + timedelta(hours=1)
body = {'deviceId': 2, 'start': start.isoformat(), 'end': end.isoformat()}
keys = [str(uuid.uuid4()) for _ in range(20)]
with ThreadPoolExecutor(max_workers=20) as pool:
    results = list(pool.map(lambda key: call('/bookings', body, student, key), keys))
check('20 overlapping reservations: one winner', dict(Counter(status for status, _ in results)), {200: 1, 409: 19})
winner = next(i for i, result in enumerate(results) if result[0] == 200)
booking = results[winner][1]
status, repeated = call('/bookings', body, student, keys[winner])
check('idempotent retry status', status, 200)
check('idempotent retry same ID', repeated['id'], booking['id'])
check('idempotency key payload mismatch', call('/bookings', {**body, 'end': (end + timedelta(minutes=5)).isoformat()}, student, keys[winner])[0], 409)
check('missing idempotency key', call('/bookings', body, student)[0], 400)
check('fractional seconds rejected', call('/bookings', {**body, 'start': body['start'] + '.123'}, student, str(uuid.uuid4()))[0], 400)
adjacent = {'deviceId': 2, 'start': end.isoformat(), 'end': (end + timedelta(hours=1)).isoformat()}
status, adjacent_booking = call('/bookings', adjacent, admin, str(uuid.uuid4()))
check('adjacent interval accepted', status, 200)
check('cannot cancel another user booking', call(f"/bookings/{adjacent_booking['id']}/cancel", {'version': 0}, student)[0], 403)
check('cancel own booking', call(f"/bookings/{booking['id']}/cancel", {'version': 0}, student)[0], 200)
check('repeat cancellation rejected', call(f"/bookings/{booking['id']}/cancel", {'version': 0}, student)[0], 409)
status, replacement = call('/bookings', body, student, str(uuid.uuid4()))
check('cancel releases slot', status, 200)
call(f"/bookings/{replacement['id']}/cancel", {'version': 0}, student)
call(f"/bookings/{adjacent_booking['id']}/cancel", {'version': 0}, admin)

status, ticket = call('/tickets', {'deviceId': 1, 'title': '自动验收：设备无法开机', 'description': '自动测试生成，已完整流转关闭。'}, student)
check('ticket created', status, 200)
path = f"/tickets/{ticket['id']}/transition"
transition = {'status': 'ACCEPTED', 'version': 0, 'note': '自动验收'}
check('skip state rejected', call(path, {**transition, 'status': 'RESOLVED'}, admin)[0], 409)
check('student cannot accept ticket', call(path, transition, student)[0], 403)
with ThreadPoolExecutor(max_workers=20) as pool:
    results = list(pool.map(lambda _: call(path, transition, admin), range(20)))
check('20 concurrent transitions: one winner', dict(Counter(status for status, _ in results)), {200: 1, 409: 19})
check('only successful transition produces event', len(call(f"/tickets/{ticket['id']}/events", token=student)[1]), 2)
check('stale version rejected', call(path, {'status': 'PROCESSING', 'version': 0, 'note': '旧版本'}, admin)[0], 409)
for version, state in [(1, 'PROCESSING'), (2, 'RESOLVED'), (3, 'CLOSED')]:
    actor = student if state == 'CLOSED' else admin
    check('transition to ' + state, call(path, {'status': state, 'version': version, 'note': '自动验收'}, actor)[0], 200)
check('complete audit history', len(call(f"/tickets/{ticket['id']}/events", token=student)[1]), 5)
check('knowledge retrieval mode', call('/knowledge/ask', {'question': '设备无法开机怎么办'}, student)[1]['mode'], 'RETRIEVAL_ONLY')
check('unknown question refuses', call('/knowledge/ask', {'question': '完全无关的量子引力话题'}, student)[1]['mode'], 'NO_EVIDENCE')
check('logout', call('/logout', {}, student)[0], 200)
check('logout invalidates token', call('/bookings', token=student)[0], 401)
call('/logout', {}, admin)
report = {'timestamp': datetime.now().isoformat(), 'passed': len(checks), 'checks': checks,
          'reservation_concurrency': {'requests': 20, 'success': 1, 'conflict': 19},
          'ticket_concurrency': {'requests': 20, 'success': 1, 'conflict': 19},
          'llm_api_tested': False, 'note': '功能并发正确性验证，不是吞吐量或性能压测。'}
Path(__file__).with_name('last-result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'All {len(checks)} checks passed.')
