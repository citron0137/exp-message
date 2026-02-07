import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';

export type MessageWsDetail = {
    id: string;
    chatRoomId: string;
    userId: string;
    content: string;
    createdAt: string;
    recipientUserId: string;
};

let client: Client | null = null;
let subscription: StompSubscription | null = null;
let connectedKey: string | null = null;

const buildWsUrl = (wsUrl: string, accessToken: string): string => {
    const delimiter = wsUrl.includes('?') ? '&' : '?';
    return `${wsUrl}${delimiter}access_token=${encodeURIComponent(accessToken)}`;
};

const safeParseJson = (raw: string): unknown => {
    try {
        return JSON.parse(raw);
    } catch {
        return null;
    }
};

const isMessageWsDetail = (data: unknown): data is MessageWsDetail => {
    if (!data || typeof data !== 'object') {
        return false;
    }

    const d = data as Partial<MessageWsDetail>;
    return (
        typeof d.id === 'string' &&
        typeof d.chatRoomId === 'string' &&
        typeof d.userId === 'string' &&
        typeof d.content === 'string' &&
        typeof d.createdAt === 'string' &&
        typeof d.recipientUserId === 'string'
    );
};

const disposeSubscription = (): void => {
    if (!subscription) {
        return;
    }

    try {
        subscription.unsubscribe();
    } catch {
        // ignore
    } finally {
        subscription = null;
    }
};

const disposeClient = async (): Promise<void> => {
    if (!client) {
        return;
    }

    disposeSubscription();

    const current = client;
    client = null;
    connectedKey = null;

    try {
        await current.deactivate();
    } catch {
        // ignore
    }
};

/**
 * WebSocket/STOMP connection manager.
 *
 * Backend notes (from tests/config):
 * - Endpoint: /ws
 * - Auth: access_token query parameter (preferred) OR Authorization header
 * - Subscription is only allowed for your own user topic:
 *   `/topic/user/{principalUserId}/...`
 */
export const wsClient = {
    async connect(params: {
        wsUrl: string;
        accessToken: string;
        userId: string;
        onMessage: (message: MessageWsDetail) => void;
        onError?: (message: string) => void;
    }): Promise<void> {
        const key = `${params.wsUrl}::${params.userId}::${params.accessToken}`;
        if (connectedKey === key) {
            return;
        }

        await disposeClient();

        const urlWithToken = buildWsUrl(params.wsUrl, params.accessToken);

        const newClient = new Client({
            brokerURL: urlWithToken,
            reconnectDelay: 3000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            debug: () => {
                // Intentionally disabled.
            },
        });

        newClient.onStompError = (frame) => {
            params.onError?.(frame.headers['message'] ?? 'STOMP error');
        };

        newClient.onWebSocketClose = () => {
            params.onError?.('WebSocket connection closed');
        };

        newClient.onConnect = () => {
            disposeSubscription();

            const destination = `/topic/user/${params.userId}/messages`;
            subscription = newClient.subscribe(destination, (message: IMessage) => {
                const parsed = safeParseJson(message.body);
                if (!isMessageWsDetail(parsed)) {
                    return;
                }

                params.onMessage(parsed);
            });
        };

        client = newClient;
        connectedKey = key;

        await newClient.activate();
    },

    async disconnect(): Promise<void> {
        await disposeClient();
    },
};
