// AsyncAPI 문서 로드 및 WebSocket 테스트 애플리케이션

let stompClient = null;
let asyncApiDoc = null;
let subscriptions = new Map();
let metadata = null;
/** 서버가 CONNECTED 프레임에 넣어준 WebSocket session ID (연결 성공 시에만 존재) */
let currentSessionId = null;

// DOM 요소
const accessTokenInput = document.getElementById('accessToken');
const asyncApiUrlInput = document.getElementById('asyncApiUrl');
const connectBtn = document.getElementById('connectBtn');
const disconnectBtn = document.getElementById('disconnectBtn');
const loadApiBtn = document.getElementById('loadApiBtn');
const connectionStatus = document.getElementById('connectionStatus');
const channelsList = document.getElementById('channelsList');
const messagesList = document.getElementById('messagesList');
const clearMessagesBtn = document.getElementById('clearMessagesBtn');
const projectTitle = document.getElementById('projectTitle');
const projectDescription = document.getElementById('projectDescription');
const projectVersion = document.getElementById('projectVersion');
const asyncApiDocs = document.getElementById('asyncApiDocs');

// 초기화
document.addEventListener('DOMContentLoaded', () => {
    initialize();
    setupEventListeners();
});

// 이벤트 리스너 설정 (주석 처리된 DOM 요소는 null일 수 있음)
function setupEventListeners() {
    if (connectBtn) connectBtn.addEventListener('click', connect);
    if (disconnectBtn) disconnectBtn.addEventListener('click', disconnect);
    if (clearMessagesBtn) clearMessagesBtn.addEventListener('click', clearMessages);
    if (loadApiBtn) loadApiBtn.addEventListener('click', loadAsyncApiDoc);
    if (asyncApiUrlInput) {
        asyncApiUrlInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') loadAsyncApiDoc();
        });
    }
}

// 초기화: 메타데이터 로드 후 AsyncAPI 문서 로드
async function initialize() {
    try {
        // 메타데이터 로드
        const metadataResponse = await fetch('./metadata.json');
        metadata = await metadataResponse.json();
        
        // AsyncAPI URL 기본값 설정
        if (metadata?.apiEndpoint && asyncApiUrlInput && !asyncApiUrlInput.value.trim()) {
            // 입력 필드가 비어있을 때만 기본값 설정
            let apiEndpoint = metadata.apiEndpoint;
            if (apiEndpoint.startsWith('/')) {
                apiEndpoint = window.location.origin + apiEndpoint;
            }
            // 정규화된 URL 설정 (이미 /websocket-docs/api로 끝나면 그대로, 아니면 추가)
            const normalized = normalizeApiUrl(apiEndpoint);
            asyncApiUrlInput.value = normalized || apiEndpoint;
        }
        
        // AsyncAPI 문서 로드
        await loadAsyncApiDoc();
    } catch (error) {
        console.error('초기화 실패:', error);
        channelsList.innerHTML = '<p class="error">초기화에 실패했습니다: ' + error.message + '</p>';
    }
}

// URL 정규화: 서버 주소나 path prefix까지만 입력해도 자동으로 /websocket-docs/api 추가
function normalizeApiUrl(url) {
    if (!url) return null;
    
    url = url.trim();
    if (!url) return null;
    
    // 이미 /websocket-docs/api로 끝나면 그대로 반환
    if (url.endsWith('/websocket-docs/api')) {
        return url;
    }
    
    // URL 파싱
    try {
        const urlObj = new URL(url);
        const pathname = urlObj.pathname;
        
        // pathname이 없거나 루트인 경우
        if (!pathname || pathname === '/') {
            urlObj.pathname = '/websocket-docs/api';
            return urlObj.toString();
        }
        
        // pathname이 있지만 /websocket-docs/api로 끝나지 않는 경우
        // pathname 끝에 /가 있으면 제거
        const cleanPath = pathname.endsWith('/') ? pathname.slice(0, -1) : pathname;
        urlObj.pathname = cleanPath + '/websocket-docs/api';
        return urlObj.toString();
    } catch (e) {
        // 상대 경로인 경우
        if (url.startsWith('./') || url.startsWith('/')) {
            const baseUrl = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '/');
            if (url.startsWith('./')) {
                url = baseUrl + url.substring(2);
            } else {
                url = window.location.origin + url;
            }
            return normalizeApiUrl(url);
        }
        // 잘못된 URL 형식
        throw new Error('올바른 URL 형식이 아닙니다');
    }
}

// AsyncAPI URL에서 metadata URL 생성
function getMetadataUrl(apiUrl) {
    try {
        const urlObj = new URL(apiUrl);
        // /websocket-docs/api를 /websocket-docs/metadata.json으로 변경
        if (urlObj.pathname.endsWith('/websocket-docs/api')) {
            urlObj.pathname = urlObj.pathname.replace('/api', '/metadata.json');
        } else if (urlObj.pathname.includes('/websocket-docs/')) {
            // 이미 /websocket-docs/가 포함되어 있으면 /api를 /metadata.json으로 변경
            urlObj.pathname = urlObj.pathname.replace('/api', '/metadata.json');
        } else {
            // pathname에서 마지막 부분을 제거하고 /websocket-docs/metadata.json 추가
            // 예: /api -> /websocket-docs/metadata.json
            // 예: /some/path/api -> /some/path/websocket-docs/metadata.json
            const pathParts = urlObj.pathname.split('/').filter(p => p);
            // 마지막이 'api'이면 제거
            if (pathParts[pathParts.length - 1] === 'api') {
                pathParts.pop();
            }
            // websocket-docs/metadata.json 추가
            pathParts.push('websocket-docs', 'metadata.json');
            urlObj.pathname = '/' + pathParts.join('/');
        }
        return urlObj.toString();
    } catch (e) {
        console.error('getMetadataUrl error:', e);
        return null;
    }
}

// AsyncAPI 문서 로드
async function loadAsyncApiDoc() {
    try {
        // 입력된 URL 사용, 없으면 기본값 사용
        let apiEndpoint = asyncApiUrlInput?.value.trim();
        
        if (apiEndpoint) {
            // URL 정규화 (내부적으로만 사용, 입력 필드는 변경하지 않음)
            const normalizedEndpoint = normalizeApiUrl(apiEndpoint);
            apiEndpoint = normalizedEndpoint || apiEndpoint;
            
            // 해당 서버에서 metadata 가져오기
            const metadataUrl = getMetadataUrl(apiEndpoint);
            if (metadataUrl) {
                try {
                    const metadataResponse = await fetch(metadataUrl);
                    if (metadataResponse.ok) {
                        metadata = await metadataResponse.json();
                        console.log('Metadata loaded from:', metadataUrl);
                    } else {
                        console.warn('Metadata를 가져올 수 없습니다:', metadataUrl);
                    }
                } catch (e) {
                    console.warn('Metadata 로드 실패:', e);
                    // metadata 로드 실패해도 계속 진행
                }
            }
        } else {
            // 기본값 사용 (현재 서버의 metadata 사용)
            if (!metadata) {
                try {
                    const metadataResponse = await fetch('./metadata.json');
                    metadata = await metadataResponse.json();
                } catch (e) {
                    console.warn('기본 metadata 로드 실패:', e);
                }
            }
            
            apiEndpoint = metadata?.apiEndpoint || '/websocket-docs/api';
            // 절대 경로 처리
            if (apiEndpoint.startsWith('/')) {
                apiEndpoint = window.location.origin + apiEndpoint;
            }
            // 기본값도 정규화
            apiEndpoint = normalizeApiUrl(apiEndpoint) || apiEndpoint;
        }
        
        if (!apiEndpoint) {
            throw new Error('AsyncAPI URL을 입력해주세요');
        }
        
        // 로드 중 표시
        channelsList.innerHTML = '<p class="loading">AsyncAPI 문서를 불러오는 중...</p>';
        asyncApiDocs.innerHTML = '<p class="loading">AsyncAPI 문서를 불러오는 중...</p>';
        
        const response = await fetch(apiEndpoint);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        asyncApiDoc = await response.json();
        renderProjectInfo();
        renderChannels();
        renderAsyncApiDocs();
    } catch (error) {
        console.error('AsyncAPI 문서 로드 실패:', error);
        channelsList.innerHTML = '<p class="error">AsyncAPI 문서를 불러올 수 없습니다: ' + error.message + '</p>';
        asyncApiDocs.innerHTML = '<p class="error">AsyncAPI 문서를 불러올 수 없습니다: ' + error.message + '</p>';
    }
}

// Markdown 렌더링 (marked 라이브러리 사용, 없으면 평문)
// breaks: true → `\n`을 `<br>`로 변환. HTML `<br>` 태그도 raw HTML로 통과.
function renderMarkdown(text) {
    if (typeof marked !== 'undefined' && marked.parse) {
        marked.use({ breaks: true });
        const result = marked.parse(String(text));
        return typeof result === 'string' ? result : escapeHtml(text).replace(/\n/g, '<br>');
    }
    return escapeHtml(text).replace(/\n/g, '<br>');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 프로젝트 정보 렌더링 (상단)
function renderProjectInfo() {
    if (!asyncApiDoc || !asyncApiDoc.info) return;
    
    const info = asyncApiDoc.info;
    if (info.title && projectTitle) {
        projectTitle.textContent = info.title;
    }
    if (info.description && projectDescription) {
        projectDescription.innerHTML = renderMarkdown(info.description);
    }
    if (info.version && projectVersion) {
        projectVersion.textContent = `Version: ${info.version}`;
    }
}

// AsyncAPI 문서 렌더링 (우측)
function renderAsyncApiDocs() {
    if (!asyncApiDoc) {
        asyncApiDocs.innerHTML = '<p class="empty">AsyncAPI 문서가 없습니다</p>';
        return;
    }
    
    let html = '<div class="asyncapi-content">';
    
    // Channels 섹션 - Swagger UI 스타일 태그로 표시
    if (asyncApiDoc.channels) {
        html += '<div class="swagger-tags">';
        
        const replyChannelAddresses = getReplyChannelAddresses();
        const channels = Object.entries(asyncApiDoc.channels);
        channels.forEach(([channelId, channel]) => {
            const address = channel.address || channelId;
            // reply 전용 채널은 우측 문서에서 제외 (응답은 요청 operation 아래 "응답 (Reply)"로 표시됨)
            const channelOperations = findOperationsForChannel(channelId);
            if (replyChannelAddresses.has(address) && channelOperations.length === 0) {
                return;
            }
            
            const uniqueId = `channel-${channelId.replace(/[^a-zA-Z0-9]/g, '-')}`;
            
            // 채널의 messages 정보 가져오기
            const messages = channel.messages || {};
            const messageEntries = Object.entries(messages);
            
            const hasMessages = messageEntries.length > 0 || channelOperations.length > 0;
            
            html += `<div class="swagger-tag">`;
            html += `<div class="swagger-tag-header" onclick="toggleChannel('${uniqueId}')">`;
            html += `<span class="swagger-tag-name">${address}</span>`;
            html += `<span class="swagger-tag-toggle" id="${uniqueId}-toggle" style="transform: rotate(-90deg);">▶</span>`;
            html += `</div>`;
            
            html += `<div class="swagger-tag-content" id="${uniqueId}-content" style="display: none;">`;
            
            if (!hasMessages) {
                html += `<div class="message-item"><span class="empty-message">메시지 없음</span></div>`;
            } else {
                // 각 operation에 대해 메시지 표시
                channelOperations.forEach((operation, index) => {
                    const direction = getDirectionForOperation(operation);
                    const messageInfo = getMessageInfoForOperation(operation, channel);
                    const isClientToServer = operation.action === 'receive';
                    const addressEscaped = (channel.address || channelId).replace(/"/g, '&quot;');
                    const messageKeyEscaped = (messageInfo.messageKey || '').replace(/"/g, '&quot;');
                    
                    html += `<div class="message-item message-item-with-actions">`;
                    html += `<span class="message-direction">[${direction}]</span>`;
                    html += `<span class="message-name">${messageInfo.name || '메시지'}</span>`;
                    
                    // 클라이언트 → 서버: 직접 보내기 버튼
                    if (isClientToServer) {
                        html += `<button type="button" class="btn btn-send-inline btn-small" data-address="${addressEscaped}" data-message-key="${messageKeyEscaped}" onclick="openSendModalFromButton(this)" title="이 destination으로 메시지 전송">보내기</button>`;
                    }
                    
                    // 메시지 스키마 정보 표시
                    if (messageInfo.schema) {
                        const exampleJson = generateExampleFromSchema(messageInfo.schema);
                        html += `<div class="message-schema">`;
                        html += `<pre class="schema-content example-json">${JSON.stringify(exampleJson, null, 2)}</pre>`;
                        html += `</div>`;
                    }
                    
                    // Reply(응답)가 있으면 응답 메시지 표시
                    const replyInfo = getReplyInfoForOperation(operation);
                    if (replyInfo.replySchema || replyInfo.replyChannelAddress) {
                        html += `<div class="message-reply">`;
                        html += `<span class="reply-label">응답 (Reply)</span>`;
                        if (replyInfo.replyChannelAddress) {
                            html += `<span class="reply-channel">→ ${escapeHtml(replyInfo.replyChannelAddress)}</span>`;
                        }
                        if (replyInfo.replySchema) {
                            const replyExample = generateExampleFromSchema(replyInfo.replySchema);
                            html += `<div class="message-schema reply-schema">`;
                            html += `<pre class="schema-content example-json">${JSON.stringify(replyExample, null, 2)}</pre>`;
                            html += `</div>`;
                        }
                        html += `</div>`;
                    }
                    
                    html += `</div>`;
                });
            }
            
            html += `</div>`;
            html += `</div>`;
        });
        
        html += '</div>';
    }
    
    html += '</div>';
    asyncApiDocs.innerHTML = html;
}

// 채널에 연결된 operations 찾기
function findOperationsForChannel(channelId) {
    if (!asyncApiDoc.operations) return [];
    
    return Object.entries(asyncApiDoc.operations)
        .filter(([_, operation]) => {
            const channelRef = operation.channel?.$ref;
            if (channelRef) {
                const refChannelId = channelRef.replace('#/channels/', '');
                return refChannelId === channelId;
            }
            return false;
        })
        .map(([_, operation]) => operation);
}

// Operation의 방향 결정
function getDirectionForOperation(operation) {
    // action이 "receive"면 서버가 받는 것 = 클라이언트 -> 서버
    // action이 "send"면 서버가 보내는 것 = 서버 -> 클라이언트
    if (operation.action === 'receive') {
        return '클라이언트 → 서버';
    } else if (operation.action === 'send') {
        return '서버 → 클라이언트';
    }
    return '알 수 없음';
}

// Operation의 reply(응답) 정보 가져오기 (AsyncAPI 3.0 Operation Reply)
function getReplyInfoForOperation(operation) {
    const result = { replyChannelAddress: null, replyMessageName: null, replySchema: null };
    if (!operation.reply) return result;

    const reply = operation.reply;
    // reply.channel.$ref -> #/channels/{channelId}
    const channelRef = reply.channel?.$ref;
    if (channelRef && asyncApiDoc.channels) {
        const channelId = channelRef.replace('#/channels/', '');
        const replyChannel = asyncApiDoc.channels[channelId];
        if (replyChannel) {
            result.replyChannelAddress = replyChannel.address || channelId;
        }
    }
    // reply.messages[0].$ref -> #/components/messages/{messageKey}
    if (reply.messages && reply.messages.length > 0) {
        const messageRef = reply.messages[0].$ref;
        if (messageRef && asyncApiDoc.components?.messages) {
            const messageKey = messageRef.replace('#/components/messages/', '');
            const message = asyncApiDoc.components.messages[messageKey];
            if (message?.payload?.$ref) {
                const schemaName = message.payload.$ref.replace('#/components/schemas/', '');
                if (asyncApiDoc.components.schemas?.[schemaName]) {
                    result.replySchema = asyncApiDoc.components.schemas[schemaName];
                }
            }
            result.replyMessageName = extractReadableMessageName(messageKey);
        }
    }
    return result;
}

// Operation의 메시지 정보 가져오기
// 메시지 키에서 읽기 쉬운 이름 추출
function extractReadableMessageName(messageKey) {
    if (!messageKey) return messageKey;
    
    // 형식: {class}_{method}_{ACTION} 또는 {class}_{method}_{ACTION}
    // 예: monolithic_common_websocket_config_doc_WebSocketDocTest_TestWebSocketClass_testClass_RECEIVE
    // -> testClass (RECEIVE)
    
    const parts = messageKey.split('_');
    if (parts.length < 2) return messageKey;
    
    // 마지막 부분이 ACTION (SEND/RECEIVE)인지 확인
    const lastPart = parts[parts.length - 1];
    const action = (lastPart === 'SEND' || lastPart === 'RECEIVE') ? lastPart : null;
    
    if (action) {
        // ACTION 이전 부분이 메서드 이름
        const methodName = parts[parts.length - 2];
        return `${methodName} (${action})`;
    }
    
    // ACTION이 없으면 마지막 부분만 반환
    return parts[parts.length - 1];
}

function getMessageInfoForOperation(operation, channel) {
    const result = { name: null, schema: null, messageKey: null };
    
    // operation의 messages에서 메시지 참조 가져오기
    if (operation.messages && operation.messages.length > 0) {
        const messageRef = operation.messages[0].$ref;
        if (messageRef) {
            // #/channels/{channelId}/messages/{messageId} 형식
            const parts = messageRef.split('/');
            const messageId = parts[parts.length - 1];
            
            // operation key에서 메시지 이름 추출 시도
            const operationKey = Object.keys(asyncApiDoc.operations || {}).find(key => {
                const op = asyncApiDoc.operations[key];
                return op && op.messages && op.messages.some(m => m.$ref === messageRef);
            });
            
            if (operationKey) {
                result.name = extractReadableMessageName(operationKey);
            } else {
                result.name = extractReadableMessageName(messageId);
            }
            
            // channel.messages에서 해당 messageId 찾기
            if (channel.messages && channel.messages[messageId]) {
                const channelMessageRef = channel.messages[messageId].$ref;
                if (channelMessageRef) {
                    // #/components/messages/{messageKey} 형식
                    const messageKey = channelMessageRef.replace('#/components/messages/', '');
                    result.messageKey = messageKey;
                    
                    // components.messages에서 실제 메시지 정보 가져오기
                    if (asyncApiDoc.components && asyncApiDoc.components.messages && asyncApiDoc.components.messages[messageKey]) {
                        const message = asyncApiDoc.components.messages[messageKey];
                        const payloadRef = message.payload?.$ref;
                        if (payloadRef) {
                            const schemaName = payloadRef.replace('#/components/schemas/', '');
                            if (asyncApiDoc.components.schemas && asyncApiDoc.components.schemas[schemaName]) {
                                result.schema = asyncApiDoc.components.schemas[schemaName];
                            }
                        }
                    }
                }
            }
        }
    } else if (channel.messages) {
        // operation에 messages가 없으면 channel의 messages에서 첫 번째 메시지 가져오기
        const messageEntries = Object.entries(channel.messages);
        if (messageEntries.length > 0) {
            const [messageId, messageRef] = messageEntries[0];
            result.name = extractReadableMessageName(messageId);
            
            // 메시지 스키마 찾기
            if (messageRef.$ref) {
                const messageKey = messageRef.$ref.replace('#/components/messages/', '');
                result.messageKey = messageKey;
                
                if (asyncApiDoc.components && asyncApiDoc.components.messages && asyncApiDoc.components.messages[messageKey]) {
                    const message = asyncApiDoc.components.messages[messageKey];
                    const payloadRef = message.payload?.$ref;
                    if (payloadRef) {
                        const schemaName = payloadRef.replace('#/components/schemas/', '');
                        if (asyncApiDoc.components.schemas && asyncApiDoc.components.schemas[schemaName]) {
                            result.schema = asyncApiDoc.components.schemas[schemaName];
                        }
                    }
                }
            }
        }
    }
    
    return result;
}

// 스키마에서 예시 JSON 생성
function generateExampleFromSchema(schema) {
    if (!schema || !schema.type) {
        return { example: 'value' };
    }

    // array 타입 처리
    if (schema.type === 'array') {
        if (schema.items) {
            const itemExample = generateExampleFromSchema(schema.items);
            return [itemExample];
        } else {
            return [];
        }
    }

    // object 타입 처리
    if (schema.type === 'object') {
        if (!schema.properties) {
            return {};
        }

        const example = {};
        Object.entries(schema.properties).forEach(([key, prop]) => {
            if (prop.type === 'string') {
                if (prop.format === 'date-time') {
                    example[key] = new Date().toISOString();
                } else if (key.toLowerCase().includes('id')) {
                    example[key] = 'example-id-123';
                } else if (key.toLowerCase().includes('email')) {
                    example[key] = 'example@example.com';
                } else if (key.toLowerCase().includes('url')) {
                    example[key] = 'https://example.com';
                } else {
                    example[key] = prop.example || `example_${key}`;
                }
            } else if (prop.type === 'number' || prop.type === 'integer') {
                example[key] = prop.example !== undefined ? prop.example : (prop.type === 'integer' ? 123 : 123.45);
            } else if (prop.type === 'boolean') {
                example[key] = prop.example !== undefined ? prop.example : true;
            } else if (prop.type === 'array') {
                if (prop.items) {
                    const itemExample = generateExampleFromSchema(prop.items);
                    example[key] = [itemExample];
                } else {
                    example[key] = [];
                }
            } else if (prop.type === 'object') {
                example[key] = generateExampleFromSchema(prop);
            } else {
                example[key] = null;
            }
        });

        return example;
    }

    // 기본 타입 처리
    if (schema.type === 'string') {
        return schema.example || 'example';
    } else if (schema.type === 'number' || schema.type === 'integer') {
        return schema.example !== undefined ? schema.example : (schema.type === 'integer' ? 0 : 0.0);
    } else if (schema.type === 'boolean') {
        return schema.example !== undefined ? schema.example : false;
    }

    return { example: 'value' };
}

// 채널 목록 렌더링 (구독 가능한 채널만 표시)
function renderChannels() {
    if (!asyncApiDoc || !asyncApiDoc.channels) {
        channelsList.innerHTML = '<p class="empty">구독 가능한 채널이 없습니다</p>';
        return;
    }

    const replyChannelAddresses = getReplyChannelAddresses();
    const allChannels = Object.entries(asyncApiDoc.channels);
    const channels = allChannels.filter(([channelId, channel]) => {
        const address = channel.address || channelId;
        return findReceiveOperation(address) || replyChannelAddresses.has(address);
    });

    if (channels.length === 0) {
        channelsList.innerHTML = '<p class="empty">구독 가능한 채널이 없습니다</p>';
        return;
    }

    channelsList.innerHTML = channels.map(([channelId, channel]) => {
        const address = channel.address || channelId;
        const messages = channel.messages || {};
        const messageKeys = Object.keys(messages);
        const hasSendOperation = findSendOperation(address);
        const hasReceiveOperation = findReceiveOperation(address);
        const isReplyChannel = replyChannelAddresses.has(address);
        const isSubscribable = hasReceiveOperation || isReplyChannel;
        
        // 구독 상태 확인: 템플릿 address로 시작하는 구독이 있는지 확인
        const subscribedItems = Array.from(subscriptions.entries())
            .filter(([dest, subInfo]) => {
                // 정확히 일치하거나, 템플릿 주소 패턴과 매칭되는지 확인
                if (dest === address) return true;
                // 파라미터가 있는 경우: 템플릿 주소로 시작하는지 확인
                if (channel.parameters) {
                    const templatePattern = address.replace(/\{[^}]+\}/g, '[^/]+');
                    const regex = new RegExp('^' + templatePattern + '$');
                    return regex.test(dest);
                }
                return false;
            })
            .map(([dest, subInfo]) => ({
                destination: dest,
                subscribedAt: subInfo.subscribedAt,
                params: subInfo.params || {},
                templateAddress: subInfo.templateAddress || dest
            }));
        
        const isSubscribed = subscribedItems.length > 0;

        // 구독 리스트 HTML 생성
        let subscriptionListHtml = '';
        if (isSubscribed && subscribedItems.length > 0) {
            subscriptionListHtml = '<div class="subscription-list">';
            subscribedItems.forEach((item) => {
                const timeStr = item.subscribedAt.toLocaleTimeString('ko-KR');
                const paramsStr = Object.keys(item.params).length > 0 
                    ? Object.entries(item.params).map(([k, v]) => `${k}=${v}`).join(', ')
                    : '';
                
                subscriptionListHtml += `
                    <div class="subscription-item">
                        <div class="subscription-info">
                            <span class="subscription-time">${timeStr}</span>
                            ${paramsStr ? `<span class="subscription-params">(${paramsStr})</span>` : ''}
                        </div>
                        <button class="btn btn-unsubscribe btn-small" onclick="unsubscribe('${item.destination}')">
                            취소
                        </button>
                    </div>
                `;
            });
            subscriptionListHtml += '</div>';
        }

        return `
            <div class="channel-card ${isSubscribed ? 'subscribed' : ''}">
                <div class="channel-card-content">
                    <div class="address">${address}</div>
                    ${isSubscribable ? `
                        <button class="btn btn-subscribe btn-small" onclick="openSubscribeModal('${channelId}', '${address}')">
                            구독
                        </button>
                    ` : ''}
                </div>
                ${subscriptionListHtml}
            </div>
        `;
    }).join('');
}

// SEND 작업 찾기
function findSendOperation(address) {
    if (!asyncApiDoc.operations) return false;
    return Object.values(asyncApiDoc.operations).some(op => {
        if (op.action === 'receive') {
            const channelRef = op.channel?.$ref;
            if (channelRef) {
                const channelId = channelRef.replace('#/channels/', '');
                const channel = asyncApiDoc.channels[channelId];
                return channel && channel.address === address;
            }
        }
        return false;
    });
}

// RECEIVE 작업 찾기 (서버가 보내는 채널 = 클라이언트가 구독하는 채널)
function findReceiveOperation(address) {
    if (!asyncApiDoc.operations) return false;
    return Object.values(asyncApiDoc.operations).some(op => {
        if (op.action === 'send') {
            const channelRef = op.channel?.$ref;
            if (channelRef) {
                const channelId = channelRef.replace('#/channels/', '');
                const channel = asyncApiDoc.channels[channelId];
                return channel && channel.address === address;
            }
        }
        return false;
    });
}

// operation.reply로 참조되는 채널 주소 집합 (reply 채널은 별도 operation 없이 reply로만 등장)
function getReplyChannelAddresses() {
    const addresses = new Set();
    if (!asyncApiDoc.operations || !asyncApiDoc.channels) return addresses;
    Object.values(asyncApiDoc.operations).forEach(op => {
        const replyChannelRef = op.reply?.channel?.$ref;
        if (replyChannelRef) {
            const channelId = replyChannelRef.replace('#/channels/', '');
            const channel = asyncApiDoc.channels[channelId];
            if (channel?.address) {
                addresses.add(channel.address);
            }
        }
    });
    return addresses;
}

// 연결 실패 시 사용자 안내 (URL + 원인 + 체크리스트)
function showConnectionError(attemptedUrl, reason, code) {
    const codeHint = code === 1002 ? ' (프로토콜 오류: 서버가 연결을 거부했거나 경로/설정이 맞지 않을 수 있음)' : '';
    const isFileProtocol = typeof window !== 'undefined' && window.location?.protocol === 'file:';
    const fileHint = isFileProtocol
        ? '\n\n※ 이 페이지를 file://로 열었다면, 서버 주소로 열어보세요. 예: http://localhost:8080/websocket-docs/'
        : '';
    const msg =
        'WebSocket 연결 실패' + codeHint + '\n\n' +
        '시도한 URL: ' + attemptedUrl + '\n' +
        '사유: ' + reason + '\n\n' +
        '확인할 것:\n' +
        '1. 백엔드 서버가 실행 중인지\n' +
        '2. URL의 host/port가 실제 서버와 같은지 (context-path가 있으면 /ws 앞에 경로가 붙어야 함)\n' +
        '3. 이 페이지를 서버에서 연 주소와 동일한 origin인지 (예: http://localhost:8080/websocket-docs/)' +
        fileHint;
    alert(msg);
}

// WebSocket 연결
function connect() {
    if (!metadata || !metadata.websocketUrl) {
        console.error('Metadata:', metadata);
        alert('WebSocket URL을 가져올 수 없습니다. AsyncAPI 문서를 먼저 로드해주세요.');
        return;
    }
    
    let url = metadata.websocketUrl;
    console.log('연결 시도:', url);

    // 토큰을 쿼리 파라미터로 추가
    const token = accessTokenInput.value.trim();
    if (token) {
        // Bearer 접두사 제거 (있는 경우)
        const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
        const separator = url.includes('?') ? '&' : '?';
        url = url + separator + 'access_token=' + encodeURIComponent(cleanToken);
    }

    // SockJS를 사용한 연결
    const socket = new SockJS(url);

    // 나가는 STOMP 프레임(CONNECT, SEND, SUBSCRIBE 등) 로그에 남기기. heartbeat(핑)는 제외.
    const originalSend = socket.send.bind(socket);
    socket.send = function(data) {
        if (data != null) {
            const raw = typeof data === 'string' ? data : (data instanceof ArrayBuffer ? new TextDecoder().decode(data) : String(data));
            const isHeartbeat = raw === '\n' || raw === '\r\n' || /^[\r\n]+$/.test(raw);
            if (!isHeartbeat) {
                const display = raw.endsWith('\u0000') ? raw.slice(0, -1) + '\n^@' : raw;
                addLogEntry('OUT', display);
            }
        }
        return originalSend(data);
    };

    // SockJS 연결 에러 처리
    const attemptedUrl = url;
    socket.onerror = (error) => {
        console.error('SockJS 연결 오류:', error);
        showConnectionError(attemptedUrl, 'SockJS 오류', null);
        updateConnectionStatus(false);
        connectBtn.disabled = false;
        disconnectBtn.disabled = true;
    };
    
    socket.onclose = (event) => {
        console.log('SockJS 연결 종료:', event);
        if (!stompClient || !stompClient.connected) {
            // 연결 전에 닫힌 경우 = 연결 실패
            const reason = event.reason || ('code ' + event.code);
            showConnectionError(attemptedUrl, reason, event.code);
        }
        if (stompClient) {
            stompClient = null;
            currentSessionId = null;
            updateConnectionStatus(false);
            connectBtn.disabled = false;
            disconnectBtn.disabled = true;
            subscriptions.clear();
            renderChannels();
        }
        addLogEntry('IN', '연결 종료 (code=' + (event.code || '') + ', reason=' + (event.reason || '') + ')');
    };
    
    const client = new StompJs.Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
            console.log('STOMP:', str);
        },
    });

    // Authorization 헤더도 설정 (STOMP CONNECT 프레임용)
    if (token) {
        const authHeader = token.startsWith('Bearer ') ? token : 'Bearer ' + token;
        client.connectHeaders = {
            'Authorization': authHeader
        };
    }

    client.onConnect = (frame) => {
        console.log('연결 성공:', frame);
        // Spring STOMP 서버는 CONNECTED 프레임에 session 헤더로 WebSocket session ID를 내려줌
        currentSessionId = frame.headers?.['session'] ?? frame.headers?.['Session'] ?? null;
        stompClient = client;
        updateConnectionStatus(true);
        connectBtn.disabled = true;
        disconnectBtn.disabled = false;
        const raw = frameToRaw('CONNECTED', frame.headers || {}, frame.body ?? '');
        addLogEntry('IN', raw);
    };

    client.onStompError = (frame) => {
        console.error('STOMP 오류:', frame);
        let errorMsg = frame.headers['message'] || '알 수 없는 오류';
        let body = frame.body;
        if (body) {
            try {
                const parsed = JSON.parse(body);
                if (parsed.code != null || parsed.message != null) {
                    body = parsed;
                    errorMsg = parsed.message || parsed.code || errorMsg;
                }
            } catch (_) { /* body 그대로 사용 */ }
        }
        const errHeaders = frame.headers || {};
        const errRaw = frameToRaw('ERROR', errHeaders, typeof body === 'string' ? body : JSON.stringify(body || { message: errorMsg }));
        addLogEntry('IN', errRaw);
        alert('서버 오류: ' + errorMsg);
        // SEND 처리 중 발생한 ERROR는 연결 유지 (CONNECT 실패 등만 연결 해제)
        const isConnectError = frame.headers['message']?.includes('CONNECT') || frame.headers['message']?.includes('Unauthorized');
        if (isConnectError) {
            updateConnectionStatus(false);
            connectBtn.disabled = false;
            disconnectBtn.disabled = true;
        }
    };

    client.onWebSocketClose = (event) => {
        console.log('연결 종료:', event);
        if (!stompClient?.connected) {
            const reason = event.reason || ('code ' + (event.code || ''));
            showConnectionError(attemptedUrl, reason, event.code);
        }
        stompClient = null;
        currentSessionId = null;
        updateConnectionStatus(false);
        connectBtn.disabled = false;
        disconnectBtn.disabled = true;
        subscriptions.clear();
        renderChannels();
        addLogEntry('IN', '연결 종료 (code=' + (event.code || '') + ', reason=' + (event.reason || '') + ')');
    };

    try {
        client.activate();
    } catch (error) {
        console.error('연결 활성화 실패:', error);
        alert('연결 활성화에 실패했습니다: ' + error.message);
        updateConnectionStatus(false);
        connectBtn.disabled = false;
        disconnectBtn.disabled = true;
    }
}

// WebSocket 연결 해제
function disconnect() {
    if (stompClient) {
        // 모든 구독 해제
        subscriptions.forEach((subInfo, address) => {
            try {
                subInfo.subscription.unsubscribe();
            } catch (e) {
                console.error('구독 해제 실패:', e);
            }
        });
        subscriptions.clear();
        renderChannels();

        stompClient.deactivate();
        stompClient = null;
        currentSessionId = null;
        updateConnectionStatus(false);
        connectBtn.disabled = false;
        disconnectBtn.disabled = true;
    }
}

// 구독 모달 열기
function openSubscribeModal(channelId, address) {
    if (!stompClient || !stompClient.connected) {
        alert('먼저 WebSocket에 연결해주세요');
        return;
    }

    // 채널 정보 찾기
    const channel = asyncApiDoc.channels[channelId];
    if (!channel) {
        alert('채널 정보를 찾을 수 없습니다');
        return;
    }

    // 파라미터가 없으면 바로 구독
    if (!channel.parameters || Object.keys(channel.parameters).length === 0) {
        subscribe(address, address, {});
        return;
    }

    // 파라미터가 있으면 모달 표시
    const parameters = channel.parameters;
    const paramKeys = Object.keys(parameters);
    
    let formFields = '';
    paramKeys.forEach(paramKey => {
        const param = parameters[paramKey];
        const description = param.description || '';
        const example = param.schema?.example || '';
        formFields += `
            <div class="form-group">
                <label for="param_${paramKey}">${paramKey}${description ? `: ${description}` : ''}</label>
                <input type="text" id="param_${paramKey}" placeholder="${example || `예: ${paramKey} 값`}" value="${example || ''}">
            </div>
        `;
    });

    const modal = document.createElement('div');
    modal.className = 'modal active';
    modal.innerHTML = `
        <div class="modal-content">
            <h3>구독: ${channelId}</h3>
            <p style="margin-bottom: 15px; color: #666; font-size: 0.9em;">
                <strong>주소:</strong> <code>${address}</code>
            </p>
            ${formFields}
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="this.closest('.modal').remove()">취소</button>
                <button class="btn btn-primary" onclick="confirmSubscribe('${channelId}', '${address}')">구독</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

// 구독 확인 및 실행
function confirmSubscribe(channelId, address) {
    const modal = document.querySelector('.modal.active');
    if (!modal) return;

    const channel = asyncApiDoc.channels[channelId];
    if (!channel || !channel.parameters) {
        subscribe(address);
        modal.remove();
        return;
    }

    // 파라미터 값 수집
    const params = {};
    let hasError = false;
    Object.keys(channel.parameters).forEach(paramKey => {
        const input = document.getElementById(`param_${paramKey}`);
        if (input) {
            const value = input.value.trim();
            if (!value) {
                alert(`${paramKey} 값을 입력해주세요`);
                hasError = true;
                return;
            }
            params[paramKey] = value;
        }
    });

    if (hasError) return;

    // 실제 destination 생성
    let destination = address;
    Object.keys(params).forEach(paramKey => {
        destination = destination.replace(`{${paramKey}}`, params[paramKey]);
    });

    modal.remove();
    subscribe(destination, address, params);
}

// 채널 구독
function subscribe(destination, templateAddress = null, params = {}) {
    if (!stompClient || !stompClient.connected) {
        alert('먼저 WebSocket에 연결해주세요');
        return;
    }

    if (subscriptions.has(destination)) {
        alert('이미 구독 중인 채널입니다');
        return;
    }

    try {
        const subscription = stompClient.subscribe(destination, (message) => {
            const raw = frameToRaw(message.command || 'MESSAGE', message.headers || {}, message.body ?? '');
            addLogEntry('IN', raw);
        });

        // 구독 정보를 객체로 저장
        subscriptions.set(destination, {
            subscription: subscription,
            subscribedAt: new Date(),
            params: params,
            templateAddress: templateAddress || destination
        });
        renderChannels();
        // 로그는 socket.send 래퍼에서 한 번만 남김 (수동 로그 제거 시 중복 방지)
    } catch (error) {
        console.error('구독 실패:', error);
        alert('구독 실패: ' + error.message);
    }
}

// 구독 해제
function unsubscribe(address) {
    const subInfo = subscriptions.get(address);
    if (subInfo) {
        try {
            subInfo.subscription.unsubscribe();
            subscriptions.delete(address);
            renderChannels();
            // 로그는 socket.send 래퍼에서 한 번만 남김 (수동 로그 제거 시 중복 방지)
        } catch (error) {
            console.error('구독 해제 실패:', error);
            alert('구독 해제 실패: ' + error.message);
        }
    }
}

// 문서 내 "보내기" 버튼에서 호출 (data-address, data-message-key 사용)
function openSendModalFromButton(buttonEl) {
    const address = (buttonEl.dataset.address || '').replace(/&quot;/g, '"');
    const messageKey = (buttonEl.dataset.messageKey || '').replace(/&quot;/g, '"');
    openSendModal(address, messageKey);
}

// 메시지 전송 모달 열기
function openSendModal(address, messageType) {
    if (!stompClient || !stompClient.connected) {
        alert('먼저 WebSocket에 연결해주세요');
        return;
    }

    // 메시지 스키마 가져오기 (없으면 빈 객체로 전송 가능)
    const schema = messageType ? getMessageSchema(messageType) : null;
    const example = schema ? generateExampleMessage(schema) : {};

    const modal = document.createElement('div');
    modal.className = 'modal active';
    modal.innerHTML = `
        <div class="modal-content">
            <h3>메시지 전송: ${escapeHtml(address)}</h3>
            <div class="form-group">
                <label>메시지 (JSON):</label>
                <textarea id="messagePayload" placeholder='예: ${escapeHtml(JSON.stringify(example, null, 2))}'>${escapeHtml(JSON.stringify(example, null, 2))}</textarea>
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="this.closest('.modal').remove()">취소</button>
                <button class="btn btn-primary" onclick="sendMessageFromModal(this.closest('.modal'))" data-address="${escapeHtml(address)}">전송</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

function escapeHtml(str) {
    if (str == null) return '';
    const s = String(str);
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// 모달에서 전송 버튼 클릭 시 (주소는 data-address로 전달)
function sendMessageFromModal(modalEl) {
    if (!modalEl || !stompClient || !stompClient.connected) return;
    const payloadTextarea = modalEl.querySelector('#messagePayload');
    const addressEl = modalEl.querySelector('[data-address]');
    const address = addressEl ? addressEl.getAttribute('data-address') : null;
    if (!address) return;

    const payloadText = payloadTextarea ? payloadTextarea.value.trim() : '';
    const body = payloadText ? payloadText : '{}';

    try {
        const payload = JSON.parse(body);
        let destination = address;
        const channel = findChannelByAddress(address);
        if (channel && channel.parameters) {
            Object.keys(channel.parameters).forEach(param => {
                if (payload[param]) {
                    destination = destination.replace(`{${param}}`, payload[param]);
                }
            });
        }

        const receiptId = 'send-' + Date.now();
        const headers = { receipt: receiptId, 'content-type': 'application/json' };
        const bodyStr = JSON.stringify(payload);
        stompClient.publish({
            destination: destination,
            headers: headers,
            body: bodyStr
        });
        // 로그는 socket.send 래퍼에서 한 번만 남김 (수동 로그 제거 시 중복 방지)
        modalEl.remove();
    } catch (error) {
        alert('메시지 전송 실패: ' + error.message);
        console.error('전송 오류:', error);
    }
}

// 메시지 전송 (기존 호환용)
function sendMessage(address) {
    const modal = document.querySelector('.modal.active');
    if (!modal) return;
    const payloadTextarea = document.getElementById('messagePayload');
    const payloadText = payloadTextarea ? payloadTextarea.value.trim() : '{}';
    if (!address) return;
    try {
        const payload = JSON.parse(payloadText || '{}');
        let destination = address;
        const channel = findChannelByAddress(address);
        if (channel && channel.parameters) {
            Object.keys(channel.parameters).forEach(param => {
                if (payload[param]) {
                    destination = destination.replace(`{${param}}`, payload[param]);
                }
            });
        }
        stompClient.publish({ destination: destination, body: JSON.stringify(payload) });
        addMessage(address, '메시지 전송', payload);
        modal.remove();
    } catch (error) {
        alert('메시지 전송 실패: ' + error.message);
        console.error('전송 오류:', error);
    }
}

// 주소로 채널 찾기
function findChannelByAddress(address) {
    if (!asyncApiDoc || !asyncApiDoc.channels) return null;
    return Object.values(asyncApiDoc.channels).find(ch => ch.address === address);
}

// 메시지 스키마 가져오기
function getMessageSchema(messageType) {
    if (!asyncApiDoc || !asyncApiDoc.components || !asyncApiDoc.components.messages) {
        return null;
    }
    const message = asyncApiDoc.components.messages[messageType];
    if (!message || !message.payload) return null;
    
    const schemaRef = message.payload.$ref;
    if (!schemaRef) return null;
    
    const schemaName = schemaRef.replace('#/components/schemas/', '');
    return asyncApiDoc.components.schemas[schemaName];
}

// 예제 메시지 생성
function generateExampleMessage(schema) {
    if (!schema || !schema.type) {
        return { example: 'value' };
    }

    // array 타입 처리
    if (schema.type === 'array') {
        if (schema.items) {
            const itemExample = generateExampleMessage(schema.items);
            return [itemExample];
        } else {
            return [];
        }
    }

    // object 타입 처리
    if (schema.type === 'object') {
        if (!schema.properties) {
            return {};
        }

        const example = {};
        Object.entries(schema.properties).forEach(([key, prop]) => {
            if (prop.type === 'string') {
                example[key] = prop.example || `example_${key}`;
            } else if (prop.type === 'number' || prop.type === 'integer') {
                example[key] = prop.example || 0;
            } else if (prop.type === 'boolean') {
                example[key] = prop.example || false;
            } else if (prop.type === 'array') {
                if (prop.items) {
                    const itemExample = generateExampleMessage(prop.items);
                    example[key] = [itemExample];
                } else {
                    example[key] = [];
                }
            } else if (prop.type === 'object') {
                example[key] = generateExampleMessage(prop);
            } else {
                example[key] = null;
            }
        });

        return example;
    }

    // 기본 타입 처리
    if (schema.type === 'string') {
        return schema.example || 'example';
    } else if (schema.type === 'number' || schema.type === 'integer') {
        return schema.example !== undefined ? schema.example : 0;
    } else if (schema.type === 'boolean') {
        return schema.example !== undefined ? schema.example : false;
    }

    return { example: 'value' };
}


// STOMP 프레임을 raw 문자열로 변환 (패치노트 예시와 동일: command + 헤더 + 빈 줄 + body + ^@)
function frameToRaw(command, headers, body) {
    const headerLines = headers && typeof headers === 'object'
        ? Object.entries(headers).map(([k, v]) => `${k}:${v}`).join('\n')
        : '';
    const bodyStr = body != null ? String(body) : '';
    const frame = headerLines ? `${command}\n${headerLines}\n\n${bodyStr}` : `${command}\n\n${bodyStr}`;
    return frame + '\n^@';
}

// 로컬 시간 HH:mm:ss.SSS (toISOString은 UTC라 9시간 차이 남)
function formatLocalTimeHHmmssSSS(date) {
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    const s = date.getSeconds().toString().padStart(2, '0');
    const ms = date.getMilliseconds().toString().padStart(3, '0');
    return `${h}:${m}:${s}.${ms}`;
}

// Grafana 로그 스타일 엔트리 추가 (가능한 raw 형식)
function addLogEntry(direction, rawContent) {
    const messageItem = document.createElement('div');
    messageItem.className = 'log-line';
    const now = new Date();
    const timeStr = formatLocalTimeHHmmssSSS(now);
    const directionClass = direction === 'OUT' ? 'out' : 'in';
    const directionLabel = direction === 'OUT' ? 'Client -> Server' : 'Server -> Client';
    messageItem.innerHTML = `
        <span class="log-time-direction ${directionClass}" title="${now.toISOString()} (UTC)">${timeStr}<br>${directionLabel}</span>
        <pre class="log-raw">${escapeHtml(rawContent)}</pre>
    `;

    const emptyText = messagesList.querySelector('.empty');
    if (emptyText) emptyText.remove();

    messagesList.insertBefore(messageItem, messagesList.firstChild);
}

// 기존 addMessage 호환: data를 raw 문자열로 변환해 로그 추가 (direction은 title로 추론)
function addMessage(channel, title, data) {
    const direction = title === '메시지 전송' ? 'OUT' : 'IN';
    const rawContent = typeof data === 'string'
        ? data
        : (data && data.raw != null)
            ? data.raw
            : (data && typeof data === 'object' && Object.keys(data).length === 0)
                ? title
                : `${title}\n${JSON.stringify(data, null, 2)}`;
    addLogEntry(direction, rawContent);
}

// 메시지 지우기
function clearMessages() {
    messagesList.innerHTML = '<p class="empty">메시지 로그가 비어 있습니다</p>';
}

// 연결 상태 업데이트 (연결 시 session ID 표시)
function updateConnectionStatus(connected) {
    const statusDot = connectionStatus.querySelector('.status-dot');
    const statusText = connectionStatus.querySelector('span:last-child');
    const tokenInputGroup = document.querySelector('.token-input-group');
    const sessionIdEl = document.getElementById('connectionSessionId');
    
    if (connected) {
        statusDot.className = 'status-dot connected';
        statusText.textContent = '연결됨';
        if (sessionIdEl) {
            sessionIdEl.textContent = currentSessionId ? `session: ${currentSessionId}` : '';
            sessionIdEl.title = currentSessionId || 'session ID 없음';
        }
        if (tokenInputGroup) {
            tokenInputGroup.style.display = 'none';
        }
    } else {
        statusDot.className = 'status-dot disconnected';
        statusText.textContent = '연결 안 됨';
        if (sessionIdEl) {
            sessionIdEl.textContent = '';
            sessionIdEl.title = '';
        }
        if (tokenInputGroup) {
            tokenInputGroup.style.display = 'flex';
        }
    }
}

// 드롭다운 토글
function toggleDropdown(id) {
    const sectionElement = document.getElementById(`${id}-section`);
    if (!sectionElement) return;
    
    sectionElement.classList.toggle('collapsed');
}

// 채널 토글 함수
function toggleChannel(channelId) {
    const content = document.getElementById(`${channelId}-content`);
    const toggle = document.getElementById(`${channelId}-toggle`);
    if (content && toggle) {
        if (content.style.display === 'none') {
            content.style.display = 'block';
            toggle.textContent = '▼';
            toggle.style.transform = 'rotate(0deg)';
        } else {
            content.style.display = 'none';
            toggle.textContent = '▶';
            toggle.style.transform = 'rotate(-90deg)';
        }
    }
}

// 전역 함수로 노출 (HTML에서 호출하기 위해)
window.subscribe = subscribe;
window.unsubscribe = unsubscribe;
window.openSubscribeModal = openSubscribeModal;
window.confirmSubscribe = confirmSubscribe;
window.openSendModal = openSendModal;
window.openSendModalFromButton = openSendModalFromButton;
window.sendMessage = sendMessage;
window.sendMessageFromModal = sendMessageFromModal;
window.toggleDropdown = toggleDropdown;
window.toggleChannel = toggleChannel;
