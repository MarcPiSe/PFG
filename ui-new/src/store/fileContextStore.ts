import { create } from "zustand";
import { STORAGE_KEYS, safeSessionStorage } from '../store/safeSessionStorage';

export interface FileContextState {
  section: 'root' | 'trash' | 'shared';
  setSection: (section: 'root' | 'trash' | 'shared') => void;
  getSection: () => 'root' | 'trash' | 'shared';
  isInSection: (section: 'root' | 'trash' | 'shared') => boolean;
  isInTrash: () => boolean;
  isInShared: () => boolean;
  isInRoot: () => boolean;
  filterByFiles: boolean;
  filterByFolders: boolean;
  sortBy: string | undefined;
  setFilterByFiles: (filterByFiles: boolean) => void;
  setFilterByFolders: (filterByFolders: boolean) => void;
  setSortBy: (sortBy: string) => void;
}

const loadState = (): Partial<FileContextState> => {
  try {
    const serializedState = safeSessionStorage.getItem(STORAGE_KEYS.CONTEXT_STATE);
    if (serializedState === null) {
      return { section: 'root' };
    }
    return JSON.parse(serializedState);
  } catch {
    return { section: 'root' };
  }
};

const saveState = (state: Partial<FileContextState>) => {
  try {
    const serializedState = JSON.stringify(state);
    safeSessionStorage.setItem(STORAGE_KEYS.CONTEXT_STATE, serializedState);
  } catch {
    // eslint-disable-next-line no-console
  }
};

export const useFileContextStore = create<FileContextState>((set, get) => {
  const initialState = loadState();

  return {
    section: initialState.section || 'root',
    setSection: (section: 'root' | 'trash' | 'shared') => {
      const currentState = get();
      if (currentState.section !== section) {
        set({ section });
        saveState({ section });
      }
    },
    getSection: () => get().section,
    isInSection: (section: 'root' | 'trash' | 'shared') => get().section === section,
    isInTrash: () => get().section === 'trash',
    isInShared: () => get().section === 'shared',
    isInRoot: () => get().section === 'root',
    filterByFiles: false,
    filterByFolders: false,
    sortBy: "type",
    setFilterByFiles: (filterByFiles: boolean) => set({ filterByFiles }),
    setFilterByFolders: (filterByFolders: boolean) => set({ filterByFolders }),
    setSortBy: (sortBy: string) => set({ sortBy })
  };
});