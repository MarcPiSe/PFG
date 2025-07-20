import { useRef, useEffect, useCallback, useMemo, useState } from 'react';
import { RiFolderLine, RiFileUploadLine, RiFolderUploadLine, RiFileCopyLine, RiScissorsLine, RiShareLine, RiDownload2Line, RiDeleteBin5Line, RiArrowGoBackLine, RiUserUnfollowLine, RiInformationLine } from '@remixicon/react';
import { cx } from '../../lib/utils';
import { useFileOperations } from '../../hooks/useFileOperations';
import { useFileSelectionStore } from '../../store/fileSelectionStore';
import { useFileContextStore } from '../../store/fileContextStore';
import { SharedUser, FileItem } from '../../types';
import { fileServiceInstance as fileService } from '../../lib/services';
import { useNotificationService } from '../../services/notificationService';
import { useShareManager } from '../../hooks/useShareManager';
import { useFileStore } from '../../store/fileStore';

interface FileManagerContextMenuProps {
  position: { x: number; y: number } | undefined;
  isOpen: boolean;
  onClose: () => void;
  file: FileItem | undefined | null;
  setOpenRenameDialog: (open: boolean) => void;
  setOpenMoveDialog: (open: boolean) => void;
  setOpenShareDialog: (open: boolean) => void;
  setOpenDrawer: (open: boolean) => void;
  setIsCreateFolderOpen: (open: boolean) => void;
}

export const FileManagerContextMenu = ({
  position,
  isOpen = false,
  onClose,
  file,
  setOpenRenameDialog,
  setOpenMoveDialog,
  setOpenShareDialog,
  setOpenDrawer,
  setIsCreateFolderOpen
}: FileManagerContextMenuProps) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);
  const [, setAccessLevel] = useState<'ADMIN' | 'READ' | 'WRITE' | undefined>(undefined);

  const fileContext = useFileContextStore();
  const notifications = useNotificationService();
  const shareManager = useShareManager();
  const isLoading = useFileStore((state) => state.isLoading);

  const isInTrash = useMemo(() => fileContext.isInTrash(), [fileContext]);
  const isInShared = useMemo(() => fileContext.isInShared(), [fileContext]);
  const isInRoot = useMemo(() => fileContext.isInRoot(), [fileContext]);

  const {
    uploadFile,
    uploadFolder,
    pasteFiles,
    deleteItem,
    restoreItem,
    revokeAccess
  } = useFileOperations();
  const currentDirectory = useFileStore((state) => state.currentDirectory);

  const {
    clipboard,
    clipboardParentId,
    isCut,
    selectedFiles,
    copyFiles,
    cutFiles,
    clearSelection,
    setClipboardFiles,
    setSelectedFiles
  } = useFileSelectionStore();

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    if(!file) {
      clearSelection();
    }

    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      const isClickInDialog = target.closest('[role="contextOption"]') !== null;
      if (!isClickInDialog) {
        onClose();
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [isOpen, onClose, file, clearSelection]);

  useEffect(() => {
    const loadAccessLevel = async () => {
      if(isInShared && file) {
        const accessLevel = await shareManager.fetchAccessRules(file.id!.toString()) as SharedUser;
        setAccessLevel(accessLevel.accessType);
      }
    };
    loadAccessLevel();
  }, [isInShared, file, shareManager]);

  const handleFileUploadLocal = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) {
      return;
    }

    const file = files[0];

    const item: FileItem = {
      id: `temp-${Date.now()}`,
      name: file.name,
      type: 'file',
      size: file.size,
      mimeType: file.type
    };

    try {
      await uploadFile({ item, file, parentId: currentDirectory.id!.toString() });
    } catch {
      // eslint-disable-line no-empty
    }
    event.target.value = '';
    onClose();
  }, [currentDirectory, uploadFile, onClose]);

  const handleFolderUploadLocal = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) {
      return;
    }

    const filesArray = Array.from(files);

    try {
      await uploadFolder({ files: filesArray, parentId: currentDirectory.id!.toString() });
    } catch {
      // eslint-disable-line no-empty
    }
    event.target.value = '';  
    onClose();
  }, [currentDirectory, uploadFolder, onClose]);

  const handlePaste = useCallback(async () => {
    onClose();
    if (clipboard.size === 0) {
      return;
    }

    if(isInRoot || (isInShared && currentDirectory.accessLevel !== 'READ')) {
      try {
        await pasteFiles({
          items: Array.from(clipboard),
          targetFolderId: currentDirectory.id.toString(),
          isCut: isCut,
          prevParentId: clipboardParentId
        });

        if(isCut) {
          setClipboardFiles(new Set(), undefined);
        }
      } catch {
        // eslint-disable-line no-empty
      }
    } else {
      notifications.error("You don't have permission to paste files");
    }
  }, [clipboard, currentDirectory, isCut, pasteFiles, onClose, clipboardParentId, setClipboardFiles, isInRoot, isInShared, notifications]);

  const handleCopy = useCallback(() => {
    onClose();
    if(isInRoot || (isInShared && currentDirectory.shared && currentDirectory.accessLevel !== 'READ')) {
      copyFiles(currentDirectory.id!.toString());
    } else {
      notifications.error("You don't have permission to copy files");
    }
  }, [copyFiles, onClose, currentDirectory, isInRoot, isInShared, notifications]);

  const handleCut = useCallback(() => {
    onClose();
    if(isInRoot || (isInShared && currentDirectory.shared && currentDirectory.accessLevel !== 'READ')) {
      cutFiles(currentDirectory.id!.toString());
    } else {
      notifications.error("You don't have permission to cut files");
    }
  }, [cutFiles, onClose, currentDirectory, isInRoot, isInShared, notifications]);

  const handleDownload = useCallback(async () => {
    if (!file) return;
    try {
      const { blob, filename } = await fileService.downloadFile(file.id) as { blob: Blob; filename: string };
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      notifications.success("Downloaded");
    } catch {
      notifications.error("Download failed");
    }
    onClose();
  }, [file, notifications, onClose]);

  const handleDelete = useCallback(async () => {
    if (!file) return;
    
    await deleteItem({ items: selectedFiles, section: isInRoot ? 'root' : 'trash' });
    clearSelection();
    onClose();
  }, [file, selectedFiles, isInRoot, deleteItem, onClose, clearSelection]);

  const handleRestore = useCallback(async () => {
    if (!file) return;
    
    await restoreItem({ items: [selectedFiles.find(f => f.id === file.id)!] });
    onClose();
  }, [file, selectedFiles, restoreItem, onClose]);

  const handleUnshare = useCallback(async () => {
    if (!file) return;
    
    await revokeAccess({items: selectedFiles});
    onClose();
  }, [file, selectedFiles, onClose, revokeAccess]);

  const handleDetails = useCallback(() => {
    if (!file) return;
    
    setOpenDrawer(true);
    onClose();
  }, [file, setOpenDrawer, onClose]);

  const renderSharedOptions = useCallback(() => {
    let list = selectedFiles;
    if(selectedFiles.length === 0 && file) {
      list = [file];
    } else if(selectedFiles.length === 0) {
      return null;
    }
    let accessLevel = 'ADMIN';
    if (list.some(f => f.accessLevel === 'WRITE')) {
      accessLevel = 'WRITE';
    }
    if (list.some(f => f.accessLevel === 'READ')) {
      accessLevel = 'READ';
    }
    if (!file || !file?.accessLevel) return null;

    const options = [];

    if(list.length === 1) {
      options.push(
        <div key="download" className={cx(
          "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
          "active:bg-gray-100 active:text-gray-900",
          ".contex-menu-item"
        )} onClick={() => {
          onClose();
          handleDownload();
        }}>
          <RiDownload2Line className="w-4 h-4" />
          <span>Download</span>
        </div>);
      }

    options.push(
      <div key="unshare" className={cx(
        "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
        "active:bg-gray-100 active:text-gray-900",
        ".contex-menu-item"
      )} onClick={() => {
        onClose();
        handleUnshare();
      }}>
        <RiUserUnfollowLine className="w-4 h-4" />
        <span>Stop seeing</span>
      </div>
    );

    if(!currentDirectory.parent) return options; 

    if (accessLevel === 'WRITE' || accessLevel === 'ADMIN') {
      options.push(
        <div key="rename" className={cx(
          "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
          "active:bg-gray-100 active:text-gray-900",
          ".contex-menu-item"
        )} onClick={() => setOpenRenameDialog(true)}>
          <span>Rename</span>
        </div>
      );

    if (accessLevel === 'ADMIN') {
      options.push(
        <div key="delete" className={cx(
          "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
          "active:bg-gray-100 active:text-gray-900",
          ".contex-menu-item"
        )} onClick={() => {
          onClose();
          handleDelete();
        }}>
          <RiDeleteBin5Line className="w-4 h-4" />
          <span>Delete</span>
        </div>
      );
    }

    options.push(
      <div key="copy" className={cx(
        "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
        "active:bg-gray-100 active:text-gray-900",
        ".contex-menu-item"
      )} onClick={() => {
        onClose();
        handleCopy();
      }}>
        <RiFileCopyLine className="w-4 h-4" />
        <span>Copy</span>
      </div>);

      if (isInShared && (currentDirectory.accessLevel === 'WRITE' || currentDirectory.accessLevel === 'ADMIN')) {
        options.push(
          <div key="cut" className={cx(
            "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
            "active:bg-gray-100 active:text-gray-900",
            ".contex-menu-item"
          )} onClick={() => {
            onClose();
            handleCut();
          }}>
            <RiScissorsLine className="w-4 h-4" />
            <span>Cut</span>
          </div>,
          <div key="move" className={cx(
            "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
            "active:bg-gray-100 active:text-gray-900",
            ".contex-menu-item"
          )} onClick={() => { onClose();
            setOpenMoveDialog(true)}}>
            <span>Move to...</span>
          </div>
        );
      }
    }

    return options;
  }, [file, isInShared, onClose, handleUnshare, handleDownload, setOpenRenameDialog, handleDelete, handleCopy, handleCut, setOpenMoveDialog, setIsCreateFolderOpen, fileInputRef, folderInputRef, currentDirectory, selectedFiles, setSelectedFiles]);

  if (isLoading || !currentDirectory) {
    return null;
  }

  if (!position) {
    return null;
  }

  if(!file && !isInRoot && isInShared && currentDirectory.accessLevel !== 'WRITE') {
    return null;
  }

  return (
    <>
      <div
        className="fixed z-50 bg-white rounded-md shadow-lg border border-gray-200"
        style={{
          top: position.y,
          left: position.x,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="min-w-[220px] p-1">
          {!file && (isInRoot || (isInShared && currentDirectory.accessLevel === 'WRITE')) ? (
            <>
              <div className={cx(
                  "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                  "active:bg-gray-100 active:text-gray-900",
                  ".contex-menu-item"
                )} onClick={() => {
                  onClose();
                  setIsCreateFolderOpen(true);
                }}>
                <RiFolderLine className="w-4 h-4" />
                <span>New folder</span>
              </div>
              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                multiple
                onChange={(e) => {
                  handleFileUploadLocal(e);
                  onClose();
                }}
              />
              <input
                type="file"
                ref={folderInputRef}
                className="hidden"
                webkitdirectory=""
                directory=""
                multiple
                onChange={(e) => {
                  handleFolderUploadLocal(e);
                  onClose();
                }}
              />
              <div className={cx(
                "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                "active:bg-gray-100 active:text-gray-900",
                ".contex-menu-item"
              )} onClick={() => fileInputRef.current?.click()}>
                <RiFileUploadLine className="h-4 w-4" />
                Upload file
              </div>
              <div className={cx(
                "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                "active:bg-gray-100 active:text-gray-900",
                ".contex-menu-item"
              )} onClick={() => folderInputRef.current?.click()}>
                <RiFolderUploadLine className="h-4 w-4" />
                Upload folder
              </div>
              {clipboard.size > 0 && (
                <div className={cx(
                  "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                  "active:bg-gray-100 active:text-gray-900",
                  ".contex-menu-item"
                )} onClick={handlePaste}>
                  <RiFileCopyLine className="w-4 h-4" />
                  <span>Paste</span>
                </div>
              )}
            </>
          ) : (
            <>
              {isInRoot && !isInShared && !isInTrash && (
                <>
                  <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() => {
                    onClose();
                    handleCopy();
                  }}>
                    <RiFileCopyLine className="w-4 h-4" />
                    <span>Copy</span>
                  </div>
                  <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() => {
                    onClose();
                    handleCut();
                  }}>
                    <RiScissorsLine className="w-4 h-4" />
                    <span>Cut</span>
                  </div>
                  {selectedFiles.length <= 1 && (
                    <div className={cx(
                      "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                      "active:bg-gray-100 active:text-gray-900",
                      ".contex-menu-item"
                    )} onClick={() => setOpenRenameDialog(true)}>
                      <span>Rename</span>
                    </div>
                  )}
                  <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() => { onClose();
                    setOpenMoveDialog(true)}}>
                    <span>Move to...</span>
                  </div>
                  <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() => {
                    onClose();
                    setOpenShareDialog(true);
                  }}>
                    <RiShareLine className="w-4 h-4" />
                    <span>Share</span>
                  </div>
                  { selectedFiles.length === 1 && <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() =>{
                    onClose();
                    handleDownload()}}>
                    <RiDownload2Line className="w-4 h-4" />
                    <span>Download</span>
                  </div>
                  }
                  <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() => {
                    onClose();
                    handleDelete();
                  }}>
                    <RiDeleteBin5Line className="w-4 h-4" />
                    <span>Delete</span>
                  </div>
                </>
              )}
              {isInTrash && (
                <>
                  <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() => {
                    onClose();
                    handleRestore();
                  }}>
                    <RiArrowGoBackLine className="w-4 h-4" />
                    <span>Restore</span>
                  </div>
                  <div className={cx(
                    "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                    "active:bg-gray-100 active:text-gray-900",
                    ".contex-menu-item"
                  )} onClick={() => {
                    onClose();
                    handleDelete();
                  }}>
                    <RiDeleteBin5Line className="w-4 h-4" />
                    <span>Delete permanently</span>
                  </div>
                </>
              )}
              {isInShared && renderSharedOptions()}
              { selectedFiles.length === 1 && <div key="details" className={cx(
                "flex items-center px-2 py-2 text-sm cursor-pointer outline-none hover:bg-gray-100 hover:text-gray-900 rounded-sm gap-2",
                "active:bg-gray-100 active:text-gray-900",
                ".contex-menu-item"
              )} onClick={() => {
                onClose();
                handleDetails();
              }}>
                <RiInformationLine className="w-4 h-4" />
                <span>Details</span>
              </div>
              }
            </>
          )}
        </div>
      </div>
    </>
  );
}; 