import { create } from "zustand";
import { FileItem } from "../types";
import React from "react";

interface FileSelectionState {
  selectedFiles: FileItem[];
  selectedFilesIds: string[];
  lastSelectedFile: string | null;
  setSelectedFiles: (files: FileItem[], last?: FileItem) => void;
  isSelected: (file: FileItem) => boolean;
  toggleSelection: (file: FileItem, event: React.MouseEvent, currentDirectory: FileItem) => void;
  clearSelection: () => void;
  selectAll: (currentDirectory: FileItem) => void;
  clipboard: Set<FileItem>;
  clipboardParentId: string | undefined;
  setClipboardFiles: (files: Set<FileItem>, parentId: string | undefined) => void;
  clearClipboardFiles: () => void;
  isCut: boolean;
  setIsCut: (isCut: boolean) => void;
  copyFiles: (parentId: string) => void;
  cutFiles: (parentId: string) => void;
  isInClipboard: (file: FileItem) => boolean;
}

export const useFileSelectionStore = create<FileSelectionState>((set, get) => ({
  selectedFiles: [],
  selectedFilesIds: [],
  lastSelectedFile: null,
  clipboard: new Set(),
  clipboardParentId: undefined,
  setClipboardFiles: (files, parentId) => {
    set({ clipboard: files });
    set({ clipboardParentId: parentId });
  },
  clearClipboardFiles: () => {
    const currentState = get();
    if (currentState.clipboard.size > 0) {
      set({ clipboard: new Set() });
    }
  },
  setSelectedFiles: (files, last) => {
    const currentState = get();
    const newSelectedFilesIds = files.map(f => f.id);
    if (JSON.stringify(currentState.selectedFilesIds) !== JSON.stringify(newSelectedFilesIds)) {
      set({ selectedFiles: files, selectedFilesIds: newSelectedFilesIds });
    }
    if (last) {
      set({ lastSelectedFile: last.id || null });
    } else {
      set({ lastSelectedFile: files[files.length - 1]?.id || null });
    }
  },
  setIsCut: (isCut) => {
    set({ isCut });
  },
  isCut: false,
  copyFiles: (parentId: string) => {
    const { selectedFiles } = get();
    const newClipboard = new Set(selectedFiles);
    set({ 
      clipboard: newClipboard,
      isCut: false,
      clipboardParentId: parentId
    });
  },
  cutFiles: (parentId: string) => {
    const { selectedFiles } = get();
    const newClipboard = new Set(selectedFiles);
    set({ 
      clipboard: newClipboard,
      isCut: true,
      clipboardParentId: parentId
    });
    set({ selectedFiles: [] });
    set({ selectedFilesIds: [] });
  },
  isSelected: (file) => get().selectedFilesIds.some(f => f === file.id),
  isInClipboard: (file) => get().clipboard.has(file),
  toggleSelection: (file, event, currentDirectory) => {
    const allFiles = [...(currentDirectory.files || []), ...(currentDirectory.subfolders || [])];
    const { selectedFiles, selectedFilesIds, lastSelectedFile } = get();

    if (event.ctrlKey || event.metaKey) {
      const isAlreadySelected = selectedFilesIds.some(f => f === file.id);
      set({
        selectedFilesIds: isAlreadySelected
          ? selectedFilesIds.filter(f => f !== file.id)
          : [...selectedFilesIds, file.id],
        lastSelectedFile: file.id,
        selectedFiles: isAlreadySelected
          ? selectedFiles.filter(f => f.id !== file.id)
          : [...selectedFiles, file]
      });
    } else if (event.shiftKey && lastSelectedFile && allFiles.length > 0) {
      const lastIndex = allFiles.findIndex(f => f.id === lastSelectedFile);
      const currentIndex = allFiles.findIndex(f => f.id === file.id);
      
      if (lastIndex !== -1 && currentIndex !== -1) {
        const [start, end] = [lastIndex, currentIndex].sort((a, b) => a - b);
        const filesInRange = allFiles.slice(start, end + 1);
        
        const hasSelectedFilesInRange = filesInRange.some(f => selectedFilesIds.includes(f.id));
        
        if (hasSelectedFilesInRange) {
          const newSelection = filesInRange.filter(f => !selectedFilesIds.includes(f.id));
          const remainingSelection = selectedFiles.filter(f => !filesInRange.some(rf => rf.id === f.id));
          
          set({
            lastSelectedFile: file.id,
            selectedFiles: [...remainingSelection, ...newSelection],
            selectedFilesIds: [...remainingSelection.map(f => f.id), ...newSelection.map(f => f.id)]
          });
        } else {
          set({
            lastSelectedFile: file.id,
            selectedFiles: [...selectedFiles, ...filesInRange],
            selectedFilesIds: [...selectedFilesIds, ...filesInRange.map(f => f.id)]
          });
        }
      }
    } else {
      set({ selectedFiles: [file], lastSelectedFile: file.id, selectedFilesIds: [file.id] });
    }
  },
  clearSelection: () => set({ selectedFiles: [], lastSelectedFile: null, selectedFilesIds: [] }),
  selectAll: (currentDirectory) => {
    const allFiles = [...(currentDirectory.files || []), ...(currentDirectory.subfolders || [])];
    set({ 
      selectedFiles: allFiles, 
      lastSelectedFile: allFiles[allFiles.length - 1]?.id || null,
      selectedFilesIds: allFiles.map(f => f.id)
    });
  }
}));

export function useIsItemSelected(file: FileItem) {
  return useFileSelectionStore((state) => state.selectedFilesIds.some(f => f === file.id));
}
function isCutFile(file: FileItem, state: FileSelectionState) {
  const res = Array.from(state.clipboard).filter(f => f.id === file.id);
  return res.length > 0 && state.isCut;
}
export function useIsCutFile(file: FileItem) {
  return useFileSelectionStore((state) => isCutFile(file, state));
}