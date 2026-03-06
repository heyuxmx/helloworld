/**
 * 将 ZhuDe App 服务器安装为 Windows 服务
 * 必须以管理员权限运行: node scripts/install-service.js
 */

const Service = require('node-windows').Service;
const path = require('path');

const svc = new Service({
  name: 'ZhuDeAppServer',
  description: 'ZhuDe App Node.js 后端服务',
  script: path.join(__dirname, '../src/index.js'),
  env: [
    { name: 'PORT', value: '3000' },
    { name: 'NODE_ENV', value: 'production' },
  ],
  // 崩溃后自动重启
  wait: 2,
  grow: 0.25,
  maxRestarts: 5,
});

svc.on('install', () => {
  console.log('服务安装成功，正在启动...');
  svc.start();
});

svc.on('start', () => {
  console.log('ZhuDeAppServer 服务已启动！');
  console.log('可用以下命令管理服务:');
  console.log('  停止: sc stop ZhuDeAppServer');
  console.log('  启动: sc start ZhuDeAppServer');
  console.log('  卸载: node scripts/uninstall-service.js');
});

svc.on('error', (err) => {
  console.error('服务操作失败:', err);
});

svc.install();
