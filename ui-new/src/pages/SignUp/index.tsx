import { useReducer, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Input } from "../../components/Input";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { authServiceInstance as authService } from "../../lib/services";
import { useNotificationService } from '../../services/notificationService';
import { useValidation } from '../../hooks/useValidation';
import useAuth from "../../hooks/userAuth";

const initialForm = {
	username: "",
	firstName: "",
	lastName: "",
	email: "",
	password: "",
	confirmPassword: "",
};

const reducer = (state, action) => {
	if (action.type === 'UPDATE_FIELD') {
		return { ...state, [action.field]: action.value };
	}
	if (action.type === 'RESET') return initialForm;
	return state;
};

const SignUp = () => {
	const [formData, dispatch] = useReducer(reducer, initialForm);
	const navigate = useNavigate();
	const notifications = useNotificationService();
	const { register } = useAuth();
	const [loading, setLoading] = useState(false);
	const { errors, validateRegistrationForm, clearFieldError } = useValidation();

	const change = (field: string, value: string) => {
		dispatch({ type: 'UPDATE_FIELD', field, value });
		
		clearFieldError(field);
		
		if ((field === 'confirmPassword' || field === 'password') && formData.password && formData.confirmPassword) {
			const pwd = field === 'password' ? value : formData.password;
			const confirmPwd = field === 'confirmPassword' ? value : formData.confirmPassword;
			
			if (confirmPwd && pwd !== confirmPwd) {
				setTimeout(() => notifications.error('Passwords do not match'), 300);
			}
		}
	};

	const submt = async (e: React.FormEvent) => {
		e.preventDefault();

		if (!validateRegistrationForm(formData)) {
			notifications.error("Please check the form fields");
			return;
		}

		setLoading(true);
		try {
			const success = await register(formData);
			
			if (success) {
				notifications.success("Registration successful");
				navigate("/");
			}
		} catch (err) {
			console.log('Registration error:', err);
			notifications.error("Registration failed. Please try again.");
		}
		setLoading(false);
	};

	const canSubmit = Object.values(formData).every(val => val.trim()) && 
		Object.values(errors).every(err => !err);

	return (
		<main className="px-4">
			<div className="max-w-lg mx-auto flex flex-col justify-center h-screen gap-10">
				<Card>
					<div className="flex items-center justify-between mb-4">
						<h2 className="text-2xl font-bold">Sign Up</h2>
						<Button variant="ghost" asChild className="w-1/6">
							<Link to="/login">Sign In</Link>
						</Button>
					</div>
					<form onSubmit={submt} className="space-y-4">
						<div>
							<label className="block">
								<span>Username</span>
								<Input
									value={formData.username}
									onChange={(e) => change('username', e.target.value)}
									hasError={!!errors.username}
									placeholder="Your username"
								/>
								{errors.username && <span className="text-xs text-red-500 block mt-1">{errors.username}</span>}
							</label>
						</div>
						
						<div>
							<label className="block">
								<span>Email</span>
								<Input
									type="email"
									value={formData.email}
									onChange={(e) => change('email', e.target.value)}
									hasError={!!errors.email}
									placeholder="your@email.com"
								/>
								{errors.email && <span className="text-xs text-red-500 block mt-1">{errors.email}</span>}
							</label>
						</div>

						<div className="grid grid-cols-2 gap-4">
							<div>
								<label className="block">
									<span>First Name</span>
									<Input
										value={formData.firstName}
										onChange={(e) => change('firstName', e.target.value)}
										hasError={!!errors.firstName}
										placeholder="First name"
									/>
									{errors.firstName && <span className="text-xs text-red-500 block mt-1">{errors.firstName}</span>}
								</label>
							</div>
							<div>
								<label className="block">
									<span>Last Name</span>
									<Input
										value={formData.lastName}
										onChange={(e) => change('lastName', e.target.value)}
										hasError={!!errors.lastName}
										placeholder="Last name"
									/>
									{errors.lastName && <span className="text-xs text-red-500 block mt-1">{errors.lastName}</span>}
								</label>
							</div>
						</div>

						<div>
							<label className="block">
								<span>Password</span>
								<Input
									type="password"
									value={formData.password}
									onChange={(e) => change('password', e.target.value)}
									hasError={!!errors.password}
								/>
								{errors.password && <span className="text-xs text-red-500 block mt-1">{errors.password}</span>}
							</label>
						</div>

						<label className="block">
							<span>Confirm Password</span>
							<Input
								type="password"
								value={formData.confirmPassword}
								onChange={(e) => change('confirmPassword', e.target.value)}
								hasError={!!errors.confirmPassword}
								placeholder="Repeat password"
							/>
							{errors.confirmPassword && <span className="text-red-500 text-xs">{errors.confirmPassword}</span>}
						</label>

						<Button
							type="submit"
							disabled={!canSubmit || loading}
							variant="primary"
							isLoading={loading}
							className="w-full mt-6"
						>
							{loading ? 'Signing up...' : 'Sign Up'}
						</Button>
					</form>
				</Card>
			</div>
		</main>
	);
};

export default SignUp;