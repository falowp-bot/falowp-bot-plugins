'use strict';

// Lossless transport only: Node-specific values must survive the JSON boundary.
function encode(value) {
    if (Buffer.isBuffer(value) || value instanceof Uint8Array) return {$buffer: Buffer.from(value).toString('base64')};
    if (typeof value === 'bigint') return {$bigint: String(value)};
    if (value instanceof Map) return {$map: [...value].map(([key, item]) => [encode(key), encode(item)])};
    if (Array.isArray(value)) return value.map(encode);
    if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, encode(item)]));
    return value;
}

function decode(value, listener) {
    if (Array.isArray(value)) return value.map(item => decode(item, listener));
    if (value && typeof value === 'object') {
        if ('$buffer' in value) return Buffer.from(value.$buffer, 'base64');
        if ('$bigint' in value) return BigInt(value.$bigint);
        if ('$map' in value) return new Map(value.$map.map(([key, item]) => [decode(key, listener), decode(item, listener)]));
        if ('$listener' in value) return listener(value.$listener);
        return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, decode(item, listener)]));
    }
    return value;
}

module.exports = {encode, decode};
