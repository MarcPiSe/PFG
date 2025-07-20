import React, { useCallback, useState, useMemo } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../Dialog";
import { Button } from "../Button";
import { FileItem } from "../../types";
import { useNotificationService } from "../../services/notificationService";
import { RiFolder2Fill, RiFileTextLine } from "@remixicon/react";
import { cx } from "../../lib/utils";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '../Accordion';
import { useFileStore } from '../../store/fileStore';

interface FolderSelectDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (folder: FileItem, itemsToMove: FileItem[]) => void;
  initialFolder?: FileItem | null;
  isLoading?: boolean;
  itemsToMove: FileItem[];
  foderToShow: FileItem;
}

const FolderSelectDialog: React.FC<FolderSelectDialogProps> = ({
  isOpen,
  onClose,
  onSelect,
  initialFolder,
  isLoading,
  itemsToMove,
  foderToShow
}) => {
  const [selectedFolder, setSelectedFolder] = useState<FileItem | null>(initialFolder || null);
  const [expandedFolders, setExpandedFolders] = useState<string[]>([]);
  const notifications = useNotificationService();
  const { currentDirectory } = useFileStore();

  const showTree = useCallback((folderToShow: FileItem) => {
    if (!folderToShow.subfolders) return null;

    return (
      <Accordion 
        key={folderToShow.id}
        type="multiple" 
        value={expandedFolders}
        onValueChange={setExpandedFolders}
        className="no-clear-select"
      >
        <AccordionItem value={folderToShow.id.toString()} className="overflow-hidden">
          <AccordionTrigger 
            disabled={folderToShow.type !== 'folder'} 
            className={cx(
              "py-1 px-2 rounded",
              folderToShow.type === 'folder' ? "hover:font-medium" : "",
              selectedFolder?.id === folderToShow.id && "text-white font-medium",
              selectedFolder?.id === folderToShow.id && "bg-indigo-100 border-l-4 border-indigo-500"
            )}
          >
            <span className="flex items-center gap-2 w-full">
              {folderToShow.type === 'folder' ? 
                <RiFolder2Fill 
                  size={16} 
                  className={cx(
                    "min-w-fit",
                    selectedFolder?.id === folderToShow.id ? "text-indigo-600" : "text-indigo-100"
                  )} 
                /> : 
                <RiFileTextLine 
                  size={16} 
                  className="min-w-fit text-indigo-100" 
                />
              }
              <span 
                className={cx(
                  "text-sm cursor-pointer flex-1 min-w-0", 
                  selectedFolder?.id === folderToShow.id ? "text-indigo-600" : "text-indigo-100"
                )}
                onClick={() => setSelectedFolder(folderToShow)}
              >
                <span className="truncate block w-full">{folderToShow.name}</span>
              </span>
            </span>
          </AccordionTrigger>
          {folderToShow.subfolders && folderToShow.subfolders.length > 0 && (
            <AccordionContent className="transition-all duration-300 scrollbar-hide">
              {folderToShow.subfolders.map(folder => (
                showTree(folder)
              ))}
            </AccordionContent>
          )}
        </AccordionItem>
      </Accordion>
    );
  }, [expandedFolders, selectedFolder]);

  const handleConfirm = useCallback(async () => {
    if (!selectedFolder) {
      return;
    }

    try {
      await onSelect(selectedFolder, itemsToMove);
      setSelectedFolder(null);
      setExpandedFolders([]);
      onClose();
    } catch {
      notifications.error('Move failed');
    }
  }, [selectedFolder, onSelect, onClose, notifications, itemsToMove]);

  const filteredFolders = useMemo(() => {
    const filter = (folder: FileItem): FileItem | null => {
      if (itemsToMove.some(item => item.id === folder.id)) {
        return null;
      }

      const newFolder: FileItem = { ...folder };

      if (folder.subfolders) {
        newFolder.subfolders = folder.subfolders
          .map(filter)
          .filter((f): f is FileItem => f !== null);
      }
      
      return newFolder;
    };

    if (!foderToShow) return null;
    return filter(foderToShow);
  }, [foderToShow, itemsToMove]);

  if (!isOpen) {
    return null;
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[425px] max-h-[80vh] scrollbar-hide" >
        <DialogHeader>
          <DialogTitle>Select destination folder</DialogTitle>
          <DialogDescription>
            Select the folder where you want to move the items
          </DialogDescription>
        </DialogHeader>
        <div className="py-4">
          <div className="flex items-center gap-2 mb-4">
            <span className="text-sm font-medium text-indigo-200">
              {selectedFolder?.name || 'Root'}
            </span>
          </div>
          <Accordion 
            type="multiple" 
            value={expandedFolders}
            onValueChange={setExpandedFolders}
            className="overflow-y-auto scrollbar-hide max-h-[50vh]"
          >
            {filteredFolders && showTree(filteredFolders)}
          </Accordion>
        </div>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            onClick={handleConfirm}
            disabled={!selectedFolder || isLoading || selectedFolder.id === currentDirectory.id}
            isLoading={isLoading}
          >
            Select
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default FolderSelectDialog; 