import { Button } from "../Button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../Dialog";
import { Input } from "../Input";
import { FileItem } from "../../types";
import { RiUserUnfollowLine } from "react-icons/ri";
import { useState, useEffect, ChangeEvent } from "react";
import { authServiceInstance as authService } from "../../lib/services";
import { useDebounce } from "../../hooks/useDebounce";
import { useShareManager } from '../../hooks/useShareManager';
import { useFileStore } from "../../store/fileStore";
import { SharedUser } from "../../types";
import { useNotificationService } from "../../services/notificationService";

interface User {
  username: string;
  email: string;
}

interface FileShare {
  fileId: string;
  fileName: string;
  accessType: 'READ' | 'WRITE';
}

interface UserShare {
  user: User;
  fileShares: FileShare[];
}

interface ShareDialogProps {
  open: boolean;
  onClose: (open: boolean) => void;
  files: FileItem[];
  onShare: (userAccess: { username: string; fileId: string; accessType: 'READ' | 'WRITE' }[], originalSharedAccess: Map<string, {username: string, fileId: string, accessType: 'READ' | 'WRITE' }[]>) => void;
}

export const ShareDialog = ({
  open,
  onClose,
  files,
  onShare
}: ShareDialogProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<UserShare[]>([]);
  const [originalSharedAccess, ] = useState<Map<string, {username: string, fileId: string, accessType: 'READ' | 'WRITE'}[]>>(new Map());
  const [isSearching, setIsSearching] = useState(false);
  const debouncedSearchQuery = useDebounce(searchQuery, 300);
  const [overOption, setOverOption] = useState(false);
  const [username, ] = useState(localStorage.getItem('username') || '');
  const fileStore = useFileStore();
	const shareManager = useShareManager();
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);
  const notifications = useNotificationService();

  const convert = (sharedUsersFinal: {fileId: string, sharedUsers: SharedUser[]}[]) => {
    const map = new Map<string, UserShare>();
    for(const item of sharedUsersFinal) {
      item.sharedUsers.forEach(user => {
        if(!map.has(user.username)) {
          map.set(user.username, {user: {username: user.username, email: user.email}, fileShares: []});
        }
        map.get(user.username)!.fileShares.push({fileId: item.fileId, fileName: user.username, accessType: user.accessType});
      });
    }
    return Array.from(map.values());
  };

  useEffect(() =>  {
    if (!open) {
      setSearchQuery('');
      setInputValue('');
      setSearchResults([]);
      setSelectedUsers([]);
      setIsSearching(false);
    } else {
      const loadSharedUsers = async () => {
        setIsLoadingUsers(true);
        const sharedUsersFinal: {fileId: string, sharedUsers: SharedUser[]}[] = []; 
        for (const file of files) {
          const sharedUsers = await shareManager.getSharedUsers(file.id!.toString());
          sharedUsersFinal.push({fileId: file.id!.toString(), sharedUsers});
        }
        sharedUsersFinal.forEach(item => {
          item.sharedUsers.forEach(user => {
            if(!originalSharedAccess.has(user.username)) {
              originalSharedAccess.set(user.username, [{username: user.username, fileId: item.fileId, accessType: user.accessType}]);
            } else {
              originalSharedAccess.get(user.username)!.push({username: user.username, fileId: item.fileId, accessType: user.accessType});
            }
          });
        });
        setSelectedUsers(convert(sharedUsersFinal));
        setIsLoadingUsers(false);
      };
      loadSharedUsers();
    }
  }, [open, files]);

  useEffect(() => {
    const searchUsers = async () => {
      if (!open || !debouncedSearchQuery) {
        setSearchResults([]);
        return;
      }

      setIsSearching(true);
      try {
        let results = await authService.searchUsers(debouncedSearchQuery);
        results = results.filter((user: User) => user.username !== username && !selectedUsers.some(u => u.user.username === user.username));
        setSearchResults(results);
      } catch {
        notifications.error("Failed to search users");
      } finally {
        setIsSearching(false);
      }
    };

    searchUsers();
  }, [debouncedSearchQuery, open, selectedUsers, username]);

  const handleUserSelect = (user: User) => {
    if (!selectedUsers.some(u => u.user.username === user.username)) {
      const initialFileShares = files.map(file => ({
        fileId: file.id,
        fileName: file.name,
        accessType: 'READ' as const
      }));
      
      setSelectedUsers([...selectedUsers, { user, fileShares: initialFileShares }]);
    }
    setSearchQuery('');
    setSearchResults([]);
  };

  const handleAccessTypeChange = (username: string, fileId: string, accessType: 'READ' | 'WRITE') => {
    setSelectedUsers(selectedUsers.map(userShare => {
      if (userShare.user.username === username) {
        return {
          ...userShare,
          fileShares: userShare.fileShares.map(fileShare => 
            fileShare.fileId === fileId ? { ...fileShare, accessType } : fileShare
          )
        };
      }
      return userShare;
    }));
  };

  const handleFileSelection = (username: string, fileId: string, selected: boolean) => {
    setSelectedUsers(selectedUsers.map(userShare => {
      if (userShare.user.username === username) {
        if (selected) {
          const file = files.find(f => f.id === fileId);
          if (file && !userShare.fileShares.some(fs => fs.fileId === fileId)) {
            return {
              ...userShare,
              fileShares: [...userShare.fileShares, {
                fileId,
                fileName: file.name,
                accessType: 'READ'
              }]
            };
          }
        } else {
          return {
            ...userShare,
            fileShares: userShare.fileShares.filter(fs => fs.fileId !== fileId)
          };
        }
      }
      return userShare;
    }));
  };

  const handleRemoveUser = (username: string) => {
    setSelectedUsers(selectedUsers.filter(userShare => userShare.user.username !== username));
  };

  const share = () => {
    const userAccess = selectedUsers.flatMap(userShare => 
      userShare.fileShares.map(fileShare => ({
        username: userShare.user.username,
        fileId: fileShare.fileId,
        accessType: fileShare.accessType
      }))
    );
    onShare(userAccess, originalSharedAccess);
    onClose(false);
  };

  const getName = (searchFile: FileItem) => {
    const folder = fileStore.currentDirectory.subfolders?.find(f => f.id === searchFile.id);
    if(folder) {
      return folder.name;
    }
    const file = fileStore.currentDirectory.files?.find(f => f.id === searchFile.id);
    if(file) {
      return file.name;
    }
    return "";
  };

  if (!open) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[600px] no-clear-select overflow-visible">
        <DialogHeader>
          <DialogTitle><span className="text-indigo-200">Share {files.length > 1 ? 'files' : 'file'}</span></DialogTitle>
          <DialogDescription>
            Share {files.length > 1 ? 'selected files' : files[0].name} with other users
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <label className="text-sm font-medium text-indigo-400/75">
              Search user
            </label>
            <div className="relative">
              <Input
                value={inputValue}
                onChange={(e: ChangeEvent<HTMLInputElement>) => {
                  setSearchQuery(e.target.value);
                  setInputValue(e.target.value);
                }}
                placeholder="Search by name or email"
                className="w-full"
                onFocus={() => {
                  setSearchQuery(inputValue);
                }}
                onBlur={() => {
                  if (!overOption) {
                    setSearchQuery('');
                  }
                }}
                disabled={isLoadingUsers}
              />
              {isSearching && (
                <div className="absolute right-2 top-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
                </div>
              )}
              {searchResults.length > 0 && (
                <div className="absolute z-50 w-full mt-1 bg-white border rounded-md shadow-lg"
                  style={{
                    maxHeight: '7.5rem',
                    overflowY: 'auto'
                  }}
                  onMouseEnter={() => setOverOption(true)}
                  onMouseLeave={() => setOverOption(false)}
                >
                  {searchResults.map(user => (
                    <div
                      key={user.username}
                      className="px-4 py-2 hover:bg-gray-100 cursor-pointer"
                      onClick={() => handleUserSelect(user)}
                    >
                      {user.username} ({user.email})
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {isLoadingUsers ? (
            <div className="flex justify-center items-center h-32">
              <span className="text-indigo-500 font-semibold">Loading users...</span>
            </div>
          ) : selectedUsers.length > 0 && (
            <div className="mt-4 opacity-100">
              <label className="text-sm font-medium text-indigo-400/75">
                Selected users
              </label>
              <div className="space-y-4 mt-2 border rounded-lg shadow-inner p-2 bg-gray-50 pr-2 overflow-auto max-h-[50vh]">
                {selectedUsers.map(userShare => (
                  <div key={userShare.user.username} className="bg-white p-4 rounded shadow-sm">
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleRemoveUser(userShare.user.username)}
                          className="text-red-500 hover:text-red-600 hover:bg-red-50"
                          disabled={isLoadingUsers}
                        >
                          <RiUserUnfollowLine className="w-4 h-4" />
                        </Button>
                        <span className="font-medium">{userShare.user.username}</span>
                        <span className="text-gray-500 text-sm">({userShare.user.email})</span>
                      </div>
                    </div>
                    <div className="space-y-2 overflow-auto max-h-[10vh]">
                      {files.map(file => (
                        <div 
                          key={file.id}
                          className="flex items-center justify-between p-2 bg-gray-50 rounded"
                          style={{ width: "calc(100% - 10px)" }}>
                          <div className="flex items-center gap-2">
                            <input
                              type="checkbox"
                              checked={userShare.fileShares.some(fs => fs.fileId === file.id)}
                              onChange={(e) => handleFileSelection(userShare.user.username, file.id, e.target.checked)}
                              className="rounded border-gray-300"
                              disabled={isLoadingUsers}
                            />
                            <span className="text-sm">{getName(file)}</span>
                          </div>
                          {userShare.fileShares.some(fs => fs.fileId === file.id) && (
                            <select
                              value={userShare.fileShares.find(fs => fs.fileId === file.id)?.accessType || 'READ'}
                              onChange={(e) => handleAccessTypeChange(userShare.user.username, file.id, e.target.value as 'READ' | 'WRITE' )}
                              className="appearance-none block py-1 px-2 text-sm text-gray-500 bg-transparent border border-gray-200 rounded focus:outline-none focus:ring-1 focus:ring-indigo-500"
                              style={{
                                backgroundImage: "none"
                              }}
                              disabled={isLoadingUsers}
                            >
                              <option value="READ">Read</option>
                              <option value="WRITE">Write</option>
                            </select>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        <DialogFooter className="mt-6">
          <Button variant="secondary" onClick={() => onClose(false)}>
            Cancel
          </Button>
          <Button 
            onClick={share}
            disabled={selectedUsers.length === 0}
          >
            Share
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}; 