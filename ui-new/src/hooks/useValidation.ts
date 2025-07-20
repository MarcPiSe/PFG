import { useState, useCallback } from 'react';
import { userServiceInstance as userService } from '../lib/services';

/* eslint-disable */

interface ValidationErrors {
    username?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    oldPassword?: string;
    newPassword?: string;
    password?: string;
    confirmPassword?: string;
}

export const useValidation = () => {
    const [errors, setErrors] = useState<ValidationErrors>({});

    
    const validateEmail = useCallback((email: string): string => {
        if (!email || email.trim().length === 0) return 'Email required';
        
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            return 'Invalid email format';
        }
        return '';
    }, []);

    const validateUsername = useCallback((username: string): string => {
        if (!username || username.trim().length === 0) return 'Username required';
        if (username.length < 3 || username.length > 50) {
            return 'Username must be 3-50 characters';
        }
        
        if (!/^[a-zA-Z0-9_]+$/.test(username)) {
            return 'Username can only contain letters, numbers and underscores';
        }
        return '';
    }, []);

    const validateName = useCallback((name: string, fieldName: string = 'name'): string => {
        if (!name || name.trim().length === 0) return `${fieldName} required`;
        if (name.length < 2 || name.length > 100) {
            return `${fieldName} must be 2-100 characters`;
        }
        return '';
    }, []);

    const validatePassword = useCallback((password: string, isRequired: boolean = true): string => {
        if (!password || password.trim().length === 0) {
            return isRequired ? 'Password required' : '';
        }
        if (password.length < 8) {
            return 'Password must be at least 8 characters';
        }
        
        if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/.test(password)) {
            return 'Password must contain uppercase, lowercase and number';
        }
        return '';
    }, []);

    const validateConfirmPassword = useCallback((password: string, confirmPassword: string): string => {
        if (!confirmPassword || confirmPassword.trim().length === 0) {
            return 'Password confirmation required';
        }
        if (password !== confirmPassword) {
            return 'Passwords do not match';
        }
        return '';
    }, []);

    const validateUsernameWithAvailability = useCallback(async (username: string, originalUsername?: string): Promise<string> => {
        const validationError = validateUsername(username);
        if (validationError) {
            return validationError;
        }

        if (originalUsername === undefined || username !== originalUsername) {
            try {
                const isTaken = await userService.checkUsernameExists(username);
                if (isTaken) {
                    return 'Username already taken';
                }
            } catch (error) {
                return 'Could not verify username';
            }
        }
        return '';
    }, [validateUsername]);

    const validateEmailWithAvailability = useCallback(async (email: string, originalEmail?: string): Promise<string> => {
        const validationError = validateEmail(email);
        if (validationError) {
            return validationError;
        }

        if (originalEmail === undefined || email !== originalEmail) {
            try {
                const isTaken = await userService.checkEmailExists(email);
                if (isTaken) {
                    return 'Email already taken';
                }
            } catch (error) {
                return 'Could not verify email';
            }
        }
        return '';
    }, [validateEmail]);

    
    const validateRegistrationForm = useCallback(async (data: {
        username: string;
        email: string;
        password: string;
        firstName: string;
        lastName: string;
        confirmPassword: string;
    }): Promise<boolean> => {
        const newErrors: ValidationErrors = {};

        const usernameError = await validateUsername(data.username);
        if (usernameError) newErrors.username = usernameError;

        const emailError = await validateEmail(data.email);
        if (emailError) newErrors.email = emailError;

        newErrors.password = validatePassword(data.password, true);
        newErrors.firstName = validateName(data.firstName, 'First name');
        newErrors.lastName = validateName(data.lastName, 'Last name');
        newErrors.confirmPassword = validateConfirmPassword(data.password, data.confirmPassword);

        setErrors(newErrors);
        return !Object.values(newErrors).some(error => error && error.length > 0);
    }, [validateUsernameWithAvailability, validateEmailWithAvailability, validatePassword, validateName, validateConfirmPassword]);

    
    const validateProfileForm = useCallback(async (data: {
        username: string;
        originalUsername?: string;
        email?: string;
        originalEmail?: string;
        firstName?: string;
        lastName?: string;
        oldPassword?: string;
        newPassword?: string;
    }): Promise<boolean> => {
        const newErrors: ValidationErrors = {};

        newErrors.username = await validateUsernameWithAvailability(data.username, data.originalUsername);
        
        if (data.firstName) newErrors.firstName = validateName(data.firstName, 'First name');
        if (data.lastName) newErrors.lastName = validateName(data.lastName, 'Last name');
        if (data.email) {
            newErrors.email = await validateEmailWithAvailability(data.email, data.originalEmail);
        }

        
        if (data.newPassword || data.oldPassword) {
            if (!data.oldPassword) {
                newErrors.oldPassword = 'Current password required';
            }
            if (!data.newPassword) {
                newErrors.newPassword = 'New password required';
            } else {
                newErrors.newPassword = validatePassword(data.newPassword, true);
            }
        }

        setErrors(newErrors);
        return !Object.values(newErrors).some(error => error && error.length > 0);
    }, [validateUsernameWithAvailability, validateEmailWithAvailability, validateName, validatePassword]);

    
    const validateLoginForm = useCallback((data: {
        username: string;
        password: string;
    }): boolean => {
        const newErrors: ValidationErrors = {};

        
        const usernameError = validateUsername(data.username);
        if (usernameError) {
            
            const emailError = validateEmail(data.username);
            if (emailError) {
                newErrors.username = 'Enter valid username or email';
            }
        }

        if (!data.password || data.password.trim().length === 0) {
            newErrors.password = 'Password required';
        }

        setErrors(newErrors);
        return !Object.values(newErrors).some(error => error !== '');
    }, [validateEmail, validateUsername]);

    
    const validateField = useCallback((fieldName: string, value: string, additionalData?: any): string => {
        switch (fieldName) {
            case 'username':
                return validateUsername(value);
            case 'email':
                return validateEmail(value);
            case 'password':
                return validatePassword(value, true);
            case 'firstName':
                return validateName(value, 'First name');
            case 'lastName':
                return validateName(value, 'Last name');
            case 'confirmPassword':
                return validateConfirmPassword(additionalData?.password || '', value);
            case 'newPassword':
                return validatePassword(value, false);
            default:
                return '';
        }
    }, [validateUsername, validateEmail, validatePassword, validateName, validateConfirmPassword]);

    
    const validateForm = useCallback(async (data: any): Promise<boolean> => {
        
        if (data.confirmPassword !== undefined) {
            return validateRegistrationForm(data);
        } else if (data.oldPassword !== undefined || data.newPassword !== undefined) {
            return await validateProfileForm(data);
        } else if (data.password !== undefined && Object.keys(data).length === 2) {
            return validateLoginForm(data);
        } else {
            return await validateProfileForm(data);
        }
    }, [validateRegistrationForm, validateProfileForm, validateLoginForm]);

    const clearErrors = useCallback(() => setErrors({}), []);

    const setFieldError = useCallback((field: string, error: string) => {
        setErrors(prev => ({ ...prev, [field]: error }));
    }, []);

    const clearFieldError = useCallback((field: string) => {
        setErrors(prev => ({ ...prev, [field]: '' }));
    }, []);

    return {
        errors,
        validateForm,
        validateRegistrationForm,
        validateProfileForm,
        validateLoginForm,
        validateField,
        validateUsernameWithAvailability,
        validateEmailWithAvailability,
        clearErrors,
        setFieldError,
        clearFieldError
    };
}; 