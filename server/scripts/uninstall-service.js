/**
 * 卸载 ZhuDe App Windows 服务
 * 必须以管理员权限运行: node scripts/uninstall-service.js
 */

const Service = require('node-windows').Service;
const path = require('path');

const svc = new Service({
  name: 'ZhuDeAppServer',
  script: path.join(__dirname, '../src/index.js'),
});

svc.on('uninstall', () => {
  console.log('ZhuDeAppServer 服务已卸载');
});

svc.uninstall();
