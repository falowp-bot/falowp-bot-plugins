'use strict';
// Electron 由 QQ 安装包提供
//noinspection NpmUsedModulesInstalled
const {app} = require('electron');
const path = require('node:path');
const os = require('node:os');
const readline = require('node:readline');
const {encode, decode} = require('./native-rpc.cjs');
const emit = (type, data = {}) => process.stdout.write('@falowp ' + JSON.stringify({type, ...encode(data)}) + '\n');
const listener = names => new Proxy({}, {
    get: (_, name) => names.includes(name) ? (...args) => emit('nativeEvent', {
        name,
        arguments: args
    }) : () => {
    }
});
let native, session, startup;
const allowed = {
    engine: ['initWithDeskTopConfig'],
    login: ['addKernelLoginListener', 'connect', 'getLoginList', 'getMachineGuid', 'getQRCodePicture', 'initConfig', 'quickLoginWithUin'],
    session: ['init'],
    startup: ['start'],
    msg: ['addKernelMsgListener', 'clickInlineKeyboardButton', 'downloadRichMedia', 'forwardMsg', 'generateMsgUniqueId', 'getMsgByClientSeqAndTime', 'getMsgEmojiLikesList', 'getMsgsByMsgId', 'getSingleMsg', 'getMultiMsg', 'getRichMediaFilePathForGuild', 'multiForwardMsgWithComment', 'recallMsg', 'sendMsg', 'sendNudge', 'sendShowInputStatusReq', 'sendSsoCmdReqByContend', 'setAllC2CAndGroupMsgRead', 'setMsgEmojiLikes', 'setMsgRead', 'setStatus'],
    group: ['addGroupEssence', 'addKernelGroupListener', 'deleteGroupBulletin', 'destroyGroup', 'getAllMemberList', 'getGroupDetailInfo', 'getGroupList', 'getGroupRecommendContactArkJson', 'getGroupShutUpMemberList', 'getMemberInfo', 'getSingleScreenNotifies', 'getUidByUins', 'getUinByUids', 'kickMember', 'modifyGroupDetailInfoV2', 'modifyGroupExtInfoV2', 'modifyGroupName', 'modifyGroupRemark', 'modifyMemberCardName', 'modifyMemberRole', 'operateSysNotify', 'quitGroup', 'removeGroupEssence', 'setGroupShutUp', 'setMemberShutUp'],
    buddy: ['addKernelBuddyListener', 'approvalFriendRequest', 'getBuddyListV2', 'getBuddyRecommendContactArkJson', 'getBuddyReq', 'setBuddyRemark'],
    profile: ['getCoreAndBaseInfo', 'setHeader', 'setLongNick'],
    like: ['getBuddyProfileLike', 'setBuddyProfileLike'],
    recent: ['getRecentContactListSnapShot', 'getRecentContactListSyncLimit'],
    msf: ['getServerTime'],
    tip: ['getPskey'],
    ticket: ['forceFetchClientKey'],
    richMedia: ['getGroupFileInfo', 'createGroupFolder', 'deleteGroupFolder', 'deleteGroupFile', 'transGroupFile'],
    album: ['deleteMedias', 'doQunComment', 'doQunLike', 'getAlbumList', 'getMediaList']
};

function service(name) {
    if (name === 'engine') return native.NodeIQQNTWrapperEngine.get();
    if (name === 'login') return native.NodeIKernelLoginService.get();
    if (name === 'startup') return startup ??= native.NodeIQQNTStartupSessionWrapper.create();
    startup ??= native.NodeIQQNTStartupSessionWrapper.create();
    session ??= native.NodeIQQNTWrapperSession.getNTWrapperSession('nt_1');
    if (name === 'session') return session;
    return session[{
        msg: 'getMsgService',
        richMedia: 'getRichMediaService',
        group: 'getGroupService',
        buddy: 'getBuddyService',
        msf: 'getMSFService',
        profile: 'getProfileService',
        like: 'getProfileLikeService',
        recent: 'getRecentContactService',
        album: 'getAlbumService',
        ticket: 'getTicketService',
        tip: 'getTipOffService'
    }[name]]();
}

const input = readline.createInterface({input: process.stdin});
input.on('close', () => app.exit(0));
input.on('line', async line => {
    let request;
    try {
        request = JSON.parse(line);
        const args = request.args;
        if (request.type !== 'native' || !allowed[args.service]?.includes(args.method)) {
            emit('result', {request: request.request, ok: false, error: '未知原生方法'});
            return;
        }
        const target = service(args.service);
        const data = await target[args.method](...decode(args.arguments, listener));
        emit('result', {request: request.request, ok: true, data: data ?? null});
    } catch (error) {
        if (request) emit('result', {request: request.request, ok: false, error: String(error.message)});
    }
});
app.whenReady().then(() => {
    const directory = process.env.FALOWP_QQ_APP;
    native = require(path.join(directory, 'wrapper.node'));
    require('./packet-bridge.cjs').install(directory);
    emit('hostReady', {
        version: require(path.join(directory, 'package.json')).version,
        arch: process.arch,
        release: os.release(),
        hostname: os.hostname(),
        home: os.homedir(),
        paths: {
            accountPath: path.join(os.homedir(), '.config', 'QQ'),
            tools: process.env.FALOWP_QQ_TOOLS,
            installer: process.env.FALOWP_QQ_MEDIA_INSTALL
        }
    });
}).catch(error => {
    process.stderr.write('QQNative initialization: ' + error.message + '\n');
    emit('failed');
    app.exit(1);
});
