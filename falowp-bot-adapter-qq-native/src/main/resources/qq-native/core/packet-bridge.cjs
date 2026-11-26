'use strict';
const fs = require('node:fs');
const path = require('node:path');
const {createHash} = require('node:crypto');
const clients = {
    arm64: '8f17c51f111f33efcff42aae11850436500d7ca7d0f82935db0b826cb1459b4a',
    x64: '236ff3ebf29310c75a41394d80db4587411d43d66208c2a7d58b160d7b7b213b'
};

function install(directory) {
    if (process.platform !== 'linux' || !clients[process.arch]) throw Error('Unsupported QQ packet platform');
    const fd = fs.openSync(path.join(directory, 'wrapper.node'), 'r');
    const hash = createHash('sha256'), block = Buffer.alloc(1024 * 1024);
    try {
        let count;
        while ((count = fs.readSync(fd, block, 0, block.length, null))) hash.update(block.subarray(0, count));
    } finally {
        fs.closeSync(fd);
    }
    if (hash.digest('hex') !== clients[process.arch]) throw Error('NTQQ version does not match the bundled packet bridge');
    require('./native/packet-linux-' + process.arch + '.node');
}

module.exports = {install};
