import { useRef, useState, useEffect, useCallback, useMemo } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import { DndContext, DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { FileItem } from '../../types';
import { fileServiceInstance as fileService } from '../../lib/services';
import { useFileSelection } from '../../hooks/useFileSelection';
import { useFileOperations } from '../../hooks/useFileOperations';
import { FileManagerContextMenu } from '../../components/FileManagerContextMenu';
import File from '../../components/File';
import { useFileContextStore } from '../../store/fileContextStore';
import { useNotificationService } from '../../services/notificationService';
import { useFileSelectionStore } from '../../store/fileSelectionStore';
import { useFileStore } from '../../store/fileStore';
import CreateNewFolderDialog from '../../components/CreateNewFolderDialog';
import { RenameDialog } from '../../components/File/RenameDialog';
import { FileDetailsDrawer } from '../../components/File/FileDetailsDrawer';
import FolderSelectDialog  from '../../components/FolderSelectDialog';
import { ShareDialog } from '../../components/File/ShareDialog';
import { useShareManager } from '../../hooks/useShareManager';
import { useSelecto } from '../../hooks/useSelecto';
import { useFileShortcuts } from '../../hooks/useFileShortcuts';
import { websocketService } from '../../lib/websocket';

const FileManager = () => {

	const [contextMenu, setContextMenu] = useState<{ isOpen: boolean; x: number; y: number }>({
		isOpen: false,
		x: 0,
		y: 0
	});
	const [initalized, setInitalized] = useState(false);

	const [isCreateFolderOpen, setIsCreateFolderOpen] = useState(false);
	const [openRenameDialog, setOpenRenameDialog] = useState(false);
	const [openShareDialog, setOpenShareDialog] = useState(false);
	const [openMoveDialog, setOpenMoveDialog] = useState(false);
	const [openDrawer, setOpenDrawer] = useState(false);
	const [file, setFile] = useState<FileItem | undefined>(undefined);


	const containerRef = useRef<HTMLDivElement>(null);
	
	const fileOperations = useFileOperations();
	const fileContext = useFileContextStore();
	const shareManager = useShareManager();
	const selectedFiles = useFileSelectionStore((state) => state.selectedFiles);
	const {clearSelection, clearClipboardFiles} = useFileSelectionStore();
	const { handleOutsideClick } = useFileSelection({containerRef});
    useFileShortcuts(containerRef);
	const notifications = useNotificationService();
	const currentDirectory = useFileStore((state) => state.currentDirectory);
	const {folderStructure} = useFileStore();
	const {
		handleMouseDown,
		handleMouseMove,
		handleMouseUp,
		selectoRef,
	} = useSelecto({containerRef: containerRef, contextMenuOpen: contextMenu.isOpen});

	const {
		createFolder,
		moveItem,
	  } = useFileOperations();

	
	const [, setActiveDragItem] = useState<FileItem | null>(null);

	const handleCloseContextMenu = useCallback(() => {
		setContextMenu({ isOpen: false, x: 0, y: 0 });
	}, []);

	const isInTrash = useMemo(() => fileContext.isInTrash(), [fileContext]);
	const isInShared = useMemo(() => fileContext.isInShared(), [fileContext]);
	

	useEffect(() => {
		const handleContextMenu = (e: Event) => { 
			e.preventDefault();
			if (e instanceof MouseEvent) {
				
				const target = e.target as HTMLElement;
				const fileId = target.closest('[data-file-id]')?.getAttribute('data-file-id');
				let file: FileItem | undefined;
				if (fileId) {
					file = currentDirectory.files?.find(f => f.id === fileId) || currentDirectory.subfolders?.find(f => f.id === fileId);	
					setFile(file);
				}
				else {
					file = undefined;
					setFile(undefined);
				}
				setContextMenu({
					isOpen: true,
					x: e.clientX,
					y: e.clientY
				});
			}
		};

		const element = containerRef.current;
		if (element) {
			element.addEventListener('contextmenu', handleContextMenu);
			return () => {
				element.removeEventListener('contextmenu', handleContextMenu);
			};
		}
	}, [currentDirectory, setContextMenu]);

  const section = useFileContextStore((state) => state.section);

	useEffect(() => {
		websocketService.update((data) => {
			if(data.elementId === currentDirectory.id || data.parentId === currentDirectory.id || data.parentId === currentDirectory.parent) {
				fileOperations.refreshAll();
			} else if(data.section === 'shared' && isInShared || data.section === 'trash' && isInTrash) {
				fileOperations.refreshAll();
			} else {
				fileOperations.refreshFolderStructure();
			}
		});
	}, []);

  useEffect(() => {
    clearClipboardFiles();
	clearSelection();
	setContextMenu({ isOpen: false, x: 0, y: 0 });
  }, [section, clearClipboardFiles, clearSelection]);

  
  useEffect(() => {
    setContextMenu({ isOpen: false, x: 0, y: 0 });
  }, [currentDirectory.id]);
	
	const loadRootData = useCallback(async () => {
		try {
			const rootFolder = await fileService.getRootFolder();
			fileOperations.setCurrentDirectory(rootFolder);
			fileContext.setSection('root');
			await fileOperations.refreshFolderStructure();
		} catch {
			notifications.error('Could not load initial files');
		}
	}, [fileContext, fileOperations, notifications]);

	useEffect(() => {
		if (!initalized) {
			setInitalized(true);
		}
	}, [loadRootData, initalized]);

	
	const handleDragStart = useCallback((event: DragStartEvent) => {
		const { active } = event;
		const draggedItem = active.data.current as FileItem;
		setActiveDragItem(draggedItem);
	}, []);

	
	const handleDragEnd = useCallback(async (event: DragEndEvent) => {
		const { active, over } = event;
		setActiveDragItem(null);
		
		if (!over || active.id === over.id) {
			return;
		}
		if(!active.data.current || !over.data.current) {
			return;
		}
		
		const draggedItem = active.data.current.file as FileItem;
		const dropTarget = over.data.current.file as FileItem;
		if (dropTarget && dropTarget.id) {
			try {
				if (draggedItem.id !== dropTarget.id && draggedItem.id !== dropTarget.parent && dropTarget.type === 'folder') {
					const itemsToMove = selectedFiles.length > 0 && selectedFiles.some(f => f === draggedItem)
						? selectedFiles
						: [draggedItem];
					await fileOperations.moveItem({
						items: itemsToMove, 
						toFolderId: dropTarget.id.toString(), 
						fromFolderId: fileOperations.currentDirectory.id || 'root'
					});
					notifications.success('Items moved successfully');
				}
			} catch {
				notifications.error('Could not move the item');
			}
		}
	}, [selectedFiles, fileOperations, notifications]);


	const handleRename = useCallback(async (newFileName: string) => {
		if (!file) return;
		fileOperations.updateItem({itemId: file.id!.toString(), folderId: file.parent!.toString(), newName: newFileName});
		setOpenRenameDialog(false);
	  }, [file, fileOperations]);
	
	  const handleMove = useCallback(async (targetFolder: FileItem, itemsToMove: FileItem[]) => {
		if (!file) return;
		await moveItem({ items: itemsToMove, toFolderId: targetFolder.id!.toString(), fromFolderId: currentDirectory.id!.toString() });
		setOpenMoveDialog(false);
		clearSelection();	
	  }, [file, currentDirectory.id, moveItem, clearSelection]);
	
	  const share = useCallback(async (userAccess: {username: string, fileId: string, accessType: 'READ' | 'WRITE' }[], originalSharedAccess: Map<string, {username: string, fileId: string, accessType: 'READ' | 'WRITE' }[]>) => {
		const promises = [];
		if(originalSharedAccess.size > 0) { 
			for (const access of userAccess) {
				if(!originalSharedAccess.has(access.username) || !originalSharedAccess.get(access.username)!.some(a => a.fileId === access.fileId)) {
					promises.push(shareManager.shareWithUser(access.fileId, access.username, access.accessType));
				} else if(originalSharedAccess.get(access.username)!.some(a => a.fileId === access.fileId && a.accessType !== access.accessType)) {
					promises.push(shareManager.handleUpdateAccess(access.fileId, access.username, access.accessType));
				}
			}
			for (const user of originalSharedAccess.keys()) {
				if(!userAccess.some(a => a.username === user)) {
					originalSharedAccess.get(user)!.forEach(a => {
						promises.push(shareManager.handleUnshareToUser(a.fileId, user));
					});
				} else {
					originalSharedAccess.get(user)!.forEach(a => {
						if(!userAccess.some(access => access.username === user && access.fileId === a.fileId)) {
							promises.push(shareManager.handleUnshareToUser(a.fileId, user));
						}
					});
				}
			}
			await Promise.all(promises);
		} else {
			for (const access of userAccess) {
				promises.push(shareManager.shareWithUser(access.fileId, access.username, access.accessType));
			}
			await Promise.all(promises);
		}
	  }, [shareManager]); 

	  const handleCreateFolder = useCallback(async (name: string) => {	
		if (!name.trim()) {
		  return;
		}
		await createFolder({ name: name, parentId: currentDirectory.id!.toString() });
		setIsCreateFolderOpen(false);
	  }, [createFolder, currentDirectory.id]);

	  const filterAndSort = useCallback((currentDirectory: FileItem) => {
		const files = [];
		console.log("subfolders");
		if(!fileContext.filterByFolders) {
			files.push(...(currentDirectory.subfolders || []));
		}
		console.log(files);
		console.log("files");
		if(!fileContext.filterByFiles) {
			files.push(...(currentDirectory.files || []));
		}
		console.log(files);
		if(fileContext.sortBy) {
			if(fileContext.sortBy === 'nameUp') {
				files.sort((a, b) => a.name.localeCompare(b.name))
			} else if(fileContext.sortBy === 'nameDown') {
				files.sort((a, b) => b.name.localeCompare(a.name))
			} else if(fileContext.sortBy === 'createdAtUp') {
				files.sort((a, b) => a.createdAt!.getTime() - b.createdAt!.getTime());
			} else if(fileContext.sortBy === 'createdAtDown') {
				files.sort((a, b) => b.createdAt!.getTime() - a.createdAt!.getTime());
			} else if(fileContext.sortBy === 'updatedAtUp') {
				files.sort((a, b) => a.updatedAt!.getTime() - b.updatedAt!.getTime());
			} else if(fileContext.sortBy === 'updatedAtDown') {
				files.sort((a, b) => b.updatedAt!.getTime() - a.updatedAt!.getTime());
			} else if(fileContext.sortBy === 'sizeUp') {
				files.sort((a, b) => a.size! - b.size!);
			} else if(fileContext.sortBy === 'sizeDown') {
				files.sort((a, b) => b.size! - a.size!);
			} else if(fileContext.sortBy === 'type') {
				files.sort((a, b) => {
					
					if (a.type === 'folder' && b.type !== 'folder') return -1;
					if (a.type !== 'folder' && b.type === 'folder') return 1;
					
					return a.type.localeCompare(b.type);
				});
			}
		}
		return files;
	  }, [fileContext]);

	const deepCopy = <T,>(obj: T): T => JSON.parse(JSON.stringify(obj));

	const getAncestor = (tFolder: FileItem): FileItem => {
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

	return (
		<DndContext
			onDragStart={handleDragStart}
			onDragEnd={handleDragEnd}
		>
			<div
				ref={containerRef}
				className="relative h-full p-4"
				onClick={(e: ReactMouseEvent) => {
					console.log('click')
					handleOutsideClick(e, selectoRef)}}
				onMouseDown={handleMouseDown}
				onMouseMove={handleMouseMove}
				onMouseUp={handleMouseUp}
			>
				<div className="grid grid-cols-[repeat(auto-fill,minmax(150px,1fr))] gap-4">
					{(filterAndSort(currentDirectory)).map((file) => (
						<File
							key={file.id}
							file={file}
						/>
					))}
				</div>
			</div>

			<FileManagerContextMenu
				position={contextMenu.isOpen ? { x: contextMenu.x, y: contextMenu.y } : undefined}
				isOpen={contextMenu.isOpen}
				onClose={handleCloseContextMenu}
				file={file}
				setOpenRenameDialog={setOpenRenameDialog}
				setOpenMoveDialog={setOpenMoveDialog}
				setOpenShareDialog={setOpenShareDialog}
				setOpenDrawer={setOpenDrawer}
				setIsCreateFolderOpen={setIsCreateFolderOpen}
			/>
			<CreateNewFolderDialog
				isOpen={isCreateFolderOpen}
				onOpenChange={setIsCreateFolderOpen}
				onConfirm={handleCreateFolder}
			/>	
			{file && (
			<>
				<RenameDialog
					open={openRenameDialog}
					onOpenChange={setOpenRenameDialog}
					file={file}
					onRename={handleRename}
				/>
				<ShareDialog
					open={openShareDialog}
					onClose={setOpenShareDialog}
					files={selectedFiles}
					onShare={share}
				/>
				<FolderSelectDialog
					isOpen={openMoveDialog}
					onClose={() => setOpenMoveDialog(false)}
					itemsToMove={selectedFiles}
					onSelect={handleMove}
					foderToShow={ isInShared ? deepCopy(getAncestor(currentDirectory)) : deepCopy(folderStructure.root)}
				/>
				<FileDetailsDrawer
					open={openDrawer}
					onOpenChange={setOpenDrawer}
					file={file}
				/>
			</>
			)}
		</DndContext>
	);
};

export default FileManager;