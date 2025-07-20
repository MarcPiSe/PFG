import { useEffect, useMemo } from 'react';
import { useFileOperations } from './useFileOperations';
import { useFileSelectionStore } from '../store/fileSelectionStore';
import { useFileContextStore } from '../store/fileContextStore';
import { useFileSelection } from './useFileSelection';
import { useFileStore } from '../store/fileStore';
import { useNotificationService } from '../services/notificationService';

export const useFileShortcuts = (
  containerRef: React.RefObject<HTMLElement>
) => {
  const {
    pasteFiles,
    deleteItem,
    revokeAccess,
  } = useFileOperations();
  const currentDirectory = useFileStore((state) => state.currentDirectory);

	const {
		clipboard,
    clipboardParentId,
    isCut,
    copyFiles,
    cutFiles,
    selectedFiles,
    selectAll,
    setClipboardFiles,
    clearSelection
	} = useFileSelectionStore();
  const { selectArrowKeys } = useFileSelection({containerRef});
  const fileContext = useFileContextStore();
  const isInRoot = useMemo(() => fileContext.isInRoot(), [fileContext]);
  const isInShared = useMemo(() => fileContext.isInShared(), [fileContext]);
  const notifications = useNotificationService();

  useEffect(() => {
    const handleKeyDown = async (event: KeyboardEvent) => {

      if (event.shiftKey && (event.key === 'ArrowUp' || event.key === 'ArrowDown' || event.key === 'ArrowLeft' || event.key === 'ArrowRight')) {
        event.preventDefault();
        selectArrowKeys(event.key);
      }

      if (event.ctrlKey || event.metaKey) {
        switch (event.key.toLowerCase()) {
          case 'c':
            event.preventDefault();
            if(isInRoot || (isInShared && currentDirectory.shared && currentDirectory.accessLevel !== 'READ')) {
              copyFiles(currentDirectory.id!.toString());
            } else {
              notifications.error("You don't have permission to copy files");
            }
            break;

          case 'x':
            event.preventDefault();
            if(isInRoot || (isInShared && currentDirectory.shared && currentDirectory.accessLevel !== 'READ')) {
              cutFiles(currentDirectory.id!.toString());
            } else {
              notifications.error("You don't have permission to cut files");
            }
            break;

          case 'v':
            event.preventDefault();
            if(isInRoot || (isInShared && currentDirectory.accessLevel !== 'READ')) {
              const items = Array.from(clipboard);
              if(items.length > 0) {
                pasteFiles({items: items, targetFolderId: currentDirectory.id!.toString(), prevParentId: clipboardParentId, isCut: isCut});
              }
              if(isCut) {
                setClipboardFiles(new Set(), undefined);
              }
            } else {
              notifications.error("You don't have permission to paste files");
            }
            break;

          case 'a':
            event.preventDefault();
            selectAll(currentDirectory);
            break;
        }
      } else if (event.key === 'Delete') {
        event.preventDefault();
        if (fileContext.section === 'shared') {
          revokeAccess({items: selectedFiles})
        } else {
          deleteItem({items: selectedFiles, section: fileContext.section});
        }
        clearSelection();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedFiles, currentDirectory, copyFiles, cutFiles, pasteFiles, selectAll, deleteItem, revokeAccess, fileContext, clipboard, isCut, selectArrowKeys, clipboardParentId, clearSelection, setClipboardFiles]);
}; 