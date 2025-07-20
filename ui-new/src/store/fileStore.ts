import { create } from 'zustand';
import { FileItem, FolderStructure } from '../types';
import { safeSessionStorage, STORAGE_KEYS } from './safeSessionStorage';
import { fileServiceInstance as fileService } from '../lib/services';
import { useFileContextStore } from './fileContextStore';
import { websocketService } from '../lib/websocket';

interface FileStore {
  currentDirectory: FileItem;
  folderStructure: FolderStructure;
  expandedFolders: string[];
  isLoading: boolean;
  setCurrentDirectory: (directory: FileItem) => void;
  setFolderStructure: (structure: FolderStructure) => void;
  setExpandedFolders: (folders: string[]) => void;
  addExpandedFolder: (folder: string) => void;
  removeExpandedFolder: (folder: string) => void;
  fetchInitialState: () => Promise<void>;
}

const initialState: Pick<FileStore, 'currentDirectory' | 'folderStructure' | 'expandedFolders'> = {
  currentDirectory: {id: '', name: '', type: 'folder', subfolders: []},
  folderStructure: {root: {id: '', name: '', type: 'folder', subfolders: []}, shared: {id: '', name: '', type: 'folder', subfolders: []}, trash: {id: '', name: '', type: 'folder', subfolders: []}},
  expandedFolders: [],
};

const getInitialState = async (): Promise<Partial<FileStore>> => {
  const section = useFileContextStore.getState().getSection();
  const token = localStorage.getItem('accessToken');
if (!token) {
    return initialState;
  }

  const folderStructure = await fileService.getFolderStructure();
  folderStructure.shared.id = 'shared';
  folderStructure.trash.id = 'trash';

  let currentDirectoryId = safeSessionStorage.getItem(STORAGE_KEYS.CURRENT_DIRECTORY_ID);
  if(currentDirectoryId === 'null' || !currentDirectoryId) {
    currentDirectoryId = null;
  }
  
  const deleted = section === 'trash';
  const currentDirectory = currentDirectoryId
    ? await fileService.getFolderById(currentDirectoryId, deleted)
    : await fileService.getRootFolder();
  const expandedFolders = JSON.parse(
    safeSessionStorage.getItem(STORAGE_KEYS.EXPANDED_FOLDERS) || '[]'
  ) as string[];

  websocketService.connect();

  return {
    currentDirectory,
    folderStructure,
    expandedFolders
  };
};

function deepFreeze<T>(obj: T): T {
  Object.freeze(obj);
  if (typeof obj === 'object' && obj !== null) {
    Object.getOwnPropertyNames(obj).forEach(prop => {
      const value = (obj as Record<string, unknown>)[prop];
      if (
        value !== null &&
        (typeof value === "object" || typeof value === "function") &&
        !Object.isFrozen(value)
      ) {
        deepFreeze(value);
      }
    });
  }
  return obj;
}

export const useFileStore = create<FileStore>((set, get) => ({
  ...initialState,
  isLoading: true,
  fetchInitialState: async () => {
    const state = await getInitialState();
    set(state);
    set({ ...state, isLoading: false });
  },
  setCurrentDirectory: (directory) => {
    deepFreeze(directory);
    set({ currentDirectory: directory });
    if (directory) {
      safeSessionStorage.setItem(
        STORAGE_KEYS.CURRENT_DIRECTORY_ID,
        directory.id
      );
    }
  },
  setFolderStructure: (structure) => {
    const currentState = get();
    if (JSON.stringify(currentState.folderStructure) !== JSON.stringify(structure)) {
      set({ folderStructure: structure });
    }
  },
  setExpandedFolders: (folders) => {
    const currentState = get();
    if (JSON.stringify(currentState.expandedFolders) !== JSON.stringify(folders)) {
      set({ expandedFolders: folders });
    }
  },
  addExpandedFolder: (folder) => {
    const currentState = get();
    if (!currentState.expandedFolders.includes(folder)) {
      set({ expandedFolders: [...currentState.expandedFolders, folder] });
    }
  },
  removeExpandedFolder: (folder) => {
    const currentState = get();
    if (currentState.expandedFolders.includes(folder)) {
      set({ expandedFolders: currentState.expandedFolders.filter(f => f !== folder) });
    }
  }
})); 

export function useIsCurrentDirectory(file: FileItem) {
  return useFileStore((state) => state.currentDirectory.id === file.id);
}

export function useIsExpandedFolder(folder: FileItem) {
  return useFileStore((state) => state.expandedFolders.includes(folder.id));
}