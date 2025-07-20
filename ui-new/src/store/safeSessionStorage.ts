export const STORAGE_KEYS = {
  CURRENT_DIRECTORY_ID: 'current-directory-id',
  EXPANDED_FOLDERS: 'expanded-folders',
  CONTEXT_STATE: 'file-context-state',
  USER_ID: 'user-id'
} as const;

export type StorageKey = typeof STORAGE_KEYS[keyof typeof STORAGE_KEYS];

export const safeStorageOperation = (operation: () => void) => {
  try {
    operation();
    return true;
  } catch {
    return false;
  }
};

export const safeSessionStorage = {
  getItem: (key: StorageKey): string | null => {
    return window.sessionStorage.getItem(key);
  },
  
  setItem: (key: StorageKey, value: string): void => {
    safeStorageOperation(() => {
      window.sessionStorage.setItem(key, value);
    });
  },
  
  removeItem: (key: StorageKey): void => {
    safeStorageOperation(() => {
      window.sessionStorage.removeItem(key);
    });
  },
  
  clear: (): void => {
    safeStorageOperation(() => {
      window.sessionStorage.clear();
    });
  }
};
