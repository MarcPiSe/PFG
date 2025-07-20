import { Button } from "../Button";
import {
  Drawer,
  DrawerBody,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "../Drawer";
import { RiUserLine, RiUserUnfollowLine } from "@remixicon/react";
import { SharedUser, FileItem } from "../../types";
import { useShareManager } from "../../hooks/useShareManager";
import { useNotificationService } from "../../services/notificationService";
import { useState, useEffect, useCallback, useMemo } from "react";
import { useFileContextStore } from "../../store/fileContextStore";
import { useAuth } from "../../hooks/userAuth";
import { fileService } from "../../lib/api";

interface FileDetailsDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  file: FileItem;
}

export const FileDetailsDrawer = ({ open, onOpenChange, file }: FileDetailsDrawerProps) => {
  const { user: currentUser } = useAuth();
  const [sharedUsers, setSharedUsers] = useState<Array<{
    id: string;
    username: string;
    email: string;
    accessType: 'READ' | 'WRITE' | 'ADMIN';
  }>>([]);

  const fileContext = useFileContextStore();
  const isInTrash = useMemo(() => fileContext.isInTrash(), [fileContext]);
  const isInShared = useMemo(() => fileContext.isInShared(), [fileContext]);

  const shareManager = useShareManager();
  const notifications = useNotificationService();
  const [fileDetails, setFileDetails] = useState<FileItem | null>(null);
  useEffect(() => {
    const fetchFileDetails = async () => {
      try {
        const details = await fileService.getElement(file.id!.toString());
        setFileDetails(details);
      } catch {
        notifications.error("Failed to load file details.");
      }
    };

    if (open && file.id) {
      fetchFileDetails();
    }
  }, []);

  const handleUpdateAccess = useCallback(async (username: string, newAccessType: 'READ' | 'WRITE' | 'ADMIN') => {
    
    try {
      await shareManager.handleUpdateAccess(file.id!.toString(), username, newAccessType);
      
      setSharedUsers(prevUsers => 
        prevUsers.map(user => 
          user.username === username 
            ? { ...user, accessType: newAccessType }
            : user
        )
      );
      
      
      notifications.success("Access updated");
    } catch {
      notifications.error("Error updating access");
    } 
  }, [file.id, shareManager, notifications]);

  const handleRemoveUser = useCallback(async (username: string) => {
    try {
      await shareManager.handleUnshareToUser(file.id!.toString(), username);
      setSharedUsers(prevUsers => prevUsers.filter(user => user.username !== username));
      notifications.success("User removed");
    } catch {
      notifications.error("Failed to remove user");
    }
  }, [file.id, shareManager, notifications]);

  const fetchAccessRules = useCallback(async () => {
    if (open && file.id) {
      try {
        const accessRules = await shareManager.fetchAccessRules(file.id.toString());
        if (accessRules && accessRules.length > 0) {
          const formattedUsers = accessRules.map((item: SharedUser) => ({
            username: item.username,
            email: item.email,
            accessType: item.accessType,
          }));
          setSharedUsers(formattedUsers);
        } else {
          setSharedUsers([]);
        }
      } catch {
        notifications.error("Error fetching access rules");
        setSharedUsers([]);
      }
    } else if (!open) {
      setSharedUsers([]);
    }
  }, [open, file.id]);

  useEffect(() => {
    fetchAccessRules();
  }, [fetchAccessRules, file.shared]);

  if (!open) {
    return null;
  }

  return (
    <Drawer open={open} onOpenChange={(newOpen) => {
      onOpenChange(newOpen);
    }}>
      <DrawerContent className="no-clear-select">
        <DrawerHeader>
          <DrawerTitle>File Details</DrawerTitle>
          <DrawerDescription>
            Detailed information about {file.name}
          </DrawerDescription>
        </DrawerHeader>
        <DrawerBody>
          <div className="flex flex-col gap-4 no-clear-select">
          <div>
        <h3 className="font-semibold mb-2">Basic Information</h3>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <span className="text-gray-500">Name:</span>
          <span className="text-white">{file.name}</span>
          <span className="text-gray-500">Type:</span>
          <span className="text-white">{file.type}</span>
          <span className="text-gray-500">Size:</span>
          <span className="text-white">{file.size ? `${file.size} bytes` : 'N/A'}</span>
          <span className="text-gray-500">Created:</span>
          <span className="text-white">{file.createdAt ? new Date(file.createdAt).toLocaleString() : 'N/A'}</span>
          <span className="text-gray-500">Modified:</span>
          <span className="text-white">{file.updatedAt ? new Date(file.updatedAt).toLocaleString() : 'N/A'}</span>
        </div>
      </div>

      {isInTrash && (
        <div>
          <h3 className="font-semibold mb-2">Trash Information</h3>
          <div className="grid grid-cols-2 gap-2 text-sm">
            <span className="text-gray-500">Deleted At:</span>
            <span className="text-white">{file.deletedAt ? new Date(file.deletedAt).toLocaleString() : 'N/A'}</span>
            {file.expirationDate && (
              <>
                <span className="text-gray-500">Expiration Date:</span>
                <span className="text-white">{new Date(file.expirationDate).toLocaleString()}</span>
              </>
            )}
          </div>
        </div>
      )}

      {isInShared && (
        <div>
          <h3 className="font-semibold mb-2">Owner Information</h3>
          <div className="grid grid-cols-2 gap-2 text-sm">
            <span className="text-gray-500">Owner Name:</span>
            <span className="text-white">{file.ownerUserName || 'N/A'}</span>
            <span className="text-gray-500">Owner Email:</span>
            <span className="text-white">{file.ownerUserEmail || 'N/A'}</span>
            {file.expirationDate && (
              <>
                <span className="text-gray-500">Expiration Date:</span>
                <span className="text-white">{new Date(file.expirationDate).toLocaleString()}</span>
              </>
            )}
          </div>
        </div>
      )}

      <div>
        <h3 className="font-semibold mb-2">Users with access</h3>
        <div className="space-y-2">
          {sharedUsers.map((user) => {
            const ownerId = fileDetails?.ownerId ?? file.ownerId;
            const isOwner = ownerId === user.username;
            const isCurrentUser = currentUser?.username === user.username;
            const isDisabled = isOwner || isCurrentUser || isInTrash;
            let access;
            if(user.accessType === 'ADMIN') {
              access = 'ADMIN';
            } else if(user.accessType === 'WRITE') {
              access = 'WRITE';
            } else {
              access = 'read';
            }
            if(access === 'ADMIN') {
              return (
              <div key={user.username} className="flex items-center justify-between bg-gray-50 p-2 rounded">
                <div className="flex items-center gap-2">
                  <RiUserLine className="w-4 h-4" />
                  <span>{user.username}</span>
                  <span>{user.email}</span>
                </div>
                <div className="flex items-center gap-2">
                  <select
                    value={access}
                    className="text-sm border rounded px-2 py-1"
                    disabled={true}
                    style={{
                      backgroundImage: "none"
                    }}
                  >
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>
              </div>
            );
            }
            return (
              <div key={user.username} className="flex items-center justify-between bg-gray-50 p-2 rounded">
                <div className="flex items-center gap-2">
                  <RiUserLine className="w-4 h-4" />
                  <span>{user.username}</span>
                  <span>{user.email}</span>
                </div>
                <div className="flex items-center gap-2">
                  <select
                    value={access}
                    onChange={(e) => handleUpdateAccess(user.username, e.target.value as 'READ' | 'WRITE' | 'ADMIN')}
                    className="text-sm border rounded px-2 py-1"
                    disabled={isDisabled}
                    style={{
                      backgroundImage: "none"
                    }}
                  >
                    <option value="READ">Read</option>
                    <option value="WRITE">Write</option>
                  </select>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => handleRemoveUser(user.username)}
                    disabled={isOwner || isInTrash}
                  >
                    <RiUserUnfollowLine className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
          </div>
        </DrawerBody>
        <DrawerFooter>
          <DrawerClose asChild>
            <Button variant="secondary">Close</Button>
          </DrawerClose>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}; 