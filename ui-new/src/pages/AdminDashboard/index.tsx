import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/userAuth';
import { useNotificationService } from '../../services/notificationService';
import { adminService } from '../../lib/services';
import { AdminUser } from '../../types';
import { EditUserDialog } from './EditUserDialog';
import { DeleteUserDialog } from './DeleteUserDialog';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { RiGroupLine } from "@remixicon/react";

export const AdminDashboard = () => {
    const navigate = useNavigate();
    const { logoutEndpoint, user } = useAuth();
    const notifications = useNotificationService();
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [loaded, setLoaded] = useState(false);
    const [activeTab, setActiveTab] = useState('users');

    const loadUsers = useCallback(async () => {
        if(loaded) return;
        try {
            setIsLoading(true);
            const data = await adminService.getAllUsers();
            setUsers(data);
            setLoaded(true);
        } catch {
            notifications.error("Load failed");
        } finally {
            setIsLoading(false);
        }
    }, [notifications, loaded]);

    useEffect(() => {
        const fetchAll = async () => {
            await loadUsers();
        };
        if(!loaded) {
            fetchAll();
        }
    }, [loaded]);


    const handleEditUser = (user: AdminUser) => {
        setSelectedUser(user);
        setIsEditDialogOpen(true);
    };

    const handleDeleteUser = (user: AdminUser) => {
        setSelectedUser(user);
        setIsDeleteDialogOpen(true);
    };

    const handleSaveEdit = async (updatedUser: Partial<AdminUser>) => {
        if (!selectedUser) return;
        try {
            await adminService.updateUser(selectedUser.username, updatedUser);
            
            notifications.success("User updated");
            setLoaded(false);
            await loadUsers();
            setIsEditDialogOpen(false);
        } catch {
            notifications.error("Update failed");
        }
    };

    const handleConfirmDelete = async () => {
        if (!selectedUser) return;
        try {
            await adminService.deleteUser(selectedUser.username);
            if(selectedUser.username === user?.username) {
                logoutEndpoint();
            }
            
            notifications.success("User deleted");
            setLoaded(false);
            await loadUsers();
            setIsDeleteDialogOpen(false);
        } catch {
            notifications.error("Delete failed");
        }
    };

    if (!user || user.role !== 'ADMIN' && user.role !== 'SUPER_ADMIN') {
        return (
            <div id="45" className="container mx-auto py-6">
                <Card className="p-6 text-center">
                    <h2 className="text-2xl font-bold mb-4">Access Denied</h2>
                    <p className="text-gray-600 mb-4">You don't have permissions to access the admin panel.</p>
                    <Button onClick={() => navigate('/')}>
                        Go Back
                    </Button>
                </Card>
            </div>
        );
    }

    const tabsData = [
        {
            label: "Users",
            value: "users",
            icon: RiGroupLine,
            content: (
                <div className="bg-white shadow-md rounded-lg overflow-hidden">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Username
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Email
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Role
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    Actions
                                </th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {users.map((u) => (
                                <tr key={u.username}>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm font-medium text-gray-900">{u.username}</div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm text-gray-500">{u.email}</div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm text-gray-500">{u.role}</div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                        <button
                                            onClick={() => handleEditUser(u)}
                                            className="text-indigo-600 hover:text-indigo-900 mr-4"
                                            disabled={u.role === 'SUPER_ADMIN' && user.role !== 'SUPER_ADMIN' || (user.role === 'ADMIN' && u.role === 'ADMIN')}
                                        >
                                            Edit
                                        </button>
                                        <button
                                            onClick={() => handleDeleteUser(u)}
                                            className="text-red-600 hover:text-red-900"
                                            disabled={u.role === 'SUPER_ADMIN' || (user.role === 'ADMIN' && u.role === 'ADMIN')}
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )
        },
    ];

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-2xl font-bold mb-6">Admin Panel</h1>
            {isLoading ? (
                <div className="flex justify-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
                </div>
            ) : (
                <div className="flex">
                    <div className="w-64 mr-8">
                        <nav className="flex flex-col gap-2 bg-gray-100 p-4 rounded-lg shadow">
                            {tabsData.map(({ label, value, icon }) => (
                                <button
                                    key={value}
                                    onClick={() => setActiveTab(value)}
                                    className={`flex items-center gap-2 px-4 py-2 rounded transition-colors font-medium text-base
                                        ${activeTab === value
                                            ? 'bg-white text-indigo-600 border-l-4 border-indigo-600 shadow'
                                            : 'text-gray-800 hover:bg-white hover:text-indigo-600'}
                                    `}
                                >
                                    {React.createElement(icon, { className: "w-5 h-5" })}
                                    {label}
                                </button>
                            ))}
                        </nav>
                    </div>
                    <div className="flex-1">
                        {tabsData.map(({ value, content }) => (
                            value === activeTab && (
                                <div key={value} className="py-0">{content}</div>
                            )
                        ))}
                    </div>
                </div>
            )}
            {selectedUser && (
                <>
                    <EditUserDialog
                        user={selectedUser}
                        isOpen={isEditDialogOpen}
                        onClose={() => setIsEditDialogOpen(false)}
                        onSave={handleSaveEdit}
                    />
                    <DeleteUserDialog
                        user={selectedUser}
                        isOpen={isDeleteDialogOpen}
                        onClose={() => setIsDeleteDialogOpen(false)}
                        onConfirm={handleConfirmDelete}
                    />
                </>
            )}
        </div>
    );
};


export default AdminDashboard; 