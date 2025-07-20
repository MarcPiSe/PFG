import { useCallback, useRef, RefObject, useEffect } from 'react';
import { FileItem } from '../types';
import { useFileSelectionStore } from '../store/fileSelectionStore';
import Selecto from 'selecto';
import { useFileOperations } from './useFileOperations';

interface UseFileSelectionProps {
  containerRef?: RefObject<HTMLElement>;
  contextMenuOpen?: boolean;
}

export const useSelecto = ({ 
  containerRef ,
  contextMenuOpen
}: UseFileSelectionProps) => {
  
  const internalContainerRef = useRef<HTMLElement | null>(null);
  const selectoRef = useRef<Selecto | null>(null);
  
  const { 
    selectedFiles,
    setSelectedFiles,
    isSelected,
    clearSelection,
    selectAll
  } = useFileSelectionStore();

  const {
    currentDirectory
  } = useFileOperations();
  
  const getContainerRef = useCallback(() => {
    return containerRef?.current || internalContainerRef.current;
  }, [containerRef]);

  useEffect(() => {
    const container = getContainerRef();
    if (!container) return;

    if (contextMenuOpen) {
      selectoRef.current = null;
      return;
    }
    
    const selecto = new Selecto({
      container,
      dragContainer: container,
      selectableTargets: ['.file-item'],
      selectByClick: true,
      selectFromInside: true,
      hitRate: 100,
      className: 'selection-area',
      preventDefault: true,
    });

    selecto.on('select', (e) => {
      e.inputEvent.stopPropagation();
      
      const addedElements = e.added as HTMLElement[];
      const removedElements = e.removed as HTMLElement[];
      
      const addedFiles = addedElements.map(el => {
        const fileId = el.getAttribute('data-file-id');
        return { id: fileId } as FileItem;
      });
      
      const currentSelection = [...useFileSelectionStore.getState().selectedFiles];
      let newSelection = [...currentSelection];
      
      addedFiles.forEach(file => {
          if (!newSelection.some(f => f.id === file.id)) {
              newSelection.push(file);
          }
      });
      
      newSelection = newSelection.filter(file => 
          !removedElements.some(el => el.getAttribute('data-file-id') === file.id)
      );
      
      setSelectedFiles(newSelection);
    });

    selecto.on('dragStart', (e) => {
      const draggingElements = container.querySelectorAll('.no-select');
      if (draggingElements.length > 0) {
        e.stop();
      }
    });
    
    selectoRef.current = selecto;
    
    return () => {
      selecto.destroy();
    };
  }, [getContainerRef, setSelectedFiles, contextMenuOpen]);

  const handleMouseDown = useCallback(() => {
    
  }, []);

  const handleMouseMove = useCallback(() => {
    
  }, []);

  const handleMouseUp = useCallback(() => {
    
  }, []);

  const setContainerRef = useCallback((ref: HTMLElement | null) => {
    internalContainerRef.current = ref;
  }, []);

  const handleSelectAll = useCallback(() => {
    selectAll(currentDirectory);
  }, [selectAll, currentDirectory]);

  const handleClearSelection = useCallback(() => {
    clearSelection();
  }, [clearSelection]);

  return {
    selectedFiles,
    setSelectedFiles: (files: FileItem[]) => {
      setSelectedFiles(files);
    },
    handleSelectAll,
    handleClearSelection,
    isSelected,
    handleMouseDown,
    handleMouseMove,
    handleMouseUp,
    setContainerRef,
    selectoRef,
  };
}; 