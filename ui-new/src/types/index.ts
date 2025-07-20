export interface FileItem {
  id: string;
  name: string;
  type: 'file' | 'folder';
  isDirectory?: boolean;
  ownerId?: string;
  subfolders?: FileItem[];
  files?: FileItem[];
  parent?: string;
  createdAt?: Date;
  updatedAt?: Date;
  size?: number;
  mimeType?: string;
  accessLevel?: 'ADMIN' | 'READ' | 'WRITE' | undefined;
  shared?: boolean;
  //trash details
  deletedAt?: Date;
  clientDeletedAt?: Date;

  //shared details
  ownerUserName?: string;
  ownerUserEmail?: string;
  ownerUserId?: string;
  expirationDate?: Date;
}

export interface FolderStructure {
  root: FileItem;
  shared: FileItem;
  trash: FileItem;
}

export interface AccessRule {
  fileId: string;
  username: string;
  email: string;
  accessType: number;
}

export interface User {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN';
}

export interface SharedUser {
  username: string;
  email: string;
  accessType: 'READ' | 'WRITE';
}

export interface ServerStats {
    totalUsers: number;
    activeUsers: number;
    totalFiles: number;
    totalFolders: number;
    uptime: number;
}

export interface StorageStats {
    totalSpace: number;
    usedSpace: number;
    availableSpace: number;
    usageByType: {
        [key: string]: number;
    };
}

export interface SystemLog {
    id: string;
    timestamp: Date;
    level: 'info' | 'warning' | 'error';
    message: string;
    userId?: string;
    action: string;
}

export interface AdminUser extends User {
    lastLogin?: Date;
    totalFiles: number;
    storageUsed: number;
    createdAt: Date;
}

export interface CreateFileRequest {
  name: string;
  parentFolderId: string;
  contentType: string;
  isFolder: boolean;
  size?: number;
} 