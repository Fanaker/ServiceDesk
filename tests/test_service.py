"""Dependency-free integration tests for the compiled Java reference server."""
import json
import os
import shutil
import socket
import subprocess
import tempfile
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class JavaServiceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix='servicedesk_test_')
        cls.workspace = Path(cls.temp.name)
        shutil.copytree(ROOT / 'data/seed', cls.workspace / 'data/seed')
        shutil.copytree(ROOT / 'web', cls.workspace / 'web')
        cls.build = ROOT / 'build'
        if not (cls.build / 'servicedesk/Main.class').exists():
            cls.build.mkdir(exist_ok=True)
            subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(cls.build), str(ROOT / 'server/src/servicedesk/Main.java')], check=True)
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            cls.port = sock.getsockname()[1]
        cls.proc = subprocess.Popen(['java', '-cp', str(cls.build), 'servicedesk.Main', str(cls.port)], cwd=cls.workspace,
                                    stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
        for _ in range(45):
            try:
                cls.req('/api/health')
                break
            except Exception:
                time.sleep(0.1)
        else:
            raise RuntimeError('Java service did not start')

    @classmethod
    def tearDownClass(cls):
        cls.proc.terminate()
        try:
            cls.proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            cls.proc.kill()
        cls.temp.cleanup()

    @classmethod
    def req(cls, path, method='GET', payload=None, role='Admin', user='Diego Flores', raw=False):
        data = urllib.parse.urlencode(payload or {}).encode() if method != 'GET' else None
        headers = {'X-Demo-Role':role,'X-Demo-User':user}
        if data is not None:
            headers['Content-Type']='application/x-www-form-urlencoded'
        request=urllib.request.Request(f'http://127.0.0.1:{cls.port}{path}', data=data,headers=headers,method=method)
        try:
            with urllib.request.urlopen(request,timeout=5) as response:
                value=response.read().decode('utf-8')
                return response.status, value if raw else json.loads(value)
        except urllib.error.HTTPError as err:
            value=err.read().decode('utf-8')
            return err.code, value if raw else json.loads(value)

    def test_01_health_and_seed(self):
        status, payload = self.req('/api/health')
        self.assertEqual(status, 200)
        self.assertEqual(payload['status'], 'ok')
        status, data = self.req('/api/bootstrap')
        self.assertEqual(status, 200)
        self.assertEqual(len(data['tickets']), 30)
        self.assertEqual(len(data['users']), 7)

    def test_02_static_assets(self):
        status, html = self.req('/',raw=True)
        self.assertEqual(status,200)
        self.assertIn('ServiceDesk | IT Operations',html)
        status, script=self.req('/assets/app.js',raw=True)
        self.assertEqual(status,200)
        self.assertIn('refresh()',script)

    def test_03_validation(self):
        code, response = self.req('/api/tickets','POST',{'title':'Bad','description':'short','priority':'Unknown'})
        self.assertEqual(code,400)
        self.assertIn('error',response)

    def test_04_demo_role_restrictions(self):
        code,_=self.req('/api/tickets/2401/status','PATCH',{'status':'Resolved'},role='Requester',user='Ana Paredes')
        self.assertEqual(code,403)
        code,_=self.req('/api/tickets/2401/assign','PATCH',{'assignee':'Lucía Ramos'},role='Technician',user='Lucía Ramos')
        self.assertEqual(code,403)

    def test_05_create_ticket_and_persist(self):
        code,t=self.req('/api/tickets','POST',{'title':'Finance printer is offline','description':'Unable to print month-end financial reports from office printer','priority':'High','category':'Hardware'},role='Requester',user='Ana Paredes')
        self.assertEqual(code,201)
        self.assertEqual(t['requester'],'Ana Paredes')
        self.assertEqual(t['status'],'Open')
        self.__class__.created=t['id']
        content=(self.workspace/'data/runtime/tickets.tsv').read_text()
        self.assertIn('Finance printer is offline',content)

    def test_06_assign_and_update_status(self):
        code,t=self.req(f'/api/tickets/{self.created}/assign','PATCH',{'assignee':'Marco Vidal'})
        self.assertEqual(code,200)
        self.assertEqual(t['assignee'],'Marco Vidal')
        code,t=self.req(f'/api/tickets/{self.created}/status','PATCH',{'status':'In progress'},role='Technician',user='Lucía Ramos')
        self.assertEqual(code,200)
        self.assertEqual(t['status'],'In progress')

    def test_07_comment_and_audit(self):
        code,_=self.req(f'/api/tickets/{self.created}/comments','POST',{'message':'Diagnostics completed and replacement scheduled.'},role='Technician',user='Lucía Ramos')
        self.assertEqual(code,201)
        code,bootstrap=self.req('/api/bootstrap')
        self.assertEqual(code,200)
        self.assertTrue(any(c['ticket']==self.created for c in bootstrap['comments']))
        self.assertTrue(any(e['ticket']==self.created and e['action']=='Comment added' for e in bootstrap['events']))

    def test_08_csv_export(self):
        code,body=self.req('/api/export',raw=True)
        self.assertEqual(code,200)
        self.assertIn('Finance printer is offline',body)
        self.assertIn('ID,Title,Priority',body)

    def test_09_restore_seed(self):
        code,_=self.req('/api/reset','POST',role='Requester',user='Ana Paredes')
        self.assertEqual(code,403)
        code,_=self.req('/api/reset','POST')
        self.assertEqual(code,200)
        _,payload=self.req('/api/bootstrap')
        self.assertEqual(len(payload['tickets']),30)


if __name__=='__main__': unittest.main()
