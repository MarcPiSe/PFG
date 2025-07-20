import { useCallback } from 'react';
import { FileItem, SharedUser } from '../types';
import { sharingServiceInstance as sharingService } from '../lib/services';
import { useNotificationService } from '../services/notificationService';

export const useShareManager = () => {

  const notifications = useNotificationService();

  const getSharedUsers = useCallback(async (fileId: string) => {
    try {
      const users = await sharingService.getSharedUsers(fileId);
      return users;
    } catch {
      notifications.error('Could not load shared users');
      return [];
    }
  }, [notifications]);

  const share = useCallback(async (files: FileItem[], user: SharedUser) => {
    try {
      await Promise.all(
        files.map(async (f) => {
          await sharingService.shareFile(f.id!.toString(), user.username, user.accessType);
          f.shared = true;
        })
      );

      return {
        success: true,
        message: `${files.length} elements shared successfully`
      };
    } catch (error) {
      console.error('Error sharing files:', error);
      return { success: false };
    }
  }, []);

  const shareWithUser = useCallback(async (fileId: string, username: string, accessType: 'READ' | 'WRITE' = 'READ') => {
    if (!username) {
      console.error("Please enter a username");
      return false;
    }
    try {
      await sharingService.shareFile(fileId, username, accessType);   
      notifications.success('Item shared successfully');
      console.log(`File ${fileId} shared with ${username} with access type ${accessType}`);
      return true;
    } catch (error) {
      console.error('Error sharing file:', error);
      notifications.error('Could not share item');
      return false;
    }
  }, []);

  const handleUnshare = useCallback(async (files: FileItem[]) => {
    try {
      await Promise.all(files.map(async (f) => {
        const users = await getSharedUsers(f.id!.toString());
        await Promise.all(users.map((user: { username: string }) => 
          sharingService.revokeAccess(f.id!.toString(), user.username)
        ));
      }));
      
      files.forEach(f => { f.shared = false; });

      return true;
    } catch {
      return false;
    }
  }, [getSharedUsers]);

  const handleUnshareToUser = useCallback(async (itemId: string, username: string) => {
    try {
      await sharingService.revokeAccess(itemId, username)
      notifications.success('Permission removed successfully');
      
      try {
        const updatedRules = await sharingService.getSharedUsers(itemId);
        return updatedRules;
      } catch (error) {
        notifications.error('Permission was removed but data could not be updated');
        throw error;
      }
    } catch (error) {
      console.error('Error unsharing:', error);
      notifications.error('Could not remove permission');
      throw error;
    } 
  }, [notifications]);

    const handleUpdateAccess = useCallback(async (fileId: string, username: string, accessType: 'READ' | 'WRITE' | 'ADMIN') => {
      const success = await sharingService.updateAccess(fileId, username, accessType);
      
      return success;
    }, []);

  const fetchAccessRules = useCallback(async (elementId: string) => {
    try {
      const data = await sharingService.getSharedUsers(elementId);
      return data;
    } catch (error) {
      console.error('Error getting permissions:', error);
      notifications.error('Could not load permissions');
    }
    return [];
  }, [notifications]);

  const handleLoadSharedUsers = useCallback(async (fileId: string) => {
    return await getSharedUsers(fileId);
  }, [getSharedUsers]);

  return {    
    share,
    shareWithUser,
    handleUnshare,
    handleUpdateAccess,
    handleLoadSharedUsers,
    handleUnshareToUser,
    fetchAccessRules,
    getSharedUsers
  };
}; 