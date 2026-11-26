'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {encode, decode} = require('../../main/resources/qq-native/core/native-rpc.cjs');
test('native binary, maps and 64-bit integers survive JSON without UTF-8 conversion', () => {
    const input = {
        buffer: Buffer.from([0, 127, 128, 255]),
        map: new Map([[0, 'cover'], ['uid', new Map([['nested', 19n]])]]),
        id: 18446744073709551615n
    };
    assert.deepEqual(decode(JSON.parse(JSON.stringify(encode(input)))), input);
});
test('listener placeholders become native callback objects only at the boundary', () => {
    const callbacks = {
        onUpdate() {
        }
    };
    assert.equal(decode({$listener: ['onUpdate']}, names => {
        assert.deepEqual(names, ['onUpdate']);
        return callbacks;
    }), callbacks);
});
test('extension bridge remains a whitelist of native methods without business handlers', () => {
    const source = require('node:fs').readFileSync(require('node:path').join(__dirname, '../../main/resources/qq-native/core/worker.cjs'), 'utf8');
    for (const method of ['getGroupDetailInfo', 'getCoreAndBaseInfo', 'getMediaList', 'getPskey', 'forceFetchClientKey', 'modifyMemberRole', 'deleteGroupBulletin', 'setMsgEmojiLikes']) assert.ok(source.includes("'" + method + "'"), method);
    assert.ok(!source.includes('get_mini_app_ark'));
    assert.ok(!source.includes('set_group_ban'));
});
