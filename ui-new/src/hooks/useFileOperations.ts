import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { FileItem, FolderStructure, CreateFileRequest } from '../types';
import { fileServiceInstance as fileService, trashServiceInstance as trashService } from '../lib/services';
import { sharingService } from '../lib/api';
import { useNotificationService } from '../services/notificationService';
import { useEffect } from 'react';
import { STORAGE_KEYS, safeSessionStorage } from '../store/safeSessionStorage';
import { useFileStore } from '../store/fileStore';
import { useFileContextStore } from '../store/fileContextStore';

export const QUERY_KEYS = {
  FOLDER_STRUCTURE: 'folderStructure',
  CURRENT_DIRECTORY: 'currentDirectory',
} as const;

// eslint-disable-next-line no-control-regex
const forbiddenChars = /[<>:"/\\|?*\x00-\x1F]/;
const forbiddenNames = /^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i;

const validateItemName = (name: string, parentFolder: FileItem, existingItemId?: string): string | null => {
  if (forbiddenChars.test(name)) {
    return 'Invalid characters in name.';
  }
  if (forbiddenNames.test(name.split('.')[0])) {
    return 'Reserved system name.';
  }

  const combinedItems = [...(parentFolder.subfolders || []), ...(parentFolder.files || [])];
  const duplicate = combinedItems.find(
    item => item.id !== existingItemId && item.name.toLowerCase() === name.toLowerCase() 
  );

  if (duplicate) {
    return `Name "${name}" already exists.`;
  }

  return null;
};

const generateUniqueName = (name: string, existingNames: string[], original: string, isFolder: boolean): string => {
  if (!existingNames.includes(name)) {
    return name;
  }
  const parts = original.split('.');
  const baseName = parts.slice(0, -1).join('.');
  const extension = parts.length > 1 ? `.${parts[parts.length - 1]}` : '';

  let num = 1;
  let newName = isFolder ? `${original} (${num})` : `${baseName} (${num})${extension}`;

  while (existingNames.includes(newName)) {
    num++;
    newName = isFolder ? `${original} (${num})` : `${baseName} (${num})${extension}`;
  }

  return newName;
};

export const useFileOperations = () => {
  const queryClient = useQueryClient();
  const notifications = useNotificationService();
  const { setCurrentDirectory: setStoreCurrentDirectory, setFolderStructure: setStoreFolderStructure, expandedFolders, setExpandedFolders,  currentDirectory } = useFileStore();  
  type UploadedTree = {
    name: string;
    files: File[];
    folders: Map<string, UploadedTree>;
  };

  const {section} = useFileContextStore();
  
  const isFolderInside = (parentFolder: FileItem, childFolderId: string): boolean => {
    if (!parentFolder.subfolders) return false;
    
    if (parentFolder.subfolders.some(folder => folder.id === childFolderId)) {
      return true;
    }
    
    return parentFolder.subfolders.some(folder => isFolderInside(folder, childFolderId));
  };

  const findInitialFolder = (structure: FolderStructure, itemId: string): FileItem | null => {
    const searchInSection = (section: FileItem): FileItem | null => {
      if (!section.subfolders && !section.files) return null;
      
      if(section.files) {
        const directFile = section.files.find(file => file.id === itemId);
        if (directFile) return section;
      }
      
      if(section.subfolders) {
        const directFolder = section.subfolders.find(folder => folder.id === itemId);
        if (directFolder) return section;
        for (const folder of section.subfolders) {
          const found = searchInSection(folder);
          if (found) return found;
        }
      }
      
      return null;
    };
    
    return searchInSection(structure.shared);
  };

  function getElement(structure: FolderStructure, id: string, section: 'root' | 'shared' | 'trash'): {item: FileItem, inSection: boolean} {
    const searchInFolder = (folder: FileItem, inSection: boolean): {item: FileItem, inSection: boolean} | undefined => {
      if (folder.subfolders) {
        const directMatch = folder.subfolders.find(f => f.id === id);
        if (directMatch) return {item: directMatch, inSection: inSection};

        for (const subfolder of folder.subfolders) {
          const found = searchInFolder(subfolder, false);
          if (found) return found;
        }
      }

      if (folder.files) {
        const fileMatch = folder.files.find(f => f.id === id);
        if (fileMatch) return {item: fileMatch, inSection: inSection};
      }

      return undefined;
    };

    return searchInFolder(structure[section], true)!;
  }

  const checkFolderPermissions = (structure: FolderStructure, targetId: string, permissions: ('READ' | 'WRITE' | 'ADMIN')[]): boolean => {
    const target = getElement(structure, targetId, 'shared');
    if(!target.item) {
      return false;
    }
    if(target.item.accessLevel) {
      if(target.inSection) {
        return permissions.includes(target.item.accessLevel) && true;
      }
      return permissions.includes(target.item.accessLevel);
    }
    return false;
  };

  const currentDirectoryQuery = useQuery({
    queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id],
    queryFn: () => {
      if(section === 'trash') {
        return fileService.getFolderById(currentDirectory.id, true);
      } else {
        return fileService.getFolderById(currentDirectory.id, false);
      }
    }, 
    staleTime: 1000 * 60 * 5,
    enabled: !!localStorage.getItem('accessToken') && !!currentDirectory.id,
  });

  const rootStructureQuery = useQuery({
    queryKey: [QUERY_KEYS.FOLDER_STRUCTURE],
    queryFn: async () => {
      const structure = await fileService.getFolderStructure();

      const applyExpansionState = (items: FileItem[]): FileItem[] => {
        return items.map(item => {
          if (item.type === 'folder') {
            return {
              ...item,
              isExpanded: expandedFolders.includes(item.id!.toString()),
              subfolders: item.subfolders ? applyExpansionState(item.subfolders) : []
            };
          }
          return item;
        });
      };

      return {
        ...structure,
        root: {
          ...structure.root,
          subfolders: applyExpansionState(structure.root.subfolders || [])
        },
        shared: {
          ...structure.shared,
          subfolders: applyExpansionState(structure.shared.subfolders || [])
        },
        trash: {
          ...structure.trash,
          subfolders: applyExpansionState(structure.trash.subfolders || [])
        }
      };
    },
    staleTime: 1000 * 60 * 5,
    enabled: !!localStorage.getItem('accessToken'),
  });

  useEffect(() => {
    if (currentDirectoryQuery.data) {
      setStoreCurrentDirectory(currentDirectoryQuery.data);
    }
  }, [currentDirectoryQuery.data, setStoreCurrentDirectory]);

  useEffect(() => {
    if (rootStructureQuery.data) {
      setStoreFolderStructure(rootStructureQuery.data);
    }
  }, [rootStructureQuery.data, setStoreFolderStructure]);

  const setCurrentDirectory = async (folder: FileItem) => {
    if (folder.id) {
      const newId = folder.id.toString();
      setStoreCurrentDirectory(folder);
      safeSessionStorage.setItem(STORAGE_KEYS.CURRENT_DIRECTORY_ID, newId);
    }
    
    await queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, folder.id], folder);
    setStoreCurrentDirectory(folder);
  };

  const setFolderStructure = async (structure: FolderStructure) => {
    
    await queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], structure);
    setStoreFolderStructure(structure);
  };

  const toggleFolderExpansion = async (folderId: string, isExpanded: boolean) => {
  
    const newExpandedFolders = isExpanded
      ? [...expandedFolders, folderId]
      : expandedFolders.filter(id => id !== folderId);
      setExpandedFolders(newExpandedFolders);
  };

  const refreshAll = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] }),
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY] })
    ]);
  };

  const refreshCurrentDirectory = async () => {
    await queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY] });
  };

  const refreshFolderStructure = async (section?: 'root' | 'shared' | 'trash') => {
    if (section) {
      await queryClient.invalidateQueries({ 
        queryKey: [QUERY_KEYS.FOLDER_STRUCTURE],
        refetchType: 'active'
      });
      
      const currentStructure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (currentStructure) {
        const updatedStructure = await fileService.getFolderStructure();
  
        const applyExpansionState = (items: FileItem[]): FileItem[] => {
          return items.map(item => {
            if (item.type === 'folder') {
              return {
                ...item,
                isExpanded: expandedFolders.includes(item.id!.toString()),
                subfolders: item.subfolders ? applyExpansionState(item.subfolders) : []
              };
            }
            return item;
          });
        };
  
        
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], {
          ...currentStructure,
          [section]: {
            ...updatedStructure[section],
            subfolders: applyExpansionState(updatedStructure[section].subfolders || [])
          }
        });
      }
    } else {
      await queryClient.invalidateQueries({ 
        queryKey: [QUERY_KEYS.FOLDER_STRUCTURE],
        refetchType: 'active'
      });
    }
  };

  const updateFolderStructure = (
    folder: FileItem, 
    targetFolderId: string, 
    updateFn: (folder: FileItem) => FileItem
  ): FileItem => {
    if (folder.id === targetFolderId) {
      return updateFn(folder);
    } else {
      return {
        ...folder,
        subfolders: folder.subfolders?.map(folder => {
          return updateFolderStructure(folder, targetFolderId, updateFn);
        })
      };
    };
  };

  function createFolderStructureRecursive(file: File, fileMap: UploadedTree, path: string[]): UploadedTree {
    if(file.name === path[0]) {
      fileMap.files.push(file);
    } else {
      if (fileMap.folders.has(path[0])) {
        fileMap = createFolderStructureRecursive(file, fileMap.folders.get(path[0])!, path.slice(1));
      } else {
        fileMap.folders.set(path[0], {name: path[0], files: [], folders: new Map<string, UploadedTree>()});
        fileMap.folders.set(path[0],createFolderStructureRecursive(file, fileMap.folders.get(path[0])!, path.slice(1)));
      }
    }
    return fileMap;
    
  }

  function createFolderStructure(files: File[]) {
    let fileMap : UploadedTree = {name: '', files: [], folders: new Map<string, UploadedTree>()};
    if(files.length !== 0) {
      const path = files[0].webkitRelativePath.split('/');
      fileMap.name = path[0];
      files.forEach(file => {
        fileMap = createFolderStructureRecursive(file, fileMap, file.webkitRelativePath.split('/').slice(1));
      });
    }
    return fileMap;
  }

  const moveItemMutation = useMutation({
    mutationFn: async ({ items, toFolderId, fromFolderId }: { 
      items: FileItem[], 
      toFolderId: string,
      fromFolderId: string
    }) => {
      if (fromFolderId === toFolderId) {
        console.log("Source and destination folders are the same. No action taken.");
        return; 
      }
      return Promise.all(items.map(item => 
        fileService.moveElement(item.id!.toString(), toFolderId)
      ));
    },
    onMutate: async ({ items, toFolderId, fromFolderId }) => {
      if (fromFolderId === toFolderId) {
        return;
      }
      const structure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (!structure) throw new Error('Unable to get folder structure');

      const toFolder = await queryClient.fetchQuery<FileItem>({
        queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, toFolderId],
        queryFn: () => fileService.getFolderById(toFolderId, false),
      });

      if (toFolder) {
        for (const itemToMove of items) {
          const validationError = validateItemName(itemToMove.name, toFolder);
          if (validationError) {
            throw new Error(validationError);
          }
        }
      }

      if (section === 'trash') {
        throw new Error('Cannot move items from trash');
      }

      const isMovingToChildFolder = items.some(item => 
        item.type === 'folder' && isFolderInside(item, toFolderId)
      );
      if (isMovingToChildFolder) {
        throw new Error('Cannot move folder into its subfolder');
      }

      if (section === 'shared') {
        const permissionDenied = items.some(item => item.accessLevel === 'READ');
        if (permissionDenied) {
          throw new Error('Insufficient permissions to move items');
        }

        const initialFolder = findInitialFolder(structure, items[0].id!.toString());
        if (!initialFolder) {
          throw new Error('Initial folder not found');
        }

        const allFromSameFolder = items.every(item => {
          const itemInitialFolder = findInitialFolder(structure, item.id!.toString());
          return itemInitialFolder?.id === initialFolder.id;
        });

        if (!allFromSameFolder) {
          throw new Error('All items must belong to same folder');
        }

        const targetFolder = await fileService.getFolderById(toFolderId);
        if (!isFolderInside(initialFolder, targetFolder.id!.toString())) {
          throw new Error('Can only move to subfolders');
        }

        if (!checkFolderPermissions(structure, initialFolder.id!.toString(), ['WRITE', 'ADMIN'])) {
          throw new Error('Insufficient permissions in folder structure');
        }

        if (!checkFolderPermissions(structure, targetFolder.id!.toString(), ['WRITE', 'ADMIN'])) {
          throw new Error('Insufficient permissions in target folder');
        }
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, fromFolderId] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });

      const previousFromDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, fromFolderId]) as FileItem;
      const previousToDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, toFolderId]) as FileItem;
      const previousStructure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]) as FolderStructure;

      const itemIds = items.map(item => item.id!.toString());

      const updatedFromDirectory: FileItem = {
        ...(previousFromDirectory as FileItem),
        files: (previousFromDirectory.files || []).filter(f => !itemIds.includes(f.id!.toString())),
        subfolders: (previousFromDirectory.subfolders || []).filter(f => !itemIds.includes(f.id!.toString()))
      };
      
      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, fromFolderId], updatedFromDirectory);

      const updatedToDirectory: FileItem = {
        ...(previousToDirectory as FileItem),
        files: (previousToDirectory.files || []).concat(items.filter(item => item.type === 'file')),
        subfolders: (previousToDirectory.subfolders || []).concat(items.filter(item => item.type === 'folder'))
      };
      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, toFolderId], updatedToDirectory);

      const foldersToMove = items.filter(item => item.type === 'folder');
      if (foldersToMove.length > 0) {
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
          const updatedFromSection = updateFolderStructure(
            old[section],
            fromFolderId,
            folder => ({
              ...folder,
              subfolders: folder.subfolders?.filter(f => !itemIds.includes(f.id!.toString())) || []
            })
          );

          const updatedToSection = updateFolderStructure(
            updatedFromSection,
            toFolderId,
            folder => ({
              ...folder,
              subfolders: [...(folder.subfolders || []), ...foldersToMove]
            })
          );

          return {
            ...old,
            [section]: updatedToSection
          };
        });
      }

      return { previousFromDirectory, previousStructure, previousToDirectory };
    },
    onError: (error: Error, variables, context) => {
      const fromFolderId = variables.items[0].parent!.toString();
      const toFolderId = variables.toFolderId;
      if (context?.previousFromDirectory) {
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, fromFolderId],
          context.previousFromDirectory
        );
      }
      if (context?.previousStructure) {
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      if (context?.previousToDirectory) {
        queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, toFolderId], context.previousToDirectory);
      }
      notifications.error(error.message || 'Error moving items');
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, fromFolderId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, toFolderId] });
    },
    onSuccess: () => {
      notifications.success('Items moved successfully');
      refreshAll();
    }
  });

  const updateItemMutation = useMutation({
    mutationFn: async ({ itemId, newName }: { 
      itemId: string, 
      folderId: string, 
      newName: string 
    }) => {
      return fileService.updateElement(itemId, newName);
    },
    onMutate: async ({ itemId, folderId, newName }): Promise<{ previousDirectory: FileItem[]; previousStructure: FolderStructure }> => {
      const currentDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, folderId]);
      if (currentDirectory) {
        const validationError = validateItemName(newName, currentDirectory, itemId);
        if (validationError) {
          throw new Error(validationError);
        }
      }
      
      const structure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (!structure) throw new Error('Unable to get folder structure');

      if (section === 'trash') {
        throw new Error('Cannot modify items in trash');
      }

      if(newName === '') {
        throw new Error('Name cannot be empty');
      }

      if (section === 'shared') {
        const element = getElement(structure, itemId, 'shared');
        if (!element.item.accessLevel || element.item.accessLevel !== 'WRITE' && element.item.accessLevel !== 'ADMIN') {
          throw new Error('Insufficient permissions to modify item');
        }

        if (!checkFolderPermissions(structure, itemId, ['WRITE', 'ADMIN'])) {
          throw new Error('Insufficient permissions to modify folder');
        }
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, folderId] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });

      const previousDirectory = queryClient.getQueryData<FileItem[]>([QUERY_KEYS.CURRENT_DIRECTORY, folderId]) || [];
      const previousStructure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE])!;

      const directoryCopy = JSON.parse(JSON.stringify(previousDirectory));
      for(const folder of directoryCopy.subfolders || []) {
        if(folder.id === itemId) {
          folder.name = newName;
          folder.updatedAt = new Date();
        }
      }
      for(const file of directoryCopy.files || []) {  
        if(file.id === itemId) {
          file.name = newName;
          file.updatedAt = new Date();
        }
      }

      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, folderId], directoryCopy as FileItem);

      queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
        return {
          ...old,
          [section]: updateFolderStructure(
              old[section],
              folderId,
              folder => ({
                ...folder,
                subfolders: folder.subfolders?.map(f => 
                  f.id === itemId ? { ...f, name: newName, updatedAt: new Date() } : f
                ) || [],
                files: folder.files?.map(f =>
                  f.id === itemId ? { ...f, name: newName, updatedAt: new Date() } : f
                ) || []
              })
            )
        };
      });

      return { previousDirectory, previousStructure };
    },
    onError: (error: Error, variables, context) => {
      if (context?.previousDirectory) {
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, variables.folderId],
          context.previousDirectory
        );
      }
      if (context?.previousStructure) {
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      
      if (error.message && (error.message.includes('409') || error.message.includes('Conflict') || error.message.includes('already exists'))) {
        notifications.error('Name already exists');
      } else {
        notifications.error(error.message || 'Error updating item');
      }
      
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, variables.folderId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
    },
    onSuccess: () => {
      notifications.success('Item updated');
      refreshCurrentDirectory();
    }
  });

  const createFolderMutation = useMutation({
    mutationFn: async ({ name, parentId }: { name: string, parentId: string }) => {
      return fileService.createFolder(name, parentId);
    },
    onMutate: async ({ name, parentId }) => {
      const structure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (!structure) throw new Error('Unable to get folder structure');

      if (section === 'trash') {
        throw new Error('Cannot create folders in trash');
      }

      if (section === 'shared') {
        if (!checkFolderPermissions(structure, parentId, ['WRITE', 'ADMIN'])) {
          throw new Error('Insufficient permissions to create folders');
        }
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, parentId] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
  
      const previousDirectory = queryClient.getQueryData([QUERY_KEYS.CURRENT_DIRECTORY, parentId]) as FileItem;
      const previousStructure = queryClient.getQueryData([QUERY_KEYS.FOLDER_STRUCTURE]);
  
      const newFolder: FileItem = {
        id: `temp-${Date.now()}`,
        name: generateUniqueName(name, previousDirectory.subfolders?.map((f: FileItem) => f.name) || [], name, true),
        type: 'folder',
        parent: parentId,
        subfolders: []
      };

      const directoryCopy = JSON.parse(JSON.stringify(previousDirectory));
      directoryCopy.subfolders = [...(directoryCopy.subfolders || []), newFolder];
  
      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, parentId], directoryCopy);
  
      queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
        return {
          ...old,
          [section]: updateFolderStructure(
              old[section],
              parentId,
              folder => ({
                ...folder,
                subfolders: [...(folder.subfolders || []), newFolder]
              })
            )
        };
      });
  
      return { previousDirectory, previousStructure };
    },
    onError: (err, variables, context) => {
      if (context?.previousDirectory) {
        
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, variables.parentId],
          context.previousDirectory
        );
      }
      if (context?.previousStructure) {
        
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      notifications.error('Error creating folder');
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, variables.parentId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
    },
    onSuccess: () => {
      notifications.success('Folder created');
      
      refreshAll();
    }
  });

  const uploadFileMutation = useMutation({
    mutationFn: async ({ item, file, parentId }: { item: FileItem, file: File, parentId: string }) => {
      const request: CreateFileRequest = {
        name: item.name,
        parentFolderId: parentId,
        isFolder: false,
        size: item.size,
        contentType: file.type
      };
      return fileService.uploadFile(request, file);
    },
    onMutate: async ({ file, parentId }) => {
      const structure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (!structure) throw new Error('Unable to get folder structure');

      if (section === 'trash') {
        throw new Error('Cannot upload files to trash');
      }

      if (section === 'shared') {
        if (!checkFolderPermissions(structure, parentId, ['WRITE', 'ADMIN'])) {
          throw new Error('Insufficient permissions to upload files');
        }
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, parentId] });

      const previousDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, parentId]);

      const newFile: FileItem = {
        id: `temp-${Date.now()}`,
        name: file.name,
        type: 'file',
        parent: parentId,
        size: file.size,
        mimeType: file.type
      };

      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, parentId], 
        (previousDirectory: FileItem) => {
            const directoryCopy = JSON.parse(JSON.stringify(previousDirectory));
            return {
                ...directoryCopy,
                files: [...(directoryCopy.files || []), newFile]
            };
        }
    );

      return { previousDirectory };
    },
    onError: (err, variables, context) => {
      if (context?.previousDirectory) {
        
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, variables.parentId],
          context.previousDirectory
        );
      }
      notifications.error('Error uploading file');
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, variables.parentId] });
    },
    onSuccess: () => {
      notifications.success('File uploaded');
      
      refreshCurrentDirectory();
    }
  });

  const uploadFolderMutation = useMutation({
    mutationFn: async ({ files, parentId }: { files: File[], parentId: string }) => {
      if (!files || files.length === 0) {
        throw new Error('No files provided');
      }

      const uploadStructure = async (structure: UploadedTree, parentId: string) => {
        const parent = await fileService.createFolder(structure.name, parentId);
        if(structure.folders.size > 0) {
          for(const folder of structure.folders.values()) {
            await uploadStructure(folder, parent.id);
          }
        }
        if(structure.files.length > 0) {
          for(const file of structure.files) {
            console.log('Uploaded folder Id:', parent.id);
            await fileService.uploadFile({name: file.name, parentFolderId: parent.id, contentType: file.type, isFolder: false, size: file.size}, file);
          }
        }
        return parent;
      }

      const fileMap = createFolderStructure(files);

      await uploadStructure(fileMap, parentId);

      const updatedFolder = await fileService.getElement(parentId);
      const updatedStructure = await fileService.getFolderStructure();
      return { updatedFolder, updatedStructure };
    },
    onMutate: async ({ files, parentId }) => {
      const structure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (!structure) throw new Error('Unable to get folder structure');

      if (section === 'trash') {
        throw new Error('Cannot upload folders to trash');
      }

      if (section === 'shared') {
        if (!checkFolderPermissions(structure, parentId, ['WRITE', 'ADMIN'])) {
          throw new Error('Insufficient permissions to upload folders');
        }
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, parentId] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });

      const previousDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, parentId]);
      const previousStructure = queryClient.getQueryData([QUERY_KEYS.FOLDER_STRUCTURE]);

      const fileToFileItem = (file: File): FileItem => {
        return {
          id: `temp-${Date.now()}`,
          name: file.name,
          type: 'file',
        }
      }

      const createOptimisticStructure = (structure: UploadedTree): FileItem => {
        const folder:FileItem = {
          id: `temp-${Date.now()}`,
          name: structure.name,
          type: 'folder',
          parent: parentId,
          size: 0,
          mimeType: 'application/x-folder',
          createdAt: new Date(),
          updatedAt: new Date(),
          subfolders: []
        }

        if(structure.folders.size > 0) {
          folder.subfolders = Array.from(structure.folders.values()).map(createOptimisticStructure);
        }
        if(structure.files.length > 0) {
          folder.files = structure.files.map(fileToFileItem);
        }
        return folder;
      };

      const fileMap = createFolderStructure(files);

      const newFolder = createOptimisticStructure(fileMap);

      const updatedDirectory: FileItem = {
        ...(previousDirectory as FileItem),
        subfolders: [...(previousDirectory?.subfolders || []), newFolder]
      };
      
      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, parentId], updatedDirectory);

      queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
        return {
          ...old,
          [section]: updateFolderStructure(
              old[section],
              parentId,
              folder => ({
                ...folder,
                subfolders: [...(folder.subfolders || []), newFolder]
              })
            )
        };
      });

      return { previousDirectory, previousStructure, optimisticStructure: newFolder };
    },
    onError: (err, variables, context) => {
      if (context?.previousDirectory) {
        
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, variables.parentId],
          context.previousDirectory
        );
      }
      if (context?.previousStructure) {
        
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      notifications.error('Error uploading folder');
      console.error('Error uploading folder:', err);
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, variables.parentId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
    },
    onSuccess: () => {
      notifications.success('Folder uploaded');
      
      refreshAll();
    }
  });

  const pasteFilesMutation = useMutation({
    mutationFn: async ({ items, targetFolderId, isCut }: { 
      items: FileItem[], 
      targetFolderId: string,
      isCut: boolean,
      prevParentId?: string
    }) => {
      const promises = items.map(async (item) => {
        if (!item.id) throw new Error('Invalid ID');
        if (isCut) {
          return fileService.moveElement(item.id.toString(), targetFolderId);
        } else {
          return fileService.copyElement(item.id.toString(), targetFolderId);
        }
      });
      return Promise.all(promises);
    },
    onMutate: async ({ items, targetFolderId, isCut, prevParentId }) => {
      const structure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (!structure) throw new Error('Unable to get folder structure');

      if (section === 'trash') {
        throw new Error('Cannot paste items in trash');
      }

      if (section === 'shared') {
        if (!checkFolderPermissions(structure, targetFolderId, ['WRITE', 'ADMIN'])) {
          throw new Error('Insufficient permissions to paste items');
        }

        if (isCut && prevParentId) {
          if (!checkFolderPermissions(structure, prevParentId, ['WRITE', 'ADMIN'])) {
            throw new Error('Insufficient permissions to move items from this location');
          }
        }

        const permissionDenied = items.some(item => item.accessLevel === 'READ');
        if (permissionDenied) {
          throw new Error('Insufficient permissions to move/copy these items');
        }
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, targetFolderId] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });

      const previousDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, prevParentId]);
      const parentDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, targetFolderId]);
      const previousStructure = queryClient.getQueryData([QUERY_KEYS.FOLDER_STRUCTURE]);

      const optimisticItems = items.map(item => {
          if(item.type === 'file') {
            const existingItem = parentDirectory?.files?.find(f => f.name === item.name);
            if(existingItem) {
              const newName = generateUniqueName(item.name, parentDirectory?.files?.map(f => f.name) || [], item.name, false);
              return {
                ...item,
                id: isCut ? item.id?.toString() : `temp-${Date.now()}-${item.id?.toString()}`,
                parent: targetFolderId,
                name: newName
              }
            } else {
              return item;
            }
          } else {
            const existingItem = parentDirectory?.subfolders?.find(f => f.name === item.name);
            if(existingItem) {
              const newName = generateUniqueName(item.name, parentDirectory?.subfolders?.map(f => f.name) || [], item.name, true);
              return {
                ...item,
                id: isCut ? item.id?.toString() : `temp-${Date.now()}-${item.id?.toString()}`,
                parent: targetFolderId,
                name: newName
              }
            } else {
              return item;
            }
          }
      });

      const directoryCopy = JSON.parse(JSON.stringify(parentDirectory));
      const updatedDirectory: FileItem = {
          ...directoryCopy,
          files: [...(directoryCopy.files || []), ...optimisticItems.filter(item => item.type === 'file')],
          subfolders: [...(directoryCopy.subfolders || []), ...optimisticItems.filter(item => item.type === 'folder')]
      };

      if (isCut && prevParentId && previousDirectory) {
        const updatedParentDirectory: FileItem = {
          ...previousDirectory,
          files: previousDirectory.files?.filter(f => !items.some(item => item.id === f.id)) || [],
          subfolders: previousDirectory.subfolders?.filter(f => !items.some(item => item.id === f.id)) || []
        };
        queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, prevParentId], updatedParentDirectory);
      }
      
      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, targetFolderId], updatedDirectory);

      queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
        const itemIds = items.map(item => item.id?.toString());
        
        if (isCut && items.length > 0) {
          if (prevParentId) {
            old = {
              ...old,
              [section]: updateFolderStructure(
                old[section],
                prevParentId,
                folder => ({
                  ...folder,
                  subfolders: folder.subfolders?.filter(f => !itemIds.includes(f.id?.toString())) || []
                })
              )
            };
          }
        }

        return {
          ...old,
          [section]: updateFolderStructure(
              old[section],
              targetFolderId,
              folder => ({
                ...folder,
                subfolders: [...(folder.subfolders || []), ...optimisticItems.filter(item => item.type === 'folder')]
              })
            )
        };
      });

      return { parentDirectory, previousStructure };
    },
    onError: (err, variables, context) => {
      if (context?.parentDirectory) {
        
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, variables.targetFolderId],
          context.parentDirectory
        );
      }
      if (context?.previousStructure) {
        
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      notifications.error('Error pasting items');
      console.error('Error pasting items:', err);
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, variables.targetFolderId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
    },
    onSuccess: (result, variables) => {
      notifications.success(
        variables.isCut 
          ? 'Items moved correctly' 
          : 'Items copied correctly'
      );
      
      refreshAll();
    }
  });

  const restoreItemMutation = useMutation({
    mutationFn: async ({ items }: { items: FileItem[] }) => {
      return Promise.all(items.map(item => trashService.restoreItem(item.id)));
    },
    onMutate: async ({ items }) => {
      if (section !== 'trash') {
        throw new Error('Can only restore items from trash');
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });

      const previousStructure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]) as FolderStructure;
      const previousDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id]) as FileItem;

      if (!previousDirectory) {
        throw new Error('Current directory not found');
      }

      const itemsToRestore = items.map(item => {
        if (!item.id) return null;
        
        const folder = previousDirectory.subfolders?.find(f => f.id === item.id);
        if (folder) return folder;

        const file = previousDirectory.files?.find(f => f.id === item.id);
        if (file) return file;

        return null;
      }).filter((item): item is FileItem => item !== null);

      queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
        return {
          ...old,
          root: updateFolderStructure(
              old.root,
              currentDirectory.id,
              folder => {
                const foldersToAdd = itemsToRestore.filter(item => item.type === 'folder');
                if (foldersToAdd.length > 0) {
                  return {
                    ...folder,
                    subfolders: [...(folder.subfolders || []), ...foldersToAdd]
                  };
                }
                return folder;
              }
            ),
          trash: updateFolderStructure(
              old.trash,
              currentDirectory.id,
              folder => {
                const itemIds = items.map(item => item.id);
                if (itemIds.length > 0) {
                  return {
                    ...folder,
                    subfolders: folder.subfolders?.filter(f => !itemIds.includes(f.id))
                  };
                }
                return folder;
              }
            )
          }
      });

      try {
      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id], (old: FileItem) => {
        const itemIds = items.map(item => item.id);
        return {
          ...old,
          subfolders: old.subfolders?.filter(f => !itemIds.includes(f.id)) || [],
          files: old.files?.filter(f => !itemIds.includes(f.id)) || []
        };
      });
    } catch (error) {
      console.error('Error updating current directory:', error);
    }

      return { previousDirectory, previousStructure };
    },
    onError: (err, variables, context) => {
      if (context?.previousStructure) {
        
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      if (context?.previousDirectory) {
        
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id],
          context.previousDirectory
        );
      }
      notifications.error('Error restoring items');
      console.error('Error restoring items:', err);
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
    },
    onSuccess: () => {
      notifications.success('Items restored correctly');
      refreshAll();
    }
  });

  const deleteItemMutation = useMutation({
    mutationFn: async ({ items, section }: { items: FileItem[], section: 'root' | 'trash' | 'shared' }) => {
      try {
        if(section === 'root') {
          return Promise.all(items.map(item => fileService.deleteElement(item.id!.toString())));
        } else if(section === 'trash') {
          return Promise.all(items.map(item => trashService.deleteItem(item.id!.toString())));
        } else {
          notifications.error('Cannot delete items from this section');
          return;
        }
      } catch (error) {
        console.error('Error deleting items:', error);
        notifications.error('Error deleting items');
        return;
      }
    },
    onMutate: async ({ items, section }) => {
      const structure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]);
      if (!structure) throw new Error('Unable to get folder structure');

      if (section === 'shared') {
        const permissionDenied = items.some(item => item.accessLevel !== 'ADMIN');
        if (permissionDenied) {
          throw new Error('Insufficient admin permissions to delete these items');
        }

        const folderId = currentDirectory.id;
        if (!checkFolderPermissions(structure, folderId, ['ADMIN'])) {
          throw new Error('Insufficient admin permissions in this folder');
        }
      }

      const itemIds = items.map(item => item.id!.toString());
      const folderId = currentDirectory.id;

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, folderId] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });

      const previousDirectory = queryClient.getQueryData<FileItem>([QUERY_KEYS.CURRENT_DIRECTORY, folderId]) as FileItem;
      const previousStructure = queryClient.getQueryData<FolderStructure>([QUERY_KEYS.FOLDER_STRUCTURE]) as FolderStructure;

      const itemsToDelete = items.map(item => {
        if (!item.id) return null;
        
        const folder = (previousDirectory.subfolders || []).find(f => f.id === item.id);
        if (folder) return folder;

        const file = (previousDirectory.files || []).find(f => f.id === item.id);
        if (file) return file;

        return null;
      }).filter((item): item is FileItem => item !== null);

      const updatedDirectory: FileItem = {
        ...previousDirectory,
        subfolders: (previousDirectory.subfolders || []).filter(f => !itemIds.includes(f.id!.toString())),
        files: (previousDirectory.files || []).filter(f => !itemIds.includes(f.id!.toString()))
      };
      
      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, folderId], updatedDirectory);

      const foldersToDelete = itemsToDelete.filter(item => item.type === 'folder');
      if (foldersToDelete.length > 0) {
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
          if (section === 'root') {
            return {
              ...old,
              root: updateFolderStructure(
                  old.root,
                  folderId,
                  folder => ({
                    ...folder,
                    subfolders: folder.subfolders?.filter(f => !itemIds.includes(f.id!.toString())) || []
                  })
                ),
              trash: updateFolderStructure(
                  old.trash,
                  folderId,
                  folder => ({
                    ...folder,
                    subfolders: [...(folder.subfolders || []), ...foldersToDelete]
                  })
                )
              };
          } else {
            return {
              ...old,
              [section]: updateFolderStructure(
                  old[section],
                  folderId,
                  folder => ({
                    ...folder,
                    subfolders: folder.subfolders?.filter(f => !itemIds.includes(f.id!.toString())) || []
                  })
                )
            };
          }
        });
      }

      return { previousDirectory, previousStructure };
    },
    onError: (err, variables, context) => {
      const folderId = variables.items[0].parent!.toString();
      if (context?.previousDirectory) {
        
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, folderId],
          context.previousDirectory
        );
      }
      if (context?.previousStructure) {
        
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      notifications.error('Error deleting items');
      console.error('Error deleting items:', err);
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY, folderId] });
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
    },
    onSuccess: (_, variables) => {
      notifications.success(
        variables.section === 'root' 
          ? 'Items moved to trash correctly'
          : 'Items deleted permanently'
      );
      refreshAll();
    }
  });

  const revokeAccessMutation = useMutation({
    mutationFn: async ({ items }: { items: FileItem[] }) => {
      return Promise.all(items.map(item => 
        sharingService.revokeAccess(item.id!.toString())
      ));
    },
    onMutate: async ({ items }) => {
      if (section !== 'shared') {
        throw new Error('Can only revoke access from shared section');
      }

      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.FOLDER_STRUCTURE] });
      await queryClient.cancelQueries({ queryKey: [QUERY_KEYS.CURRENT_DIRECTORY] });

      const previousStructure = queryClient.getQueryData([QUERY_KEYS.FOLDER_STRUCTURE]);
      const previousDirectory = queryClient.getQueryData([QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id]);

      const itemIds = items.map(item => item.id!.toString()) || [];

      queryClient.setQueryData([QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id], (old: FileItem) => {
        return {
          ...old,
          subfolders: old.subfolders?.filter(f => !itemIds.includes(f.id!.toString())) || [],
          files: old.files?.filter(f => !itemIds.includes(f.id!.toString())) || []
        };
      });

      queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], (old: FolderStructure) => {
        const itemIds = items.map(item => item.id!.toString());
        return {
          ...old,
            shared: updateFolderStructure(
              old.shared,
              currentDirectory.id,
              folder => ({
                ...folder,
                subfolders: folder.subfolders?.filter(f => !itemIds.includes(f.id!.toString())) || [],
                files: folder.files?.filter(f => !itemIds.includes(f.id!.toString())) || []
              })
            )
        };
      });

      return { previousStructure, previousDirectory };
    },
    onError: (err, variables, context) => {
      if (context?.previousStructure) {
        
        queryClient.setQueryData([QUERY_KEYS.FOLDER_STRUCTURE], context.previousStructure);
      }
      if (context?.previousDirectory) {
        
        queryClient.setQueryData(
          [QUERY_KEYS.CURRENT_DIRECTORY, currentDirectory.id],
          context.previousDirectory
        );
      }
      notifications.error('Error revoking access');
    },
    onSuccess: () => {
      notifications.success('Access revoked correctly');
      refreshAll();
    }
  });

  return {
    folderStructure: rootStructureQuery.data,
    folderStructureIsLoading: rootStructureQuery.isLoading,
    folderStructureIsError: rootStructureQuery.isError,
    folderStructureError: rootStructureQuery.error,
    setCurrentDirectory,
    setFolderStructure,
    refreshAll,
    refreshFolderStructure,
    refreshCurrentDirectory,
    createFolder: createFolderMutation.mutate,
    moveItem: moveItemMutation.mutate,
    updateItem: updateItemMutation.mutate,
    uploadFile: uploadFileMutation.mutate,
    uploadFolder: uploadFolderMutation.mutate,
    pasteFiles: pasteFilesMutation.mutate,
    toggleFolderExpansion,
    isMoving: moveItemMutation.isPending,
    isUpdating: updateItemMutation.isPending,
    isUploading: uploadFileMutation.isPending,
    isUploadingFolder: uploadFolderMutation.isPending,
    isPasting: pasteFilesMutation.isPending,
    restoreItem: restoreItemMutation.mutate,
    deleteItem: deleteItemMutation.mutate,
    isRestoring: restoreItemMutation.isPending,
    isDeleting: deleteItemMutation.isPending,
    revokeAccess: revokeAccessMutation.mutate,
    isRevokingAccess: revokeAccessMutation.isPending,
    currentDirectory: currentDirectoryQuery.data,
    currentDirectoryIsLoading: currentDirectoryQuery.isLoading,
    currentDirectoryIsError: currentDirectoryQuery.isError,
    currentDirectoryError: currentDirectoryQuery.error,
    expandedFolders
  };
}; 