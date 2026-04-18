import { create } from 'zustand';
import { listMyChannels, type WorkspaceChannel } from '@/shared/api/admin-core';
import { toApiErrorMessage } from '@/shared/api/http';

interface WorkspaceState {
  channels: WorkspaceChannel[];
  activeChannelId: string | null;
  loading: boolean;
  errorMessage: string | null;
  loadChannels: () => Promise<void>;
  setActiveChannelId: (channelId: string) => void;
}

export const useWorkspaceStore = create<WorkspaceState>((set, get) => ({
  channels: [],
  activeChannelId: null,
  loading: false,
  errorMessage: null,
  async loadChannels() {
    set({ loading: true, errorMessage: null });
    try {
      const result = await listMyChannels();
      const currentActive = get().activeChannelId;
      const nextActive =
        currentActive && result.items.some((item) => item.channel.id === currentActive)
          ? currentActive
          : result.items[0]?.channel.id ?? null;
      set({
        channels: result.items,
        activeChannelId: nextActive,
        loading: false,
        errorMessage: null,
      });
    } catch (error) {
      set({
        channels: [],
        activeChannelId: null,
        loading: false,
        errorMessage: toApiErrorMessage(error, 'Unable to load workspaces.'),
      });
    }
  },
  setActiveChannelId(channelId) {
    set({ activeChannelId: channelId });
  },
}));
