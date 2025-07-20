import { useState, useEffect, useMemo } from "react";
import { RiUserSettingsFill, RiDeleteBin2Line, RiCheckLine, RiCloseLine } from "@remixicon/react";
import { Button } from "../Button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../Dialog";
import { Input } from "../Input";
import { Tooltip } from "../Tooltip";
import { Toaster } from "../Toaster";
import { userServiceInstance as userService } from "../../lib/services";
import { useValidation } from "../../hooks/useValidation";
import { useAuth } from "../../hooks/userAuth";
import { useNotificationService } from '../../services/notificationService';
import { useDebounce } from "../../hooks/useDebounce";

interface UserData {
	username: string;
	firstName: string;
	lastName: string;
	email: string;
}

interface ValidationState {
	isChecking: boolean;
	isAvailable: boolean;
	error?: string;
}

export const UserSettings = () => {
	const { logoutEndpoint } = useAuth();
	const { errors, validateProfileForm, clearErrors, clearFieldError, setFieldError, validateField } = useValidation();
	const [isOpen, setIsOpen] = useState(false);
	const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
	const [deleteConfirmation, setDeleteConfirmation] = useState("");
	const [userData, setUserData] = useState<UserData>({
		username: "",
		firstName: "",
		lastName: "",
		email: ""
	});
	const [initialUserData, setInitialUserData] = useState<UserData | null>(null);
	const notifications = useNotificationService();
	const [newPassword, setNewPassword] = useState("");
	const [oldPassword, setOldPassword] = useState("");
	const [loading, setLoading] = useState(false);
	const { user: currentUser } = useAuth();
	
	const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN';

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

	const debouncedUsername = useDebounce(userData.username, 500);
	const debouncedEmail = useDebounce(userData.email, 500);

	useEffect(() => {
		const loadUserData = async () => {
			if (!isOpen) {
				return;
			}
			if(!initialUserData) {
				try {
					setLoading(true);
					const data = await userService.getCurrentUser();
					setUserData(data);
					setInitialUserData(data);
				} catch {
					notifications.error("Failed to load user data");
				} finally {
					setLoading(false);
				}
			}
		};

		loadUserData();
	}, [isOpen, notifications, initialUserData]);

	useEffect(() => {
		if (!isOpen) return;
		const fieldsToValidate: (keyof UserData)[] = ['firstName', 'lastName', 'username', 'email'];
		fieldsToValidate.forEach(field => {
			const error = validateField(field, userData[field as keyof UserData]);
			if (error) {
				setFieldError(field, error);
			} else {
				clearFieldError(field);
			}
		});

		if (newPassword.length > 0) {
			const passwordError = validateField('newPassword', newPassword);
			if (passwordError) {
				setFieldError('newPassword', passwordError);
			} else {
				clearFieldError('newPassword');
			}
		} else {
			clearFieldError('newPassword');
		}

	}, [userData, newPassword, validateField, setFieldError, clearFieldError, isOpen]);

	useEffect(() => {
		const checkUsernameAvailability = async () => {
			if (!initialUserData || debouncedUsername === initialUserData.username) {
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
					error: isTaken ? 'Username already taken' : undefined
				});
			} catch {
				setUsernameValidation({ isChecking: false, isAvailable: false, error: 'Error checking username' });
			}
		};
		if (debouncedUsername && isOpen) checkUsernameAvailability();
	}, [debouncedUsername, initialUserData, errors.username, isOpen]);

	useEffect(() => {
		const checkEmailAvailability = async () => {
			if (!initialUserData || debouncedEmail === initialUserData.email) {
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
					error: isTaken ? 'Email already in use' : undefined
				});
			} catch {
				setEmailValidation({ isChecking: false, isAvailable: false, error: 'Error checking email' });
			}
		};
		if (debouncedEmail && isOpen) checkEmailAvailability();
	}, [debouncedEmail, initialUserData, errors.email, isOpen]);

	const handleUpdateProfile = async () => {
		const formData = {
			...userData,
			originalUsername: initialUserData?.username,
			originalEmail: initialUserData?.email,
			oldPassword,
			newPassword
		};

		const isValid = await validateProfileForm(formData);

		if (!isValid) {
			notifications.error("Please correct form errors");
			return;
		}

		try {
			setLoading(true);
			
			await userService.updateUserProfile(userData);
			
			if (newPassword && oldPassword) {
				await userService.changePassword(oldPassword, newPassword);
			}

			notifications.success("Profile updated");

			setNewPassword("");
			setOldPassword("");
			
			setIsOpen(false);
			setUsernameValidation({ isChecking: false, isAvailable: true, error: undefined });
			setEmailValidation({ isChecking: false, isAvailable: true, error: undefined });
		} catch {
			notifications.error("Update failed");
		} finally {
			setLoading(false);
		}
	};

	const handleClose = () => {
		setIsOpen(false);
		clearErrors();
		setNewPassword("");
		setOldPassword("");
		setInitialUserData(null);
		setUserData({
			username: "",
			firstName: "",
			lastName: "",
			email: ""
		});
		setUsernameValidation({ isChecking: false, isAvailable: true, error: undefined });
		setEmailValidation({ isChecking: false, isAvailable: true, error: undefined });
	};

	const handleDeleteAccount = async () => {
		if (deleteConfirmation !== userData.username) {
			notifications.error("Username doesn't match");
			return;
		}

		try {
			setLoading(true);
			await userService.deleteAccount();
			
			notifications.success("Account deleted");
			
			logoutEndpoint();
		} catch {
			notifications.error("Delete failed");
		} finally {
			setLoading(false);
			setIsDeleteDialogOpen(false);
		}
	};

	const handleFieldChange = (field: keyof UserData, value: string) => {
		setUserData(prev => ({ ...prev, [field]: value }));
		clearFieldError(field);
	};

	const hasChanges = useMemo(() => {
		if (!initialUserData) return false;
		return (
			userData.username !== initialUserData.username ||
			userData.email !== initialUserData.email ||
			userData.firstName !== initialUserData.firstName ||
			userData.lastName !== initialUserData.lastName ||
			newPassword.length > 0
		);
	}, [userData, newPassword, initialUserData]);

	const isFormValid = useMemo(() => {
		const hasValidationErrors = Object.values(errors).some(error => error && error.length > 0);
		const hasAvailabilityError = !usernameValidation.isAvailable || !emailValidation.isAvailable;
		const isChecking = usernameValidation.isChecking || emailValidation.isChecking;
		
		return !hasValidationErrors && !hasAvailabilityError && !isChecking;
	}, [errors, usernameValidation, emailValidation]);

	const isSaveDisabled = !hasChanges || !isFormValid;

	const renderValidationIcon = (field: 'username' | 'email') => {
		if (field === 'username') {
			if (usernameValidation.isChecking) {
				return <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>;
			}
			if (userData.username !== initialUserData?.username && userData.username.length > 0 && !errors.username) {
				return usernameValidation.isAvailable ? 
					<RiCheckLine className="h-4 w-4 text-green-600" /> : 
					<RiCloseLine className="h-4 w-4 text-red-600" />;
			}
		} else if (field === 'email') {
			if (emailValidation.isChecking) {
				return <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>;
			}
			if (userData.email !== initialUserData?.email && userData.email.length > 0 && !errors.email) {
				return emailValidation.isAvailable ? 
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

	return (<>
		<Toaster />
		<div id="36" className="flex justify-center">
			<Dialog open={isOpen} onOpenChange={setIsOpen}>
				<Tooltip side="left" content="User Settings" triggerAsChild={true}>
					<DialogTrigger asChild>
						<Button className="p-2" variant="primary"><RiUserSettingsFill /></Button>
					</DialogTrigger>
				</Tooltip>
				<DialogContent className="sm:max-w-lg">
					<DialogHeader>
						<DialogTitle>User Settings</DialogTitle>
						<DialogDescription className="mt-1 text-sm leading-6">
							<form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
								<div>
									<label className="block text-sm font-medium mb-1">
										Username:
									</label>
									<div className="relative">
										<Input
											value={userData.username}
											onChange={(e) => handleFieldChange('username', e.target.value)}
											hasError={!!getUsernameError()}
											className="pr-10"
										/>
										<div className="absolute right-3 top-1/2 transform -translate-y-1/2">
											{renderValidationIcon('username')}
										</div>
									</div>
									{getUsernameError() && (
										<span className="text-xs text-red-500 mt-1 block">{getUsernameError()}</span>
									)}
								</div>
								<div>
									<label className="block text-sm font-medium mb-1">
										First Name:
									</label>
									<Input
										value={userData.firstName}
										onChange={(e) => handleFieldChange('firstName', e.target.value)}
										hasError={!!errors.firstName}
									/>
									{errors.firstName && (
										<span className="text-xs text-red-500 mt-1 block">{errors.firstName}</span>
									)}
								</div>
								<div>
									<label className="block text-sm font-medium mb-1">
										Last Name:
									</label>
									<Input
										value={userData.lastName}
										onChange={(e) => handleFieldChange('lastName', e.target.value)}
										hasError={!!errors.lastName}
									/>
									{errors.lastName && (
										<span className="text-xs text-red-500 mt-1 block">{errors.lastName}</span>
									)}
								</div>
								<div>
									<label className="block text-sm font-medium mb-1">
										Email:
									</label>
									<div className="relative">
										<Input
											type="email"
											value={userData.email}
											onChange={(e) => handleFieldChange('email', e.target.value)}
											hasError={!!getEmailError()}
											className="pr-10"
										/>
										<div className="absolute right-3 top-1/2 transform -translate-y-1/2">
											{renderValidationIcon('email')}
										</div>
									</div>
									{getEmailError() && (
										<span className="text-xs text-red-500 mt-1 block">{getEmailError()}</span>
									)}
								</div>
								<div className="space-y-4 border-t pt-4 mt-4">
									<h4 className="font-medium">Change Password</h4>
									<div>
										<label className="block text-sm font-medium mb-1">
											Current Password:
										</label>
										<Input
											type="password"
											value={oldPassword}
											onChange={(e) => {
												setOldPassword(e.target.value);
												clearFieldError('oldPassword');
											}}
											hasError={!!errors.oldPassword}
										/>
										{errors.oldPassword && (
											<span className="text-xs text-red-500 mt-1 block">{errors.oldPassword}</span>
										)}
									</div>
									<div>
										<label className="block text-sm font-medium mb-1">
											New Password:
										</label>
										<Input
											type="password"
											value={newPassword}
											onChange={(e) => {
												setNewPassword(e.target.value);
												clearFieldError('newPassword');
											}}
											hasError={!!errors.newPassword}
										/>
										{errors.newPassword && (
											<span className="text-xs text-red-500 mt-1 block">{errors.newPassword}</span>
										)}
									</div>
								</div>

								{!isSuperAdmin && (
								<div className="space-y-4 border-t pt-4 mt-4">
									<div className="flex items-center justify-between">
										<Button
											variant="destructive"
											onClick={() => setIsDeleteDialogOpen(true)}
											className="flex items-center gap-2"
										>
											<RiDeleteBin2Line />
											Delete Account
										</Button>
										</div>
									</div>
								)}
							</form>
						</DialogDescription>
					</DialogHeader>
					<DialogFooter className="mt-6">
						<DialogClose asChild>
							<Button
								className="mt-2 w-full sm:mt-0 sm:w-fit"
								variant="secondary"
								onClick={handleClose}
							>
								Cancel
							</Button>
						</DialogClose>
						<Button 
							className="w-full sm:w-fit" 
							onClick={handleUpdateProfile}
							disabled={loading || isSaveDisabled}
						>
							{loading ? "Updating..." : "Update Profile"}
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			<Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
				<DialogContent className="sm:max-w-lg">
					<DialogHeader>
						<DialogTitle className="text-red-600">Delete Account</DialogTitle>
						<DialogDescription className="mt-1 text-sm leading-6">
							<p className="mb-4">This action is irreversible. All your data and files will be deleted.</p>
							<p className="mb-4">To confirm, type your username: <strong>{userData.username}</strong></p>
							<Input
								value={deleteConfirmation}
								onChange={(e) => setDeleteConfirmation(e.target.value)}
								placeholder="Type your username"
							/>
						</DialogDescription>
					</DialogHeader>
					<DialogFooter className="mt-6">
						<Button
							variant="secondary"
							onClick={() => setIsDeleteDialogOpen(false)}
						>
							Cancel
						</Button>
						<Button 
							variant="destructive"
							onClick={handleDeleteAccount}
							disabled={loading || deleteConfirmation !== userData.username}
						>
							{loading ? "Deleting..." : "Delete Account Permanently"}
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>
		</div>
	</>
	)
};