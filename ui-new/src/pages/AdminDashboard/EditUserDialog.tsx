import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../../components/Dialog';
import { Button } from '../../components/Button';
import { Input } from '../../components/Input';
import { AdminUser } from '../../types';
import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../../hooks/userAuth';
import { useDebounce } from '../../hooks/useDebounce';
import { useValidation } from '../../hooks/useValidation';
import { userService } from '../../lib/api';
import { RiCheckLine, RiCloseLine, RiEyeLine, RiEyeOffLine } from '@remixicon/react';

interface EditUserDialogProps {
  user: AdminUser;
  isOpen: boolean;
  onClose: () => void;
  onSave: (updatedUser: Partial<AdminUser>) => void;
}

interface ValidationState {
  isChecking: boolean;
  isAvailable: boolean;
  error?: string;
}

export const EditUserDialog = ({ user, isOpen, onClose, onSave }: EditUserDialogProps) => {
  const [editForm, setEditForm] = useState<{
    username: string;
    password: string;
    email: string;
    firstName: string;
    lastName: string;
    role: 'USER' | 'ADMIN' | 'SUPER_ADMIN';
  }>({
    username: user.username,
    password: '',
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    role: user.role as 'USER' | 'ADMIN' | 'SUPER_ADMIN'
  });

  const [showPassword, setShowPassword] = useState(false);
  const [usernameValidation, setUsernameValidation] = useState<ValidationState>({
    isChecking: false,
    isAvailable: true,
    error: undefined
  });
  const [emailValidation, setEmailValidation] = useState<ValidationState>({
    isChecking: false,
    isAvailable: true,
    error: undefined
  });

  const { validateField, errors, setFieldError, clearFieldError, clearErrors } = useValidation();
  const { user: currentUser } = useAuth();
  
  const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN';
  
  const debouncedUsername = useDebounce(editForm.username, 500);
  const debouncedEmail = useDebounce(editForm.email, 500);

  useEffect(() => {
    const emailFormatError = validateField('email', editForm.email);
    if (emailFormatError) {
      setFieldError('email', emailFormatError);
    } else {
      clearFieldError('email');
    }

    const firstNameError = validateField('firstName', editForm.firstName);
    if (firstNameError) {
      setFieldError('firstName', firstNameError);
    } else {
      clearFieldError('firstName');
    }

    const lastNameError = validateField('lastName', editForm.lastName);
    if (lastNameError) {
      setFieldError('lastName', lastNameError);
    } else {
      clearFieldError('lastName');
    }

    const usernameFormatError = validateField('username', editForm.username);
    if (usernameFormatError) {
      setFieldError('username', usernameFormatError);
    } else {
      clearFieldError('username');
    }

    if (isSuperAdmin && editForm.password.length > 0) {
      const passwordError = validateField('password', editForm.password);
      if (passwordError) {
        setFieldError('password', passwordError);
      } else {
        clearFieldError('password');
      }
    } else {
      clearFieldError('password');
    }
  }, [editForm, validateField, setFieldError, clearFieldError, isSuperAdmin]);

  useEffect(() => {
    const checkUsernameAvailability = async () => {
      if (debouncedUsername === user.username) {
        setUsernameValidation({ isChecking: false, isAvailable: true, error: undefined });
        return;
      }
      if (errors.username) {
        setUsernameValidation({ isChecking: false, isAvailable: false, error: undefined });
        return;
      }
      setUsernameValidation(prev => ({ ...prev, isChecking: true }));
      try {
        const isTaken = await userService.checkUsernameExists(debouncedUsername);
        setUsernameValidation({
          isChecking: false,
          isAvailable: !isTaken,
          error: isTaken ? 'Username already exists' : undefined
        });
      } catch {
        setUsernameValidation({ isChecking: false, isAvailable: false, error: 'Verification error' });
      }
    };
    checkUsernameAvailability();
  }, [debouncedUsername, user.username, errors.username]);

  useEffect(() => {
    const checkEmailAvailability = async () => {
      if (debouncedEmail === user.email) {
        setEmailValidation({ isChecking: false, isAvailable: true, error: undefined });
        return;
      }
      if (errors.email) {
        setEmailValidation({ isChecking: false, isAvailable: false, error: undefined });
        return;
      }
      setEmailValidation(prev => ({ ...prev, isChecking: true }));
      try {
        const isTaken = await userService.checkEmailExists(debouncedEmail);
        setEmailValidation({
          isChecking: false,
          isAvailable: !isTaken,
          error: isTaken ? 'Email already exists' : undefined
        });
      } catch {
        setEmailValidation({ isChecking: false, isAvailable: false, error: 'Verification error' });
      }
    };
    checkEmailAvailability();
  }, [debouncedEmail, user.email, errors.email]);

  const hasChanges = useMemo(() => {
    return (
      editForm.username !== user.username ||
      editForm.email !== user.email ||
      editForm.firstName !== user.firstName ||
      editForm.lastName !== user.lastName ||
      editForm.role !== user.role ||
      (isSuperAdmin && editForm.password.length > 0)
    );
  }, [editForm, user, isSuperAdmin]);

  const isFormValid = useMemo(() => {
    const hasValidationErrors = Object.values(errors).some(error => error && error !== '');
    const hasAvailabilityError = !usernameValidation.isAvailable || !emailValidation.isAvailable;
    const isChecking = usernameValidation.isChecking || emailValidation.isChecking;
    
    return !hasValidationErrors && !hasAvailabilityError && !isChecking;
  }, [errors, usernameValidation, emailValidation]);

  const isSaveDisabled = !hasChanges || !isFormValid;

  const renderValidationIcon = (field: 'username' | 'email' | 'password') => {
    if (field === 'username') {
      if (usernameValidation.isChecking) {
        return <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>;
      }
      if (editForm.username !== user.username && editForm.username.length > 0 && !errors.username) {
        return usernameValidation.isAvailable ? 
          <RiCheckLine className="h-4 w-4 text-green-600" /> : 
          <RiCloseLine className="h-4 w-4 text-red-600" />;
      }
    } else if (field === 'email') {
      if (emailValidation.isChecking) {
        return <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>;
      }
      if (editForm.email !== user.email && editForm.email.length > 0 && !errors.email) {
        return emailValidation.isAvailable ? 
          <RiCheckLine className="h-4 w-4 text-green-600" /> : 
          <RiCloseLine className="h-4 w-4 text-red-600" />;
      }
    } else if (field === 'password' && isSuperAdmin) {
      if (editForm.password.length > 0) {
        return !errors.password ? 
          <RiCheckLine className="h-4 w-4 text-green-600" /> : 
          <RiCloseLine className="h-4 w-4 text-red-600" />;
      }
    }
    return null;
  };

  const getUsernameError = () => {
    if (errors.username) return errors.username;
    if (usernameValidation.error) return usernameValidation.error;
    return undefined;
  };

  const getEmailError = () => {
    if (errors.email) return errors.email;
    if (emailValidation.error) return emailValidation.error;
    return undefined;
  };

  const handleSave = () => {
    const updatedUser: Partial<AdminUser> = {
      username: editForm.username,
      email: editForm.email,
      firstName: editForm.firstName,
      lastName: editForm.lastName,
      role: editForm.role
    };

    if (isSuperAdmin && editForm.password.length > 0) {
      updatedUser.password = editForm.password;
    }

    onSave(updatedUser);
  };

  useEffect(() => {
    if (isOpen) {
      setEditForm({
        username: user.username,
        password: '',
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        role: user.role as 'USER' | 'ADMIN' | 'SUPER_ADMIN'
      });

      setShowPassword(false);
      setUsernameValidation({ isChecking: false, isAvailable: true, error: undefined });
      setEmailValidation({ isChecking: false, isAvailable: true, error: undefined });
      clearErrors();
    }
  }, [user, isOpen, clearErrors]);

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit User</DialogTitle>
          <DialogDescription>
            Modify the selected user data.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">Username</label>
            <div className="relative">
              <Input
                value={editForm.username}
                onChange={e => setEditForm(prev => ({ ...prev, username: e.target.value }))}
                className={`pr-10 ${
                  getUsernameError() ? 'border-red-500' : 
                  (usernameValidation.isAvailable && editForm.username !== user.username) ? 'border-green-500' : ''
                }`}
              />
              <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                {renderValidationIcon('username')}
              </div>
            </div>
            {getUsernameError() && (
              <p className="text-sm text-red-600">{getUsernameError()}</p>
            )}
          </div>
          
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">Email</label>
            <div className="relative">
              <Input
                type="email"
                value={editForm.email}
                onChange={e => setEditForm(prev => ({ ...prev, email: e.target.value }))}
                className={`pr-10 ${
                  getEmailError() ? 'border-red-500' : 
                  (emailValidation.isAvailable && editForm.email !== user.email) ? 'border-green-500' : ''
                }`}
              />
              <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                {renderValidationIcon('email')}
              </div>
            </div>
            {getEmailError() && (
              <p className="text-sm text-red-600">{getEmailError()}</p>
            )}
          </div>

          {isSuperAdmin && (
            <div className="space-y-2">
              <label className="text-sm font-medium text-gray-300">New Password</label>
              <div className="relative">
                <Input
                  type={showPassword ? "text" : "password"}
                  value={editForm.password}
                  onChange={e => setEditForm(prev => ({ ...prev, password: e.target.value }))}
                  placeholder="Leave empty to keep current"
                  className={`pr-20 ${
                    errors.password ? 'border-red-500' : 
                    (!errors.password && editForm.password.length > 0) ? 'border-green-500' : ''
                  }`}
                />
                <div className="absolute right-3 top-1/2 transform -translate-y-1/2 flex items-center space-x-1">
                  {renderValidationIcon('password')}
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    {showPassword ? (
                      <RiEyeOffLine className="h-4 w-4" />
                    ) : (
                      <RiEyeLine className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>
              {errors.password && (
                <p className="text-sm text-red-600">{errors.password}</p>
              )}
              <p className="text-xs text-gray-500">
                Only super admins can change passwords. Leave empty to keep current password.
              </p>
            </div>
          )}
          
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">First Name</label>
            <Input
              value={editForm.firstName}
              onChange={e => setEditForm(prev => ({ ...prev, firstName: e.target.value }))}
              className={errors.firstName ? 'border-red-500' : ''}
            />
            {errors.firstName && (
              <p className="text-sm text-red-600">{errors.firstName}</p>
            )}
          </div>
          
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">Last Name</label>
            <Input
              value={editForm.lastName}
              onChange={e => setEditForm(prev => ({ ...prev, lastName: e.target.value }))}
              className={errors.lastName ? 'border-red-500' : ''}
            />
            {errors.lastName && (
              <p className="text-sm text-red-600">{errors.lastName}</p>
            )}
          </div>
          {isSuperAdmin && editForm.role !== 'SUPER_ADMIN' &&(
            <>
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">Role</label>
            <select
              className="w-full rounded-md border border-input bg-background px-3 py-2"
              value={editForm.role}
              onChange={e => setEditForm(prev => ({ ...prev, role: e.target.value as 'USER' | 'ADMIN' | 'SUPER_ADMIN' }))}
              disabled={!currentUser || currentUser && currentUser.role !== 'SUPER_ADMIN' || (currentUser.role === 'ADMIN' && user.role === 'ADMIN')}
            >
              <option value="USER">User</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </div>
            </>
          )}
        </div>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button 
            onClick={handleSave}
            disabled={isSaveDisabled}
            className={isSaveDisabled ? 'opacity-50 cursor-not-allowed' : ''}
          >
            Save Changes
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}; 