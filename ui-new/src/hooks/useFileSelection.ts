import { useCallback, useRef, RefObject } from 'react';
import { FileItem } from '../types';
import { useFileSelectionStore } from '../store/fileSelectionStore';
import { useFileOperations } from './useFileOperations';
import Selecto from 'selecto';

interface UseFileSelectionProps {
  containerRef?: RefObject<HTMLElement>;
}

export const useFileSelection = ({ 
  containerRef
}: UseFileSelectionProps) => {
  const internalContainerRef = useRef<HTMLElement | null>(null);
  
  const { 
    selectedFiles,
    selectedFilesIds,
    setSelectedFiles,
    isSelected,
    toggleSelection,
    clearSelection,
    selectAll,
    lastSelectedFile
  } = useFileSelectionStore();

  const {
    currentDirectory
  } = useFileOperations();
  
  const getContainerRef = useCallback(() => {
    return containerRef?.current || internalContainerRef.current;
  }, [containerRef]);

  const updateLastSelectedFile = useCallback((files: FileItem[]) => {
    const container = getContainerRef();
    if (!container || files.length === 0) return;

    const fileElements = Array.from(container.querySelectorAll('[data-file-id]'));
    const lastElement = fileElements.find(el => 
      files.some(f => f.id?.toString() === el.getAttribute('data-file-id'))
    );

    if (lastElement) {
      const fileId = lastElement.getAttribute('data-file-id');
      setSelectedFiles([{ id: fileId } as FileItem]);
    }
  }, [getContainerRef, setSelectedFiles]);
  
  const handleLeftClick = useCallback((file: FileItem, event: React.MouseEvent) => {
    if (event.ctrlKey) {
      toggleSelection(file, event, currentDirectory);
    } else if (event.shiftKey) {
      const container = getContainerRef();
      if (!container) return;

      const fileElements = Array.from(container.querySelectorAll('[data-file-id]'));
      const lastSelectedElement = fileElements.find(el => 
        selectedFilesIds.some(f => f === el.getAttribute('data-file-id'))
      );
      const currentElement = fileElements.find(el => 
        el.getAttribute('data-file-id') === file.id?.toString()
      );

      if (lastSelectedElement && currentElement) {
        const lastIndex = fileElements.indexOf(lastSelectedElement);
        const currentIndex = fileElements.indexOf(currentElement);
        const [start, end] = [lastIndex, currentIndex].sort((a, b) => a - b);
        
        const newSelection = fileElements
          .slice(start, end + 1)
          .map(el => {
            const fileId = el.getAttribute('data-file-id');
            return { id: fileId } as FileItem;
          });
        setSelectedFiles(newSelection);
      }
    } else {
      setSelectedFiles([file]);
    }
  }, [toggleSelection, setSelectedFiles, currentDirectory, selectedFilesIds, getContainerRef]);

  const handleRightClick = useCallback((file: FileItem) => {
    if (!isSelected(file)) {
      setSelectedFiles([file]);
    }
  }, [isSelected, setSelectedFiles]);

  const handleOutsideClick = useCallback((event: React.MouseEvent, selectoRef?: React.RefObject<Selecto>) => { 
    const target = event.target as HTMLElement;
    if (!target.closest(".no-clear-select") && !target.closest('.file-item') && !target.closest('.contex-menu-item') && !(event.target as HTMLElement).querySelector('.selection-area')) {
      clearSelection();
    }
    if (selectoRef && selectoRef.current && selectoRef.current?.getSelectedTargets()?.length === 0) {
      clearSelection();
    }
  }, [clearSelection]);

  const handleSelectAll = useCallback(() => {
    selectAll(currentDirectory);
  }, [selectAll, currentDirectory]);

  const handleClearSelection = useCallback(() => {
    clearSelection();
  }, [clearSelection]);

  const getElement = useCallback((fileId: string, container: HTMLElement) => {
    const fileElements = Array.from(container.querySelectorAll('[data-file-id]'));
    const lastSelectedElement = fileElements.find(el => 
      el.getAttribute('data-file-id') === fileId
    );
    return lastSelectedElement;
  }, []);

  const selectedIndices = useCallback((fileElements: Element[], selectedFilesIds: string[]) => {
    const firstSelectedIndex = Math.min(...selectedFilesIds.map(id => fileElements.findIndex(el => el.getAttribute('data-file-id') === id)));
    const lastSelectedIndex = Math.max(...selectedFilesIds.map(id => fileElements.findIndex(el => el.getAttribute('data-file-id') === id)));
    return {first: firstSelectedIndex, last: lastSelectedIndex};
  }, []);

  const selectArrowKeys = useCallback((direction: "ArrowUp" | "ArrowDown" | "ArrowLeft" | "ArrowRight") => {
    const container = getContainerRef();
    if (!container || !lastSelectedFile) return;
    const fileElements = Array.from(container.querySelectorAll('[data-file-id]'));
    const lastSelectedElement = getElement(lastSelectedFile, container);
    if (lastSelectedElement) {
      const lastIndex = fileElements.indexOf(lastSelectedElement);
      
      const containerWidth = container.clientWidth;
      const itemWidth = 150;
      const itemsPerRow = Math.floor(containerWidth / itemWidth);
      
      if (direction === "ArrowLeft" || direction === "ArrowRight") {
        const movement = direction === "ArrowLeft" ? -1 : 1;
        const newIndex = Math.max(0, Math.min(fileElements.length - 1, lastIndex + movement));
        const {first, last} = selectedIndices(fileElements, selectedFilesIds);
        
        const isExpanding = (direction === "ArrowRight" && newIndex > last) || 
                           (direction === "ArrowLeft" && newIndex < first);
        
        let newSelection: FileItem[] = [];
        
        if (isExpanding) {
          const [start, end] = direction === "ArrowRight" ? [first, newIndex] : [newIndex, last];
          newSelection = fileElements
            .slice(start, end + 1)
            .map(el => ({ id: el.getAttribute('data-file-id') } as FileItem));
        } else {
          const [start, end] = direction !== "ArrowRight" ? [first, newIndex] : [newIndex, last];
          newSelection = fileElements
            .slice(start, end + 1)
            .map(el => ({ id: el.getAttribute('data-file-id') } as FileItem));
        }
        
        if (newSelection.length === 0) {
          newSelection = [{ id: fileElements[newIndex].getAttribute('data-file-id') } as FileItem];
        }
        
        setSelectedFiles(newSelection, { id: fileElements[newIndex].getAttribute('data-file-id') } as FileItem);
      } else if (direction === "ArrowUp" || direction === "ArrowDown") {        
        const [start, end] = direction === "ArrowUp" ? 
                              [Math.max(0, lastIndex-itemsPerRow), lastIndex] : 
                              [lastIndex, Math.min(fileElements.length-1, lastIndex+itemsPerRow)];
                    
        const newFile = direction === "ArrowUp" ? { id: fileElements[start].getAttribute('data-file-id') } as FileItem : 
                                                  { id: fileElements[end].getAttribute('data-file-id') } as FileItem;
        
        const newSelection = fileElements
          .slice(start, end+1)
          .map(el => ({ id: el.getAttribute('data-file-id') } as FileItem));
        
        setSelectedFiles(newSelection, newFile);
      }
    }
  }, [getContainerRef, lastSelectedFile, setSelectedFiles, getElement, selectedFilesIds, selectedIndices]);

  return {
    selectedFiles,
    setSelectedFiles: (files: FileItem[]) => {
      setSelectedFiles(files);
      updateLastSelectedFile(files);
    },
    handleLeftClick,
    handleRightClick,
    handleSelectAll,
    handleClearSelection,
    isSelected,
    handleOutsideClick,
    selectArrowKeys,
    lastSelectedFile
  };
}; 