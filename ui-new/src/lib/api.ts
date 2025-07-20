import axios from 'axios';
import { CreateFileRequest, FileItem, FolderStructure, User } from '../types';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8762',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    config.headers['X-Client-Type'] = 'web';

    return config;
});

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        const authRoutes = ['/users/auth/login', '/users/auth/keep-alive', '/users/auth/register'];
        const refreshToken = localStorage.getItem('refreshToken');

        if (error.response?.status === 401 && !originalRequest._retry && !authRoutes.includes(originalRequest.url) && refreshToken) {
            originalRequest._retry = true;

            try {
                const response = await api.post('/users/auth/keep-alive', null, {
                    headers: {
                        'X-Refresh-Token': refreshToken
                    }
                });

                const newAccessToken = response.headers['authorization']?.split(' ')[1];
                const newRefreshToken = response.headers['x-refresh-token'];

                if (newAccessToken && newRefreshToken) {
                    localStorage.setItem('accessToken', newAccessToken);
                    localStorage.setItem('refreshToken', newRefreshToken);

                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                    return api(originalRequest);
                }
            } catch (refreshError) {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('username');
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export const getCurrentUsername = (): string => {
    const username = localStorage.getItem('username');
    if (!username) {
        authService.logout();
        throw new Error('Username not found. Please log in again.');
    }
    return username;
};

export const authService = {
    login: async (username: string, password: string) => {
        const response = await api.post('/users/auth/login', { username, password });

        const accessToken = response.headers['authorization']?.split(' ')[1];
        const refreshToken = response.headers['x-refresh-token'];

        if (!accessToken || !refreshToken) {
            throw new Error('Could not obtain authentication tokens');
        }

        return {
            accessToken,
            refreshToken
        };
    },

    register: async (userData: {
        username: string;
        password: string;
        email: string;
        firstName: string;
        lastName: string;
    }) => {
        const response = await api.post('/users/auth/register', userData);

        const accessToken = response.headers['authorization']?.split(' ')[1];
        const refreshToken = response.headers['x-refresh-token'];

        if (!accessToken || !refreshToken) {
            throw new Error('Could not obtain authentication tokens');
        }

        localStorage.setItem('username', userData.username);

        return {
            accessToken,
            refreshToken
        };
    },

    logout: () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('username');
    },

    searchUsers: async (query: string) => {
        const response = await api.get(`/users/search?q=${encodeURIComponent(query)}`);
        return response.data.map((user: User) => ({
            username: user.username,
            email: user.email
        }));
    },
};

export const fileService = {
    getRootFolder: async () => {
        const response = await api.get('/files/root');
        return response.data;
    },

    getFolderStructure: async () => {
        const root = await fileService.getRootFolder();
        const shared = await sharingService.getSharedRootFolder();
        shared.id = "shared";
        const trash = await trashService.getTrashRootFolder();
        trash.id = "trash";
        const structure: FolderStructure = {
            root,
            shared,
            trash,
        };
        return structure;
    },

    getElement: async (elementId: string) => {
        const response = await api.get(`/files/${elementId}`);
        return response.data;
    },

    downloadFile: async (elementId: string) => {
        const response = await api.get(`/files/${elementId}/download`, {
            responseType: 'blob'
        });

        const contentDisposition = response.headers['content-disposition'];
        let filename = 'download'; 
        if (contentDisposition) {
            const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
            if (filenameMatch && filenameMatch.length > 1) {
                filename = filenameMatch[1];
            }
        }

        return { blob: response.data, filename };
    },

    getFolderById: async (folderId: string, deleted?: boolean) => {
        if(folderId === "shared") {
            const shared = await sharingService.getSharedRootFolder();
            shared.id = "shared";
            return shared;
        } else if(folderId === "trash") {
            const trash = await trashService.getTrashRootFolder();
            trash.id = "trash";
            return trash;
        }
        let url = `/files/${folderId}/full`;
        if (deleted !== undefined) {
            url += `?deleted=${deleted}`;
        }
        const response = await api.get(url);
        return response.data;
    },

    uploadFile: async (request: CreateFileRequest, file: File): Promise<FileItem> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('request', new Blob([JSON.stringify(request)], {
            type: 'application/json'
        }));

        const response = await api.post('/files', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response.data;
    },

    createFolder: async (name: string, parentId: string) => {
        try {
            const formData = new FormData();
            formData.append("request", new Blob(
                [
                    JSON.stringify(
                        {
                            "name": name,
                            "contentType": "folder",
                            "size": 0,
                            "parentFolderId": parentId,
                            "isFolder": true
                        }
                    )],
                { type: "application/json" }
            ));

            const response = await api.post('/files', formData, {
                headers: {
                    "Content-Type": "multipart/form-data"
                }
            });
            console.log("Folder created successfully with id:", response.data.id);
            return response.data;
        } catch (error) {
            console.error("Error creating folder:", error);
            throw error;
        }
    },

    updateElement: async (elementId: string, name: string) => {
        const response = await api.put(`/files/${elementId}`, { name });
        return response.data;
    },

    copyElement: async (elementId: string, newParentId: string) => {
        const response = await api.post(`/files/${elementId}/copy/${newParentId}`);
        return response.data;
    },

    moveElement: async (elementId: string, newParentId: string) => {
        const response = await api.put(`/files/${elementId}/move/${newParentId}`);
        return response.data;
    },

    deleteElement: async (elementId: string) => {
        await api.delete(`/files/${elementId}`);
    }
};

export const sharingService = {
    getSharedRootFolder: async () => {
        const response = await api.get('/share/root');
        return response.data;
    },

    shareFile: async (fileId: string, username: string, accessType: 'READ' | 'WRITE') => {
        await api.post(`/share`, {
            elementId: fileId,
            user: username,
            accessType
        });
    },

    getSharedUsers: async (elementId: string) => {
        const response = await api.get(`/share/user/${elementId}`);
        return response.data;
    },

    revokeAccess: async (fileId: string, username?: string) => {
        if(username) {
            await api.delete(`/share/${fileId}/user/${username}`);
        } else {
            await api.delete(`/share/${fileId}/user/${getCurrentUsername()}`);
        }
    },

    generateShareUrl: async (fileId: string) => {
        const response = await api.post(`/share/url/${fileId}`);
        return response.data.shareUrl;
    },

    updateAccess: async (fileId: string, username: string, accessType: 'READ' | 'WRITE' | 'ADMIN') => {
        await api.put(`/share`, {
            elementId: fileId,
            usernameToUpdate: username,
            newAccessType: accessType
        });
    },
};

export const trashService = {
    getTrashRootFolder: async () => {
        const response = await api.get('/trash/root');
        return response.data;
    },

    restoreItem: async (elementId: string) => {
        await api.put(`/trash/${elementId}/restore`);
    },

    deleteItem: async (elementId: string) => {
        await api.delete(`/trash/${elementId}`);
    }
};

export const userService = {
    checkUsernameExists: async (username: string): Promise<boolean> => {
        const response = await api.get(`/users/check-username?username=${encodeURIComponent(username)}`);
        return !response.data;
    },

    checkEmailExists: async (email: string): Promise<boolean> => {
        const response = await api.get(`/users/check-email?email=${encodeURIComponent(email)}`);
        return !response.data;
    },

    getCurrentUser: async () => {
        const response = await api.get('/users');
        return response.data;
    },

    updateUserProfile: async (userData: {
        username?: string;
        firstName?: string;
        lastName?: string;
        email?: string;
    }) => {
        const response = await api.put('/users/profile', userData);
        return response.data;
    },

    changePassword: async (oldPassword: string, newPassword: string) => {
        await api.put('/users/password', {
            oldPassword,
            newPassword
        });
    },

    deleteAccount: async () => {
        await api.delete('/users');
    }
};

export const adminService = {
    getAllUsers: async () => {
        const response = await api.get('/admin/users');
        return response.data;
    },

    getUser: async (username: string) => {
        const response = await api.get(`/admin/users/${username}`);
        return response.data;
    },

    updateUser: async (username: string, userData: {
        username?: string;
        email?: string;
        firstName?: string;
        lastName?: string;
        role?: string;
        password?: string;
    }) => {
        const response = await api.put(`/admin/users/${username}`, userData);
        return response.data;
    },

    deleteUser: async (username: string) => {
        await api.delete(`/admin/users/${username}`);
    },
};

export default api; 