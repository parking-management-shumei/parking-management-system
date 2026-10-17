"""Run the built JAR + UI proxy on temporary ports and verify file persistence after restart.

Run from any directory: python scripts/smoke.py
Prerequisites: mvn package and npm run build. No real credentials/services are used.
"""
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / '.cache'
CACHE.mkdir(exist_ok=True)


def port():
    with socket.socket() as server:
        server.bind(('127.0.0.1', 0))
        return server.getsockname()[1]


def wait(url, process):
    for _ in range(180):
        if process.poll() is not None:
            raise RuntimeError('Service exited; inspect .cache/smoke-*.log')
        try:
            with urllib.request.urlopen(url, timeout=1) as response:
                if response.status == 200:
                    return
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(.25)
    raise RuntimeError('Service did not become ready')


def stop(process):
    if process is not None and process.poll() is None:
        process.terminate()
        process.wait(timeout=15)


def main():
    backend_port, ui_port = port(), port()
    while backend_port == ui_port:
        ui_port = port()
    password = secrets.token_urlsafe(24)
    database = './.cache/smoke-db-' + secrets.token_hex(8)
    env = dict(os.environ, ADMIN_PASSWORD=password, SERVER_PORT=str(backend_port),
               SERVER_ADDRESS='127.0.0.1',
               DB_URL='jdbc:h2:file:' + database + ';MODE=MySQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=0',
               DB_USER='sa', DB_PASSWORD='', COZE_API_TOKEN='', COZE_BOT_ID='',
               COZE_WF_CHARGE='', COZE_WF_REVERSE_SEARCH='', COZE_WF_ROUTE_RECOMMEND='')
    proxy_env = dict(os.environ, PORT=str(ui_port), HOST='127.0.0.1',
                     API_TARGET='http://127.0.0.1:' + str(backend_port))
    flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
    base = 'http://127.0.0.1:' + str(ui_port)
    jar = ROOT / 'AI-BOTSpringboot/target/parking-backend-0.1.0.jar'
    if not jar.exists() or not (ROOT / 'dist/index.html').exists():
        raise RuntimeError('Build the backend and frontend first')
    backend = proxy = None
    with (CACHE / 'smoke-backend.log').open('w', encoding='utf-8') as blog, (CACHE / 'smoke-proxy.log').open('w', encoding='utf-8') as plog:
        def start_backend():
            child = subprocess.Popen(['java', '-Duser.timezone=Asia/Shanghai', '-jar', str(jar)],
                                     cwd=ROOT, env=env, stdout=blog, stderr=subprocess.STDOUT,
                                     creationflags=flags)
            try:
                wait('http://127.0.0.1:' + str(backend_port) + '/api/health', child)
                return child
            except Exception:
                stop(child)
                raise

        def client():
            return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

        def call(opener, method, path, body=None, token=None):
            headers = {'Content-Type': 'application/json'}
            if token:
                headers['X-XSRF-TOKEN'] = token
            payload = json.dumps(body).encode() if body is not None else None
            with opener.open(urllib.request.Request(base + path, payload, headers, method=method), timeout=10) as response:
                return json.load(response)['data']

        def login(opener):
            token = call(opener, 'GET', '/api/csrf')['token']
            result = call(opener, 'POST', '/user/login', {'no': 'admin', 'password': password}, token)
            assert 'password' not in result['user']
            return token, result['user']['id']

        try:
            backend = start_backend()
            proxy = subprocess.Popen(['node', 'server.js'], cwd=ROOT, env=proxy_env, stdout=plog,
                                     stderr=subprocess.STDOUT, creationflags=flags)
            wait(base + '/api/health', proxy)
            opener = client()
            token, owner = login(opener)
            print('PASS: packaged backend, UI proxy, real session cookie and CSRF', flush=True)
            record = call(opener, 'POST', '/api/parking/entry',
                          {'lotId': 1, 'userId': owner, 'numberplate': '京ASM001', 'spaceCode': 'B1-SM001'}, token)
            order = call(opener, 'POST', '/api/parking/exit/' + str(record['id']), {}, token)
            assert order['amount'] == 0 and order['status'] == 'UNPAID'
            same = call(opener, 'POST', '/api/parking/exit/' + str(record['id']), {}, token)
            assert same['id'] == order['id']
            paid = call(opener, 'POST', '/api/parking/orders/' + str(order['id']) + '/paid', {}, token)
            assert paid['status'] == 'PAID'
            overview = call(opener, 'GET', '/api/parking/overview')
            assert overview['lots'][0]['occupied'] == 0
            with opener.open(base + '/operations') as response:
                assert response.status == 200 and b'<div id="app"' in response.read()
            try:
                call(opener, 'GET', '/api/nonexistent')
                raise AssertionError('Unknown API was hidden by HTML fallback')
            except urllib.error.HTTPError as error:
                assert error.code == 404
            print('PASS: entry -> exit -> order -> receipt; deep-link fallback and API 404', flush=True)
            stop(backend)
            backend = start_backend()
            opener = client()
            login(opener)
            records = call(opener, 'GET', '/api/parking/records')
            orders = call(opener, 'GET', '/api/parking/orders')
            assert any(r['id'] == record['id'] and r['exitTime'] for r in records)
            assert any(o['id'] == order['id'] and o['status'] == 'PAID' for o in orders)
            print('PASS: records and paid order survive backend process restart', flush=True)
        finally:
            stop(proxy)
            stop(backend)


if __name__ == '__main__':
    main()
