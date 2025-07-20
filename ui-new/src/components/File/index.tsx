import { Fragment, useState, useRef, useEffect, useMemo } from "react";
import { RiFile2Fill, RiFolder2Fill, RiCheckLine } from "@remixicon/react";
import { Card } from "../Card";
import { useDraggable, useDroppable } from "@dnd-kit/core";
import { fileServiceInstance as fileService } from "../../lib/services";
import { FileItem } from "../../types";
import { cx } from "../../lib/utils";
import FolderSelectDialog from "../FolderSelectDialog";
import { useShareManager } from "../../hooks/useShareManager";
import { useFileSelectionStore, useIsCutFile, useIsItemSelected } from "../../store/fileSelectionStore";
import { useNotificationService } from "../../services/notificationService";
import { useFileOperations } from "../../hooks/useFileOperations";
import { ShareDialog } from './ShareDialog';
import { RenameDialog } from "./RenameDialog";
import { useFileSelection } from "../../hooks/useFileSelection";
import { FileDetailsDrawer } from "./FileDetailsDrawer";
import { useFileStore } from "../../store/fileStore";
import { useFileContextStore } from "../../store/fileContextStore";
//import { useFileSelectionDebug } from "../../hooks/useFileSelectionDebug";
//import { useFileStoreDebug } from "../../hooks/useFileStoreDebug";
//import { useFileContextStoreDebug } from "../../hooks/useFileContextStoreDebug";

/*interface SharedUser {
  id: string;
  username: string;
  email: string;
  accessType: 'READ' | 'WRITE';
}*/

interface FileProps {
  file: FileItem;
}

const File = ({ 
  file
}: FileProps) => {
  

  const [openDrawer, setOpenDrawer] = useState(false);
  const [openRenameDialog, setOpenRenameDialog] = useState(false);
  const [openShareDialog, setOpenShareDialog] = useState(false);
  const [openMoveDialog, setOpenMoveDialog] = useState(false);
  const dragTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [, setSharedUsers] = useState<Array<{
    id: string;
    username: string;
    email: string;
    accessType: 'READ' | 'WRITE';
  }>>([]);
  const isItemSelected = useIsItemSelected(file);
  const isCutFile = useIsCutFile(file);

  const shareManager = useShareManager();
  const notifications = useNotificationService();
  
	const fileContext = useFileContextStore();
  
	const isInTrash = useMemo(() => fileContext.isInTrash(), [fileContext]);
	const isInShared = useMemo(() => fileContext.isInShared(), [fileContext]);
  
  const { currentDirectory, folderStructure } = useFileStore();
  
  const fileOperations = useFileOperations();
  const containerRef = useRef<HTMLDivElement>(null);
  const selectedFiles = useFileSelectionStore(state => state.selectedFiles);
  const fileSelection = useFileSelection({
    containerRef
  });

  const {
    attributes,
    listeners,
    setNodeRef: setNodeRefDraggable,
    transform,
    isDragging: isDndDragging,
  } = useDraggable({
    id: file.id!.toString(),
    data: {
      file,
      type: 'file'
    },
  });
  const { setNodeRef: setNodeRefDroppable, isOver } = useDroppable({
    id: file.id!.toString(),
    data: {
      file,
      type: 'file'
    },
    disabled: file.type !== 'folder'
  });

  const handleMouseDown = (e: React.MouseEvent) => {
    
    if (e.button === 0 && !isDndDragging) {
      dragTimeoutRef.current = setTimeout(() => {
        
        setIsDragging(true);
      }, 500);
    }
    
    if (isDndDragging) {
      e.stopPropagation();
    }
  };

  const handleMouseUp = (e: React.MouseEvent) => {
    
    if (dragTimeoutRef.current) {
      clearTimeout(dragTimeoutRef.current);
      dragTimeoutRef.current = null;
    }
    setIsDragging(false);
    if (e.button === 0) {
      fileSelection.handleLeftClick(file, e);
    }
  };

  const setRefs = (element: HTMLDivElement) => {
    setNodeRefDraggable(element);
    if (file.type === 'folder') {
      setNodeRefDroppable(element);
    }
  };

  const handleConfirmRename = async (newFileName: string) => {
    if (isInTrash || isInShared) return;
    fileOperations.updateItem({itemId: file.id!.toString(), folderId: file.parent!.toString(), newName: newFileName});
  };

  const shareConfirm = async (userAccess: {username: string, fileId: string, accessType: 'READ' | 'WRITE'}[], originalSharedAccess: Map<string, {username: string, fileId: string, accessType: 'READ' | 'WRITE' }[]>) => {
    if(originalSharedAccess.size > 0) { 
      const userAccessFiltered = userAccess.filter(
        (access) => !originalSharedAccess.has(access.username) || JSON.stringify(originalSharedAccess.get(access.username)) !== JSON.stringify(access)
      );
      if(userAccessFiltered.length > 0) {
        const promises = [];
        for (const access of userAccessFiltered) {
          if(!originalSharedAccess.has(access.username)) {
            promises.push(shareManager.shareWithUser(access.fileId, access.username, access.accessType));
          } else {
            promises.push(shareManager.handleUpdateAccess(access.fileId, access.username, access.accessType));
          }
        }
        for (const username of originalSharedAccess.keys()) {
          if(!userAccess.some(access => access.username === username)) {
            originalSharedAccess.get(username)!.forEach(access => {
              promises.push(shareManager.handleUnshareToUser(access.fileId, username));
            });
          } else {
            originalSharedAccess.get(username)!.forEach(access => {
              if(!userAccess.some(access => access.username === username && access.fileId === access.fileId)) {
                promises.push(shareManager.handleUnshareToUser(access.fileId, username));
              }
            });
          }
        }
        await Promise.all(promises);
      }
    } else {
      const promises = userAccess.map(async (access) => {
        shareManager.shareWithUser(access.fileId, access.username, access.accessType);
      });
      await Promise.all(promises);
    }
  };

  const handleMoveConfirm = async (targetFolder: FileItem, itemsToMove: FileItem[]) => {
    await fileOperations.moveItem({items: itemsToMove, toFolderId: targetFolder.id!.toString(), fromFolderId: file.parent!.toString()});
  };

  const handleDoubleClick = async () => {
    if (file.type === 'folder') {
      fileOperations.setCurrentDirectory(file);
    } else {
      notifications.info("Downloading...");
      try {
        const { blob, filename } = await fileService.downloadFile(file.id!.toString()) as { blob: Blob; filename: string };
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
    }
  };

  const style = transform ? {
    transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
    zIndex: 50,
  } : undefined;

  useEffect(() => {
    if (openDrawer && file.shared) {
      shareManager.fetchAccessRules(file.id!.toString())
        .then(({ data }) => {
          const formattedUsers = data.map((item: { user: { id: string; username: string; email: string }; accessType: 'READ' | 'WRITE' | 'ADMIN' }) => ({
            id: item.user.id,
            username: item.user.username,
            email: item.user.email,
            accessType: item.accessType
          }));
          setSharedUsers(formattedUsers);
        })
        .catch(() => {
          notifications.error("Failed to load shared users");
        });
    }
  }, [openDrawer, file.id, file.shared, shareManager, notifications]);

  const getAncestor = (tFolder: FileItem): FileItem  => {
    if (!folderStructure.shared.subfolders) return tFolder;
    
    const recSearch = (folder: FileItem): boolean => {
      if (folder.id === tFolder.id) return true;
      
      if (folder.subfolders) {
        return folder.subfolders.some(subfolder => recSearch(subfolder));
      }
      
      return false;
    };
    
    for (const sharedSubfolder of folderStructure.shared.subfolders) {
      if (recSearch(sharedSubfolder)) {
        return sharedSubfolder;
      }
    }
    
    return tFolder;
  };
  

  //useFileSelectionDebug();
  //useFileStoreDebug();
  //useFileContextStoreDebug();

  return (
    <Fragment>
      <Card 
          ref={setRefs}
          className={cx(
            "flex flex-col items-center justify-center w-full h-32 p-2",
            "cursor-pointer transition-colors duration-200",
            "bg-gray-200 hover:bg-gray-300",
            "touch-none",
            isItemSelected ? "bg-blue-50" : "",
            isDragging || isDndDragging ? "opacity-50 bg-gray-200" : "",
            isDndDragging ? "no-select" : "file-item",
            isDndDragging ? "shadow-lg" : "",
            isOver && file.type === 'folder' ? "ring-2 ring-blue-500 bg-blue-50" : "",
            isCutFile ? "opacity-50" : ""
          )}
          data-file-type="file" 
          data-file-id={file.id}
          onContextMenu={(e) => {
            if (!isDndDragging) {
              e.preventDefault();
              e.stopPropagation();
              fileSelection.handleRightClick(file);
            } 
          }}
          onDoubleClick={(e) => {
            if (!isDndDragging) {
              handleDoubleClick();
            } else {
              e.stopPropagation();
            }
          }}
          style={style}
          onMouseDown={(e) => {
            handleMouseDown(e);
            if (isDndDragging) {
              e.stopPropagation();
            }
          }}
          onMouseUp={handleMouseUp}
          {...(isDndDragging ? {} : attributes)}
          {...listeners}
        >
          <div id="17" className="relative">
            {file.type === "folder" ? (
              <RiFolder2Fill size={48} className={cx("text-gray-700", isItemSelected && "text-blue-500")} />
            ) : (
              <RiFile2Fill size={48} className={cx("text-gray-700", isItemSelected && "text-blue-500")} />
            )}
            {isItemSelected && (
              <div id="18" className="absolute -top-1 -right-1 bg-blue-500 rounded-full p-0.5">
                <RiCheckLine className="w-3 h-3 text-white" />
              </div>
            )}
          </div>
          <p className="text-sm line-clamp-1 text-gray-800">{file.name}</p>
        </Card>
      <>
      {openRenameDialog && (
      <RenameDialog
        open={openRenameDialog}
        onOpenChange={setOpenRenameDialog}
        file={file}
        onRename={handleConfirmRename}
      />
      )}
      {openShareDialog && (
      <ShareDialog
        open={openShareDialog}
        onClose={setOpenShareDialog}
        files={selectedFiles || [file]}
        onShare={shareConfirm}
      />
      )}
      {openMoveDialog && (
      <FolderSelectDialog
        isOpen={openMoveDialog}
        onClose={() => setOpenMoveDialog(false)}
        itemsToMove={selectedFiles?.length > 0 && selectedFiles.some((f: FileItem) => f.id === file.id) ? selectedFiles : [file]}
        onSelect={handleMoveConfirm}
        foderToShow={ isInShared ? (getAncestor(currentDirectory) || folderStructure.shared) : folderStructure.root}
      />
      )}
      {openDrawer && (
      <FileDetailsDrawer
        open={openDrawer}
        onOpenChange={setOpenDrawer}
        file={file}
      />
      )}
    </>
    </Fragment>
  );
};

export default File;
