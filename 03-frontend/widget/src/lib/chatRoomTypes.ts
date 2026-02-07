export type ChatRoomListItem = {
    id: string;
    name: string;
    createdByUserId: string;
    createdAt: string;
    updatedAt: string;
};

export type ChatRoomCreateRequest = {
    name: string;
};

export type ChatRoomCreateResponse = {
    id: string;
    name: string;
    createdByUserId: string;
    createdAt: string;
    updatedAt: string;
};
